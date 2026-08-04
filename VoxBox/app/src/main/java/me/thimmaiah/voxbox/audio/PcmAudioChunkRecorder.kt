package me.thimmaiah.voxbox.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RecordedAudioChunk(
    val id: String,
    val offsetMs: Long,
    val durationMs: Long,
    val wavBytes: ByteArray,
)

class PcmAudioChunkRecorder(
    private val scope: CoroutineScope,
    private val chunkDurationMs: Long = 20_000,
    private val sampleRate: Int = 16_000,
) {
    private var recorder: AudioRecord? = null
    private var job: Job? = null
    @Volatile private var requestedStop = false

    val isRecording: Boolean
        get() = job?.isActive == true

    @SuppressLint("MissingPermission")
    fun start(
        onChunk: (RecordedAudioChunk) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (isRecording) return
        requestedStop = false
        val minimumBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minimumBuffer <= 0) {
            onError("This device could not allocate a microphone buffer.")
            return
        }
        val activeRecorder = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(max(minimumBuffer * 2, 8_192))
                .build()
        } catch (error: Exception) {
            onError(error.message ?: "The microphone could not be opened.")
            return
        }
        if (activeRecorder.state != AudioRecord.STATE_INITIALIZED) {
            activeRecorder.release()
            onError("The microphone recorder could not be initialized.")
            return
        }
        recorder = activeRecorder
        job = scope.launch(Dispatchers.IO) {
            val noiseSuppressor: NoiseSuppressor? = runCatching {
                if (NoiseSuppressor.isAvailable()) {
                    NoiseSuppressor.create(activeRecorder.audioSessionId)
                } else {
                    null
                }
            }.getOrNull()
            val gainControl: AutomaticGainControl? = runCatching {
                if (AutomaticGainControl.isAvailable()) {
                    AutomaticGainControl.create(activeRecorder.audioSessionId)
                } else {
                    null
                }
            }.getOrNull()
            try {
                activeRecorder.startRecording()
                captureLoop(activeRecorder, onChunk)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!requestedStop) onError(error.message ?: "Audio capture stopped unexpectedly.")
            } finally {
                runCatching { if (activeRecorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) activeRecorder.stop() }
                noiseSuppressor?.release()
                gainControl?.release()
                activeRecorder.release()
                if (recorder === activeRecorder) recorder = null
            }
        }
    }

    fun stop() {
        cancelImmediately()
    }

    /**
     * Stops the microphone read, lets the capture loop emit its final useful partial WAV, and
     * returns only after the callback has received that chunk.
     */
    suspend fun stopAndDrain() {
        requestedStop = true
        val activeJob = job
        runCatching { recorder?.stop() }
        activeJob?.join()
        if (job === activeJob) job = null
    }

    fun cancelImmediately() {
        requestedStop = true
        runCatching { recorder?.stop() }
        job?.cancel()
        job = null
    }

    private fun captureLoop(
        activeRecorder: AudioRecord,
        onChunk: (RecordedAudioChunk) -> Unit,
    ) {
        val bytesPerSecond = sampleRate * 2
        val targetBytes = (bytesPerSecond * chunkDurationMs / 1_000).toInt()
        val minimumUsefulBytes = bytesPerSecond / 2
        val readBuffer = ByteArray(8_192)
        val accumulator = PcmChunkAccumulator(
            targetBytes = targetBytes,
            minimumUsefulBytes = minimumUsefulBytes,
            sampleRate = sampleRate,
        )
        while (job?.isActive == true && !requestedStop) {
            val count = activeRecorder.read(readBuffer, 0, readBuffer.size, AudioRecord.READ_BLOCKING)
            if (count < 0) {
                if (requestedStop) break
                throw IllegalStateException("Microphone read failed ($count).")
            }
            if (count == 0) continue
            accumulator.append(readBuffer, count = count).forEach(onChunk)
        }
        accumulator.finish()?.let(onChunk)
    }
}

internal class PcmChunkAccumulator(
    private val targetBytes: Int,
    private val minimumUsefulBytes: Int,
    private val sampleRate: Int,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    private val pending = ByteArrayOutputStream(targetBytes)
    private var emittedDurationMs = 0L
    private var finished = false

    init {
        require(targetBytes > 0 && targetBytes % 2 == 0)
        require(minimumUsefulBytes in 2..targetBytes && minimumUsefulBytes % 2 == 0)
        require(sampleRate > 0)
    }

    fun append(bytes: ByteArray, offset: Int = 0, count: Int = bytes.size - offset): List<RecordedAudioChunk> {
        check(!finished) { "Cannot append PCM after the accumulator is finished." }
        require(offset >= 0 && count >= 0 && offset + count <= bytes.size)
        val chunks = mutableListOf<RecordedAudioChunk>()
        var sourceOffset = offset
        val sourceEnd = offset + count
        while (sourceOffset < sourceEnd) {
            val writeCount = minOf(sourceEnd - sourceOffset, targetBytes - pending.size())
            pending.write(bytes, sourceOffset, writeCount)
            sourceOffset += writeCount
            if (pending.size() == targetBytes) chunks += takeChunk()
        }
        return chunks
    }

    fun finish(): RecordedAudioChunk? {
        if (finished) return null
        finished = true
        return if (pending.size() >= minimumUsefulBytes) takeChunk() else null
    }

    private fun takeChunk(): RecordedAudioChunk {
        val pcm = pending.toByteArray()
        pending.reset()
        val duration = pcmDurationMs(pcm.size, sampleRate)
        return RecordedAudioChunk(
            id = idFactory(),
            offsetMs = emittedDurationMs,
            durationMs = duration,
            wavBytes = encodePcm16MonoWav(pcm, sampleRate),
        ).also { emittedDurationMs += duration }
    }
}

internal fun pcmDurationMs(byteCount: Int, sampleRate: Int): Long =
    byteCount.toLong() * 1_000 / (sampleRate * 2L)

internal fun encodePcm16MonoWav(pcmBytes: ByteArray, sampleRate: Int = 16_000): ByteArray {
    require(sampleRate > 0)
    require(pcmBytes.size % 2 == 0) { "PCM16 data must contain whole samples." }
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(36 + pcmBytes.size)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16)
    header.putShort(1)
    header.putShort(1)
    header.putInt(sampleRate)
    header.putInt(sampleRate * 2)
    header.putShort(2)
    header.putShort(16)
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(pcmBytes.size)
    return header.array() + pcmBytes
}

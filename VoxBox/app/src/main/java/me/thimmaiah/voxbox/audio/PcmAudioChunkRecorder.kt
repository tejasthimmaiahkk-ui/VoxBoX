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
    /**
     * How much of the chunk rose clearly above the running noise floor. Near-silent chunks (a
     * lecturer writing on the board, a pause) are not worth a transcription call.
     */
    val voicedMs: Long = durationMs,
)

class PcmAudioChunkRecorder(
    private val scope: CoroutineScope,
    /** Hard ceiling: a lecturer who never pauses still gets a chunk this often. */
    private val chunkDurationMs: Long = 28_000,
    /**
     * After this much audio the recorder starts looking for a pause to cut at, so a fast or slow
     * speaker is split between clauses instead of at a fixed 20-second mark mid-word.
     */
    private val softChunkDurationMs: Long = 16_000,
    private val pauseMs: Long = 360,
    /**
     * How much of the previous chunk each chunk repeats. Two seconds comfortably contains a
     * spoken clause, which is the unit that was being lost at the seam.
     */
    private val overlapMs: Long = 2_000,
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
            overlapBytes = (bytesPerSecond * overlapMs / 1_000).toInt(),
            softTargetBytes = evenBytes(bytesPerSecond * softChunkDurationMs / 1_000),
            pauseBytes = evenBytes(bytesPerSecond * pauseMs / 1_000),
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

    private fun evenBytes(value: Long): Int = (value.toInt() / 2) * 2
}

/**
 * Slices the continuous microphone stream into overlapping chunks.
 *
 * The audio itself was never lost — one AudioRecord, read in a loop — but each chunk was
 * transcribed on its own, so a sentence spanning a boundary was cut in half and the model, seeing
 * a fragment at the edge of a clip, dropped it. In a real lecture that removed whole clauses.
 *
 * Each chunk therefore repeats the last [overlapBytes] of the previous one. The seam is covered
 * twice, so no word is only ever seen as a fragment, and the duplicate text is removed afterwards
 * by [trimRepeatedPrefix]. Offsets stay true to the recording clock: a chunk's offset is where its
 * *new* audio begins, not where its overlap does.
 *
 * Boundaries follow the speaker. Every 20 ms frame is compared against an adaptive noise floor
 * (it falls quickly to quiet and rises slowly under steady fan or crowd noise); once a chunk
 * passes [softTargetBytes] it is cut at the first [pauseBytes] run of non-speech frames, and
 * [targetBytes] is the hard ceiling for a lecturer who never stops. The same classification
 * gives each chunk its [RecordedAudioChunk.voicedMs].
 */
internal class PcmChunkAccumulator(
    private val targetBytes: Int,
    private val minimumUsefulBytes: Int,
    private val sampleRate: Int,
    private val overlapBytes: Int = 0,
    private val softTargetBytes: Int = targetBytes,
    private val pauseBytes: Int = 0,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    private val pending = ByteArrayOutputStream(targetBytes)
    private var emittedDurationMs = 0L
    private var carriedBytes = 0
    private var finished = false

    private val frameBytes = maxOf(2, sampleRate * 2 / 50 / 2 * 2)
    private val frame = ByteArray(frameBytes)
    private var frameFill = 0
    private var noiseFloor = -1.0
    private var voicedBytes = 0
    private var quietRunBytes = 0

    init {
        require(targetBytes > 0 && targetBytes % 2 == 0)
        require(minimumUsefulBytes in 2..targetBytes && minimumUsefulBytes % 2 == 0)
        require(sampleRate > 0)
        require(overlapBytes >= 0 && overlapBytes % 2 == 0)
        require(overlapBytes < targetBytes) { "Overlap must leave room for new audio." }
        require(softTargetBytes in 2..targetBytes)
        require(pauseBytes >= 0)
    }

    fun append(bytes: ByteArray, offset: Int = 0, count: Int = bytes.size - offset): List<RecordedAudioChunk> {
        check(!finished) { "Cannot append PCM after the accumulator is finished." }
        require(offset >= 0 && count >= 0 && offset + count <= bytes.size)
        val chunks = mutableListOf<RecordedAudioChunk>()
        var sourceOffset = offset
        val sourceEnd = offset + count
        while (sourceOffset < sourceEnd) {
            val writeCount = minOf(
                sourceEnd - sourceOffset,
                targetBytes - pending.size(),
                frameBytes - frameFill,
            )
            pending.write(bytes, sourceOffset, writeCount)
            System.arraycopy(bytes, sourceOffset, frame, frameFill, writeCount)
            frameFill += writeCount
            sourceOffset += writeCount
            if (frameFill == frameBytes) {
                classifyFrame()
                frameFill = 0
            }
            val atPause = pauseBytes > 0 && pending.size() >= softTargetBytes && quietRunBytes >= pauseBytes
            if (pending.size() == targetBytes || atPause) chunks += takeChunk()
        }
        return chunks
    }

    fun finish(): RecordedAudioChunk? {
        if (finished) return null
        finished = true
        // Only the carried overlap is left: that audio has already been transcribed once, so
        // emitting it again would duplicate the tail of the lecture rather than extend it.
        val newBytes = pending.size() - carriedBytes
        return if (newBytes >= minimumUsefulBytes) takeChunk() else null
    }

    private fun classifyFrame() {
        val rms = frameRms(frame, frameBytes)
        noiseFloor = when {
            noiseFloor < 0 -> rms
            rms < noiseFloor -> noiseFloor * 0.7 + rms * 0.3
            else -> noiseFloor * 0.995 + rms * 0.005
        }
        if (rms > maxOf(noiseFloor * SPEECH_OVER_FLOOR, MIN_SPEECH_RMS)) {
            voicedBytes += frameBytes
            quietRunBytes = 0
        } else {
            quietRunBytes += frameBytes
        }
    }

    private fun takeChunk(): RecordedAudioChunk {
        val pcm = pending.toByteArray()
        pending.reset()
        val carryFrom = maxOf(0, pcm.size - overlapBytes)
        val carried = pcm.size - carryFrom
        if (overlapBytes > 0 && carried > 0) pending.write(pcm, carryFrom, carried)

        val duration = pcmDurationMs(pcm.size, sampleRate)
        val overlapMs = pcmDurationMs(carriedBytes, sampleRate)
        val chunk = RecordedAudioChunk(
            id = idFactory(),
            // The overlap replays audio the previous chunk already covered, so this chunk starts
            // that much earlier on the recording clock.
            offsetMs = (emittedDurationMs - overlapMs).coerceAtLeast(0),
            durationMs = duration,
            wavBytes = encodePcm16MonoWav(pcm, sampleRate),
            voicedMs = pcmDurationMs(minOf(voicedBytes, pcm.size), sampleRate),
        )
        emittedDurationMs += duration - overlapMs
        carriedBytes = if (overlapBytes > 0) carried else 0
        voicedBytes = 0
        quietRunBytes = 0
        return chunk
    }

    private companion object {
        /** About +8 dB over the floor: a voice carries over a fan, steady hum does not. */
        const val SPEECH_OVER_FLOOR = 2.5
        /** Below this a frame is silence on any phone microphone, whatever the floor says. */
        const val MIN_SPEECH_RMS = 120.0
    }
}

internal fun frameRms(pcm: ByteArray, length: Int): Double {
    var sum = 0.0
    var samples = 0
    var i = 0
    while (i + 1 < length) {
        val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort().toDouble()
        sum += sample * sample
        samples++
        i += 2
    }
    return if (samples == 0) 0.0 else kotlin.math.sqrt(sum / samples)
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

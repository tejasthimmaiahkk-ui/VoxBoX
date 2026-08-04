package me.thimmaiah.voxbox.audio

enum class TranscriptionSource {
    OPENAI,
    MOCK,
}

data class TranscribedSegment(
    val id: String,
    val speakerId: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

data class AudioTranscription(
    val sessionId: String,
    val chunkId: String,
    val text: String,
    val durationMs: Long,
    val segments: List<TranscribedSegment>,
    val source: TranscriptionSource,
)

interface AudioTranscriptionClient {
    suspend fun transcribe(
        sessionId: String,
        chunkId: String,
        offsetMs: Long,
        wavBytes: ByteArray,
        language: String = "",
    ): AudioTranscription
}

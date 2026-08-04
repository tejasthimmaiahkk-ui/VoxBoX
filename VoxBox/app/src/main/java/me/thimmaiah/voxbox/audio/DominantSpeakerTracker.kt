package me.thimmaiah.voxbox.audio

enum class SpeakerFocusStatus {
    LEARNING,
    FOCUSED,
    AMBIGUOUS,
    MANUAL,
    UNAVAILABLE,
}

data class SpeakerFocusSnapshot(
    val status: SpeakerFocusStatus,
    val selectedSpeakerId: String?,
    val observedVoicedMs: Long,
    val leadingShare: Double,
    val reason: String,
)

/**
 * Chooses a dominant label only inside one transcription response. Diarization labels such as
 * A/B are not stable identities across separately uploaded chunks, so no duration is accumulated
 * between requests and a manual choice expires when the next chunk arrives.
 */
class PerChunkSpeakerTracker(
    private val minimumLeadingShare: Double = 0.58,
    private val minimumMargin: Double = 0.15,
) {
    private var durationBySpeaker: Map<String, Long> = emptyMap()
    private var manualSpeakerId: String? = null

    fun evaluate(chunkId: String, segments: List<TranscribedSegment>): SpeakerFocusSnapshot {
        require(chunkId.isNotBlank()) { "Chunk id cannot be blank." }
        manualSpeakerId = null
        val seen = mutableSetOf<String>()
        val durations = mutableMapOf<String, Long>()
        segments.forEach { segment ->
            val speaker = segment.speakerId.trim()
            if (speaker.isBlank() || !seen.add(segment.id)) return@forEach
            val duration = (segment.endMs - segment.startMs).coerceAtLeast(0)
            durations[speaker] = durations.getOrDefault(speaker, 0L) + duration
        }
        durationBySpeaker = durations
        return snapshot()
    }

    fun selectManually(speakerId: String?): SpeakerFocusSnapshot {
        val normalized = speakerId?.trim()?.takeIf(String::isNotBlank)
        manualSpeakerId = normalized?.takeIf(durationBySpeaker::containsKey)
        return snapshot()
    }

    fun currentSpeakerIds(): List<String> = durationBySpeaker.keys.sorted()

    fun snapshot(): SpeakerFocusSnapshot {
        val total = durationBySpeaker.values.sum()
        manualSpeakerId?.let { manual ->
            return SpeakerFocusSnapshot(
                status = SpeakerFocusStatus.MANUAL,
                selectedSpeakerId = manual,
                observedVoicedMs = total,
                leadingShare = durationBySpeaker.getOrDefault(manual, 0L).toDouble() / total.coerceAtLeast(1),
                reason = "Manual choice applies only to the latest audio chunk; diarization labels reset on the next chunk.",
            )
        }
        if (durationBySpeaker.isEmpty() || total == 0L) {
            return SpeakerFocusSnapshot(
                status = SpeakerFocusStatus.UNAVAILABLE,
                selectedSpeakerId = null,
                observedVoicedMs = 0,
                leadingShare = 0.0,
                reason = "This audio chunk had no usable diarized labels; no speaker focus was claimed.",
            )
        }
        val ranked = durationBySpeaker.entries.sortedByDescending(Map.Entry<String, Long>::value)
        val leader = ranked.first()
        val leadingShare = leader.value.toDouble() / total
        val runnerUpShare = ranked.getOrNull(1)?.value?.toDouble()?.div(total) ?: 0.0
        val focused = leadingShare >= minimumLeadingShare && leadingShare - runnerUpShare >= minimumMargin
        return SpeakerFocusSnapshot(
            status = if (focused) SpeakerFocusStatus.FOCUSED else SpeakerFocusStatus.AMBIGUOUS,
            selectedSpeakerId = leader.key.takeIf { focused },
            observedVoicedMs = total,
            leadingShare = leadingShare,
            reason = if (focused) {
                "Dominant label selected inside this chunk only; it is not treated as the same person in later chunks."
            } else {
                "Speaker activity is too close in this chunk; review or mark its speaker manually."
            },
        )
    }
}

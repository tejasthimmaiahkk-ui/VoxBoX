package me.thimmaiah.voxbox.session

sealed interface GeneratedMarkdownUpdateResult {
    val revision: Long

    data class Apply(override val revision: Long) : GeneratedMarkdownUpdateResult
    data class Duplicate(override val revision: Long) : GeneratedMarkdownUpdateResult
    data class Conflict(
        override val revision: Long,
        val reason: String,
    ) : GeneratedMarkdownUpdateResult

    data class Invalid(
        override val revision: Long,
        val reason: String,
    ) : GeneratedMarkdownUpdateResult

    data class Missing(
        override val revision: Long = 0,
        val reason: String,
    ) : GeneratedMarkdownUpdateResult
}

object GeneratedMarkdownRevision {
    fun decide(
        currentRevision: Long,
        lastPatchId: String?,
        currentMarkdown: String,
        patchId: String,
        expectedRevision: Long,
        newMarkdown: String,
    ): GeneratedMarkdownUpdateResult {
        if (currentRevision < 0) {
            return GeneratedMarkdownUpdateResult.Invalid(
                revision = currentRevision,
                reason = "Stored revision cannot be negative.",
            )
        }
        if (patchId.isBlank()) {
            return GeneratedMarkdownUpdateResult.Invalid(
                revision = currentRevision,
                reason = "Patch id cannot be blank.",
            )
        }
        if (newMarkdown.isBlank()) {
            return GeneratedMarkdownUpdateResult.Invalid(
                revision = currentRevision,
                reason = "A generated note update cannot erase the note with blank Markdown.",
            )
        }
        if (lastPatchId == patchId) {
            return if (currentMarkdown == newMarkdown) {
                GeneratedMarkdownUpdateResult.Duplicate(currentRevision)
            } else {
                GeneratedMarkdownUpdateResult.Conflict(
                    revision = currentRevision,
                    reason = "The patch id was already used for different Markdown.",
                )
            }
        }
        if (expectedRevision != currentRevision) {
            return GeneratedMarkdownUpdateResult.Conflict(
                revision = currentRevision,
                reason = "Expected revision $expectedRevision but the session is at $currentRevision.",
            )
        }
        if (currentRevision == Long.MAX_VALUE) {
            return GeneratedMarkdownUpdateResult.Invalid(
                revision = currentRevision,
                reason = "The session revision limit was reached.",
            )
        }
        return GeneratedMarkdownUpdateResult.Apply(currentRevision + 1)
    }
}

package me.thimmaiah.voxbox.session

/** What kind of problem the end-of-session check found. */
enum class VerificationFindingKind {
    FORMULA,
    CONCEPT,
    UNITS,
    TERMINOLOGY,
    OTHER,
}

/**
 * One review annotation from the end-of-session check.
 *
 * There is deliberately no replacement-note field. A finished note is captured evidence, so this
 * pass can only annotate it; accepting a suggestion stays a human decision.
 */
data class VerificationFinding(
    val claim: String,
    val issue: String,
    val suggestion: String,
    val kind: VerificationFindingKind,
    val severity: String,
    val confidence: Double,
)

data class NoteVerification(
    val sessionId: String,
    val requestId: String,
    val findings: List<VerificationFinding>,
    val checkedFormulas: List<String>,
    val checkedConcepts: List<String>,
    val warnings: List<String>,
    val source: NoteRefinementSource,
) {
    /** Findings the user should look at first. */
    val warningCount: Int
        get() = findings.count { it.severity == "warning" }
}

interface NoteVerificationClient {
    suspend fun verify(
        sessionId: String,
        requestId: String,
        noteMarkdown: String,
        subjectHint: String,
    ): NoteVerification
}

/** Renders findings as a labelled review section appended to the note. */
internal fun appendVerificationFindings(markdown: String, verification: NoteVerification): String {
    if (verification.findings.isEmpty()) return markdown
    val additions = buildList {
        add("### End-of-session check")
        add(
            "> Automated review of formulas, units and concepts. These are suggestions for you to " +
                "confirm; nothing in the note above was changed.",
        )
        verification.findings.forEach { finding ->
            val label = finding.kind.name.lowercase().replaceFirstChar { it.titlecase() }
            add("- **$label · ${finding.severity}:** ${finding.claim.trim()}")
            add("  - Issue: ${finding.issue.trim()}")
            add("  - Suggested: ${finding.suggestion.trim()}")
        }
    }
    return listOf(markdown.trimEnd(), additions.joinToString("\n"))
        .filter(String::isNotBlank)
        .joinToString("\n\n")
}

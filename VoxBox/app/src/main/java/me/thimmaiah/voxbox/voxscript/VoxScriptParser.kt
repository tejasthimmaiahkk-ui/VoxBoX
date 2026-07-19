package me.thimmaiah.voxbox.voxscript

import java.util.Locale

sealed interface VoxScriptResult {
    val sourceText: String

    data class PlainDictation(
        override val sourceText: String,
    ) : VoxScriptResult

    data class Heading(
        val text: String,
        override val sourceText: String,
    ) : VoxScriptResult

    data class BulletPoint(
        val text: String,
        override val sourceText: String,
    ) : VoxScriptResult

    data class PieChart(
        val percentage: Int,
        val color: String,
        val label: String,
        override val sourceText: String,
    ) : VoxScriptResult

    data class InvalidCommand(
        val reason: String,
        override val sourceText: String,
    ) : VoxScriptResult
}

class VoxScriptParser(
    wakeWords: Set<String> = setOf("vox", "tejas", "note"),
) {
    private val normalizedWakeWords = wakeWords.map { it.lowercase(Locale.ROOT) }.toSet()
    private val supportedColors = setOf(
        "red", "orange", "yellow", "green", "blue", "purple", "pink", "black", "white",
    )

    fun parse(rawText: String): VoxScriptResult {
        val source = rawText.trim()
        if (source.isBlank()) return VoxScriptResult.PlainDictation(source)

        val normalized = source
            .lowercase(Locale.ROOT)
            .replace(Regex("[,.!?;:]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val firstWord = normalized.substringBefore(' ')
        if (firstWord !in normalizedWakeWords) return VoxScriptResult.PlainDictation(source)

        val command = normalized.removePrefix(firstWord).trim()
        if (command.isBlank()) {
            return VoxScriptResult.InvalidCommand("Say a command after the wake word.", source)
        }

        parseTextCommand(command, source)?.let { return it }
        if (command.contains("pie chart") || command.startsWith("pie")) {
            return parsePieChart(command, source)
        }

        return VoxScriptResult.InvalidCommand(
            reason = "This VoxScript command is not supported yet.",
            sourceText = source,
        )
    }

    private fun parseTextCommand(command: String, source: String): VoxScriptResult? {
        val heading = Regex("^(?:heading|title)\\s+(.+)$").matchEntire(command)
        if (heading != null) {
            return VoxScriptResult.Heading(heading.groupValues[1].trim(), source)
        }

        val bullet = Regex("^(?:bullet point|bullet)\\s+(.+)$").matchEntire(command)
        if (bullet != null) {
            return VoxScriptResult.BulletPoint(bullet.groupValues[1].trim(), source)
        }
        return null
    }

    private fun parsePieChart(command: String, source: String): VoxScriptResult {
        val percentage = Regex("(\\d{1,3})\\s*(?:%|percent)")
            .find(command)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: return VoxScriptResult.InvalidCommand("A pie chart needs a percentage.", source)

        if (percentage !in 0..100) {
            return VoxScriptResult.InvalidCommand("Pie-chart percentage must be between 0 and 100.", source)
        }

        val color = supportedColors.firstOrNull { colorName ->
            Regex("\\b${Regex.escape(colorName)}\\b").containsMatchIn(command)
        } ?: return VoxScriptResult.InvalidCommand("A pie chart needs a supported color.", source)

        val label = Regex("(?:label|tag)\\s+(.+)$")
            .find(command)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return VoxScriptResult.InvalidCommand("A pie chart needs a label or tag.", source)

        return VoxScriptResult.PieChart(
            percentage = percentage,
            color = color,
            label = label,
            sourceText = source,
        )
    }
}

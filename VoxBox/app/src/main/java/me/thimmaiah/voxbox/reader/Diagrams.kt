package me.thimmaiah.voxbox.reader

/**
 * Diagrams the note model writes as text instead of photographing.
 *
 * A process ("parent nucleus → daughter + alpha particle") or a simple curve (binding energy per
 * nucleon) is clearer drawn cleanly than as a crop of a hand-drawn board, and as text it costs no
 * upload, survives editing and renders in Obsidian too. Complex figures — a cell, a circuit — stay
 * as board photos. Only a small Mermaid subset is understood; anything else falls back to code.
 */
sealed interface DiagramSpec {
    data class Flow(val nodes: List<FlowNode>, val edges: List<FlowEdge>) : DiagramSpec
    data class Chart(
        val title: String,
        val xLabel: String,
        val yLabel: String,
        val categories: List<String>,
        val series: List<ChartSeries>,
    ) : DiagramSpec
}

data class FlowNode(val id: String, val label: String, val decision: Boolean = false)
data class FlowEdge(val from: String, val to: String, val label: String = "")
data class ChartSeries(val bar: Boolean, val values: List<Float>)

private val flowHeader = Regex("^(flowchart|graph)(\\s+(LR|RL|TD|TB|BT))?\\s*$", RegexOption.IGNORE_CASE)
private val arrow = Regex("\\s*(?:-->|---|==>|-\\.->)\\s*(?:\\|([^|]*)\\|\\s*)?")
// Every bracket is escaped, including closing ones. The JVM accepts a bare "}" or "]", so these
// passed every unit test, but Android's ICU engine rejects them and the class failed to load,
// crashing the reader the first time a live note contained a diagram.
private val nodeToken = Regex("^([A-Za-z0-9_]+)\\s*(\\[\\[(.*)\\]\\]|\\[(.*)\\]|\\(\\((.*)\\)\\)|\\((.*)\\)|\\{(.*)\\})?$")
private val quoted = Regex("\"([^\"]*)\"")
private val numberList = Regex("\\[([^\\]]*)\\]")

fun parseDiagram(source: String): DiagramSpec? {
    val lines = source.lines().map(String::trim).filter { it.isNotEmpty() && !it.startsWith("%%") }
    val head = lines.firstOrNull() ?: return null
    return when {
        flowHeader.matches(head) -> parseFlow(lines.drop(1))
        head.startsWith("xychart", ignoreCase = true) -> parseChart(lines.drop(1))
        else -> null
    }
}

private fun parseFlow(lines: List<String>): DiagramSpec.Flow? {
    val nodes = LinkedHashMap<String, FlowNode>()
    val edges = mutableListOf<FlowEdge>()
    fun node(token: String): String? {
        val match = nodeToken.matchEntire(token.trim().trimEnd(';')) ?: return null
        val id = match.groupValues[1]
        val label = (2..7).asSequence().map { match.groupValues.getOrElse(it + 1) { "" } }
            .firstOrNull { it.isNotBlank() }?.trim()?.removeSurrounding("\"")
        val existing = nodes[id]
        if (existing == null || label != null) {
            nodes[id] = FlowNode(id, label ?: existing?.label ?: id, decision = match.groupValues[7].isNotBlank())
        }
        return id
    }
    for (line in lines) {
        if (line.startsWith("style") || line.startsWith("classDef") || line.startsWith("class ") ||
            line.startsWith("subgraph") || line == "end"
        ) continue
        val parts = mutableListOf<String>()
        val labels = mutableListOf<String>()
        var last = 0
        arrow.findAll(line).forEach { found ->
            parts += line.substring(last, found.range.first)
            labels += found.groupValues[1].trim()
            last = found.range.last + 1
        }
        parts += line.substring(last)
        val ids = parts.map { node(it) ?: return null }
        for (i in 0 until ids.size - 1) edges += FlowEdge(ids[i], ids[i + 1], labels[i])
    }
    if (nodes.isEmpty() || nodes.size > MAX_FLOW_NODES) return null
    return DiagramSpec.Flow(nodes.values.toList(), edges)
}

private fun parseChart(lines: List<String>): DiagramSpec.Chart? {
    var title = ""
    var xLabel = ""
    var yLabel = ""
    var categories = emptyList<String>()
    val series = mutableListOf<ChartSeries>()
    for (line in lines) {
        val lower = line.lowercase()
        when {
            lower.startsWith("title") -> title = quoted.find(line)?.groupValues?.get(1) ?: line.drop(5).trim()
            lower.startsWith("x-axis") -> {
                xLabel = quoted.find(line)?.groupValues?.get(1).orEmpty()
                categories = numberList.find(line)?.groupValues?.get(1)
                    ?.split(',')?.map { it.trim().removeSurrounding("\"") }.orEmpty()
            }
            lower.startsWith("y-axis") -> yLabel = quoted.find(line)?.groupValues?.get(1).orEmpty()
            lower.startsWith("line") || lower.startsWith("bar") -> {
                val values = numberList.find(line)?.groupValues?.get(1)
                    ?.split(',')?.mapNotNull { it.trim().toFloatOrNull() }.orEmpty()
                if (values.isNotEmpty()) series += ChartSeries(bar = lower.startsWith("bar"), values = values)
            }
        }
    }
    if (series.isEmpty()) return null
    val count = series.maxOf { it.values.size }
    if (categories.size != count) categories = (1..count).map(Int::toString)
    return DiagramSpec.Chart(title, xLabel, yLabel, categories, series)
}

private const val MAX_FLOW_NODES = 16

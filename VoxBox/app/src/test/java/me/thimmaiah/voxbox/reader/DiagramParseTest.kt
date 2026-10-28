package me.thimmaiah.voxbox.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagramParseTest {
    @Test
    fun aMermaidFlowchartBecomesNodesAndLabelledEdges() {
        val blocks = parseMarkdownBlocks(
            """
            ## Alpha decay
            ```mermaid
            flowchart TD
              A[Parent nucleus: radium] --> B[Daughter nucleus: radon]
              A -->|emits| C((Alpha particle))
              B --> D{Stable?}
            ```
            """.trimIndent(),
        )
        val diagram = blocks.filterIsInstance<MdBlock.Diagram>().single().spec as DiagramSpec.Flow
        assertEquals(listOf("Parent nucleus: radium", "Daughter nucleus: radon", "Alpha particle", "Stable?"), diagram.nodes.map { it.label })
        assertEquals(FlowEdge("A", "C", "emits"), diagram.edges[1])
        assertTrue(diagram.nodes.last().decision)
    }

    @Test
    fun chainsOnOneLineProduceEveryEdge() {
        val flow = parseDiagram("graph LR\nA[Mass] --> B[Energy] --> C[Power]") as DiagramSpec.Flow
        assertEquals(2, flow.edges.size)
    }

    @Test
    fun anXyChartKeepsItsValuesAndLabels() {
        val chart = parseDiagram(
            """
            xychart-beta
              title "Binding energy per nucleon"
              x-axis "Mass number" [4, 16, 56, 120]
              y-axis "MeV"
              line [7.1, 8.0, 8.8, 8.5]
            """.trimIndent(),
        ) as DiagramSpec.Chart
        assertEquals("Binding energy per nucleon", chart.title)
        assertEquals(listOf("4", "16", "56", "120"), chart.categories)
        assertEquals(8.8f, chart.series.single().values[2])
    }

    @Test
    fun unsupportedMermaidFallsBackToCode() {
        assertNull(parseDiagram("sequenceDiagram\nAlice->>Bob: hi"))
        val blocks = parseMarkdownBlocks("```mermaid\nsequenceDiagram\nAlice->>Bob: hi\n```")
        assertTrue(blocks.single() is MdBlock.Code)
    }
}

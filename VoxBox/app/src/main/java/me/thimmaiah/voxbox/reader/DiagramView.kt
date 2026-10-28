package me.thimmaiah.voxbox.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbShape

/** Draws a [DiagramSpec] natively, so it reads the same offline and follows the app theme. */
@Composable
fun DiagramView(spec: DiagramSpec, bodyStyle: TextStyle, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(VbShape.media)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
    ) {
        when (spec) {
            is DiagramSpec.Flow -> FlowDiagram(spec, bodyStyle)
            is DiagramSpec.Chart -> ChartDiagram(spec, bodyStyle)
        }
    }
}

/**
 * Phones are narrow, so every flow is drawn top to bottom whatever direction the source asked for.
 * A step that simply leads to the next one gets an arrow; branches and loops are spelled out under
 * the step, which stays readable where a drawn spaghetti of edges would not.
 */
@Composable
private fun FlowDiagram(spec: DiagramSpec.Flow, bodyStyle: TextStyle) {
    val status = LocalVbStatus.current
    val labelOf = spec.nodes.associate { it.id to it.label }
    Column(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Flow diagram: " + spec.nodes.joinToString(", then ") { it.label } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        spec.nodes.forEachIndexed { index, node ->
            Text(
                text = node.label,
                style = bodyStyle.copy(fontSize = bodyStyle.fontSize * 0.95f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 300.dp)
                    .clip(VbShape.media)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = if (node.decision) 2.dp else 1.dp,
                        color = if (node.decision) MaterialTheme.colorScheme.primary else status.fg2,
                        shape = VbShape.media,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            val outgoing = spec.edges.filter { it.from == node.id }
            val next = spec.nodes.getOrNull(index + 1)?.id
            val straight = outgoing.singleOrNull()?.takeIf { it.to == next }
            when {
                straight != null -> Text(
                    text = if (straight.label.isBlank()) "↓" else "↓  ${straight.label}",
                    style = bodyStyle.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                outgoing.isNotEmpty() -> outgoing.forEach { edge ->
                    Text(
                        text = "→ " + (if (edge.label.isBlank()) "" else "${edge.label}: ") + labelOf[edge.to].orEmpty(),
                        style = bodyStyle.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                next != null -> Text("", Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ChartDiagram(spec: DiagramSpec.Chart, bodyStyle: TextStyle) {
    val status = LocalVbStatus.current
    val measurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = status.fg2
    val labelStyle = TextStyle(fontSize = 10.sp, color = status.fg2)
    if (spec.title.isNotBlank()) {
        Text(spec.title, style = bodyStyle, color = MaterialTheme.colorScheme.onSurface)
    }
    val all = spec.series.flatMap { it.values }
    val min = minOf(0f, all.min())
    val max = all.max().takeIf { it > min } ?: (min + 1f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 8.dp)
            .semantics {
                contentDescription = "Chart ${spec.title}: " +
                    spec.categories.zip(spec.series.first().values).joinToString { (x, y) -> "$x is $y" }
            },
    ) {
        val left = 40.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val width = size.width - left - 8.dp.toPx()
        val count = spec.categories.size
        fun x(i: Int) = left + if (count <= 1) width / 2 else width * i / (count - 1)
        fun y(v: Float) = bottom - (v - min) / (max - min) * (bottom - 8.dp.toPx())
        drawLine(axisColor, Offset(left, 0f), Offset(left, bottom), 1.dp.toPx())
        drawLine(axisColor, Offset(left, bottom), Offset(left + width, bottom), 1.dp.toPx())
        listOf(min, max).forEach { v ->
            drawText(measurer, trimNumber(v), Offset(0f, y(v) - 7.dp.toPx()), labelStyle)
        }
        spec.categories.forEachIndexed { i, label ->
            if (count <= 8 || i % (count / 6 + 1) == 0) {
                drawText(measurer, label.take(6), Offset(x(i) - 8.dp.toPx(), bottom + 4.dp.toPx()), labelStyle)
            }
        }
        spec.series.forEach { series ->
            if (series.bar) {
                val barWidth = (width / maxOf(count, 1)) * 0.6f
                series.values.forEachIndexed { i, v ->
                    drawRect(
                        lineColor.copy(alpha = 0.55f),
                        topLeft = Offset(x(i) - barWidth / 2, y(v)),
                        size = Size(barWidth, bottom - y(v)),
                    )
                }
            } else {
                val path = Path()
                series.values.forEachIndexed { i, v ->
                    if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
                    drawCircle(lineColor, 3.dp.toPx(), Offset(x(i), y(v)))
                }
                drawPath(path, lineColor, style = Stroke(2.dp.toPx()))
            }
        }
    }
    if (spec.xLabel.isNotBlank() || spec.yLabel.isNotBlank()) {
        Text(
            text = listOf(spec.yLabel, spec.xLabel).filter(String::isNotBlank).joinToString("  vs  "),
            style = labelStyle,
        )
    }
}

private fun trimNumber(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else "%.2f".format(value)

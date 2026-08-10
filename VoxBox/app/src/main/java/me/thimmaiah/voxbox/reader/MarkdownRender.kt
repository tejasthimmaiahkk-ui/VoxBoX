package me.thimmaiah.voxbox.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbMono
import me.thimmaiah.voxbox.ui.theme.VbShape

/**
 * Renders parsed Markdown.
 *
 * Replaces showing the source: a note full of `##` and `**` is harder to read than the transcript
 * it was made from, which defeats the point of writing it.
 */
@Composable
fun MarkdownBody(
    markdown: String,
    bodyStyle: TextStyle,
    query: String = "",
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
) {
    val blocks = parseMarkdownBlocks(markdown)
    Column(modifier) {
        blocks.forEach { block -> MarkdownBlock(block, bodyStyle, query, onEdit) }
    }
}

@Composable
private fun MarkdownBlock(
    block: MdBlock,
    bodyStyle: TextStyle,
    query: String,
    onEdit: (() -> Unit)?,
) {
    val status = LocalVbStatus.current
    val tapToEdit = if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier

    when (block) {
        is MdBlock.Heading -> {
            Spacer(Modifier.height(if (block.level <= 2) 18.dp else 12.dp))
            Text(
                text = annotate(block.spans, query, MaterialTheme.colorScheme.primary),
                style = when (block.level) {
                    1 -> MaterialTheme.typography.displaySmall
                    2 -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.titleMedium
                },
                color = MaterialTheme.colorScheme.onBackground,
                modifier = tapToEdit,
            )
            Spacer(Modifier.height(6.dp))
        }

        is MdBlock.Paragraph -> Text(
            text = annotate(block.spans, query, MaterialTheme.colorScheme.primary),
            style = bodyStyle,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = tapToEdit.padding(vertical = 5.dp),
        )

        is MdBlock.Bullet -> Row(
            modifier = Modifier.padding(
                start = (block.depth * 16).dp,
                top = 3.dp,
                bottom = 3.dp,
            ),
        ) {
            Box(
                Modifier
                    // Aligns the dot with the first text line rather than the block's top edge.
                    .padding(top = with(bodyStyle) { (fontSize.value * 0.62f).dp })
                    .size(5.dp)
                    .clip(VbShape.pill)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = annotate(block.spans, query, MaterialTheme.colorScheme.primary),
                style = bodyStyle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = tapToEdit,
            )
        }

        is MdBlock.Numbered -> Row(
            modifier = Modifier.padding(
                start = (block.depth * 16).dp,
                top = 3.dp,
                bottom = 3.dp,
            ),
        ) {
            Text(
                text = "${block.number}.",
                style = bodyStyle.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = annotate(block.spans, query, MaterialTheme.colorScheme.primary),
                style = bodyStyle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = tapToEdit,
            )
        }

        is MdBlock.Quote -> {
            // A callout carries meaning the app depends on: a "note" callout is where an accepted
            // AI suggestion is recorded, and it has to look unlike the lecturer's own words.
            val tone = when (block.callout?.lowercase()) {
                "warning", "caution" -> status.review
                "note", "info", "tip" -> MaterialTheme.colorScheme.primary
                else -> status.fg3
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(VbShape.media)
                    .background(tone.copy(alpha = 0.08f))
                    .padding(12.dp),
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(with(bodyStyle) { (lineHeight.value * 1.4f).dp })
                        .clip(VbShape.pill)
                        .background(tone),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    if (block.callout != null) {
                        Text(
                            text = block.callout.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = tone,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        text = annotate(block.spans, query, MaterialTheme.colorScheme.primary),
                        style = bodyStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Never wrapped: a wrapped equation is a wrong equation.
        is MdBlock.Code -> Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(VbShape.media)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState())
                .padding(14.dp),
        ) {
            Text(
                text = block.text,
                style = bodyStyle.copy(
                    fontFamily = VbMono,
                    fontSize = bodyStyle.fontSize * 0.92f,
                ),
                color = if (block.isMath) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                softWrap = false,
            )
        }

        MdBlock.Divider -> Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
                .height(1.dp)
                .background(status.line),
        )
    }
}

/** Turns styled runs into one annotated string, marking find matches as it goes. */
@Composable
private fun annotate(
    spans: List<MdSpan>,
    query: String,
    highlightColor: androidx.compose.ui.graphics.Color,
) = buildAnnotatedString {
    val status = LocalVbStatus.current
    spans.forEach { span ->
        val style = SpanStyle(
            fontWeight = if (span.bold) FontWeight.Bold else null,
            fontStyle = if (span.italic) FontStyle.Italic else null,
            fontFamily = if (span.code || span.math) VbMono else null,
            color = when {
                span.math -> highlightColor
                span.code -> status.fg2
                else -> androidx.compose.ui.graphics.Color.Unspecified
            },
            background = if (span.highlight) {
                highlightColor.copy(alpha = 0.22f)
            } else {
                androidx.compose.ui.graphics.Color.Unspecified
            },
        )
        withStyle(style) {
            if (query.isBlank()) {
                append(span.text)
                return@withStyle
            }
            var index = 0
            while (index < span.text.length) {
                val hit = span.text.indexOf(query, index, ignoreCase = true)
                if (hit < 0) {
                    append(span.text.substring(index))
                    break
                }
                append(span.text.substring(index, hit))
                withStyle(SpanStyle(background = highlightColor.copy(alpha = 0.26f))) {
                    append(span.text.substring(hit, hit + query.length))
                }
                index = hit + query.length
            }
        }
    }
}

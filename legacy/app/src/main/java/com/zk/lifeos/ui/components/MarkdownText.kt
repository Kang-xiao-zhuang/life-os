package com.zk.lifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.zk.lifeos.model.MdBlock
import com.zk.lifeos.model.Markdown
import com.zk.lifeos.ui.theme.Space

/**
 * Renders what [Markdown] parsed.
 *
 * The type sizes here are relative to whatever [style] is handed in, not absolute — a review rendered
 * inside a card and a note rendered in a row want the same *relationships*, not the same numbers.
 * Headings therefore step up from the body size instead of jumping to the screen-title size, which
 * would make a `#` inside a card compete with the card's own title.
 */
@Composable
fun MarkdownText(
    source: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    val blocks = remember(source) { Markdown.parse(source) }
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Space.tight),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = inline(block.text),
                    style = style.copy(
                        fontSize = style.fontSize * when (block.level) {
                            1 -> 1.35f
                            2 -> 1.18f
                            else -> 1.06f
                        },
                        lineHeight = style.lineHeight * when (block.level) {
                            1 -> 1.35f
                            2 -> 1.18f
                            else -> 1.06f
                        },
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = color,
                )

                is MdBlock.Paragraph -> Text(text = inline(block.text), style = style, color = color)

                is MdBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(Space.row)) {
                    block.items.forEach { item -> BulletLine("•", item, style, color) }
                }

                is MdBlock.Numbered -> Column(verticalArrangement = Arrangement.spacedBy(Space.row)) {
                    block.items.forEachIndexed { index, item ->
                        BulletLine("${index + 1}.", item, style, color)
                    }
                }

                // A rule down the left rather than a quotation mark: it survives being nested in a
                // card, and it doesn't compete with the Chinese quotation marks people actually type.
                // IntrinsicSize.Min + fillMaxHeight is what actually gives the rule a height: a
                // Spacer in a Row is zero-height by default, so without this the quote just looked
                // indented and the rule was never drawn at all.
                is MdBlock.Quote -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Spacer(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(scheme.outline, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = inline(block.text),
                        style = style,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

/** Marker column is fixed-width so wrapped lines line up under the text, not under the bullet. */
@Composable
private fun BulletLine(
    marker: String,
    text: String,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = marker,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(18.dp),
        )
        Text(text = inline(text), style = style, color = color, modifier = Modifier.fillMaxWidth())
    }
}

/** Inline markers → span styles. */
@Composable
private fun inline(text: String): AnnotatedString {
    val codeColor = MaterialTheme.colorScheme.primary
    return remember(text, codeColor) {
        buildAnnotatedString {
            Markdown.inlineSpans(text).forEach { span ->
                withStyle(
                    SpanStyle(
                        fontWeight = if (span.bold) FontWeight.SemiBold else null,
                        fontStyle = if (span.italic) FontStyle.Italic else null,
                        fontFamily = if (span.code) FontFamily.Monospace else null,
                        color = if (span.code) codeColor else androidx.compose.ui.graphics.Color.Unspecified,
                    )
                ) {
                    append(span.text)
                }
            }
        }
    }
}

package com.anadcurukovic.berylux.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType

/**
 * Rank progress as a ruler: ticks every tenth, a line along the bottom, and the red filling from
 * the left. At 0 it reads empty.
 */
@Composable
fun RankRuler(fraction: Float, contentDescription: String, modifier: Modifier = Modifier) {
    val colors = Berylux.colors
    val filled = fraction.coerceIn(0f, 1f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(RULER_HEIGHT.dp)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(filled, 0f..1f)
            },
    ) {
        val line = 1.dp.toPx()
        val bar = 4.dp.toPx()
        val baseline = size.height - line / 2
        drawLine(colors.onBackgroundFaded, Offset(0f, baseline), Offset(size.width, baseline), line)
        for (i in 0..TICKS) {
            val x = (size.width - line) * i / TICKS + line / 2
            val tall = i % (TICKS / 2) == 0
            val top = if (tall) 0f else size.height / 2
            drawLine(colors.onBackgroundFaded, Offset(x, top), Offset(x, size.height), line)
        }
        drawRect(colors.accent, Offset(0f, size.height - bar), Size(size.width * filled, bar))
    }
}

/** The day's slots as segments: [used] of [total] filled in red. */
@Composable
fun SlotSegments(used: Int, total: Int, contentDescription: String, modifier: Modifier = Modifier) {
    val colors = Berylux.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SEGMENT_HEIGHT.dp)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(used.toFloat(), 0f..total.toFloat(), steps = total - 1)
            },
        horizontalArrangement = Arrangement.spacedBy(SEGMENT_GAP.dp),
    ) {
        repeat(total) { i ->
            val color = if (i < used) colors.accent else colors.line
            Box(Modifier.weight(1f).height(SEGMENT_HEIGHT.dp).drawBehind { drawRect(color) })
        }
    }
}

/** A HUD readout: a small label over its value, both in the mono face. */
@Composable
fun HudReadout(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = Berylux.colors
    Column(modifier) {
        Text(label, style = BeryluxType.Label, color = colors.onBackgroundFaded)
        Text(value, style = BeryluxType.Number, color = colors.onBackground)
    }
}

private const val RULER_HEIGHT = 12
private const val TICKS = 10
private const val SEGMENT_HEIGHT = 8
private const val SEGMENT_GAP = 3

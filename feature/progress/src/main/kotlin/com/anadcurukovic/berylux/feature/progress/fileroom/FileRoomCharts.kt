package com.anadcurukovic.berylux.feature.progress.fileroom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.Spacing
import com.anadcurukovic.berylux.core.designsystem.hud
import com.anadcurukovic.berylux.core.domain.CalendarDay
import com.anadcurukovic.berylux.core.domain.CategoryCount
import com.anadcurukovic.berylux.core.domain.DayMark
import com.anadcurukovic.berylux.core.domain.WeekLux
import com.anadcurukovic.berylux.feature.progress.R
import java.time.DayOfWeek
import java.time.format.TextStyle

/** Twelve weeks of Lux, oldest first: this week in red beryl, the best week's Lux above its bar. */
@Composable
internal fun LuxBars(weeks: List<WeekLux>, modifier: Modifier = Modifier) {
    val colors = Berylux.colors
    val measurer = rememberTextMeasurer()
    val numberStyle = BeryluxType.Number.copy(fontSize = 11.sp, color = colors.ink)
    val best = weeks.maxOf { it.lux }
    // On a tie, the most recent best week carries the number.
    val bestIndex = weeks.indexOfLast { it.lux == best }
    Canvas(modifier.fillMaxWidth().height(BARS_HEIGHT.dp)) {
        val number = measurer.measure(best.toString(), numberStyle)
        val top = number.size.height + Spacing.XSmall.toPx()
        val gap = BAR_GAP.dp.toPx()
        val barWidth = (size.width - gap * (weeks.size - 1)) / weeks.size
        val chartHeight = size.height - top
        drawLine(colors.inkFaded, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        if (best == 0) return@Canvas
        weeks.forEachIndexed { i, week ->
            val height = chartHeight * week.lux / best
            drawRect(
                color = if (i == weeks.lastIndex) colors.stamp else colors.ink,
                topLeft = Offset(i * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
            )
        }
        val x = bestIndex * (barWidth + gap) + (barWidth - number.size.width) / 2
        drawText(number, topLeft = Offset(x.coerceIn(0f, size.width - number.size.width), 0f))
    }
}

/**
 * Five weeks from Monday, this week last: a tiny CLOSED stamp on each day that counts, a dashed
 * ring on a day a freeze saved, and a ring around today.
 */
@Composable
internal fun StreakCalendar(days: List<CalendarDay>, modifier: Modifier = Modifier) {
    val colors = Berylux.colors
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier.widthIn(max = CALENDAR_MAX_WIDTH.dp), verticalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { weekday ->
                Text(
                    text = weekday.getDisplayName(TextStyle.NARROW, locale),
                    style = BeryluxType.Label,
                    color = colors.inkFaded,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Canvas(Modifier.fillMaxWidth().aspectRatio(DAYS_A_WEEK / (days.size / DAYS_A_WEEK).toFloat())) {
            val cell = size.width / DAYS_A_WEEK
            days.forEachIndexed { i, day ->
                // Each mark sits in the middle of its cell.
                val center = Offset((i % DAYS_A_WEEK + HALF) * cell, (i / DAYS_A_WEEK + HALF) * cell)
                when (day.mark) {
                    DayMark.CLOSED -> stamp(center, cell * STAMP_SHARE, colors.stamp)
                    DayMark.SAVED -> savedRing(center, cell * RING_SHARE, colors.stamp)
                    DayMark.NONE -> Unit
                }
                if (day.isToday) drawCircle(colors.ink, cell * TODAY_SHARE, center, style = Stroke(2.dp.toPx()))
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
        ) {
            Legend(stringResource(R.string.file_room_closed).hud()) {
                stamp(center, size.minDimension * LEGEND_STAMP_SHARE, colors.stamp)
            }
            Legend(stringResource(R.string.file_room_saved).hud()) {
                savedRing(center, size.minDimension / 2, colors.stamp)
            }
        }
    }
}

@Composable
private fun Legend(label: String, mark: DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.XSmall)) {
        Canvas(Modifier.size(LEGEND_MARK.dp), onDraw = mark)
        Text(label, style = BeryluxType.Label, color = Berylux.colors.inkFaded)
    }
}

/** The CLOSED stamp in small: a double box, tilted the way the big one is. */
private fun DrawScope.stamp(center: Offset, side: Float, red: Color) {
    val outer = 2.dp.toPx()
    val gap = 2.dp.toPx()
    rotate(STAMP_TILT, center) {
        drawRect(red, Offset(center.x - side / 2, center.y - side / 2), Size(side, side), style = Stroke(outer))
        val inner = side - 2 * (outer + gap)
        if (inner > 0f) {
            val topLeft = Offset(center.x - inner / 2, center.y - inner / 2)
            drawRect(red, topLeft, Size(inner, inner), style = Stroke(1.dp.toPx()))
        }
    }
}

private fun DrawScope.savedRing(center: Offset, radius: Float, red: Color) {
    val dash = 3.dp.toPx()
    drawCircle(
        color = red,
        radius = radius,
        center = center,
        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
    )
}

/** Closed cases by category, most first: the name, then a bar with the count at its end. */
@Composable
internal fun CategoryBars(categories: List<CategoryCount>, rowDescription: (CategoryCount) -> String) {
    val colors = Berylux.colors
    val most = categories.maxOfOrNull { it.closed } ?: return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        categories.forEach { category ->
            Column(Modifier.clearAndSetSemantics { contentDescription = rowDescription(category) }) {
                Text(category.name, style = BeryluxType.Text)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    Canvas(Modifier.fillMaxWidth(BAR_SHARE * category.closed / most).height(CATEGORY_BAR.dp)) {
                        drawRect(colors.ink)
                    }
                    Text(category.closed.toString(), style = BeryluxType.Number)
                }
            }
        }
    }
}

private const val BARS_HEIGHT = 120
private const val BAR_GAP = 4
private const val DAYS_A_WEEK = 7
private const val CALENDAR_MAX_WIDTH = 360
private const val STAMP_SHARE = 0.5f
private const val RING_SHARE = 0.26f
private const val TODAY_SHARE = 0.44f
private const val STAMP_TILT = -7f
private const val LEGEND_MARK = 16
private const val LEGEND_STAMP_SHARE = 0.8f
private const val HALF = 0.5f
private const val CATEGORY_BAR = 10

/** The longest bar takes 4/5 of the row, leaving room for its count at 200% text. */
private const val BAR_SHARE = 0.8f

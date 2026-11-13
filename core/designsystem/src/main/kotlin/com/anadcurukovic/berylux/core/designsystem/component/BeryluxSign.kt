package com.anadcurukovic.berylux.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.Spacing

/**
 * The BERYLUX sign: one letter per streak day, a deeper red each week for four weeks, then a
 * stamp from day 29 until the streak ends. A newly lit letter flickers on. On the daylight
 * theme it hangs on a dark plate. The neon reads the same there. With reduced motion a letter
 * lights without the flicker. TalkBack reads [contentDescription].
 */
@Composable
fun BeryluxSign(streakDays: Int, contentDescription: String, modifier: Modifier = Modifier, flicker: Boolean = true) {
    val plate = if (Berylux.colors.isDark) Modifier else Modifier.background(PlateColor).padding(Spacing.Small)
    Box(modifier.clearAndSetSemantics { this.contentDescription = contentDescription }.then(plate)) {
        when (val state = SignState.of(streakDays)) {
            is SignState.Lighting -> Letters(state, flicker && !Berylux.reducedMotion)
            SignState.Stamped -> Stamp()
        }
    }
}

@Composable
private fun Letters(state: SignState.Lighting, flicker: Boolean) {
    val newest = remember { Animatable(1f) }
    LaunchedEffect(state.lit, state.week) {
        if (flicker && state.lit > 0) {
            newest.snapTo(0f)
            newest.animateTo(
                1f,
                keyframes {
                    durationMillis = FLICKER_MS
                    flickerFrames.forEach { (time, value) -> value at time }
                },
            )
        }
    }
    Row {
        Word.forEachIndexed { i, letter ->
            val lit = i < state.lit
            Text(
                text = letter.toString(),
                style = BeryluxType.Heading.copy(fontSize = LETTER_SIZE.sp),
                color = if (lit) WeekReds[state.week] else UnlitColor,
                modifier = if (lit && i == state.lit - 1) Modifier.alpha(newest.value) else Modifier,
            )
        }
    }
}

@Composable
private fun Stamp() {
    Text(
        text = Word,
        style = BeryluxType.Heading.copy(fontSize = LETTER_SIZE.sp),
        color = StampRed,
        modifier = Modifier
            .rotate(STAMP_TILT)
            .drawBehind {
                val outer = 2.dp.toPx()
                val gap = 3.dp.toPx()
                drawRect(StampRed, style = Stroke(outer))
                drawRect(
                    StampRed,
                    topLeft = Offset(gap + outer, gap + outer),
                    size = Size(size.width - 2 * (gap + outer), size.height - 2 * (gap + outer)),
                    style = Stroke(1.dp.toPx()),
                )
            }
            .padding(horizontal = Spacing.Medium, vertical = Spacing.XSmall),
    )
}

internal const val Word = "BERYLUX"
private const val LETTER_SIZE = 32
internal const val STAMP_TILT = -5f
private const val FLICKER_MS = 400

/** The sign gets deeper each week. Week 4 is 2.6:1 on the desk, Anad's call; the count beside it reads in full. */
internal val WeekReds = listOf(Color(0xFFFF6E8C), Color(0xFFE43A64), Color(0xFFC8284F), Color(0xFFA8183F))
internal val UnlitColor = Color(0xFF3C3038)
internal val StampRed = Color(0xFFE43A64)
private val PlateColor = Color(0xFF121014)

/** Three quick flickers over 400 ms: (time in ms, brightness). */
private val flickerFrames = listOf(0 to 0f, 70 to 1f, 140 to 0.2f, 210 to 1f, 280 to 0.4f, 340 to 1f, 400 to 1f)

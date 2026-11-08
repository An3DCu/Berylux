package com.anadcurukovic.berylux.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.Spacing

/**
 * The CLOSED stamp, a double box with the closing date. With [land] on it drops from 1.4 times
 * its size, lands in 180 ms with a short haptic, and its ink spreads over 300 ms; off, it just
 * sits there (an old case, or reduced motion).
 */
@Composable
fun ClosedStamp(label: String, date: String, modifier: Modifier = Modifier, land: Boolean = false) {
    val red = Berylux.colors.stamp
    val drop = remember { Animatable(if (land) DROP_FROM else 1f) }
    val ink = remember { Animatable(if (land) INK_FROM else 1f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(land) {
        if (!land) return@LaunchedEffect
        drop.animateTo(1f, tween(LAND_MS, easing = FastOutLinearInEasing))
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        ink.animateTo(1f, tween(SPREAD_MS, easing = LinearOutSlowInEasing))
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = drop.value
                scaleY = drop.value
                rotationZ = TILT
                // The ink is faint the moment it lands, then soaks in.
                alpha = ink.value
            }
            .drawBehind {
                val outer = (OUTER_STROKE * ink.value).dp.toPx()
                val gap = 3.dp.toPx()
                drawRect(red, style = Stroke(outer))
                drawRect(
                    red,
                    topLeft = Offset(gap + outer, gap + outer),
                    size = Size(size.width - 2 * (gap + outer), size.height - 2 * (gap + outer)),
                    style = Stroke(1.dp.toPx()),
                )
            }
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
    ) {
        Text(label, style = BeryluxType.Subheading, color = red)
        Text(date, style = BeryluxType.Label, color = red)
    }
}

private const val DROP_FROM = 1.4f
private const val INK_FROM = 0.6f
private const val LAND_MS = 180
private const val SPREAD_MS = 300
private const val TILT = -7f
private const val OUTER_STROKE = 3f

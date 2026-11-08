package com.anadcurukovic.berylux.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.random.Random

/**
 * Grain and scanlines over everything this draws, at the effects [level] from Settings. The
 * grain is a fixed tile of dark specks: it shows on paper and the lighter pieces, and leaves the
 * dark desk nearly clean. It only draws; touches pass straight through.
 */
fun Modifier.beryluxEffects(level: Int): Modifier = drawWithCache {
    val strength = effectsStrength(level)
    val grain = if (strength.grain > 0f) ShaderBrush(ImageShader(Noise, TileMode.Repeated, TileMode.Repeated)) else null
    val gap = SCANLINE_GAP.dp.toPx()
    val line = 1.dp.toPx()
    onDrawWithContent {
        drawContent()
        if (grain != null) drawRect(grain, alpha = (GRAIN_ALPHA * strength.grain).coerceAtMost(1f))
        if (strength.scanlines > 0f) {
            val color = Color.Black.copy(alpha = strength.scanlines)
            var y = 0f
            while (y < size.height) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), line)
                y += gap
            }
        }
    }
}

private const val NOISE_SIZE = 192
private const val SCANLINE_GAP = 3
private const val GRAIN_ALPHA = 0.5f
private const val SPECK_ALPHA = 0.35f

/** One tile of specks from a fixed seed: the same grain on every launch and in every test. */
private val Noise = run {
    val random = Random(SEED)
    val pixels = IntArray(NOISE_SIZE * NOISE_SIZE) {
        val v = random.nextFloat()
        // The further from the middle, the stronger the speck.
        val alpha = (abs(v - MIDDLE) * 2 * SPECK_ALPHA * FULL).toInt()
        android.graphics.Color.argb(alpha, 0, 0, 0)
    }
    Bitmap.createBitmap(pixels, NOISE_SIZE, NOISE_SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private const val SEED = 412
private const val MIDDLE = 0.5f
private const val FULL = 255

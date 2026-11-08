package com.anadcurukovic.berylux.core.designsystem.component

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** A gem's four tones, from the lit table down to the deepest facet. */
@Immutable
data class GemPalette(val light: Color, val mid: Color, val dark: Color, val deep: Color)

/** The six beryls, one per rank, in their mineral colors. */
object GemPalettes {
    val Goshenite = GemPalette(Color(0xFFEEF2F2), Color(0xFFC7CFD1), Color(0xFF9AA3A6), Color(0xFF6F777A))
    val Heliodor = GemPalette(Color(0xFFF3D27A), Color(0xFFD9A62C), Color(0xFFA77B16), Color(0xFF7A5A0E))
    val Morganite = GemPalette(Color(0xFFF5C9D3), Color(0xFFDC98A8), Color(0xFFB06A7C), Color(0xFF84495A))
    val Aquamarine = GemPalette(Color(0xFFB4E1EE), Color(0xFF6FB8CF), Color(0xFF3D8AA3), Color(0xFF276478))
    val Emerald = GemPalette(Color(0xFF5CC796), Color(0xFF239062), Color(0xFF14613F), Color(0xFF0B432B))
    val RedBeryl = GemPalette(Color(0xFFE43A64), Color(0xFFB51C45), Color(0xFF9E1538), Color(0xFF7A0F2B))
}

/**
 * A brilliant-cut gem seen from the side. With [sweep] on, a band of light crosses the facets
 * every 4 seconds; that needs AGSL (Android 13 and newer), and older phones get the still gem.
 */
@Composable
fun Gem(palette: GemPalette, contentDescription: String, modifier: Modifier = Modifier, sweep: Boolean = false) {
    val animate = sweep && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val phase = if (animate) {
        rememberInfiniteTransition(label = "gem").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                keyframes {
                    durationMillis = SWEEP_EVERY_MS
                    0f at 0 using LinearEasing
                    1f at SWEEP_TAKES_MS
                    1f at SWEEP_EVERY_MS
                },
                RepeatMode.Restart,
            ),
            label = "sweep",
        ).value
    } else {
        null
    }
    GemCanvas(palette, phase, modifier.semantics { this.contentDescription = contentDescription })
}

/** The gem itself, with the light band at [sweepPhase] (0 to 1), or none when it's null. */
@Composable
internal fun GemCanvas(palette: GemPalette, sweepPhase: Float?, modifier: Modifier = Modifier) {
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        Facets.forEach { (points, tone) -> drawPath(facet(points), tone(palette)) }
        if (sweepPhase != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            drawSweep(sweepPhase)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun DrawScope.drawSweep(phase: Float) {
    val shader = RuntimeShader(SWEEP_SHADER)
    shader.setFloatUniform("size", size.width, size.height)
    shader.setFloatUniform("phase", phase)
    // Source-atop keeps the light on the gem's own pixels.
    drawRect(ShaderBrush(shader), blendMode = BlendMode.SrcAtop)
}

/** One facet as points on an 80 by 80 grid, scaled to the canvas. */
private fun DrawScope.facet(points: List<Pair<Float, Float>>): Path = Path().apply {
    val scale = Size(size.width / GRID, size.height / GRID)
    points.forEachIndexed { i, (x, y) ->
        if (i == 0) moveTo(x * scale.width, y * scale.height) else lineTo(x * scale.width, y * scale.height)
    }
    close()
}

private const val GRID = 80f
private const val SWEEP_EVERY_MS = 4_000
private const val SWEEP_TAKES_MS = 1_200

/** The side view: the table and crown on top, a thin girdle, the pavilion down to the point. */
private val Facets: List<Pair<List<Pair<Float, Float>>, (GemPalette) -> Color>> = listOf(
    listOf(28f to 14f, 52f to 14f, 60f to 30f, 20f to 30f) to { it.light },
    listOf(28f to 14f, 20f to 30f, 8f to 30f) to { it.mid },
    listOf(52f to 14f, 72f to 30f, 60f to 30f) to { it.mid },
    listOf(8f to 30f, 72f to 30f, 72f to 34f, 8f to 34f) to { it.deep },
    listOf(8f to 34f, 28f to 34f, 40f to 74f) to { it.dark },
    listOf(28f to 34f, 52f to 34f, 40f to 74f) to { it.mid },
    listOf(52f to 34f, 72f to 34f, 40f to 74f) to { it.deep },
)

/** A soft diagonal band of light, moving from the top left to the bottom right as phase goes 0 to 1. */
private const val SWEEP_SHADER = """
    uniform float2 size;
    uniform float phase;
    half4 main(float2 p) {
        float d = (p.x + p.y * 0.6) / (size.x * 1.6) - (phase * 1.6 - 0.3);
        float band = exp(-d * d * 60.0) * 0.6;
        return half4(band, band, band, band);
    }
"""

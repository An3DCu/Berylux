package com.anadcurukovic.berylux.core.designsystem.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withRotation
import com.anadcurukovic.berylux.core.designsystem.R

/**
 * Pictures for the home screen widgets, which can't use the app's fonts as text: the BERYLUX
 * sign and the CLOSED stamp, drawn in Bebas Neue on a bitmap. Sizes are in pixels.
 */
object WidgetArt {
    /** The sign as it stands on [streakDays], the same letters, reds and stamp as [BeryluxSign]. */
    fun sign(context: Context, streakDays: Int, letterPx: Float): Bitmap {
        val paint = bebas(context, letterPx)
        return when (val state = SignState.of(streakDays)) {
            is SignState.Lighting -> letters(paint, state)
            SignState.Stamped -> boxed(
                paint = paint.apply { color = StampRed.toArgb() },
                text = Word,
                tilt = STAMP_TILT,
                double = true,
            )
        }
    }

    /** The CLOSED stamp on the Today's case widget: one box, tilted like the app's. */
    fun closedStamp(context: Context, label: String, letterPx: Float, red: Color): Bitmap =
        boxed(bebas(context, letterPx).apply { color = red.toArgb() }, label, tilt = CLOSED_TILT, double = false)

    private fun bebas(context: Context, letterPx: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ResourcesCompat.getFont(context, R.font.bebas_neue_regular) ?: Typeface.DEFAULT_BOLD
        textSize = letterPx
        letterSpacing = LETTER_SPACING
    }

    private fun letters(paint: Paint, state: SignState.Lighting): Bitmap {
        val metrics = paint.fontMetrics
        val bitmap = createBitmap(paint.measureText(Word).toInt() + 1, (metrics.descent - metrics.ascent).toInt() + 1)
        val canvas = Canvas(bitmap)
        var x = 0f
        Word.forEachIndexed { i, letter ->
            paint.color = (if (i < state.lit) WeekReds[state.week] else UnlitColor).toArgb()
            canvas.drawText(letter.toString(), x, -metrics.ascent, paint)
            x += paint.measureText(letter.toString())
        }
        return bitmap
    }

    /** [text] in a box, the whole thing turned by [tilt] degrees; [double] adds the inner line. */
    private fun boxed(paint: Paint, text: String, tilt: Float, double: Boolean): Bitmap {
        val unit = paint.textSize / UNITS_PER_LETTER
        val metrics = paint.fontMetrics
        val width = paint.measureText(text) + 2 * PAD_X * unit
        val height = metrics.descent - metrics.ascent + 2 * PAD_Y * unit
        val box = RectF(0f, 0f, width, height)
        // Room for the box at any tilt up to a few degrees.
        val side = maxOf(box.width(), box.height()) * ROOM
        val bitmap = createBitmap(side.toInt(), (box.height() + box.width() * TILT_ROOM).toInt())
        val canvas = Canvas(bitmap)
        box.offset((bitmap.width - box.width()) / 2, (bitmap.height - box.height()) / 2)
        val line = Paint(paint).apply { style = Paint.Style.STROKE }
        canvas.withRotation(tilt, box.centerX(), box.centerY()) {
            line.strokeWidth = OUTER * unit
            drawRect(box, line)
            if (double) {
                val gap = (OUTER + GAP) * unit
                line.strokeWidth = unit
                drawRect(RectF(box.left + gap, box.top + gap, box.right - gap, box.bottom - gap), line)
            }
            drawText(text, box.left + PAD_X * unit, box.top + PAD_Y * unit - metrics.ascent, paint)
        }
        return bitmap
    }

    // In the app the sign's letters are 32 sp with 2, 3 and 1 dp lines: one unit is 1/16 of a letter.
    private const val UNITS_PER_LETTER = 16f
    private const val OUTER = 2f
    private const val GAP = 3f
    private const val PAD_X = 8f
    private const val PAD_Y = 4f
    private const val CLOSED_TILT = -7f
    private const val ROOM = 1.02f
    private const val TILT_ROOM = 0.14f
    private const val LETTER_SPACING = 0.03f
}

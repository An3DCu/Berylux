package com.anadcurukovic.berylux.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/** Every text color against what it sits on, in both themes: at least 4.5:1 (WCAG AA). */
class ContrastTest {
    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private fun BeryluxColors.textPairs() = mapOf(
        "text on background" to (onBackground to background),
        "faded text on background" to (onBackgroundFaded to background),
        "text on raised" to (onBackground to raised),
        "faded text on raised" to (onBackgroundFaded to raised),
        "accent on background" to (accent to background),
        "text on accent" to (onAccent to accent),
        "ink on paper" to (ink to paper),
        "faded ink on paper" to (inkFaded to paper),
        "stamp on paper" to (stamp to paper),
        "text on stamp" to (onStamp to stamp),
    )

    private fun assertReadable(name: String, colors: BeryluxColors) {
        colors.textPairs().forEach { (pair, colorsOf) ->
            val ratio = contrast(colorsOf.first, colorsOf.second)
            assertWithMessage("$name: $pair is %.2f:1".format(ratio)).that(ratio).isAtLeast(AA)
        }
    }

    @Test
    fun `the dark desk passes for every text color`() = assertReadable("desk", DeskColors)

    @Test
    fun `case paper passes for every text color`() = assertReadable("case paper", CasePaperColors)

    private companion object {
        const val AA = 4.5
    }
}

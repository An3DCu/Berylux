package com.anadcurukovic.berylux.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.Spacing
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Every generated piece, still, in both themes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h1100dp-xhdpi")
class PiecesScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun desk() = capture(daylight = false, name = "pieces_desk")

    @Test
    fun casePaper() = capture(daylight = true, name = "pieces_case_paper")

    private fun capture(daylight: Boolean, name: String) {
        compose.setContent { BeryluxTheme(daylight = daylight) { Sheet() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = options)
    }

    private companion object {
        // Recorded on Windows, checked on Linux in CI: up to 1% of the pixels may differ.
        @OptIn(ExperimentalRoborazziApi::class)
        val options = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
        )
    }
}

@Composable
private fun Sheet() {
    val colors = Berylux.colors
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.background).padding(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            listOf(
                GemPalettes.Goshenite,
                GemPalettes.Heliodor,
                GemPalettes.Morganite,
                GemPalettes.Aquamarine,
                GemPalettes.Emerald,
                GemPalettes.RedBeryl,
            ).forEach { GemCanvas(it, sweepPhase = null, modifier = Modifier.size(48.dp)) }
        }
        GemCanvas(GemPalettes.RedBeryl, sweepPhase = 0.45f, modifier = Modifier.size(96.dp))
        listOf(0, 3, 10, 17, 24, 29).forEach { day ->
            BeryluxSign(streakDays = day, contentDescription = "Streak: $day days", flicker = false)
        }
        RankRuler(fraction = 0f, contentDescription = "Rank progress")
        RankRuler(fraction = 0.24f, contentDescription = "Rank progress")
        SlotSegments(used = 2, total = 5, contentDescription = "2 of 5 cases")
        HudReadout(label = "LUX", value = "120")
        Box(Modifier.fillMaxWidth().height(170.dp)) {
            CaseCard(caseNumber = 412) {
                Text("Fix it", style = BeryluxType.Title)
                Text("Mend something broken instead of replacing it.", style = BeryluxType.Text)
            }
            ClosedStamp(
                label = "CLOSED",
                date = "08 NOV 2026",
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(Spacing.Large),
            )
        }
        Box(Modifier.fillMaxWidth().height(80.dp).beryluxEffects(level = 3)) {
            CaseCard { Text("Grain and scanlines at level 3", style = BeryluxType.Text) }
        }
    }
}

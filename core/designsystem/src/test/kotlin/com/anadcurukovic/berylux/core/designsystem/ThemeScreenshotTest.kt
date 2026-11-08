package com.anadcurukovic.berylux.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The theme on one sheet: the type scale, a case card, and the Material pieces the screens use.
 * Record with `recordRoborazziDebug`; CI runs `verifyRoborazziDebug` against the saved images.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h740dp-xhdpi")
class ThemeScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun desk() = capture(daylight = false, name = "theme_desk")

    @Test
    fun casePaper() = capture(daylight = true, name = "theme_case_paper")

    private fun capture(daylight: Boolean, name: String) {
        compose.setContent { BeryluxTheme(daylight = daylight) { Specimen() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = options)
    }

    private companion object {
        // The images are recorded on Windows and checked on Linux in CI, where text edges can
        // differ by a few pixels. More than 1% of the pixels changing fails the check.
        @OptIn(ExperimentalRoborazziApi::class)
        val options = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
        )
    }
}

@Composable
private fun Specimen() {
    val colors = Berylux.colors
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.background).padding(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Text("Today's docket", style = BeryluxType.Heading, color = colors.onBackground)
        Text("Bonus case", style = BeryluxType.Subheading, color = colors.onBackground)
        Text("120 LUX · 2/5", style = BeryluxType.Number, color = colors.onBackgroundFaded)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.paper, CaseCardShape)
                .padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.XSmall),
        ) {
            Text("CREATE · MEDIUM", style = BeryluxType.Label, color = colors.stamp)
            Text("Fix it", style = BeryluxType.Title, color = colors.ink)
            Text("Mend something broken instead of replacing it.", style = BeryluxType.Text, color = colors.inkFaded)
            Text(
                text = "Take the case",
                style = BeryluxType.Button,
                color = colors.onStamp,
                modifier = Modifier.background(colors.stamp, StampShape).padding(Spacing.Small),
            )
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Text("A Material card until #26", modifier = Modifier.padding(Spacing.Large))
        }
        Button(onClick = {}) { Text("Material button") }
        Text("Čuruković · Đ Š Ž", style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
    }
}

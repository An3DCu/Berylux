package com.anadcurukovic.berylux.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anadcurukovic.berylux.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The main flow on a device or emulator, with the system clock: take the daily case, close it,
 * and see Lux, rank and streak move. The same flow runs on Robolectric in BeryluxAppTest.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainFlowTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun inject() = hilt.inject()

    private fun waitForText(text: String) =
        compose.waitUntilAtLeastOneExists(hasText(text, substring = true), timeoutMillis = 10_000)

    private fun tap(text: String) = compose.onNodeWithText(text).performClick()

    @Test
    fun closingTheDailyCaseMovesLuxRankAndStreak() {
        compose.waitUntilAtLeastOneExists(
            hasContentDescription("Welcome to Berylux") and isHeading(),
            timeoutMillis = 10_000,
        )
        compose.onNode(hasSetTextAction()).performTextInput("Sam")
        tap("Open the first file")
        waitForText("GOSHENITE · 0 LUX")

        waitForText("Take the case")
        compose.onAllNodesWithText("Take the case").onFirst().performScrollTo().performClick()
        waitForText("ON THE DESK: 1 OF 5")
        tap("Cases")
        compose.waitUntilAtLeastOneExists(hasText("Close"), timeoutMillis = 10_000)
        tap("Close")
        tap("Case closed")
        waitForText("LUX EARNED")

        tap("Today")
        compose.waitUntilDoesNotExist(hasText("GOSHENITE · 0 LUX"), timeoutMillis = 10_000)
        tap("Progress")
        compose.waitUntilAtLeastOneExists(hasText("LUX TO HELIODOR", substring = true), timeoutMillis = 10_000)
        compose.onAllNodesWithText("500 LUX TO HELIODOR", substring = true).assertCountEquals(0)
        waitForText("1 DAY ·")
        waitForText("Today's on the record.")
    }
}

package com.anadcurukovic.berylux.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
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
        compose.waitUntilAtLeastOneExists(hasText("Welcome to Berylux") and isHeading(), timeoutMillis = 10_000)
        compose.onNode(hasSetTextAction()).performTextInput("Sam")
        tap("Start")
        waitForText("Goshenite · 0 Lux")
        waitForText("500 Lux to Heliodor")

        waitForText("Take it")
        compose.onAllNodesWithText("Take it").onFirst().performScrollTo().performClick()
        waitForText("1 of 5 cases today")
        tap("Cases")
        compose.waitUntilAtLeastOneExists(hasText("Close"), timeoutMillis = 10_000)
        tap("Close")
        tap("Close it")
        waitForText("Lux earned")

        tap("Today")
        compose.waitUntilDoesNotExist(hasText("Goshenite · 0 Lux"), timeoutMillis = 10_000)
        compose.onAllNodesWithText("500 Lux to Heliodor").assertCountEquals(0)
        tap("Progress")
        waitForText("1 day")
        waitForText("Today counts already.")
    }
}

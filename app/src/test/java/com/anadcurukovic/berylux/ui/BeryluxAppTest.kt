package com.anadcurukovic.berylux.ui

import android.os.Looper
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.anadcurukovic.berylux.MainActivity
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * The real app on Robolectric: Hilt, Room, DataStore and every screen, as a player would use it.
 * Each test starts with a fresh install, at 9 PM on the clock from TestTimeModule.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
// A normal phone screen; Robolectric's default is 320 by 470 dp.
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class BeryluxAppTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var clock: FakeTimeSource

    @Before
    fun inject() = hilt.inject()

    /** Moves the clock, then lets a minute pass so the app's day check runs. */
    private fun clockTo(instant: String) {
        clock.instant = Instant.parse(instant)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(1))
        compose.waitForIdle()
    }

    // Room answers on its own threads, so wait for text instead of assuming it's there.
    private fun waitForText(text: String) =
        compose.waitUntilAtLeastOneExists(hasText(text, substring = true), timeoutMillis = 5_000)

    private fun assertOn(title: String) =
        compose.waitUntilAtLeastOneExists(hasText(title) and isHeading(), timeoutMillis = 5_000)

    /** Waits for the text first: right after a tab change, the next screen may still be loading. */
    private fun tap(text: String) {
        compose.waitUntilAtLeastOneExists(hasText(text), timeoutMillis = 5_000)
        compose.onNodeWithText(text).performClick()
    }

    private fun pressBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /** The catalog is a lazy list: cards below the screen only exist once scrolled to. */
    private fun openCategory(name: String) {
        assertOn("Catalog")
        compose.waitUntilAtLeastOneExists(hasText("Body"), timeoutMillis = 5_000)
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(name))
        tap(name)
        assertOn(name)
    }

    /** Takes the first case Today still offers. [taken] is how many the day has after it. */
    private fun takeFirstCase(taken: Int) {
        waitForText("Take it")
        compose.onAllNodesWithText("Take it").onFirst().performScrollTo().performClick()
        waitForText("$taken of 5 cases today")
    }

    private fun closeFirstOpenCase() {
        compose.waitUntilAtLeastOneExists(hasText("Close"), timeoutMillis = 5_000)
        compose.onAllNodesWithText("Close").onFirst().performClick()
        tap("Close it")
        waitForText("Lux earned")
    }

    private fun startAs(name: String) {
        assertOn("Welcome to Berylux")
        compose.onNode(hasSetTextAction()).performTextInput(name)
        tap("Start")
        assertOn("Daily case")
    }

    @Test
    fun firstLaunchAsksForANameThenOpensToday() {
        startAs("Sam")
        waitForText("Sam")
        waitForText("0 of 5 cases today")
    }

    @Test
    fun startingWithoutANameSaysWhy() {
        assertOn("Welcome to Berylux")
        tap("Start")
        waitForText("Enter a name to start.")
    }

    @Test
    fun takingACaseUsesASlot() {
        startAs("Sam")
        compose.onAllNodesWithText("Take it").onFirst().performScrollTo().performClick()
        waitForText("Taken:")
        waitForText("1 of 5 cases today")
    }

    @Test
    fun aCaseTakenOnTodayIsClosedFromCasesAfterAYes() {
        startAs("Sam")
        compose.onAllNodesWithText("Take it").onFirst().performScrollTo().performClick()
        waitForText("Taken:")
        tap("Cases")
        waitForText("Today · 1 of 5 cases")
        tap("Close")
        tap("Close it")
        waitForText("Closed:")
        waitForText("Lux earned")
    }

    @Test
    fun droppingACaseAsksFirstAndCostsFiveLux() {
        startAs("Sam")
        compose.onAllNodesWithText("Take it").onFirst().performScrollTo().performClick()
        waitForText("Taken:")
        tap("Cases")
        waitForText("Today · 1 of 5 cases")
        tap("Drop")
        waitForText("It costs 5 Lux and frees the slot.")
        compose.onAllNodesWithText("Drop").onLast().performClick()
        waitForText("Dropped:")
        waitForText("Today · 0 of 5 cases")
    }

    @Test
    fun aChallengeYouWriteCanBeTakenThenDeleted() {
        startAs("Sam")
        tap("Catalog")
        openCategory("My cases")
        waitForText("You haven't written any yet.")
        tap("New challenge")
        assertOn("New challenge")
        compose.onNode(hasSetTextAction() and hasText("Title")).performTextInput("Call grandma")
        tap("Save")

        assertOn("My cases")
        waitForText("Call grandma")
        tap("Take it")
        waitForText("Taken: Call grandma")

        tap("Edit")
        assertOn("Edit challenge")
        tap("Delete")
        // The case is still open, so no streak day can go with it.
        waitForText("Its case goes with it.")
        compose.onAllNodesWithText("may leave your streak", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Delete").onLast().performClick()
        assertOn("My cases")
        waitForText("You haven't written any yet.")
    }

    @Test
    fun anEmptyCasesTabLeadsToToday() {
        startAs("Sam")
        tap("Cases")
        waitForText("No cases today.")
        tap("Go to Today")
        assertOn("Daily case")
    }

    @Test
    fun backWalksDownTheTabThenToTodayThenLeaves() {
        startAs("Sam")
        tap("Catalog")
        openCategory("My cases")
        tap("New challenge")
        assertOn("New challenge")

        pressBack()
        assertOn("My cases")
        pressBack()
        // The catalog kept its place near My cases, below its heading.
        waitForText("Big, rare jobs.")
        pressBack()
        assertOn("Daily case")
        pressBack()
        compose.activityRule.scenario.onActivity { check(it.isFinishing) { "Back on Today should leave the app" } }
    }

    @Test
    fun eachTabRemembersWhereYouWere() {
        startAs("Sam")
        tap("Catalog")
        openCategory("Body")
        tap("Cases")
        assertOn("Earlier")
        tap("Catalog")
        assertOn("Body")
    }

    @Test
    fun profileOpensFromTheSigilAndSettingsFromProfile() {
        startAs("Sam")
        compose.onNode(hasContentDescription("Open your profile")).performClick()
        assertOn("Profile")
        tap("Settings")
        assertOn("Settings")
        pressBack()
        assertOn("Profile")
        pressBack()
        assertOn("Daily case")
    }

    @Test
    fun rivalsOpenFromProgress() {
        startAs("Sam")
        tap("Progress")
        tap("Rivals this week")
        assertOn("Rivals")
        waitForText("Ledger")
        waitForText("Your first week.")
    }

    @Test
    fun closingTheFirstCaseUnlocksAnAchievementAnywhere() {
        startAs("Sam")
        compose.onAllNodesWithText("Take it").onFirst().performScrollTo().performClick()
        waitForText("Taken:")
        tap("Cases")
        waitForText("Today · 1 of 5 cases")
        tap("Close")
        tap("Close it")
        waitForText("Unlocked: First case closed")

        tap("Progress")
        assertOn("Progress")
        waitForText("Achievements · 1 of 17")
        waitForText("New")
    }

    @Test
    fun thePlaceSurvivesARecreate() {
        startAs("Sam")
        tap("Catalog")
        openCategory("Mind")
        compose.activityRule.scenario.recreate()
        assertOn("Mind")
    }

    @Test
    fun closingTheDailyCaseMovesLuxRankAndStreak() {
        startAs("Sam")
        waitForText("Goshenite · 0 Lux")
        waitForText("500 Lux to Heliodor")
        tap("Progress")
        assertOn("Progress")
        waitForText("0 days")

        tap("Today")
        takeFirstCase(taken = 1)
        tap("Cases")
        closeFirstOpenCase()

        tap("Today")
        compose.waitUntilDoesNotExist(hasText("Goshenite · 0 Lux"), timeoutMillis = 5_000)
        compose.onAllNodesWithText("500 Lux to Heliodor").assertCountEquals(0)
        tap("Progress")
        waitForText("1 day")
        waitForText("Today counts already.")
    }

    @Test
    fun theDayEndsAt4AMOpenCasesGoColdAndTheStreakMoves() {
        startAs("Sam")
        takeFirstCase(taken = 1)
        takeFirstCase(taken = 2)
        tap("Cases")
        waitForText("Today · 2 of 5 cases")
        closeFirstOpenCase()

        // 3:59 AM in Sarajevo: still Sunday's game day.
        clockTo("2026-11-09T02:59:00Z")
        waitForText("Today · 2 of 5 cases")

        // 4:01 AM: Monday's game day. The open case went cold; the closed one stays closed.
        clockTo("2026-11-09T03:01:00Z")
        waitForText("No cases today.")
        waitForText("Went cold when its day ended")
        tap("Progress")
        waitForText("1 day")
        waitForText("Close a case today to keep it going.")

        tap("Today")
        waitForText("0 of 5 cases today")
        takeFirstCase(taken = 1)
        tap("Cases")
        closeFirstOpenCase()
        tap("Progress")
        waitForText("2 days")
    }
}

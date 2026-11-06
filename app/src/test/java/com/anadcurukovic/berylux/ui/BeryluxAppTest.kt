package com.anadcurukovic.berylux.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Taps through the real app shell on Robolectric: tabs, links, Back and restored state. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BeryluxAppTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun showApp() = compose.setContent { MaterialTheme { BeryluxApp() } }

    private fun assertOn(title: String) {
        compose.onNode(hasText(title) and isHeading()).assertExists()
    }

    private fun tap(text: String) = compose.onNodeWithText(text).performClick()

    private fun pressBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun opensOnToday() {
        showApp()
        assertOn("Today")
    }

    @Test
    fun backWalksDownTheTabThenToTodayThenLeaves() {
        showApp()
        tap("Catalog")
        tap("Open a category")
        tap("New challenge")
        assertOn("New challenge")

        pressBack()
        assertOn("Category 1")
        pressBack()
        assertOn("Catalog")
        pressBack()
        assertOn("Today")
        pressBack()
        compose.activityRule.scenario.onActivity { check(it.isFinishing) { "Back on Today should leave the app" } }
    }

    @Test
    fun eachTabRemembersWhereYouWere() {
        showApp()
        tap("Catalog")
        tap("Open a category")
        tap("Cases")
        assertOn("Cases")
        tap("Catalog")
        assertOn("Category 1")
    }

    @Test
    fun profileAndSettingsOpenFromToday() {
        showApp()
        tap("Profile")
        tap("Settings")
        assertOn("Settings")
        pressBack()
        assertOn("Profile")
        pressBack()
        assertOn("Today")
    }

    @Test
    fun rivalsOpenFromProgress() {
        showApp()
        tap("Progress")
        tap("Rivals")
        assertOn("Rivals")
    }

    @Test
    fun thePlaceSurvivesARecreate() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MaterialTheme { BeryluxApp() } }
        tap("Catalog")
        tap("Open a category")
        restoration.emulateSavedInstanceStateRestore()
        assertOn("Category 1")
    }
}

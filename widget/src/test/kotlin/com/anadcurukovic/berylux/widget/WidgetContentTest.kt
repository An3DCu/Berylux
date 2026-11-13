package com.anadcurukovic.berylux.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import com.anadcurukovic.berylux.core.domain.DailyOnDesk
import com.anadcurukovic.berylux.core.domain.DailyStanding
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.navigation.AppLinks
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.time.Duration.Companion.seconds

/** What each widget shows, and where its taps go, for each state. */
@RunWith(RobolectricTestRunner::class)
class WidgetContentTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val thirtyPages = Challenge(7, 1, "Thirty pages", "", Difficulty.MEDIUM)

    /** The app's launcher activity, which the links open; this module's own manifest has none. */
    @Before
    fun setUp() {
        val main = ComponentName(context.packageName, "com.anadcurukovic.berylux.MainActivity")
        val launcher = IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(main)
            addIntentFilterForActivity(main, launcher)
        }
    }

    private fun daily(standing: DailyStanding, lux: Int = 38, caseId: Long? = null) =
        DailyOnDesk(thirtyPages, "Mind", standing, lux, caseId)

    @Test
    fun `the sign says when there's no streak yet`() = runGlanceAppWidgetUnitTest(TIMEOUT) {
        setContext(context)
        setAppWidgetSize(SMALL)
        provideComposable { SignContent(streak = 0) }
        onNode(hasText("NO STREAK YET")).assertExists()
        onNode(hasContentDescription("Streak: 0 days")).assertExists()
    }

    @Test
    fun `the sign gives the day, and a tap opens Today`() = runGlanceAppWidgetUnitTest(TIMEOUT) {
        setContext(context)
        setAppWidgetSize(SMALL)
        provideComposable { SignContent(streak = 3) }
        onNode(hasText("DAY 3")).assertExists()
        onNode(hasContentDescription("Streak: 3 days")).assertExists()
        onNode(hasStartActivityClickAction(AppLinks.openToday(context))).assertExists()
    }

    @Test
    fun `not taken, it offers to take the case on Today`() = runGlanceAppWidgetUnitTest(TIMEOUT) {
        setContext(context)
        setAppWidgetSize(WIDE)
        provideComposable { CaseContent(daily(DailyStanding.NOT_TAKEN)) }
        onNode(hasText("DAILY CASE")).assertExists()
        onNode(hasText("Thirty pages")).assertExists()
        onNode(hasText("MIND · MEDIUM · +38 LUX")).assertExists()
        onNode(hasText("TAKE THE CASE")).assertExists()
        onNode(hasText("CLOSE")).assertDoesNotExist()
    }

    @Test
    fun `taken, Close opens the app's own question for that case`() = runGlanceAppWidgetUnitTest(TIMEOUT) {
        setContext(context)
        setAppWidgetSize(WIDE)
        provideComposable { CaseContent(daily(DailyStanding.OPEN, caseId = 12)) }
        onNode(hasText("CLOSE")).assertExists()
        onNode(hasStartActivityClickAction(AppLinks.closeCase(context, 12))).assertExists()
        onNode(hasStartActivityClickAction(AppLinks.closeCase(context, 99))).assertDoesNotExist()
        assertThat(AppLinks.caseToClose(AppLinks.closeCase(context, 12))).isEqualTo(12)
        onNode(hasText("TAKE THE CASE")).assertDoesNotExist()
    }

    @Test
    fun `closed, it shows the stamp and what the case earned`() = runGlanceAppWidgetUnitTest(TIMEOUT) {
        setContext(context)
        setAppWidgetSize(WIDE)
        provideComposable { CaseContent(daily(DailyStanding.CLOSED, caseId = 12)) }
        onNode(hasText("MIND · MEDIUM · +38 LUX EARNED")).assertExists()
        onNode(hasContentDescription("Closed")).assertExists()
        onNode(hasText("CLOSE")).assertDoesNotExist()
    }

    @Test
    fun `before there's a player, a tap opens the welcome screen`() = runGlanceAppWidgetUnitTest(TIMEOUT) {
        setContext(context)
        setAppWidgetSize(WIDE)
        provideComposable { CaseContent(daily = null) }
        onNode(hasText("No case on file yet.")).assertExists()
        onNode(hasStartActivityClickAction(AppLinks.open(context))).assertExists()
    }

    private companion object {
        // Glance gives a test 2 seconds by default. The first test in a run also loads Robolectric,
        // Glance and the fonts, which took 14 seconds on CI.
        val TIMEOUT = 30.seconds
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

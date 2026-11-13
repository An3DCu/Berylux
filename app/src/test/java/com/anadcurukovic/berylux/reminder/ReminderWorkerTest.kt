package com.anadcurukovic.berylux.reminder

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.anadcurukovic.berylux.core.domain.DailyPicker
import com.anadcurukovic.berylux.core.domain.MorningDocketCaseUseCase
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.UserSettings
import com.anadcurukovic.berylux.core.testing.FakeChallengeRepository
import com.anadcurukovic.berylux.core.testing.FakeProfileRepository
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** The worker on its own, with fakes behind it, at 9 AM in Sarajevo on 12 November 2026. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ReminderWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sarajevo = ZoneId.of("Europe/Sarajevo")
    private val time = FakeTimeSource(Instant.parse("2026-11-12T08:00:00Z"), sarajevo)
    private val categories = listOf(Category(1, "Body", "", CategoryKind.REGULAR))
    private val challenges = (1L..6L).map { Challenge(it, 1, "Body $it", "", Difficulty.EASY) }
    private val profiles = FakeProfileRepository()
    private val notifications = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        runBlocking { profiles.createGuest(name = "Sam", pickSeed = 42) }
    }

    private fun allowNotifications() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun runWorker(settings: UserSettings): ListenableWorker.Result {
        val repo = FakeSettingsRepository(settings)
        val notice = ReminderNotice(context).also { it.createChannel() }
        val docket = MorningDocketCaseUseCase(
            profiles,
            FakeChallengeRepository(categories = categories, challenges = challenges),
            time,
        )
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ) = ReminderWorker(appContext, workerParameters, repo, docket, notice, ReminderScheduler(appContext, time))
        }
        return runBlocking {
            TestListenableWorkerBuilder<ReminderWorker>(context).setWorkerFactory(factory).build().doWork()
        }
    }

    private fun booked(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(ReminderScheduler.NAME).get()
            .filter { it.state == WorkInfo.State.ENQUEUED }

    @Test
    fun `it names the day's daily case, opens Today, and books the next morning`() {
        allowNotifications()
        assertThat(runWorker(UserSettings(reminderOn = true))).isEqualTo(ListenableWorker.Result.success())

        // The case DailyPicker picks for Sam on 12 November, the calendar day at 9 AM.
        val daily = DailyPicker.pick(challenges, categories, LocalDate.of(2026, 11, 12), profileSeed = 42).daily!!
        val shown = shadowOf(notifications).allNotifications.single()
        assertThat(shadowOf(shown).contentTitle.toString()).isEqualTo("Today's docket is in.")
        assertThat(shadowOf(shown).contentText.toString()).isEqualTo("${daily.title} leads the day.")
        assertThat(shown.channelId).isEqualTo(ReminderNotice.CHANNEL)
        assertThat(shadowOf(shown.contentIntent).savedIntent.action).isEqualTo("com.anadcurukovic.berylux.OPEN_TODAY")

        // Run at 9:00 sharp, the next one is a full day away: 9 AM on the 13th.
        val next = booked().single()
        assertThat(next.initialDelayMillis).isEqualTo(TimeUnit.HOURS.toMillis(24))
    }

    @Test
    fun `turned off since it was booked, it shows nothing and books nothing`() {
        allowNotifications()
        runWorker(UserSettings(reminderOn = false))
        assertThat(shadowOf(notifications).allNotifications).isEmpty()
        assertThat(booked()).isEmpty()
    }

    @Test
    fun `without Android's permission it shows nothing, and still books the next morning`() {
        runWorker(UserSettings(reminderOn = true, reminderTime = LocalTime.of(9, 30)))
        assertThat(shadowOf(notifications).allNotifications).isEmpty()
        // 9:00 now, 9:30 is half an hour away.
        assertThat(booked().single().initialDelayMillis).isEqualTo(TimeUnit.MINUTES.toMillis(30))
    }
}

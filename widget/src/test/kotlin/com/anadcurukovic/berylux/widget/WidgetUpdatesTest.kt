package com.anadcurukovic.berylux.widget

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.testing.FakeSettingsRepository
import com.anadcurukovic.berylux.core.testing.FakeTimeSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Noon in Sarajevo on 12 November 2026, with the day starting at 4 AM. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WidgetUpdatesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val time = FakeTimeSource(Instant.parse("2026-11-12T11:00:00Z"))
    private val updates = WidgetUpdates(context, GameClock(time, FakeSettingsRepository()), time)

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    private fun booked(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(WidgetUpdates.NAME).get()
            .filter { it.state == WorkInfo.State.ENQUEUED }

    @Test
    fun `the redraw is booked for the end of the game day`() {
        runBlocking { updates.bookEndOfDay() }
        // From noon to 4 AM the next morning is 16 hours.
        assertThat(booked().single().initialDelayMillis).isEqualTo(TimeUnit.HOURS.toMillis(16))
    }

    @Test
    fun `with no widget on the home screen the worker draws nothing and books nothing`() {
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, params: WorkerParameters) =
                WidgetDayWorker(appContext, params, updates)
        }
        val result = runBlocking {
            TestListenableWorkerBuilder<WidgetDayWorker>(context).setWorkerFactory(factory).build().doWork()
        }
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(booked()).isEmpty()
    }
}

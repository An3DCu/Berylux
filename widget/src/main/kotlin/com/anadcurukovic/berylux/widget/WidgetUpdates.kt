package com.anadcurukovic.berylux.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.anadcurukovic.berylux.core.domain.GameClock
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject

/**
 * Draws both widgets again, and keeps one redraw booked with WorkManager for the end of the game
 * day while any widget is on the home screen. No exact alarm: a few minutes late is fine.
 */
class WidgetUpdates @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: GameClock,
    private val time: TimeSource,
) {
    private val work: WorkManager get() = WorkManager.getInstance(context)

    suspend fun anyPlaced(): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(SignWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(CaseWidget::class.java).isNotEmpty()
    }

    suspend fun drawAll() {
        SignWidget().updateAll(context)
        CaseWidget().updateAll(context)
    }

    /** The redraw at the end of today, in place of whatever was waiting. */
    suspend fun bookEndOfDay() {
        work.enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, endOfDay())
    }

    /** From the worker itself: the next redraw waits behind the one running instead of cancelling it. */
    suspend fun bookAfterThis() {
        work.enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, endOfDay())
    }

    fun cancel() {
        work.cancelUniqueWork(NAME)
    }

    private suspend fun endOfDay(): OneTimeWorkRequest {
        val wait = Duration.between(time.now(), clock.endOfToday())
        return OneTimeWorkRequestBuilder<WidgetDayWorker>().setInitialDelay(wait).build()
    }

    companion object {
        const val NAME = "widget_day"

        /**
         * From a widget's receiver, outside Hilt: redraw now and book the end of the day, or stop
         * once the last widget is gone.
         */
        fun runNow(context: Context) {
            val now = OneTimeWorkRequestBuilder<WidgetDayWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, now)
        }
    }
}

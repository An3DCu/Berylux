package com.anadcurukovic.berylux.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.anadcurukovic.berylux.core.domain.MorningDocket
import com.anadcurukovic.berylux.core.domain.repository.TimeSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalTime
import javax.inject.Inject

/**
 * Hands the next morning docket to WorkManager, or takes it back. No exact alarm: the notice may
 * come a few minutes late when the phone is saving power, and that's fine for a morning nudge.
 */
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val time: TimeSource,
) {
    private val work: WorkManager get() = WorkManager.getInstance(context)

    /** One docket at the next [at] on the phone's clock, in place of whatever was waiting. */
    fun schedule(at: LocalTime) {
        work.enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request(at))
    }

    /** From the worker itself: the next docket waits behind the one running instead of cancelling it. */
    fun scheduleAfterThis(at: LocalTime) {
        work.enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request(at))
    }

    fun cancel() {
        work.cancelUniqueWork(NAME)
    }

    private fun request(at: LocalTime): OneTimeWorkRequest {
        val now = time.now()
        val wait = Duration.between(now, MorningDocket.nextAt(now, time.zone(), at))
        return OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(wait).build()
    }

    companion object {
        const val NAME = "morning_docket"
    }
}

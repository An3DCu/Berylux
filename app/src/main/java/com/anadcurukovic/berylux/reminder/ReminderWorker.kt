package com.anadcurukovic.berylux.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anadcurukovic.berylux.core.domain.MorningDocketCaseUseCase
import com.anadcurukovic.berylux.core.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Sends the morning docket, then books the next one. A reminder turned off in the meantime ends here. */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
    private val docketCase: MorningDocketCaseUseCase,
    private val notice: ReminderNotice,
    private val scheduler: ReminderScheduler,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val current = settings.settings.first()
        if (!current.reminderOn) return Result.success()
        docketCase()?.let { notice.show(it.title) }
        scheduler.scheduleAfterThis(current.reminderTime)
        return Result.success()
    }
}

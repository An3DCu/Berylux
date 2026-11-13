package com.anadcurukovic.berylux.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Redraws the widgets for the new game day and books the next one. With none placed, it stops. */
@HiltWorker
class WidgetDayWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updates: WidgetUpdates,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!updates.anyPlaced()) return Result.success()
        updates.drawAll()
        updates.bookAfterThis()
        return Result.success()
    }
}

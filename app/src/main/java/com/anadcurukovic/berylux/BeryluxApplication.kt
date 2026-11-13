package com.anadcurukovic.berylux

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.anadcurukovic.berylux.reminder.ReminderNotice
import com.anadcurukovic.berylux.reminder.ReminderSync
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hilt builds the app-wide object graph from here: database, settings, repositories. WorkManager
 * starts from here too, with Hilt's factory, so the reminder's worker gets its repositories.
 */
@HiltAndroidApp
class BeryluxApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var reminderNotice: ReminderNotice

    @Inject lateinit var reminderSync: ReminderSync

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        reminderNotice.createChannel()
        scope.launch { reminderSync.run() }
    }
}

package com.anadcurukovic.berylux.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.anadcurukovic.berylux.MainActivity
import com.anadcurukovic.berylux.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The "Morning docket" channel and its one notice, which opens Today. */
class ReminderNotice @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    /** Made at start, so Android's notification settings list it before the first notice. */
    fun createChannel() {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.reminder_channel))
                .build(),
        )
    }

    /** Shows the notice for [caseTitle]. Without Android's permission it quietly shows nothing. */
    fun show(caseTitle: String) {
        val denied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (denied || !manager.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context,
            0,
            MainActivity.openTodayIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notice = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(RED_BERYL)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text, caseTitle))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTICE_ID, notice)
    }

    companion object {
        const val CHANNEL = "morning_docket"
        const val NOTICE_ID = 1

        /** The bright red beryl of the dark desk; Android uses it for the icon and the app name. */
        private const val RED_BERYL = 0xFFE43A64.toInt()
    }
}

package com.anadcurukovic.berylux.feature.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.CaseCard
import com.anadcurukovic.berylux.core.designsystem.component.StampButton
import com.anadcurukovic.berylux.core.designsystem.component.StampOutlineButton
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The morning docket: Off and On stamps, and the time from Android's own clock picker. While it's
 * on but Android has notifications off for the app, the card says so instead, with a way there.
 */
@Composable
internal fun MorningDocketCard(on: Boolean, time: LocalTime, onOn: (Boolean) -> Unit, onTime: (LocalTime) -> Unit) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    // Back from Android's settings or its prompt: look again.
    LifecycleResumeEffect(Unit) {
        allowed = notificationsAllowed(context)
        onPauseOrDispose { }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    val colors = Berylux.colors
    CaseCard {
        Text(stringResource(R.string.settings_reminder_title), style = BeryluxType.Title)
        if (on && !allowed) {
            Text(stringResource(R.string.settings_reminder_denied), style = BeryluxType.Text, color = colors.stamp)
            StampOutlineButton(
                text = stringResource(R.string.settings_reminder_open),
                onClick = { context.startActivity(appNotificationSettings(context)) },
                modifier = Modifier.padding(top = 4.dp),
            )
            return@CaseCard
        }
        FlowRow(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OnOffStamp(stringResource(R.string.settings_reminder_off), picked = !on) { onOn(false) }
            OnOffStamp(stringResource(R.string.settings_reminder_on), picked = on) {
                onOn(true)
                if (needsPermission(context)) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        TimeLine(time) { showClockPicker(context, time, colors.isDark, onTime) }
        Text(stringResource(R.string.settings_reminder_note), style = BeryluxType.Text, color = colors.inkFaded)
    }
}

/** Like the theme and motion stamps: the picked one filled, the other an outline. */
@Composable
private fun OnOffStamp(text: String, picked: Boolean, onPick: () -> Unit) {
    if (picked) {
        StampButton(text, onClick = {}, modifier = Modifier.semantics { selected = true })
    } else {
        StampOutlineButton(text, onClick = onPick)
    }
}

/** "At 9:00 AM", the time underlined; tapping it opens the picker. */
@Composable
private fun TimeLine(time: LocalTime, onTap: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("h:mm a", locale) }
    val clock = formatter.format(time)
    val line = stringResource(R.string.settings_reminder_at, clock)
    val start = line.indexOf(clock)
    Text(
        text = buildAnnotatedString {
            append(line)
            if (start >= 0) addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, start + clock.length)
        },
        style = BeryluxType.Text,
        modifier = Modifier.clickable(role = Role.Button, onClick = onTap).padding(vertical = 12.dp),
    )
}

/** Android's own clock picker, in the phone's 12 or 24 hour style, with the app's red. */
private fun showClockPicker(context: Context, time: LocalTime, dark: Boolean, onTime: (LocalTime) -> Unit) {
    val theme = if (dark) R.style.Berylux_ClockPicker_Desk else R.style.Berylux_ClockPicker_Paper
    TimePickerDialog(
        context,
        theme,
        { _, hour, minute -> onTime(LocalTime.of(hour, minute)) },
        time.hour,
        time.minute,
        DateFormat.is24HourFormat(context),
    ).show()
}

private fun needsPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/** On Android 13 and newer this is false until the permission is granted. */
private fun notificationsAllowed(context: Context): Boolean =
    !needsPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun appNotificationSettings(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

package com.anadcurukovic.berylux.feature.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.StampButton
import com.anadcurukovic.berylux.core.designsystem.component.StampOutlineButton
import com.anadcurukovic.berylux.core.ui.clockText
import java.time.LocalTime

/** Under the narrator's line: "Want the docket at 9 AM?" with YES and NO. */
@Composable
internal fun ReminderOffer(time: LocalTime, onAnswer: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.cases_reminder_offer, clockText(time)),
            style = BeryluxType.Text,
            color = Berylux.colors.onBackground,
        )
        StampButton(stringResource(R.string.cases_reminder_yes), onClick = { onAnswer(true) })
        StampOutlineButton(stringResource(R.string.cases_reminder_no), onClick = { onAnswer(false) }, onDesk = true)
    }
}

/**
 * The answer, with Android's notification prompt after a yes on Android 13 and newer. A no, or a
 * no to Android, still counts as answered; Settings shows when Android has notifications off.
 */
@Composable
internal fun rememberReminderAnswer(onAnswer: (Boolean) -> Unit): (Boolean) -> Unit {
    val context = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return { yes ->
        onAnswer(yes)
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (yes && needsPermission) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

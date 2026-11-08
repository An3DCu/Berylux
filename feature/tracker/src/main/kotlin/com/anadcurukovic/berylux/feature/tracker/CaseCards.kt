package com.anadcurukovic.berylux.feature.tracker

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.CardActions
import com.anadcurukovic.berylux.core.designsystem.component.CaseCard
import com.anadcurukovic.berylux.core.designsystem.component.ClosedStamp
import com.anadcurukovic.berylux.core.designsystem.component.StampButton
import com.anadcurukovic.berylux.core.designsystem.component.StampOutlineButton
import com.anadcurukovic.berylux.core.designsystem.hud
import com.anadcurukovic.berylux.core.domain.LuxRules
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.ui.label
import java.time.format.DateTimeFormatter

/* One case's card, the dialog that asks before closing or dropping, and the status names. */

/**
 * A case as a sheet from the file. An open one has Drop and Close; a closed one carries the
 * CLOSED stamp with its date, which drops in when [stampLands].
 */
@Composable
internal fun CaseFileCard(
    case: CaseUi,
    onClose: (CaseUi) -> Unit,
    onDrop: (CaseUi) -> Unit,
    modifier: Modifier = Modifier,
    stampLands: Boolean = false,
) {
    val colors = Berylux.colors
    // Your own challenges give no Lux, so they show none; a drop still costs it.
    val custom = case.file.challenge.isCustom
    val luxLine = when (case.status) {
        CaseStatus.OPEN -> if (custom) "" else stringResource(R.string.cases_lux_pays, case.lux)
        CaseStatus.CLOSED -> if (custom) "" else stringResource(R.string.cases_lux_earned, case.lux)
        CaseStatus.DROPPED -> stringResource(R.string.cases_lux_cost, case.lux)
        CaseStatus.COLD -> stringResource(R.string.cases_went_cold_note)
    }
    // Your own challenges have no difficulty.
    val meta = if (custom) {
        case.categoryName
    } else {
        stringResource(R.string.cases_card_meta, case.categoryName, case.file.challenge.difficulty.label())
    }
    CaseCard(modifier = modifier, caseNumber = case.id) {
        Text(case.title, style = BeryluxType.Title)
        Text(meta.hud(), style = BeryluxType.Label, color = colors.inkFaded)
        CardActions(
            lead = {
                Text(
                    text = luxLine.hud(),
                    style = BeryluxType.Label,
                    color = colors.inkFaded,
                    modifier = Modifier.weight(1f),
                )
            },
        ) {
            when (case.status) {
                CaseStatus.OPEN -> {
                    StampOutlineButton(stringResource(R.string.cases_drop), onClick = { onDrop(case) })
                    StampButton(stringResource(R.string.cases_close), onClick = { onClose(case) })
                }
                CaseStatus.CLOSED -> ClosedStamp(
                    label = stringResource(R.string.cases_stamp),
                    date = case.file.case.closedOn?.let { stampDate().format(it).hud() }.orEmpty(),
                    land = stampLands,
                )
                CaseStatus.DROPPED, CaseStatus.COLD -> Text(
                    text = case.status.label().hud(),
                    style = BeryluxType.Label,
                    color = colors.stamp,
                )
            }
        }
    }
}

/** The stamp's date, as a filing clerk would ink it: 08 NOV 2026. */
@Composable
private fun stampDate(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern("dd MMM yyyy", locale) }
}

@Composable
internal fun ConfirmDialog(pending: PendingAction, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val case = pending.case
    when (pending) {
        is PendingAction.Close -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.cases_close_title, case.title)) },
            text = {
                Text(
                    if (case.file.challenge.isCustom) {
                        stringResource(R.string.cases_close_text_custom)
                    } else {
                        stringResource(R.string.cases_close_text, case.lux)
                    },
                )
            },
            confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.cases_close_confirm)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cases_close_dismiss)) } },
        )
        is PendingAction.Drop -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.cases_drop_title, case.title)) },
            text = { Text(stringResource(R.string.cases_drop_text, LuxRules.DROP_PENALTY)) },
            confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.cases_drop_confirm)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cases_drop_dismiss)) } },
        )
    }
}

@Composable
private fun CaseStatus.label(): String = stringResource(
    when (this) {
        CaseStatus.OPEN -> R.string.cases_status_open
        CaseStatus.CLOSED -> R.string.cases_status_closed
        CaseStatus.COLD -> R.string.cases_status_cold
        CaseStatus.DROPPED -> R.string.cases_status_dropped
    },
)

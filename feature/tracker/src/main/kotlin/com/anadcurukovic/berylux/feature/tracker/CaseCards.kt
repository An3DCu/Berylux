package com.anadcurukovic.berylux.feature.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anadcurukovic.berylux.core.domain.LuxRules
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.ui.label

/* One case's card, the dialog that asks before closing or dropping, and the status names. */

@Composable
internal fun CaseCard(case: CaseUi, onClose: (CaseUi) -> Unit, onDrop: (CaseUi) -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // Your own challenges give no Lux, so they show none; a drop still costs it.
    val custom = case.file.challenge.isCustom
    val luxLine = when (case.status) {
        CaseStatus.OPEN -> if (custom) "" else stringResource(R.string.cases_lux_pays, case.lux)
        CaseStatus.CLOSED -> if (custom) "" else stringResource(R.string.cases_lux_earned, case.lux)
        CaseStatus.DROPPED -> stringResource(R.string.cases_lux_cost, case.lux)
        CaseStatus.COLD -> stringResource(R.string.cases_went_cold_note)
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(case.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(case.status.label(), style = MaterialTheme.typography.labelMedium, color = muted)
            }
            Text(
                // Your own challenges have no difficulty.
                text = if (custom) {
                    case.categoryName
                } else {
                    stringResource(R.string.cases_card_meta, case.categoryName, case.file.challenge.difficulty.label())
                },
                style = MaterialTheme.typography.labelMedium,
                color = muted,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = luxLine,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                if (case.status == CaseStatus.OPEN) {
                    TextButton(onClick = { onDrop(case) }) { Text(stringResource(R.string.cases_drop)) }
                    Button(onClick = { onClose(case) }) { Text(stringResource(R.string.cases_close)) }
                }
            }
        }
    }
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

package com.anadcurukovic.berylux.feature.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.model.Case
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Cases, stateful: gets its ViewModel from Hilt and shows its messages in a snackbar. */
@Composable
fun CasesScreen(
    onOpenToday: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CasesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel, resources) {
        viewModel.messageFlow.collect { message ->
            val text = when (message) {
                // Only your own challenges pay nothing, and they show no Lux.
                is CasesMessage.Closed -> if (message.lux == 0) {
                    resources.getString(R.string.cases_closed_own, message.title)
                } else {
                    resources.getString(R.string.cases_closed, message.title, message.lux)
                }
                is CasesMessage.Dropped -> resources.getString(R.string.cases_dropped, message.title, message.lux)
                CasesMessage.WentCold -> resources.getString(R.string.cases_went_cold)
                CasesMessage.Failed -> resources.getString(R.string.cases_failed)
            }
            snackbar.showSnackbar(text)
        }
    }
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        CasesContent(
            state = state,
            onOpenToday = onOpenToday,
            onClose = viewModel::onClose,
            onDrop = viewModel::onDrop,
            contentPadding = padding,
        )
    }
    (state as? CasesUiState.Content)?.pending?.let { pending ->
        ConfirmDialog(pending, onConfirm = viewModel::onConfirm, onDismiss = viewModel::onDismiss)
    }
}

/** Cases, stateless. */
@Composable
fun CasesContent(
    state: CasesUiState,
    onOpenToday: () -> Unit,
    onClose: (CaseUi) -> Unit,
    onDrop: (CaseUi) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        CasesUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        CasesUiState.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.cases_error), style = MaterialTheme.typography.bodyLarge)
        }
        is CasesUiState.Content -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle(stringResource(R.string.cases_today_title, state.used, state.limit)) }
            if (state.today.isEmpty()) {
                item { EmptyToday(onOpenToday) }
            }
            items(state.today, key = { it.id }) { CaseCard(it, onClose, onDrop) }
            history(state.history)
        }
    }
}

private fun LazyListScope.history(days: List<DayUi>) {
    item { SectionTitle(stringResource(R.string.cases_history_title)) }
    if (days.isEmpty()) {
        item { Note(stringResource(R.string.cases_history_empty)) }
    }
    days.forEach { day ->
        item(key = "day-${day.day}") { DayHeader(day) }
        items(day.cases, key = { it.id }) { CaseCard(it, onClose = {}, onDrop = {}) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
}

@Composable
private fun EmptyToday(onOpenToday: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Note(stringResource(R.string.cases_today_empty))
        OutlinedButton(onClick = onOpenToday) { Text(stringResource(R.string.cases_go_to_today)) }
    }
}

@Composable
private fun DayHeader(day: DayUi) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Text(
        text = stringResource(R.string.cases_day_summary, formatter.format(day.day), day.closed, day.luxEarned),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 4.dp).semantics { heading() },
    )
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview
@Composable
private fun CasesPreview() {
    val today = LocalDate.of(2026, 11, 6)
    fun case(id: Long, title: String, status: CaseStatus, lux: Int, on: LocalDate = today) = CaseUi(
        file = CaseFile(
            case = Case(id, id, status, isDaily = false, acceptedAt = Instant.EPOCH, acceptedOn = on),
            challenge = Challenge(id, 1, title, "", Difficulty.MEDIUM),
        ),
        categoryName = "Body",
        lux = lux,
    )
    MaterialTheme {
        CasesContent(
            state = CasesUiState.Content(
                today = listOf(
                    case(1, "Screen curfew", CaseStatus.OPEN, 25),
                    case(2, "Brisk walk", CaseStatus.CLOSED, 38),
                    case(3, "Plank set", CaseStatus.DROPPED, 5),
                ),
                used = 2,
                history = listOf(
                    DayUi(
                        today.minusDays(1),
                        listOf(
                            case(4, "Thirty pages", CaseStatus.CLOSED, 25, today.minusDays(1)),
                            case(5, "Cold shower", CaseStatus.COLD, 0, today.minusDays(1)),
                        ),
                    ),
                ),
                pending = null,
            ),
            onOpenToday = {},
            onClose = {},
            onDrop = {},
        )
    }
}

package com.anadcurukovic.berylux.feature.rivals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.CaseCardShape
import com.anadcurukovic.berylux.core.designsystem.hud
import com.anadcurukovic.berylux.core.domain.BoardRow
import com.anadcurukovic.berylux.core.domain.RivalBoard
import com.anadcurukovic.berylux.core.model.Rival
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The rivals board, stateful. */
@Composable
fun RivalsScreen(modifier: Modifier = Modifier, viewModel: RivalsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(modifier = modifier) { padding -> RivalsContent(state, contentPadding = padding) }
}

/** The rivals board, stateless. */
@Composable
fun RivalsContent(
    state: RivalsUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        RivalsUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        RivalsUiState.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.rivals_error), style = BeryluxType.Text)
        }
        is RivalsUiState.Content -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Header(state.board) }
            itemsIndexed(state.board.rows, key = { _, row -> row.rival?.name ?: "you" }) { index, row ->
                BoardRowCard(place = index + 1, row = row)
            }
        }
    }
}

@Composable
private fun Header(board: RivalBoard) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val size = board.rows.size
    val colors = Berylux.colors
    Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.rivals_title),
            style = BeryluxType.Heading,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.rivals_week, formatter.format(board.weekStart)),
            style = BeryluxType.Text,
        )
        Text(stringResource(R.string.rivals_place, board.place, size), style = BeryluxType.Subheading)
        Text(
            text = board.lastWeekPlace?.let { stringResource(R.string.rivals_last_week, it, size) }
                ?: stringResource(R.string.rivals_first_week),
            style = BeryluxType.Text,
            color = colors.onBackgroundFaded,
        )
    }
}

@Composable
private fun BoardRowCard(place: Int, row: BoardRow) {
    val colors = Berylux.colors
    // The player's row is a sheet of paper; the bots' sit on the desk, a step up.
    val sheet = if (row.isYou) colors.paper else colors.raised
    val text = if (row.isYou) colors.ink else colors.onBackground
    val faded = if (row.isYou) colors.inkFaded else colors.onBackgroundFaded
    Row(
        modifier = Modifier.fillMaxWidth().background(sheet, CaseCardShape).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("$place", style = BeryluxType.Subheading, color = text, modifier = Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = row.rival?.handle ?: stringResource(R.string.rivals_you),
                style = BeryluxType.Title,
                color = text,
            )
            row.rival?.let { rival ->
                Text(
                    text = (stringResource(R.string.rivals_bot) + " · " + stringResource(rival.habit())).hud(),
                    style = BeryluxType.Label,
                    color = faded,
                )
            }
        }
        Text(stringResource(R.string.rivals_lux, row.lux).hud(), style = BeryluxType.Number, color = text)
    }
}

private fun Rival.habit(): Int = when (this) {
    Rival.LEDGER -> R.string.rivals_habit_ledger
    Rival.FLASHBULB -> R.string.rivals_habit_flashbulb
    Rival.SNOOZE -> R.string.rivals_habit_snooze
    Rival.NIGHTJAR -> R.string.rivals_habit_nightjar
    Rival.GRINDSTONE -> R.string.rivals_habit_grindstone
}

@Preview
@Composable
private fun RivalsPreview() {
    BeryluxTheme {
        RivalsContent(
            RivalsUiState.Content(
                RivalBoard(
                    weekStart = LocalDate.of(2026, 11, 2),
                    rows = listOf(
                        BoardRow(Rival.GRINDSTONE, 412),
                        BoardRow(null, 355),
                        BoardRow(Rival.LEDGER, 340),
                        BoardRow(Rival.NIGHTJAR, 210),
                        BoardRow(Rival.FLASHBULB, 180),
                        BoardRow(Rival.SNOOZE, 60),
                    ),
                    lastWeekPlace = 3,
                ),
            ),
        )
    }
}

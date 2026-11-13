package com.anadcurukovic.berylux.feature.progress.fileroom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.CaseCard
import com.anadcurukovic.berylux.core.designsystem.hud
import com.anadcurukovic.berylux.core.domain.CaseHistory
import com.anadcurukovic.berylux.core.domain.CaseHistoryState
import com.anadcurukovic.berylux.core.model.Case
import com.anadcurukovic.berylux.core.model.CaseFile
import com.anadcurukovic.berylux.core.model.CaseStatus
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.feature.progress.R
import java.time.Instant
import java.time.LocalDate

/** The file room, stateful. */
@Composable
fun FileRoomScreen(modifier: Modifier = Modifier, viewModel: FileRoomViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(modifier = modifier) { padding -> FileRoomContent(state, contentPadding = padding) }
}

/** The file room, stateless: three panels of case paper on the desk. */
@Composable
fun FileRoomContent(
    state: FileRoomUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        FileRoomUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        FileRoomUiState.Error -> Box(modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.file_room_error), style = BeryluxType.Text)
        }
        is FileRoomUiState.Content -> Panels(state.history, modifier, contentPadding)
    }
}

@Composable
private fun Panels(history: CaseHistoryState, modifier: Modifier, contentPadding: PaddingValues) {
    val resources = LocalResources.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.file_room_title),
                style = BeryluxType.Heading,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (!history.anyClosed) {
            item {
                Text(
                    text = stringResource(R.string.file_room_empty),
                    style = BeryluxType.Text,
                    color = Berylux.colors.onBackgroundFaded,
                )
            }
        }
        item {
            val summary = stringResource(
                R.string.file_room_lux_summary,
                history.luxThisWeek,
                history.luxInWeeks,
                history.bestWeek,
            )
            Panel(stringResource(R.string.file_room_lux_title)) {
                Box(Modifier.clearAndSetSemantics { contentDescription = summary }) { LuxBars(history.weeks) }
                // Each label gets half the row and wraps there at large text sizes.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label(stringResource(R.string.file_room_weeks_ago), Modifier.weight(1f))
                    Label(stringResource(R.string.file_room_this_week), Modifier.weight(1f), TextAlign.End)
                }
            }
        }
        item {
            val saved = history.savedDays
            val closed = stringResource(R.string.file_room_calendar_summary, history.closedDays, history.daysSoFar)
            val freezes = resources.getQuantityString(R.plurals.file_room_calendar_saved, saved, saved)
            val summary = if (saved > 0) "$closed $freezes" else closed
            Panel(stringResource(R.string.file_room_calendar_title)) {
                Box(Modifier.clearAndSetSemantics { contentDescription = summary }) { StreakCalendar(history.calendar) }
            }
        }
        item {
            Panel(stringResource(R.string.file_room_categories_title)) {
                CategoryBars(history.categories) {
                    resources.getString(R.string.file_room_category_row, it.name, it.closed)
                }
            }
        }
    }
}

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    CaseCard {
        Text(title, style = BeryluxType.Subheading, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(text.hud(), style = BeryluxType.Label, color = Berylux.colors.inkFaded, textAlign = align, modifier = modifier)
}

@Preview
@Composable
private fun FileRoomPreview() {
    val categories = listOf(
        Category(1, "Mind", "", CategoryKind.REGULAR),
        Category(2, "Body", "", CategoryKind.REGULAR),
    )
    val today = LocalDate.of(2026, 11, 13)
    val files = (0L..20L).map { back ->
        val day = today.minusDays(back * 2)
        CaseFile(
            Case(back, back % 2 + 1, CaseStatus.CLOSED, false, Instant.EPOCH, day, Instant.EPOCH, day, 25),
            Challenge(back % 2 + 1, back % 2 + 1, "Case", "", Difficulty.MEDIUM),
        )
    }
    BeryluxTheme {
        FileRoomContent(FileRoomUiState.Content(CaseHistory.of(files, categories, today)))
    }
}

package com.anadcurukovic.berylux.feature.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.domain.RankRules
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.ui.SigilAvatar
import com.anadcurukovic.berylux.core.ui.label

/** Today, stateful: gets its ViewModel from Hilt and shows its messages in a snackbar. */
@Composable
fun TodayScreen(
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel, resources) {
        viewModel.messageFlow.collect { message ->
            val text = when (message) {
                is TodayMessage.Taken -> resources.getString(R.string.today_taken, message.title)
                TodayMessage.DayFull -> resources.getString(R.string.today_day_full)
                TodayMessage.AlreadyTaken -> resources.getString(R.string.today_already_taken)
                TodayMessage.Failed -> resources.getString(R.string.today_failed)
            }
            snackbar.showSnackbar(text)
        }
    }
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        TodayContent(
            state = state,
            onOpenProfile = onOpenProfile,
            onTake = viewModel::onTake,
            onFilter = viewModel::onFilter,
            contentPadding = padding,
        )
    }
}

/** Today, stateless. */
@Composable
fun TodayContent(
    state: TodayUiState,
    onOpenProfile: () -> Unit,
    onTake: (PickUi) -> Unit,
    onFilter: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        TodayUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        TodayUiState.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.today_error), style = MaterialTheme.typography.bodyLarge)
        }
        is TodayUiState.Content -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(state, onOpenProfile) }
            item { SectionTitle(stringResource(R.string.today_daily_title)) }
            item { state.daily?.let { PickCard(it, onTake) } ?: EmptyNote() }
            item { SectionTitle(stringResource(R.string.today_bonus_title)) }
            item { state.bonus?.let { PickCard(it, onTake) } ?: EmptyNote() }
            item { SectionTitle(stringResource(R.string.today_picks_title)) }
            item { Filters(state.filters, state.selectedFilter, onFilter) }
            if (state.picks.isEmpty()) item { EmptyNote() }
            items(state.picks, key = { it.challenge.id }) { PickCard(it, onTake) }
        }
    }
}

@Composable
private fun Header(state: TodayUiState.Content, onOpenProfile: () -> Unit) {
    val profileLabel = stringResource(R.string.today_open_profile)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SigilAvatar(
                seed = state.sigilSeed,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onOpenProfile)
                    .semantics { contentDescription = profileLabel },
            )
            Column(Modifier.weight(1f)) {
                Text(state.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.today_rank_line, state.rank.rank.label(), state.lux),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        LinearProgressIndicator(progress = { state.rank.fraction }, modifier = Modifier.fillMaxWidth())
        val next = state.rank.next
        Text(
            text = if (next == null) {
                stringResource(R.string.today_top_rank)
            } else {
                stringResource(R.string.today_to_next_rank, state.rank.luxToNext, next.label())
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(R.string.today_slots, state.used, state.limit),
            style = MaterialTheme.typography.labelLarge,
        )
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
private fun Filters(categories: List<Category>, selected: Long?, onFilter: (Long?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onFilter(null) },
                label = { Text(stringResource(R.string.today_filter_all)) },
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selected == category.id,
                onClick = { onFilter(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}

@Composable
private fun PickCard(pick: PickUi, onTake: (PickUi) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(pick.challenge.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.today_card_meta, pick.categoryName, pick.challenge.difficulty.label()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(pick.challenge.description, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.today_card_lux, pick.lux),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                if (pick.taken) {
                    OutlinedButton(onClick = {}, enabled = false) { Text(stringResource(R.string.today_card_taken)) }
                } else {
                    Button(onClick = { onTake(pick) }) { Text(stringResource(R.string.today_card_take)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyNote() {
    Text(
        text = stringResource(R.string.today_nothing_here),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview
@Composable
private fun TodayPreview() {
    val body = Category(1, "Body", "", CategoryKind.REGULAR)
    fun pick(id: Long, title: String, lux: Int, taken: Boolean = false) = PickUi(
        challenge = Challenge(id, 1, title, "A short description of the challenge.", Difficulty.MEDIUM),
        categoryName = "Body",
        lux = lux,
        isDaily = false,
        taken = taken,
    )
    MaterialTheme {
        TodayContent(
            state = TodayUiState.Content(
                name = "Sam",
                sigilSeed = 1234,
                lux = 1_250,
                rank = RankRules.progress(1_250),
                used = 2,
                daily = pick(1, "Brisk walk", 38).copy(isDaily = true),
                bonus = pick(2, "Speak up", 50),
                picks = listOf(pick(3, "Plank set", 25, taken = true), pick(4, "Thirty pages", 25)),
                filters = listOf(body),
                selectedFilter = null,
            ),
            onOpenProfile = {},
            onTake = {},
            onFilter = {},
        )
    }
}

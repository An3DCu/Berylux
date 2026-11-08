package com.anadcurukovic.berylux.feature.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.BeryluxSign
import com.anadcurukovic.berylux.core.designsystem.component.CardActions
import com.anadcurukovic.berylux.core.designsystem.component.CaseCard
import com.anadcurukovic.berylux.core.designsystem.component.RankRuler
import com.anadcurukovic.berylux.core.designsystem.component.SlotSegments
import com.anadcurukovic.berylux.core.designsystem.component.StampButton
import com.anadcurukovic.berylux.core.designsystem.component.StampOutlineButton
import com.anadcurukovic.berylux.core.designsystem.hud
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
            Text(stringResource(R.string.today_error), style = BeryluxType.Text)
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
    val colors = Berylux.colors
    val profileLabel = stringResource(R.string.today_open_profile)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BeryluxSign(
                streakDays = state.streak,
                contentDescription = pluralStringResource(R.plurals.today_sign_description, state.streak, state.streak),
            )
            Text(signDay(state.streak), style = BeryluxType.Label, color = colors.onBackgroundFaded)
        }
        Text(
            text = stringResource(R.string.today_heading),
            style = BeryluxType.Heading,
            modifier = Modifier.semantics { heading() },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SigilAvatar(
                seed = state.sigilSeed,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onOpenProfile)
                    .semantics { contentDescription = profileLabel },
            )
            Column(Modifier.weight(1f)) {
                Text(state.name, style = BeryluxType.Title)
                Text(
                    text = stringResource(R.string.today_rank_line, state.rank.rank.label(), state.lux).hud(),
                    style = BeryluxType.Label,
                    color = colors.onBackgroundFaded,
                )
            }
        }
        RankRuler(fraction = state.rank.fraction, contentDescription = stringResource(R.string.today_rank_progress))
        Text(
            text = stringResource(R.string.today_slots, state.used, state.limit).hud(),
            style = BeryluxType.Label,
            color = colors.onBackgroundFaded,
        )
        SlotSegments(
            used = state.used,
            total = state.limit,
            contentDescription = stringResource(R.string.today_slots_description, state.used, state.limit),
        )
    }
}

/** The count beside the sign: the letters lit this week, or the whole streak once it's stamped. */
@Composable
private fun signDay(streak: Int): String = when {
    streak <= 0 -> stringResource(R.string.today_sign_none)
    streak > STAMPED_FROM - 1 -> stringResource(R.string.today_sign_stamped, streak)
    else -> stringResource(R.string.today_sign_week, (streak - 1) % DAYS_A_WEEK + 1)
}.hud()

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = BeryluxType.Subheading,
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
                label = { Text(stringResource(R.string.today_filter_all), style = BeryluxType.Button) },
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selected == category.id,
                onClick = { onFilter(category.id) },
                label = { Text(category.name, style = BeryluxType.Button) },
            )
        }
    }
}

@Composable
private fun PickCard(pick: PickUi, onTake: (PickUi) -> Unit) {
    val colors = Berylux.colors
    val meta = stringResource(R.string.today_card_meta, pick.categoryName, pick.challenge.difficulty.label()) +
        " · " + stringResource(R.string.today_card_lux, pick.lux)
    CaseCard {
        Text(pick.challenge.title, style = BeryluxType.Title)
        Text(meta.hud(), style = BeryluxType.Label, color = colors.inkFaded)
        Text(pick.challenge.description, style = BeryluxType.Text)
        CardActions {
            if (pick.taken) {
                StampOutlineButton(stringResource(R.string.today_card_taken), onClick = {}, enabled = false)
            } else {
                StampButton(stringResource(R.string.today_card_take), onClick = { onTake(pick) })
            }
        }
    }
}

@Composable
private fun EmptyNote() {
    Text(
        text = stringResource(R.string.today_nothing_here),
        style = BeryluxType.Text,
        color = Berylux.colors.onBackgroundFaded,
    )
}

private const val DAYS_A_WEEK = 7
private const val STAMPED_FROM = 29

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
    BeryluxTheme {
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
                streak = 3,
            ),
            onOpenProfile = {},
            onTake = {},
            onFilter = {},
        )
    }
}

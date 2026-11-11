package com.anadcurukovic.berylux.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.BeryluxSign
import com.anadcurukovic.berylux.core.designsystem.component.CaseCard
import com.anadcurukovic.berylux.core.designsystem.component.Gem
import com.anadcurukovic.berylux.core.designsystem.component.RankRuler
import com.anadcurukovic.berylux.core.designsystem.component.StampOutlineButton
import com.anadcurukovic.berylux.core.designsystem.hud
import com.anadcurukovic.berylux.core.domain.RankRules
import com.anadcurukovic.berylux.core.domain.StreakState
import com.anadcurukovic.berylux.core.model.Achievement
import com.anadcurukovic.berylux.core.ui.gemPalette
import com.anadcurukovic.berylux.core.ui.label
import com.anadcurukovic.berylux.core.ui.textRes
import com.anadcurukovic.berylux.core.ui.titleRes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Progress, stateful. Each time it comes into view, it marks what's new since the last look. */
@Composable
fun ProgressScreen(
    onOpenRivals: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onShown() }
    Scaffold(modifier = modifier) { padding ->
        ProgressContent(state, onOpenRivals, contentPadding = padding)
    }
}

/** Progress, stateless: the record, with the rank's gem, the streak's sign and the commendations. */
@Composable
fun ProgressContent(
    state: ProgressUiState,
    onOpenRivals: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        ProgressUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        ProgressUiState.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.progress_error), style = BeryluxType.Text)
        }
        is ProgressUiState.Content -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Title(stringResource(R.string.progress_title), BeryluxType.Heading) }
            item { RankSection(state) }
            item { StreakSection(state.streak) }
            item {
                StampOutlineButton(
                    text = stringResource(R.string.progress_rivals),
                    onClick = onOpenRivals,
                    modifier = Modifier.fillMaxWidth(),
                    onDesk = true,
                )
            }
            item {
                Title(
                    stringResource(R.string.progress_achievements_title, state.unlockedCount, state.achievements.size),
                    BeryluxType.Subheading,
                )
            }
            items(state.achievements, key = { it.achievement.key }) { AchievementCard(it) }
        }
    }
}

@Composable
private fun Title(text: String, style: TextStyle) {
    Text(text = text, style = style, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
}

@Composable
private fun RankSection(state: ProgressUiState.Content) {
    val colors = Berylux.colors
    val rankName = state.rank.rank.label()
    val next = state.rank.next
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Gem(
                palette = state.rank.rank.gemPalette(),
                contentDescription = rankName,
                modifier = Modifier.size(GEM_SIZE.dp),
                sweep = true,
            )
            Column {
                Text(rankName, style = BeryluxType.Subheading)
                val toNext = if (next == null) {
                    stringResource(R.string.progress_top_rank)
                } else {
                    stringResource(R.string.progress_to_next, state.rank.luxToNext, next.label())
                }
                Text(
                    text = stringResource(R.string.progress_lux_line, state.lux, toNext).hud(),
                    style = BeryluxType.Label,
                    color = colors.onBackgroundFaded,
                )
            }
        }
        RankRuler(fraction = state.rank.fraction, contentDescription = stringResource(R.string.progress_rank_progress))
    }
}

@Composable
private fun StreakSection(streak: StreakState) {
    val colors = Berylux.colors
    val days = pluralStringResource(R.plurals.progress_streak_days, streak.current, streak.current)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Title(stringResource(R.string.progress_streak_title), BeryluxType.Subheading)
        BeryluxSign(streakDays = streak.current, contentDescription = days)
        val longest = pluralStringResource(R.plurals.progress_streak_longest, streak.longest, streak.longest)
        Text(
            text = "$days · $longest".hud(),
            style = BeryluxType.Label,
            color = colors.onBackgroundFaded,
        )
        Text(
            text = stringResource(
                if (streak.doneToday) R.string.progress_streak_done_today else R.string.progress_streak_not_yet,
            ),
            style = BeryluxType.Text,
        )
        Text(
            text = stringResource(
                if (streak.freezesBanked > 0) R.string.progress_freeze_banked else R.string.progress_freeze_none,
            ),
            style = BeryluxType.Text,
            color = colors.onBackgroundFaded,
        )
        if (streak.frozenDays.isNotEmpty()) {
            Text(
                text = pluralStringResource(
                    R.plurals.progress_frozen_days,
                    streak.frozenDays.size,
                    streak.frozenDays.size,
                ),
                style = BeryluxType.Text,
                color = colors.onBackgroundFaded,
            )
        }
    }
}

/** An earned commendation is a paper card with its date; a sealed one is on shaded paper. */
@Composable
private fun AchievementCard(item: AchievementUi) {
    val colors = Berylux.colors
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("dd MMM yyyy", locale) }
    val earned = item.unlockedAt != null
    CaseCard(shaded = !earned) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(item.achievement.titleRes()),
                style = BeryluxType.Title,
                modifier = Modifier.weight(1f),
            )
            if (item.isNew) {
                Text(stringResource(R.string.progress_new), style = BeryluxType.Button, color = colors.stamp)
            }
        }
        Text(stringResource(item.achievement.textRes()), style = BeryluxType.Text)
        Text(
            text = item.unlockedAt?.let {
                stringResource(R.string.progress_unlocked_on, formatter.format(it.atZone(ZoneId.systemDefault())))
            }?.hud() ?: stringResource(R.string.progress_locked).hud(),
            style = BeryluxType.Label,
            color = if (earned) colors.stamp else colors.inkFaded,
        )
    }
}

private const val GEM_SIZE = 72

@Preview
@Composable
private fun ProgressPreview() {
    BeryluxTheme {
        ProgressContent(
            state = ProgressUiState.Content(
                lux = 640,
                rank = RankRules.progress(640),
                streak = StreakState(
                    current = 4,
                    longest = 9,
                    freezesBanked = 1,
                    frozenDays = emptyList(),
                    doneToday = true,
                ),
                achievements = Achievement.entries.mapIndexed { index, achievement ->
                    AchievementUi(achievement, if (index < 2) Instant.EPOCH else null, isNew = index == 1)
                },
            ),
            onOpenRivals = {},
        )
    }
}

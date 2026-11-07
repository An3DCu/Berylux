package com.anadcurukovic.berylux.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.domain.RankRules
import com.anadcurukovic.berylux.core.domain.StreakState
import com.anadcurukovic.berylux.core.model.Achievement
import com.anadcurukovic.berylux.core.ui.label
import com.anadcurukovic.berylux.core.ui.textRes
import com.anadcurukovic.berylux.core.ui.titleRes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

/** Progress, stateless. */
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
            Text(stringResource(R.string.progress_error), style = MaterialTheme.typography.bodyLarge)
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
            item { Title(stringResource(R.string.progress_title), MaterialTheme.typography.headlineSmall) }
            item { RankCard(state) }
            item { StreakCard(state.streak) }
            item {
                OutlinedButton(onClick = onOpenRivals, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.progress_rivals))
                }
            }
            item {
                Title(
                    stringResource(R.string.progress_achievements_title, state.unlockedCount, state.achievements.size),
                    MaterialTheme.typography.titleMedium,
                )
            }
            items(state.achievements, key = { it.achievement.key }) { AchievementRow(it) }
        }
    }
}

@Composable
private fun Title(text: String, style: TextStyle) {
    Text(text = text, style = style, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
}

@Composable
private fun RankCard(state: ProgressUiState.Content) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.progress_rank_line, state.rank.rank.label(), state.lux),
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(progress = { state.rank.fraction }, modifier = Modifier.fillMaxWidth())
            val next = state.rank.next
            Text(
                text = if (next == null) {
                    stringResource(R.string.progress_top_rank)
                } else {
                    stringResource(R.string.progress_to_next, state.rank.luxToNext, next.label())
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StreakCard(streak: StreakState) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.progress_streak_title), style = MaterialTheme.typography.labelLarge)
            Text(
                text = pluralStringResource(R.plurals.progress_streak_days, streak.current, streak.current),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = pluralStringResource(R.plurals.progress_streak_longest, streak.longest, streak.longest),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    if (streak.doneToday) R.string.progress_streak_done_today else R.string.progress_streak_not_yet,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    if (streak.freezesBanked > 0) R.string.progress_freeze_banked else R.string.progress_freeze_none,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (streak.frozenDays.isNotEmpty()) {
                Text(
                    text = pluralStringResource(
                        R.plurals.progress_frozen_days,
                        streak.frozenDays.size,
                        streak.frozenDays.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementUi) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(item.achievement.titleRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.unlockedAt == null) muted else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (item.isNew) Badge { Text(stringResource(R.string.progress_new)) }
            }
            Text(stringResource(item.achievement.textRes()), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = item.unlockedAt?.let {
                    stringResource(R.string.progress_unlocked_on, formatter.format(it.atZone(ZoneId.systemDefault())))
                } ?: stringResource(R.string.progress_locked),
                style = MaterialTheme.typography.labelMedium,
                color = muted,
            )
        }
    }
}

@Preview
@Composable
private fun ProgressPreview() {
    MaterialTheme {
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

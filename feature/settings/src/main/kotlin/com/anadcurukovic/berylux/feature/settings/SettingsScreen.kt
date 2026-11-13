package com.anadcurukovic.berylux.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.CaseCard
import com.anadcurukovic.berylux.core.designsystem.component.StampButton
import com.anadcurukovic.berylux.core.designsystem.component.StampOutlineButton
import com.anadcurukovic.berylux.core.designsystem.hud
import com.anadcurukovic.berylux.core.model.MotionMode
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.core.model.UserSettings
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Settings, stateful. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(modifier = modifier) { padding ->
        SettingsContent(
            state = state,
            onEarlier = viewModel::onEarlier,
            onLater = viewModel::onLater,
            onTheme = viewModel::onTheme,
            onEffectsLevel = viewModel::onEffectsLevel,
            onMotion = viewModel::onMotion,
            onReminderOn = viewModel::onReminderOn,
            onReminderTime = viewModel::onReminderTime,
            contentPadding = padding,
        )
    }
}

/**
 * Settings, stateless: five cards, the start hour, the morning docket, the theme, the grain and
 * scanlines, and motion.
 */
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onEarlier: () -> Unit,
    onLater: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onEffectsLevel: (Int) -> Unit,
    onMotion: (MotionMode) -> Unit,
    modifier: Modifier = Modifier,
    onReminderOn: (Boolean) -> Unit = {},
    onReminderTime: (LocalTime) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        SettingsUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        SettingsUiState.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.settings_error), style = BeryluxType.Text)
        }
        is SettingsUiState.Content -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_heading),
                style = BeryluxType.Heading,
                modifier = Modifier.semantics { heading() },
            )
            HourCard(state, onEarlier, onLater)
            MorningDocketCard(state.reminderOn, state.reminderTime, onReminderOn, onReminderTime)
            ThemeCard(state.theme, onTheme)
            EffectsCard(state.effectsLevel, onEffectsLevel)
            MotionCard(state.motion, onMotion)
        }
    }
}

@Composable
private fun HourCard(state: SettingsUiState.Content, onEarlier: () -> Unit, onLater: () -> Unit) {
    val colors = Berylux.colors
    CaseCard {
        Text(stringResource(R.string.settings_hour_title), style = BeryluxType.Title)
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val earlier = stringResource(R.string.settings_hour_earlier)
            val later = stringResource(R.string.settings_hour_later)
            StampOutlineButton(
                text = MINUS,
                onClick = onEarlier,
                modifier = Modifier.semantics { contentDescription = earlier },
            )
            Text(hourText(state.shownHour), style = BeryluxType.Subheading)
            StampOutlineButton(
                text = PLUS,
                onClick = onLater,
                modifier = Modifier.semantics { contentDescription = later },
            )
        }
        Text(
            text = state.pendingHour?.let { stringResource(R.string.settings_hour_pending, hourText(it)) }
                ?: stringResource(R.string.settings_hour_note, hourText(state.hour)),
            style = BeryluxType.Text,
            color = colors.inkFaded,
        )
    }
}

@Composable
private fun ThemeCard(selected: ThemeMode, onTheme: (ThemeMode) -> Unit) {
    CaseCard {
        Text(stringResource(R.string.settings_theme_title), style = BeryluxType.Title)
        FlowRow(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { theme ->
                val name = stringResource(theme.labelRes())
                // The picked theme is a filled stamp; the others are outlines.
                if (theme == selected) {
                    StampButton(name, onClick = {}, modifier = Modifier.semantics { this.selected = true })
                } else {
                    StampOutlineButton(name, onClick = { onTheme(theme) })
                }
            }
        }
    }
}

@Composable
private fun EffectsCard(level: Int, onEffectsLevel: (Int) -> Unit) {
    val colors = Berylux.colors
    val total = UserSettings.EFFECTS_LEVELS.last
    CaseCard {
        Text(stringResource(R.string.settings_effects_title), style = BeryluxType.Title)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            UserSettings.EFFECTS_LEVELS.forEach { step ->
                val description = stringResource(R.string.settings_effects_set, step, total)
                val color = if (step <= level) colors.stamp else colors.paperShade
                Box(
                    Modifier
                        .weight(1f)
                        .height(SEGMENT_TOUCH.dp)
                        .clickable(role = Role.Button) { onEffectsLevel(step) }
                        .semantics {
                            contentDescription = description
                            selected = step == level
                        }
                        .drawBehind {
                            // A thin bar inside a 48dp touch target.
                            val bar = SEGMENT_BAR.dp.toPx()
                            drawRect(
                                color = color,
                                topLeft = center.copy(x = 0f, y = center.y - bar / 2),
                                size = size.copy(height = bar),
                            )
                        },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScaleLabel(stringResource(R.string.settings_effects_clean))
            ScaleLabel(stringResource(R.string.settings_effects_level, level, total))
            ScaleLabel(stringResource(R.string.settings_effects_heavy))
        }
    }
}

@Composable
private fun ScaleLabel(text: String) {
    Text(text.hud(), style = BeryluxType.Label, color = Berylux.colors.inkFaded)
}

/** An hour as the phone writes it: 4 AM, or 04:00 where clocks run to 24. */
@Composable
private fun hourText(hour: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("h a", locale) }
    return formatter.format(LocalTime.of(hour, 0))
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.DARK -> R.string.settings_theme_dark
    ThemeMode.DAYLIGHT -> R.string.settings_theme_paper
    ThemeMode.SYSTEM -> R.string.settings_theme_system
}

private const val MINUS = "−"
private const val PLUS = "+"
private const val SEGMENT_TOUCH = 48
private const val SEGMENT_BAR = 8

@Preview
@Composable
private fun SettingsPreview() {
    BeryluxTheme {
        SettingsContent(
            state = SettingsUiState.Content(
                hour = 4,
                pendingHour = 6,
                theme = ThemeMode.DARK,
                effectsLevel = 3,
                motion = MotionMode.FOLLOW_PHONE,
                reminderOn = true,
                reminderTime = LocalTime.of(9, 0),
            ),
            onEarlier = {},
            onLater = {},
            onTheme = {},
            onEffectsLevel = {},
            onMotion = {},
        )
    }
}

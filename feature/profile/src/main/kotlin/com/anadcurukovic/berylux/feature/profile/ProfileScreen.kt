package com.anadcurukovic.berylux.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.CardActions
import com.anadcurukovic.berylux.core.designsystem.component.CaseCard
import com.anadcurukovic.berylux.core.designsystem.component.StampButton
import com.anadcurukovic.berylux.core.designsystem.component.StampOutlineButton
import com.anadcurukovic.berylux.core.designsystem.hud
import com.anadcurukovic.berylux.core.domain.NameProblem
import com.anadcurukovic.berylux.core.domain.NameRules
import com.anadcurukovic.berylux.core.domain.RankRules
import com.anadcurukovic.berylux.core.model.Profile
import com.anadcurukovic.berylux.core.ui.SigilAvatar
import com.anadcurukovic.berylux.core.ui.label
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The personnel file, stateful. Filing it goes back to where it was opened from. */
@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val flowState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel, resources) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                ProfileEvent.Done -> onDone()
                ProfileEvent.Failed -> snackbar.showSnackbar(resources.getString(R.string.profile_failed))
            }
        }
    }
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        ProfileContent(
            state = flowState,
            name = viewModel.name,
            onNameChange = viewModel::onNameChange,
            onDrawAnother = viewModel::onDrawAnother,
            onSave = viewModel::onSave,
            onOpenSettings = onOpenSettings,
            contentPadding = padding,
        )
    }
}

/** The personnel file, stateless. [name] is the field's text, kept apart for fast typing. */
@Composable
fun ProfileContent(
    state: ProfileUiState,
    name: String,
    onNameChange: (String) -> Unit,
    onDrawAnother: () -> Unit,
    onSave: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        ProfileUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        ProfileUiState.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.profile_error), style = BeryluxType.Text)
        }
        is ProfileUiState.Content -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_heading),
                style = BeryluxType.Heading,
                modifier = Modifier.semantics { heading() },
            )
            FileCard(state, name, onNameChange, onDrawAnother, onSave)
            StampOutlineButton(
                text = stringResource(R.string.profile_settings),
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                onDesk = true,
            )
        }
    }
}

@Composable
private fun FileCard(
    state: ProfileUiState.Content,
    name: String,
    onNameChange: (String) -> Unit,
    onDrawAnother: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = Berylux.colors
    val profile = state.profile
    CaseCard {
        Text(
            text = stringResource(R.string.profile_on_file, onFileDate(profile.createdAt)).hud(),
            style = BeryluxType.Label,
            color = colors.stamp,
        )
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SigilAvatar(seed = state.sigilSeed, size = 64.dp)
            StampOutlineButton(stringResource(R.string.welcome_draw_another), onClick = onDrawAnother)
        }
        NameField(name, state.problem, onNameChange)
        Text(
            text = pluralStringResource(
                R.plurals.profile_summary,
                state.streak,
                RankRules.rankFor(profile.lux).label(),
                profile.lux,
                state.streak,
            ).hud(),
            style = BeryluxType.Label,
            color = colors.inkFaded,
        )
        CardActions {
            StampButton(stringResource(R.string.profile_save), onClick = onSave, enabled = !state.saving)
        }
    }
}

/** The name, in ink on the paper; a problem shows in the stamp's red. */
@Composable
private fun NameField(name: String, problem: NameProblem?, onNameChange: (String) -> Unit) {
    val colors = Berylux.colors
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.profile_name_label).hud(), style = BeryluxType.Label) },
        singleLine = true,
        isError = problem != null,
        supportingText = {
            when (problem) {
                NameProblem.EMPTY -> Text(stringResource(R.string.welcome_name_empty))
                NameProblem.TOO_LONG -> Text(stringResource(R.string.welcome_name_too_long, NameRules.MAX_LENGTH))
                null -> Unit
            }
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
            focusedBorderColor = colors.stamp,
            unfocusedBorderColor = colors.inkFaded,
            focusedLabelColor = colors.stamp,
            unfocusedLabelColor = colors.inkFaded,
            cursorColor = colors.stamp,
            errorBorderColor = colors.stamp,
            errorLabelColor = colors.stamp,
            errorSupportingTextColor = colors.stamp,
            errorTextColor = colors.ink,
            errorCursorColor = colors.stamp,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The day the player went on file, as the stamp writes dates: 06 NOV 2026. */
@Composable
private fun onFileDate(createdAt: Instant): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("dd MMM yyyy", locale) }
    return formatter.format(createdAt.atZone(ZoneId.systemDefault()))
}

@Preview
@Composable
private fun ProfilePreview() {
    BeryluxTheme {
        ProfileContent(
            state = ProfileUiState.Content(
                profile = Profile(1, "Anad", lux = 15, pickSeed = 42, createdAt = Instant.EPOCH),
                streak = 1,
                sigilSeed = 42,
                problem = null,
                saving = false,
            ),
            name = "Anad",
            onNameChange = {},
            onDrawAnother = {},
            onSave = {},
            onOpenSettings = {},
        )
    }
}

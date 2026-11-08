package com.anadcurukovic.berylux.feature.profile.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.StampButton
import com.anadcurukovic.berylux.core.designsystem.component.Wordmark
import com.anadcurukovic.berylux.core.domain.NameProblem
import com.anadcurukovic.berylux.core.domain.NameRules
import com.anadcurukovic.berylux.core.ui.SigilAvatar
import com.anadcurukovic.berylux.feature.profile.R

/** The first screen anyone sees. Stateful: it gets its ViewModel from Hilt. */
@Composable
fun WelcomeScreen(modifier: Modifier = Modifier, viewModel: WelcomeViewModel = hiltViewModel()) {
    val flowState by viewModel.state.collectAsStateWithLifecycle()
    // The field shows the ViewModel's name directly, not the copy that comes through the flow.
    val state = flowState.copy(name = viewModel.name)
    WelcomeContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onDrawAnother = viewModel::onDrawAnother,
        onStart = viewModel::onStart,
        modifier = modifier,
    )
}

/** Stateless, so it can be previewed and tested with any state. */
@Composable
fun WelcomeContent(
    state: WelcomeUiState,
    onNameChange: (String) -> Unit,
    onDrawAnother: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Wordmark(contentDescription = stringResource(R.string.welcome_title))
            Text(
                text = stringResource(R.string.welcome_tagline),
                style = BeryluxType.Text,
                color = Berylux.colors.onBackgroundFaded,
            )
            SigilAvatar(seed = state.seed, size = 112.dp)
            TextButton(onClick = onDrawAnother) { Text(stringResource(R.string.welcome_draw_another)) }
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.welcome_name_label)) },
                singleLine = true,
                isError = state.problem != null,
                supportingText = {
                    when (state.problem) {
                        NameProblem.EMPTY -> Text(stringResource(R.string.welcome_name_empty))
                        NameProblem.TOO_LONG ->
                            Text(stringResource(R.string.welcome_name_too_long, NameRules.MAX_LENGTH))
                        null -> Unit
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onStart() }),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.failed) {
                Text(
                    text = stringResource(R.string.welcome_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            StampButton(
                text = stringResource(R.string.welcome_start),
                onClick = onStart,
                enabled = !state.starting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun WelcomePreview() {
    BeryluxTheme {
        WelcomeContent(
            state = WelcomeUiState(name = "Sam", seed = 1234),
            onNameChange = {},
            onDrawAnother = {},
            onStart = {},
        )
    }
}

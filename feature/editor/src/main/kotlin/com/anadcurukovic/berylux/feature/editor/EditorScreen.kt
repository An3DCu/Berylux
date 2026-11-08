package com.anadcurukovic.berylux.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.anadcurukovic.berylux.core.domain.ChallengeProblem
import com.anadcurukovic.berylux.core.domain.ChallengeRules

/** The editor, stateful. Its ViewModel is made for [challengeId], null for a new challenge. */
@Composable
fun EditorScreen(
    challengeId: Long?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = hiltViewModel<EditorViewModel, EditorViewModel.Factory>(
        creationCallback = { factory -> factory.create(challengeId) },
    ),
) {
    val flowState by viewModel.uiState.collectAsStateWithLifecycle()
    // The fields show the ViewModel's text directly, not the copy that comes through the flow.
    val state = (flowState as? EditorUiState.Form)
        ?.copy(title = viewModel.title, description = viewModel.description)
        ?: flowState
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel, resources) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                EditorEvent.Done -> onDone()
                EditorEvent.Failed -> snackbar.showSnackbar(resources.getString(R.string.editor_failed))
            }
        }
    }
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        EditorContent(
            state = state,
            onTitleChange = viewModel::onTitleChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onSave = viewModel::onSave,
            onDelete = viewModel::onDelete,
            contentPadding = padding,
        )
    }
    val form = state as? EditorUiState.Form
    if (form?.confirmDelete == true) {
        DeleteDialog(form, onConfirm = viewModel::onConfirmDelete, onDismiss = viewModel::onDismissDelete)
    }
}

/** The editor, stateless. */
@Composable
fun EditorContent(
    state: EditorUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        EditorUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        EditorUiState.NotFound -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.editor_not_found), style = MaterialTheme.typography.bodyLarge)
        }
        is EditorUiState.Form -> Form(
            state,
            onTitleChange,
            onDescriptionChange,
            onSave,
            onDelete,
            modifier.padding(contentPadding),
        )
    }
}

@Composable
private fun Form(
    state: EditorUiState.Form,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(if (state.isNew) R.string.editor_new_title else R.string.editor_edit_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.editor_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val titleProblem = when {
            ChallengeProblem.TITLE_EMPTY in state.problems -> stringResource(R.string.editor_title_empty)
            ChallengeProblem.TITLE_TOO_LONG in state.problems ->
                stringResource(R.string.editor_title_too_long, ChallengeRules.TITLE_MAX)
            else -> null
        }
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.editor_title_label)) },
            singleLine = true,
            isError = titleProblem != null,
            supportingText = {
                Text(titleProblem ?: stringResource(R.string.editor_count, state.titleLength, ChallengeRules.TITLE_MAX))
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        val descriptionProblem = if (ChallengeProblem.DESCRIPTION_TOO_LONG in state.problems) {
            stringResource(R.string.editor_description_too_long, ChallengeRules.DESCRIPTION_MAX)
        } else {
            null
        }
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.editor_description_label)) },
            minLines = 3,
            isError = descriptionProblem != null,
            supportingText = {
                Text(
                    descriptionProblem ?: stringResource(
                        R.string.editor_count,
                        state.descriptionLength,
                        ChallengeRules.DESCRIPTION_MAX,
                    ),
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.editor_save))
        }
        if (!state.isNew) {
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.editor_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DeleteDialog(state: EditorUiState.Form, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_delete_title, ChallengeRules.cleanTitle(state.title))) },
        text = {
            Text(
                when {
                    state.caseCount == 0 -> stringResource(R.string.editor_delete_text_none)
                    state.closedCount == 0 ->
                        pluralStringResource(R.plurals.editor_delete_cases, state.caseCount, state.caseCount)
                    else -> pluralStringResource(R.plurals.editor_delete_cases, state.caseCount, state.caseCount) +
                        " " + pluralStringResource(R.plurals.editor_delete_streak, state.closedCount)
                },
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.editor_delete_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.editor_delete_dismiss)) } },
    )
}

@Preview
@Composable
private fun EditorPreview() {
    MaterialTheme {
        EditorContent(
            state = EditorUiState.Form(
                isNew = false,
                title = "Read a poem",
                description = "Out loud, to someone.",
                problems = emptySet(),
                saving = false,
                confirmDelete = false,
                caseCount = 2,
                closedCount = 1,
            ),
            onTitleChange = {},
            onDescriptionChange = {},
            onSave = {},
            onDelete = {},
        )
    }
}

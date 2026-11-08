package com.anadcurukovic.berylux.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.anadcurukovic.berylux.core.designsystem.StampShape
import com.anadcurukovic.berylux.core.designsystem.component.CardActions
import com.anadcurukovic.berylux.core.designsystem.component.CaseCard
import com.anadcurukovic.berylux.core.designsystem.component.StampButton
import com.anadcurukovic.berylux.core.designsystem.component.StampOutlineButton
import com.anadcurukovic.berylux.core.designsystem.hud
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Challenge
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.ui.label

/** One category, stateful. Its ViewModel is made for [categoryId]. */
@Composable
fun CategoryScreen(
    categoryId: Long,
    onNewChallenge: () -> Unit,
    onEditChallenge: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryViewModel = hiltViewModel<CategoryViewModel, CategoryViewModel.Factory>(
        creationCallback = { factory -> factory.create(categoryId) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel, resources) {
        viewModel.messageFlow.collect { message ->
            val text = when (message) {
                is CategoryMessage.Taken -> resources.getString(R.string.category_taken_message, message.title)
                CategoryMessage.DayFull -> resources.getString(R.string.category_day_full)
                CategoryMessage.AlreadyTaken -> resources.getString(R.string.category_already_taken)
                CategoryMessage.Failed -> resources.getString(R.string.category_failed)
            }
            snackbar.showSnackbar(text)
        }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if ((state as? CategoryUiState.Content)?.isMyCases == true) {
                ExtendedFloatingActionButton(
                    onClick = onNewChallenge,
                    shape = StampShape,
                    containerColor = Berylux.colors.stamp,
                    contentColor = Berylux.colors.onStamp,
                ) {
                    Text(stringResource(R.string.category_new), style = BeryluxType.Button)
                }
            }
        },
    ) { padding ->
        CategoryContent(
            state = state,
            onDifficulty = viewModel::onDifficulty,
            onTake = viewModel::onTake,
            onEdit = onEditChallenge,
            contentPadding = padding,
        )
    }
}

/** One category, stateless. */
@Composable
fun CategoryContent(
    state: CategoryUiState,
    onDifficulty: (Difficulty?) -> Unit,
    onTake: (ChallengeUi) -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        CategoryUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        CategoryUiState.NotFound -> Message(stringResource(R.string.category_not_found), modifier)
        CategoryUiState.Error -> Message(stringResource(R.string.category_error), modifier)
        is CategoryUiState.Content -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                // Room for the floating button over the last card.
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(state) }
            if (!state.isMyCases) {
                item { Filters(state.difficulty, onDifficulty) }
            }
            if (state.challenges.isEmpty()) {
                item {
                    Note(
                        stringResource(
                            if (state.isMyCases) R.string.category_my_cases_empty else R.string.category_empty,
                        ),
                    )
                }
            }
            items(state.challenges, key = { it.challenge.id }) { item ->
                ChallengeCard(item, onTake = onTake, onEdit = onEdit)
            }
        }
    }
}

@Composable
private fun Header(state: CategoryUiState.Content) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = state.category.name,
            style = BeryluxType.Heading,
            modifier = Modifier.semantics { heading() },
        )
        Text(state.category.description, style = BeryluxType.Text)
        if (state.isMyCases) Note(stringResource(R.string.category_my_cases_note))
    }
}

@Composable
private fun Filters(selected: Difficulty?, onDifficulty: (Difficulty?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onDifficulty(null) },
                label = { Text(stringResource(R.string.category_filter_all), style = BeryluxType.Button) },
            )
        }
        items(Difficulty.entries) { difficulty ->
            FilterChip(
                selected = selected == difficulty,
                onClick = { onDifficulty(difficulty) },
                label = { Text(difficulty.label(), style = BeryluxType.Button) },
            )
        }
    }
}

@Composable
private fun ChallengeCard(item: ChallengeUi, onTake: (ChallengeUi) -> Unit, onEdit: (Long) -> Unit) {
    val challenge = item.challenge
    val colors = Berylux.colors
    CaseCard {
        Text(challenge.title, style = BeryluxType.Title)
        // Your own challenges have no difficulty and give no Lux.
        if (!challenge.isCustom) {
            Text(
                text = challenge.difficulty.label().hud(),
                style = BeryluxType.Label,
                color = colors.inkFaded,
            )
        }
        if (challenge.description.isNotEmpty()) {
            Text(challenge.description, style = BeryluxType.Text)
        }
        CardActions(
            lead = {
                val lux = if (challenge.isCustom) "" else stringResource(R.string.category_lux, item.lux)
                Text(
                    text = lux.hud(),
                    style = BeryluxType.Label,
                    color = colors.inkFaded,
                    modifier = Modifier.weight(1f),
                )
            },
        ) {
            if (challenge.isCustom) {
                StampOutlineButton(stringResource(R.string.category_edit), onClick = { onEdit(challenge.id) })
            }
            when (item.offer) {
                Offer.TAKE -> StampButton(stringResource(R.string.category_take), onClick = { onTake(item) })
                Offer.TAKEN -> StampOutlineButton(
                    stringResource(R.string.category_taken),
                    onClick = {},
                    enabled = false
                )
                Offer.ON_TODAY -> StampOutlineButton(
                    stringResource(R.string.category_on_today),
                    onClick = {},
                    enabled = false
                )
            }
        }
    }
}

@Composable
private fun Message(text: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = BeryluxType.Text)
    }
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = BeryluxType.Text, color = Berylux.colors.onBackgroundFaded)
}

@Preview
@Composable
private fun CategoryPreview() {
    fun item(id: Long, title: String, difficulty: Difficulty, lux: Int, offer: Offer) = ChallengeUi(
        challenge = Challenge(id, 1, title, "A short description of the challenge.", difficulty),
        lux = lux,
        offer = offer,
    )
    BeryluxTheme {
        CategoryContent(
            state = CategoryUiState.Content(
                category = Category(1, "Body", "Moving, strength and stamina.", CategoryKind.REGULAR),
                challenges = listOf(
                    item(1, "Brisk walk", Difficulty.EASY, 10, Offer.ON_TODAY),
                    item(2, "Plank set", Difficulty.MEDIUM, 25, Offer.TAKEN),
                    item(3, "Long run", Difficulty.HARD, 50, Offer.TAKE),
                ),
                difficulty = null,
            ),
            onDifficulty = {},
            onTake = {},
            onEdit = {},
        )
    }
}

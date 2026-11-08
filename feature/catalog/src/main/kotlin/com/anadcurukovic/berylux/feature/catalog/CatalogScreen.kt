package com.anadcurukovic.berylux.feature.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.designsystem.component.CaseCard
import com.anadcurukovic.berylux.core.designsystem.hud
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind

/** The catalog, stateful. */
@Composable
fun CatalogScreen(
    onOpenCategory: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(modifier = modifier) { padding ->
        CatalogContent(state, onOpenCategory, contentPadding = padding)
    }
}

/** The catalog, stateless: every category as a card. */
@Composable
fun CatalogContent(
    state: CatalogUiState,
    onOpenCategory: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (state) {
        CatalogUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        CatalogUiState.Error -> Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.catalog_error), style = BeryluxType.Text)
        }
        is CatalogUiState.Content -> LazyColumn(
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
                Column {
                    Text(
                        text = stringResource(R.string.catalog_title),
                        style = BeryluxType.Heading,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.catalog_subtitle),
                        style = BeryluxType.Text,
                        color = Berylux.colors.onBackgroundFaded,
                    )
                }
            }
            itemsIndexed(state.categories, key = { _, item -> item.category.id }) { index, item ->
                CategoryCard(item, fileNumber = index + 1, onClick = { onOpenCategory(item.category.id) })
            }
        }
    }
}

/** A category as a file in the archive: its number on the tab, then its name and count. */
@Composable
private fun CategoryCard(item: CategoryUi, fileNumber: Int, onClick: () -> Unit) {
    val colors = Berylux.colors
    val label = if (item.category.kind == CategoryKind.CUSTOM) {
        stringResource(R.string.catalog_file_yours)
    } else {
        stringResource(R.string.catalog_file_number, fileNumber)
    }
    CaseCard(modifier = Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.category.name, style = BeryluxType.Title, modifier = Modifier.weight(1f))
            Text(label, style = BeryluxType.Label, color = colors.stamp)
        }
        Text(
            text = pluralStringResource(R.plurals.catalog_count, item.count, item.count).hud(),
            style = BeryluxType.Label,
            color = colors.inkFaded,
        )
        Text(item.category.description, style = BeryluxType.Text)
    }
}

@Preview
@Composable
private fun CatalogPreview() {
    BeryluxTheme {
        CatalogContent(
            state = CatalogUiState.Content(
                listOf(
                    CategoryUi(Category(1, "Body", "Moving, strength and stamina.", CategoryKind.REGULAR), 8),
                    CategoryUi(Category(8, "My cases", "Challenges you wrote yourself.", CategoryKind.CUSTOM), 1),
                ),
            ),
            onOpenCategory = {},
        )
    }
}

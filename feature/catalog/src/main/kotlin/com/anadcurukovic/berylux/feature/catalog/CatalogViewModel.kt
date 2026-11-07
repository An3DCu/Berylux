package com.anadcurukovic.berylux.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anadcurukovic.berylux.core.domain.repository.ChallengeRepository
import com.anadcurukovic.berylux.core.model.Category
import com.anadcurukovic.berylux.core.model.CategoryKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategoryUi(
    val category: Category,
    val count: Int,
)

sealed interface CatalogUiState {
    data object Loading : CatalogUiState

    data class Content(val categories: List<CategoryUi>) : CatalogUiState

    data object Error : CatalogUiState
}

/** Every category with how many challenges it holds. "My cases" comes last. */
@HiltViewModel
class CatalogViewModel @Inject constructor(challenges: ChallengeRepository) : ViewModel() {
    val uiState: StateFlow<CatalogUiState> =
        combine(challenges.categories, challenges.challenges) { categories, all ->
            val counts = all.groupingBy { it.categoryId }.eachCount()
            val ordered = categories.sortedWith(compareBy({ it.kind == CategoryKind.CUSTOM }, { it.id }))
            CatalogUiState.Content(ordered.map { CategoryUi(it, counts[it.id] ?: 0) })
        }
            .catch<CatalogUiState> { emit(CatalogUiState.Error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CatalogUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

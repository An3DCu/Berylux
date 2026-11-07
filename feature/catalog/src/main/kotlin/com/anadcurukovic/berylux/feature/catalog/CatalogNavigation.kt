package com.anadcurukovic.berylux.feature.catalog

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.CatalogRoute
import com.anadcurukovic.berylux.core.navigation.CategoryRoute

/** Catalog: every category, and each category's challenges. "My cases" leads to the editor. */
fun EntryProviderScope<NavKey>.catalogEntries(
    onOpenCategory: (categoryId: Long) -> Unit,
    onNewChallenge: () -> Unit,
    onEditChallenge: (challengeId: Long) -> Unit,
) {
    entry<CatalogRoute> {
        CatalogScreen(onOpenCategory = onOpenCategory)
    }
    entry<CategoryRoute> { key ->
        CategoryScreen(
            categoryId = key.categoryId,
            onNewChallenge = onNewChallenge,
            onEditChallenge = onEditChallenge,
        )
    }
}

package com.anadcurukovic.berylux.feature.catalog

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.core.navigation.CatalogRoute
import com.anadcurukovic.berylux.core.navigation.CategoryRoute
import com.anadcurukovic.berylux.core.ui.PlaceholderLink
import com.anadcurukovic.berylux.core.ui.PlaceholderScreen

/** Catalog: every category, and each category's challenges. */
fun EntryProviderScope<NavKey>.catalogEntries(
    onOpenCategory: (categoryId: Long) -> Unit,
    onNewChallenge: () -> Unit,
) {
    entry<CatalogRoute> {
        PlaceholderScreen(
            title = "Catalog",
            links = listOf(PlaceholderLink("Open a category") { onOpenCategory(1) }),
        )
    }
    entry<CategoryRoute> { key ->
        PlaceholderScreen(
            title = "Category ${key.categoryId}",
            links = listOf(PlaceholderLink("New challenge", onNewChallenge)),
        )
    }
}

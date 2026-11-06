package com.anadcurukovic.berylux.ui

import androidx.annotation.DrawableRes
import androidx.navigation3.runtime.NavKey
import com.anadcurukovic.berylux.R
import com.anadcurukovic.berylux.core.navigation.CasesRoute
import com.anadcurukovic.berylux.core.navigation.CatalogRoute
import com.anadcurukovic.berylux.core.navigation.ProgressRoute
import com.anadcurukovic.berylux.core.navigation.TodayRoute

/** The four tabs, in the order they appear. Today is where the app starts and is left. */
enum class TopLevelTab(val route: NavKey, val label: String, @DrawableRes val icon: Int) {
    TODAY(TodayRoute, "Today", R.drawable.ic_tab_today),
    CASES(CasesRoute, "Cases", R.drawable.ic_tab_cases),
    CATALOG(CatalogRoute, "Catalog", R.drawable.ic_tab_catalog),
    PROGRESS(ProgressRoute, "Progress", R.drawable.ic_tab_progress),
}

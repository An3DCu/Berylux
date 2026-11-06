package com.anadcurukovic.berylux.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/*
 * Every screen's key. They're serializable so the back stacks survive process death, and they
 * live here so a feature can open another feature's screen without depending on it.
 */

@Serializable
data object TodayRoute : NavKey

@Serializable
data object CasesRoute : NavKey

@Serializable
data object CatalogRoute : NavKey

@Serializable
data class CategoryRoute(val categoryId: Long) : NavKey

/** A null id makes a new custom challenge; an id edits that one. */
@Serializable
data class EditorRoute(val challengeId: Long? = null) : NavKey

@Serializable
data object ProgressRoute : NavKey

@Serializable
data object RivalsRoute : NavKey

@Serializable
data object ProfileRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

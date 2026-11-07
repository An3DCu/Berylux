package com.anadcurukovic.berylux.core.model

/**
 * Regular categories feed the daily case and the picks; bonus categories feed the bonus case.
 * The custom category holds the player's own challenges, which are never picked.
 */
enum class CategoryKind {
    REGULAR,
    BONUS,
    CUSTOM,
}

data class Category(
    val id: Long,
    val name: String,
    val description: String,
    val kind: CategoryKind,
)

/** The id of "My cases", the category every custom challenge goes in. */
const val MY_CASES_CATEGORY_ID = 8L

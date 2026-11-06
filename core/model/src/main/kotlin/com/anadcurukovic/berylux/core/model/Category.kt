package com.anadcurukovic.berylux.core.model

/** Regular categories feed the daily case and the picks; bonus categories feed the bonus case. */
enum class CategoryKind {
    REGULAR,
    BONUS,
}

data class Category(
    val id: Long,
    val name: String,
    val description: String,
    val kind: CategoryKind,
)

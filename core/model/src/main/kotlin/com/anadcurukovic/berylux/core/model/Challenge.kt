package com.anadcurukovic.berylux.core.model

/**
 * Something to do. Built-in challenges come with the app; custom ones are written by the player
 * and never give Lux.
 */
data class Challenge(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
    val isCustom: Boolean = false,
)

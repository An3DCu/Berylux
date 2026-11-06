package com.anadcurukovic.berylux.core.model

enum class ThemeMode {
    DARK,
    DAYLIGHT,
    SYSTEM,
}

/**
 * @property dayStartHour the hour a new game day starts, 0 to 23.
 * @property effectsLevel how strong grain, scanlines and glow are, 1 (clean) to 5 (heavy).
 */
data class UserSettings(
    val dayStartHour: Int = DEFAULT_DAY_START_HOUR,
    val theme: ThemeMode = ThemeMode.DARK,
    val effectsLevel: Int = DEFAULT_EFFECTS_LEVEL,
) {
    companion object {
        const val DEFAULT_DAY_START_HOUR = 4
        const val DEFAULT_EFFECTS_LEVEL = 3
        val DAY_START_HOURS = 0..23
        val EFFECTS_LEVELS = 1..5
    }
}

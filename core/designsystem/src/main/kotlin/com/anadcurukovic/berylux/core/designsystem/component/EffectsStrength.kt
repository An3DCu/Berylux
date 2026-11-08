package com.anadcurukovic.berylux.core.designsystem.component

/** How strong the grain and scanlines are at one effects level. */
internal data class EffectsStrength(val grain: Float, val scanlines: Float)

/** Level 1 is clean, 3 is medium grain with faint scanlines (the default), 5 is heavy. */
private val Levels = listOf(
    EffectsStrength(grain = 0f, scanlines = 0f),
    EffectsStrength(grain = 0.5f, scanlines = 0f),
    EffectsStrength(grain = 1f, scanlines = 0.04f),
    EffectsStrength(grain = 1.4f, scanlines = 0.06f),
    EffectsStrength(grain = 1.8f, scanlines = 0.09f),
)

internal fun effectsStrength(level: Int): EffectsStrength = Levels[level.coerceIn(1, Levels.size) - 1]

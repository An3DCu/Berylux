package com.anadcurukovic.berylux.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalBeryluxColors = staticCompositionLocalOf { DeskColors }
private val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Berylux's theme. [daylight] picks the case paper colors over the dark desk. The Material
 * roles are filled from the same colors. The Material pieces the screens still use match them.
 */
@Composable
fun BeryluxTheme(daylight: Boolean = false, reducedMotion: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (daylight) CasePaperColors else DeskColors
    CompositionLocalProvider(LocalBeryluxColors provides colors, LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = BeryluxTypography,
            shapes = BeryluxShapes,
            content = content,
        )
    }
}

/** Berylux's own colors, for what Material has no role for: paper, ink and stamps. */
object Berylux {
    val colors: BeryluxColors
        @Composable @ReadOnlyComposable
        get() = LocalBeryluxColors.current

    /** Calm motion: no movement between screens, the stamp, the sign and the gem held still. */
    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReducedMotion.current
}

/** Until the screens are restyled, Material's surfaces are desk tones and its text is chalk or ink. */
internal fun BeryluxColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = raised,
        onPrimaryContainer = onBackground,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = raised,
        onSecondaryContainer = onBackground,
        tertiary = accent,
        onTertiary = onAccent,
        background = background,
        onBackground = onBackground,
        surface = background,
        onSurface = onBackground,
        surfaceVariant = raised,
        onSurfaceVariant = onBackgroundFaded,
        surfaceContainerLowest = background,
        surfaceContainerLow = raised,
        surfaceContainer = raised,
        surfaceContainerHigh = raised,
        surfaceContainerHighest = raised,
        inverseSurface = onBackground,
        inverseOnSurface = background,
        outline = line,
        outlineVariant = line,
        error = accent,
        onError = onAccent,
    )
}

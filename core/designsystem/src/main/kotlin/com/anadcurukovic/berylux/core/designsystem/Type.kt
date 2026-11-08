package com.anadcurukovic.berylux.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Headings and buttons. Bebas Neue has capitals only. */
val Bebas = FontFamily(Font(R.font.bebas_neue_regular))

/** The typewriter: titles and text. */
val SpecialElite = FontFamily(Font(R.font.special_elite_regular))

/** Numbers and labels. */
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

/** The type scale, one style per use. */
object BeryluxType {
    val Heading = TextStyle(fontFamily = Bebas, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = 1.sp)
    val Subheading = TextStyle(fontFamily = Bebas, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = 1.sp)
    val Button = TextStyle(fontFamily = Bebas, fontSize = 18.sp, lineHeight = 22.sp, letterSpacing = 1.sp)
    val Title = TextStyle(fontFamily = SpecialElite, fontSize = 20.sp, lineHeight = 26.sp)
    val Text = TextStyle(fontFamily = SpecialElite, fontSize = 16.sp, lineHeight = 22.sp)
    val Number = TextStyle(fontFamily = PlexMono, fontSize = 14.sp, lineHeight = 20.sp)
    val Label = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
    )
}

/** The scale mapped onto Material's slots, for the Material pieces the screens still use. */
internal val BeryluxTypography = Typography(
    displayLarge = BeryluxType.Heading,
    displayMedium = BeryluxType.Heading,
    displaySmall = BeryluxType.Heading,
    headlineLarge = BeryluxType.Heading,
    headlineMedium = BeryluxType.Subheading,
    headlineSmall = BeryluxType.Subheading,
    titleLarge = BeryluxType.Title,
    titleMedium = BeryluxType.Title,
    titleSmall = BeryluxType.Text,
    bodyLarge = BeryluxType.Text,
    bodyMedium = BeryluxType.Text,
    bodySmall = BeryluxType.Number,
    labelLarge = BeryluxType.Button,
    labelMedium = BeryluxType.Label,
    labelSmall = BeryluxType.Label,
)

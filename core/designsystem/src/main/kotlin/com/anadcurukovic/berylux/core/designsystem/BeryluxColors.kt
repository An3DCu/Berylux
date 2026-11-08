package com.anadcurukovic.berylux.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Berylux's colors: a case file on a desk. Red beryl is the only accent. [accent] is the red
 * that reads on [background] (the bright one on the dark desk); [stamp] is the deep red for
 * stamps and buttons on paper.
 */
@Immutable
data class BeryluxColors(
    val isDark: Boolean,
    val background: Color,
    val raised: Color,
    val onBackground: Color,
    val onBackgroundFaded: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val paper: Color,
    val paperShade: Color,
    val ink: Color,
    val inkFaded: Color,
    val stamp: Color,
    val onStamp: Color,
)

private val Desk = Color(0xFF121014)
private val DeskRaised = Color(0xFF1E1A20)
private val Chalk = Color(0xFFECE4D2)
private val ChalkFaded = Color(0xFFA49AA3)
private val DeskLine = Color(0xFF3A3238)
private val RedBerylBright = Color(0xFFE43A64)
private val Paper = Color(0xFFE8DFC8)
private val PaperShade = Color(0xFFD8CDB2)
private val Ink = Color(0xFF2A2420)
private val InkFaded = Color(0xFF5A4F45)
private val RedBeryl = Color(0xFF9E1538)
private val StampText = Color(0xFFF4ECE0)
private val Page = Color(0xFFEFE7D4)
private val Card = Color(0xFFF7F1E3)
private val PageLine = Color(0xFFCFC3A8)

/** The dark desk, the default theme. */
val DeskColors = BeryluxColors(
    isDark = true,
    background = Desk,
    raised = DeskRaised,
    onBackground = Chalk,
    onBackgroundFaded = ChalkFaded,
    line = DeskLine,
    accent = RedBerylBright,
    onAccent = Desk,
    paper = Paper,
    paperShade = PaperShade,
    ink = Ink,
    inkFaded = InkFaded,
    stamp = RedBeryl,
    onStamp = StampText,
)

/** Daylight: the case file itself, cream paper and ink. */
val CasePaperColors = BeryluxColors(
    isDark = false,
    background = Page,
    raised = Card,
    onBackground = Ink,
    onBackgroundFaded = InkFaded,
    line = PageLine,
    accent = RedBeryl,
    onAccent = StampText,
    paper = Card,
    paperShade = PaperShade,
    ink = Ink,
    inkFaded = InkFaded,
    stamp = RedBeryl,
    onStamp = StampText,
)

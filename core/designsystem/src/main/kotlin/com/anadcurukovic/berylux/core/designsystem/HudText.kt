package com.anadcurukovic.berylux.core.designsystem

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase

/** A HUD line in capitals, in the language the app is shown in. */
fun String.hud(): String = toUpperCase(Locale.current)

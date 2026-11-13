package com.anadcurukovic.berylux.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** A time of day as the app writes it: 9 AM on the hour, 9:30 AM otherwise, in the phone's language. */
@Composable
fun clockText(time: LocalTime): String {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = if (time.minute == 0) "h a" else "h:mm a"
    val formatter = remember(locale, pattern) { DateTimeFormatter.ofPattern(pattern, locale) }
    return formatter.format(time)
}

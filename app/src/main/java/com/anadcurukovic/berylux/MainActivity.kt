package com.anadcurukovic.berylux

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.ui.AppViewModel
import com.anadcurukovic.berylux.ui.BeryluxApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(barStyle(daylight = false), barStyle(daylight = false))
        setContent {
            val theme by appViewModel.theme.collectAsStateWithLifecycle()
            val daylight = when (theme) {
                ThemeMode.DARK -> false
                ThemeMode.DAYLIGHT -> true
                ThemeMode.SYSTEM -> !isSystemInDarkTheme()
            }
            // The bar icons follow the app's theme, not the phone's light or dark mode.
            LaunchedEffect(daylight) { enableEdgeToEdge(barStyle(daylight), barStyle(daylight)) }
            BeryluxTheme(daylight = daylight) {
                BeryluxApp(appViewModel = appViewModel)
            }
        }
    }

    /** Light bars get dark icons and the other way round; the app draws behind both. */
    private fun barStyle(daylight: Boolean): SystemBarStyle =
        if (daylight) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
}

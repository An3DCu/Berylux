package com.anadcurukovic.berylux

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anadcurukovic.berylux.core.designsystem.BeryluxTheme
import com.anadcurukovic.berylux.core.designsystem.component.beryluxEffects
import com.anadcurukovic.berylux.core.model.ThemeMode
import com.anadcurukovic.berylux.ui.AppViewModel
import com.anadcurukovic.berylux.ui.BeryluxApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    /** The phone's "remove animations" setting, read again each time the app comes back. */
    private var phoneRemovesAnimations by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opened from the morning docket: go to Today. Not again when the activity is recreated.
        if (savedInstanceState == null && intent?.action == ACTION_OPEN_TODAY) appViewModel.openToday()
        enableEdgeToEdge(barStyle(daylight = false), barStyle(daylight = false))
        setContent {
            val theme by appViewModel.theme.collectAsStateWithLifecycle()
            val effectsLevel by appViewModel.effectsLevel.collectAsStateWithLifecycle()
            val motion by appViewModel.motion.collectAsStateWithLifecycle()
            val reducedMotion = motion.isReduced(phoneRemovesAnimations)
            val daylight = when (theme) {
                ThemeMode.DARK -> false
                ThemeMode.DAYLIGHT -> true
                ThemeMode.SYSTEM -> !isSystemInDarkTheme()
            }
            // The bar icons follow the app's theme, not the phone's light or dark mode.
            LaunchedEffect(daylight) { enableEdgeToEdge(barStyle(daylight), barStyle(daylight)) }
            BeryluxTheme(daylight = daylight, reducedMotion = reducedMotion) {
                // The grain and scanlines lie over every screen, the bars included.
                Box(Modifier.fillMaxSize().beryluxEffects(effectsLevel)) {
                    BeryluxApp(appViewModel = appViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        phoneRemovesAnimations =
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_OPEN_TODAY) appViewModel.openToday()
    }

    /** Light bars get dark icons and the other way round; the app draws behind both. */
    private fun barStyle(daylight: Boolean): SystemBarStyle =
        if (daylight) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }

    companion object {
        private const val ACTION_OPEN_TODAY = "com.anadcurukovic.berylux.OPEN_TODAY"

        /** Opens the app on Today, over a running one or as a fresh start. */
        fun openTodayIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_TODAY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

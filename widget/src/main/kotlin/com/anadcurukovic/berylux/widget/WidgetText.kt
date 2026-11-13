package com.anadcurukovic.berylux.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.anadcurukovic.berylux.core.domain.HomeWidgetsState
import com.anadcurukovic.berylux.core.domain.HomeWidgetsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow

/** The phone's own fonts: condensed for labels and buttons, mono for the case, as Anad picked. */
internal object WidgetText {
    private val Condensed = FontFamily("sans-serif-condensed")

    fun label(color: Color, size: TextUnit) =
        TextStyle(color = ColorProvider(color), fontSize = size, fontFamily = Condensed, fontWeight = FontWeight.Medium)

    fun case(color: Color, size: TextUnit) =
        TextStyle(color = ColorProvider(color), fontSize = size, fontFamily = FontFamily.Monospace)
}

/** Glance builds widgets outside Hilt's reach; this is how they get the app's repositories. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun homeWidgets(): HomeWidgetsUseCase
}

/** The widgets' state, collected while a widget's session runs; an update to a running one only recomposes it. */
internal fun Context.homeWidgetsState(): Flow<HomeWidgetsState> =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java).homeWidgets()()

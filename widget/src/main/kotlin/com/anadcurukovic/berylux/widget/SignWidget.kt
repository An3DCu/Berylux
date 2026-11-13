package com.anadcurukovic.berylux.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.anadcurukovic.berylux.core.designsystem.DeskColors
import com.anadcurukovic.berylux.core.designsystem.component.WidgetArt
import com.anadcurukovic.berylux.core.navigation.AppLinks
import kotlinx.coroutines.flow.first

/** The sign: BERYLUX lit for the streak, the day under it. A tap opens Today. */
class SignWidget : GlanceAppWidget() {
    // The sign is drawn for the widget's own size, so it fills the width at any size.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val states = context.homeWidgetsState()
        val first = states.first()
        provideContent {
            val state by states.collectAsState(first)
            SignContent(state.streak)
        }
    }
}

class SignWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SignWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdates.runNow(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdates.runNow(context)
    }
}

@Composable
internal fun SignContent(streak: Int) {
    val context = LocalContext.current
    val day = if (streak == 0) {
        context.getString(R.string.widget_sign_none)
    } else {
        context.getString(R.string.widget_sign_day, streak)
    }
    val inner = LocalSize.current.width - PADDING * 2
    val letterPx = (inner.value * SIGN_SHARE / WORD_IN_LETTERS * context.resources.displayMetrics.density)
        .coerceAtLeast(1f)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_desk))
            .padding(PADDING)
            .clickable(actionStartActivity(AppLinks.openToday(context))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(WidgetArt.sign(context, streak, letterPx)),
            contentDescription = context.resources.getQuantityString(R.plurals.widget_sign_description, streak, streak),
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.fillMaxWidth(),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(day.uppercase(), style = WidgetText.label(DeskColors.onBackgroundFaded, 12.sp))
    }
}

private val PADDING = 12.dp

/** BERYLUX in Bebas is about 2.7 letters wide; the lit sign takes 4/5 of the width, the stamp a bit more. */
private const val WORD_IN_LETTERS = 2.7f
private const val SIGN_SHARE = 0.8f

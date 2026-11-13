package com.anadcurukovic.berylux.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.anadcurukovic.berylux.core.designsystem.DeskColors
import com.anadcurukovic.berylux.core.designsystem.component.WidgetArt
import com.anadcurukovic.berylux.core.domain.DailyOnDesk
import com.anadcurukovic.berylux.core.domain.DailyStanding
import com.anadcurukovic.berylux.core.navigation.AppLinks
import com.anadcurukovic.berylux.core.ui.labelRes
import kotlinx.coroutines.flow.first

/**
 * Today's case: the daily case on its paper. Not taken, TAKE THE CASE opens Today; taken, CLOSE
 * opens the app's own "Close it?"; closed, the CLOSED stamp. A tap anywhere else opens Today.
 */
class CaseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val states = context.homeWidgetsState()
        val first = states.first()
        provideContent {
            val state by states.collectAsState(first)
            CaseContent(state.daily)
        }
    }
}

class CaseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaseWidget()

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
internal fun CaseContent(daily: DailyOnDesk?) {
    val context = LocalContext.current
    // Before there's a player, the app opens on its welcome screen.
    val tap = if (daily == null) AppLinks.open(context) else AppLinks.openToday(context)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_desk))
            .padding(12.dp)
            .clickable(actionStartActivity(tap)),
    ) {
        Text(
            text = context.getString(R.string.widget_case_label).uppercase(),
            style = WidgetText.label(DeskColors.onBackgroundFaded, 11.sp),
        )
        Spacer(GlanceModifier.height(4.dp))
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .background(ImageProvider(R.drawable.widget_paper), ContentScale.FillBounds)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (daily == null) {
                Text(context.getString(R.string.widget_case_none), style = WidgetText.case(DeskColors.ink, 15.sp))
            } else {
                Paper(daily)
            }
        }
    }
}

@Composable
private fun ColumnScope.Paper(daily: DailyOnDesk) {
    val context = LocalContext.current
    val lux = if (daily.standing == DailyStanding.CLOSED) {
        context.getString(R.string.widget_case_lux_earned, daily.lux)
    } else {
        context.getString(R.string.widget_case_lux_pays, daily.lux)
    }
    val meta = context.getString(
        R.string.widget_case_meta,
        daily.categoryName,
        context.getString(daily.challenge.difficulty.labelRes()),
        lux,
    )
    Text(daily.challenge.title, style = WidgetText.case(DeskColors.ink, 15.sp), maxLines = 2)
    Text(meta.uppercase(), style = WidgetText.label(DeskColors.inkFaded, 12.sp), maxLines = 1)
    Spacer(GlanceModifier.defaultWeight())
    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        when (daily.standing) {
            DailyStanding.NOT_TAKEN ->
                StampButton(context.getString(R.string.widget_case_take), AppLinks.openToday(context))
            DailyStanding.OPEN -> StampButton(
                context.getString(R.string.widget_case_close),
                AppLinks.closeCase(context, requireNotNull(daily.caseId)),
            )
            DailyStanding.CLOSED -> {
                val label = context.getString(R.string.widget_case_closed)
                Image(
                    provider = ImageProvider(
                        WidgetArt.closedStamp(
                            context,
                            label.uppercase(),
                            STAMP_LETTER_DP * context.resources.displayMetrics.density,
                            DeskColors.stamp,
                        ),
                    ),
                    contentDescription = label,
                    modifier = GlanceModifier.height(STAMP_HEIGHT.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

/** A red stamp with the label in the phone's condensed font. */
@Composable
private fun StampButton(label: String, opens: Intent) {
    Box(
        modifier = GlanceModifier
            .background(ImageProvider(R.drawable.widget_stamp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionStartActivity(opens)),
    ) {
        Text(label.uppercase(), style = WidgetText.label(DeskColors.onStamp, 13.sp))
    }
}

private const val STAMP_LETTER_DP = 22f
private const val STAMP_HEIGHT = 40

package com.anadcurukovic.berylux.feature.tracker

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anadcurukovic.berylux.core.designsystem.Berylux
import com.anadcurukovic.berylux.core.designsystem.BeryluxType
import com.anadcurukovic.berylux.core.domain.Narration

/** The narrator's line under a freshly closed case, in the typewriter. */
@Composable
internal fun NarratorLine(just: JustClosed, modifier: Modifier = Modifier) {
    val line = when (val narration = just.narration) {
        Narration.Close -> stringArrayResource(R.array.narrator_close).let { it[just.line % it.size] }
        Narration.RankUp -> stringArrayResource(R.array.narrator_rank_up).let { it[just.line % it.size] }
        is Narration.Milestone -> stringResource(MilestoneLines.getValue(narration.day))
    }
    Text(
        text = "\u201C$line\u201D",
        style = BeryluxType.Text,
        color = Berylux.colors.onBackgroundFaded,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/** One line for each streak milestone, by day. */
private val MilestoneLines = mapOf(
    7 to R.string.narrator_day_7,
    14 to R.string.narrator_day_14,
    21 to R.string.narrator_day_21,
    28 to R.string.narrator_day_28,
    29 to R.string.narrator_day_29,
)

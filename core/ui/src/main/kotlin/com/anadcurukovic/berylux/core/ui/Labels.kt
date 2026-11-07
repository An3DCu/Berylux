package com.anadcurukovic.berylux.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anadcurukovic.berylux.core.model.Difficulty
import com.anadcurukovic.berylux.core.model.GemRank

/** On-screen names for model values, shared by every feature. */
@Composable
fun GemRank.label(): String = stringResource(
    when (this) {
        GemRank.GOSHENITE -> R.string.core_ui_rank_goshenite
        GemRank.HELIODOR -> R.string.core_ui_rank_heliodor
        GemRank.MORGANITE -> R.string.core_ui_rank_morganite
        GemRank.AQUAMARINE -> R.string.core_ui_rank_aquamarine
        GemRank.EMERALD -> R.string.core_ui_rank_emerald
        GemRank.RED_BERYL -> R.string.core_ui_rank_red_beryl
    },
)

@Composable
fun Difficulty.label(): String = stringResource(
    when (this) {
        Difficulty.EASY -> R.string.core_ui_difficulty_easy
        Difficulty.MEDIUM -> R.string.core_ui_difficulty_medium
        Difficulty.HARD -> R.string.core_ui_difficulty_hard
    },
)

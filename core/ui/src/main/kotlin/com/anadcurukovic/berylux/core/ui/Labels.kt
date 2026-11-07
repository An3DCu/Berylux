package com.anadcurukovic.berylux.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.anadcurukovic.berylux.core.model.Achievement
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

private class AchievementLabel(@StringRes val title: Int, @StringRes val text: Int)

/** Every achievement's name and what it takes, as plain resources a message outside Compose can use. */
private val ACHIEVEMENT_LABELS = mapOf(
    Achievement.FIRST_CLOSED to AchievementLabel(
        R.string.core_ui_achievement_first_closed,
        R.string.core_ui_achievement_first_closed_text,
    ),
    Achievement.CLOSED_10 to AchievementLabel(
        R.string.core_ui_achievement_closed_10,
        R.string.core_ui_achievement_closed_10_text,
    ),
    Achievement.CLOSED_50 to AchievementLabel(
        R.string.core_ui_achievement_closed_50,
        R.string.core_ui_achievement_closed_50_text,
    ),
    Achievement.CLOSED_100 to AchievementLabel(
        R.string.core_ui_achievement_closed_100,
        R.string.core_ui_achievement_closed_100_text,
    ),
    Achievement.FULL_DAY to AchievementLabel(
        R.string.core_ui_achievement_full_day,
        R.string.core_ui_achievement_full_day_text,
    ),
    Achievement.HARD_10 to AchievementLabel(
        R.string.core_ui_achievement_hard_10,
        R.string.core_ui_achievement_hard_10_text,
    ),
    Achievement.BONUS_5 to AchievementLabel(
        R.string.core_ui_achievement_bonus_5,
        R.string.core_ui_achievement_bonus_5_text,
    ),
    Achievement.EVERY_CATEGORY to AchievementLabel(
        R.string.core_ui_achievement_every_category,
        R.string.core_ui_achievement_every_category_text,
    ),
    Achievement.OWN_CASE to AchievementLabel(
        R.string.core_ui_achievement_own_case,
        R.string.core_ui_achievement_own_case_text,
    ),
    Achievement.STREAK_7 to AchievementLabel(
        R.string.core_ui_achievement_streak_7,
        R.string.core_ui_achievement_streak_7_text,
    ),
    Achievement.STREAK_30 to AchievementLabel(
        R.string.core_ui_achievement_streak_30,
        R.string.core_ui_achievement_streak_30_text,
    ),
    Achievement.FREEZE_SAVED to AchievementLabel(
        R.string.core_ui_achievement_freeze_saved,
        R.string.core_ui_achievement_freeze_saved_text,
    ),
    Achievement.RANK_HELIODOR to AchievementLabel(
        R.string.core_ui_achievement_rank_heliodor,
        R.string.core_ui_achievement_rank_heliodor_text,
    ),
    Achievement.RANK_MORGANITE to AchievementLabel(
        R.string.core_ui_achievement_rank_morganite,
        R.string.core_ui_achievement_rank_morganite_text,
    ),
    Achievement.RANK_AQUAMARINE to AchievementLabel(
        R.string.core_ui_achievement_rank_aquamarine,
        R.string.core_ui_achievement_rank_aquamarine_text,
    ),
    Achievement.RANK_EMERALD to AchievementLabel(
        R.string.core_ui_achievement_rank_emerald,
        R.string.core_ui_achievement_rank_emerald_text,
    ),
    Achievement.RANK_RED_BERYL to AchievementLabel(
        R.string.core_ui_achievement_rank_red_beryl,
        R.string.core_ui_achievement_rank_red_beryl_text,
    ),
)

/** The achievement's name. */
@StringRes
fun Achievement.titleRes(): Int = ACHIEVEMENT_LABELS.getValue(this).title

/** What it takes to unlock it. */
@StringRes
fun Achievement.textRes(): Int = ACHIEVEMENT_LABELS.getValue(this).text

package com.anadcurukovic.berylux.core.model

/**
 * The 17 achievements, in the order Progress lists them. [key] is what the database stores, so it
 * never changes, even if a name on screen does.
 */
enum class Achievement(val key: String) {
    FIRST_CLOSED("first_case"),
    CLOSED_10("closed_10"),
    CLOSED_50("closed_50"),
    CLOSED_100("closed_100"),
    FULL_DAY("full_day"),
    HARD_10("hard_10"),
    BONUS_5("bonus_5"),
    EVERY_CATEGORY("every_category"),
    OWN_CASE("own_case"),
    STREAK_7("streak_7"),
    STREAK_30("streak_30"),
    FREEZE_SAVED("freeze_saved"),
    RANK_HELIODOR("rank_heliodor"),
    RANK_MORGANITE("rank_morganite"),
    RANK_AQUAMARINE("rank_aquamarine"),
    RANK_EMERALD("rank_emerald"),
    RANK_RED_BERYL("rank_red_beryl"),
    ;

    companion object {
        fun fromKey(key: String): Achievement? = entries.firstOrNull { it.key == key }
    }
}

/** The five bots on the rivals board. Each has its own habit; see the rivals rules. */
enum class Rival(val handle: String) {
    LEDGER("Ledger"),
    FLASHBULB("Flashbulb"),
    SNOOZE("Snooze"),
    NIGHTJAR("Nightjar"),
    GRINDSTONE("Grindstone"),
}

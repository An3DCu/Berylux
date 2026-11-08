package com.anadcurukovic.berylux.core.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/**
 * Version 1 to 2:
 * - Deleting a custom challenge now deletes its cases. SQLite can't change a foreign key in place,
 *   so the cases table is copied into a new one with the new key, then swapped in.
 * - The "My cases" category is added for the player's own challenges.
 *
 * The SQL matches the exported schema in `schemas/.../2.json`; the migration test checks it.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    private val statements = listOf(
        "CREATE TABLE IF NOT EXISTS `cases_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`profile_id` INTEGER NOT NULL, `challenge_id` INTEGER NOT NULL, `status` TEXT NOT NULL, " +
            "`is_daily` INTEGER NOT NULL, `accepted_at` INTEGER NOT NULL, `accepted_on` INTEGER NOT NULL, " +
            "`closed_at` INTEGER, `closed_on` INTEGER, `lux_earned` INTEGER NOT NULL, " +
            "FOREIGN KEY(`profile_id`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
            "FOREIGN KEY(`challenge_id`) REFERENCES `challenges`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "INSERT INTO `cases_new` SELECT `id`, `profile_id`, `challenge_id`, `status`, `is_daily`, " +
            "`accepted_at`, `accepted_on`, `closed_at`, `closed_on`, `lux_earned` FROM `cases`",
        "DROP TABLE `cases`",
        "ALTER TABLE `cases_new` RENAME TO `cases`",
        "CREATE INDEX IF NOT EXISTS `index_cases_challenge_id` ON `cases` (`challenge_id`)",
        "CREATE INDEX IF NOT EXISTS `index_cases_profile_id_accepted_on` ON `cases` (`profile_id`, `accepted_on`)",
        "CREATE INDEX IF NOT EXISTS `index_cases_profile_id_closed_on` ON `cases` (`profile_id`, `closed_on`)",
        "INSERT OR IGNORE INTO `categories` (`id`, `name`, `description`, `kind`) " +
            "VALUES (8, 'My cases', 'Challenges you wrote yourself.', 'CUSTOM')",
    )

    // Used by the app, which opens the database through SupportSQLite.
    override fun migrate(db: SupportSQLiteDatabase) = statements.forEach(db::execSQL)

    // Used by the migration test, which opens it through an SQLite driver.
    override fun migrate(connection: SQLiteConnection) = statements.forEach(connection::execSQL)
}

/**
 * Version 2 to 3: the sigil gets its own seed, so redrawing it leaves the daily picks and the rival
 * bots alone. Each profile's sigil seed starts as its pick seed, so every sigil looks the same.
 */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    private val statements = listOf(
        "ALTER TABLE `profiles` ADD COLUMN `sigil_seed` INTEGER NOT NULL DEFAULT 0",
        "UPDATE `profiles` SET `sigil_seed` = `pick_seed`",
    )

    override fun migrate(db: SupportSQLiteDatabase) = statements.forEach(db::execSQL)

    override fun migrate(connection: SQLiteConnection) = statements.forEach(connection::execSQL)
}

/** Every migration, oldest first. A new version adds its migration here. */
internal val ALL_MIGRATIONS = listOf(MIGRATION_1_2, MIGRATION_2_3)

package com.anadcurukovic.berylux.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The harness for migration tests. Each new database version adds a test here that opens the old
 * exported schema, fills it, migrates and checks the data survived.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = context().getDatabasePath("migration-test.db"),
        driver = AndroidSQLiteDriver(),
        databaseClass = BeryluxDatabase::class,
    )

    @Test
    fun `version 1 can be created from its exported schema`() {
        val connection = helper.createDatabase(1)
        val statement = connection.prepare("SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'cases'")
        statement.step()
        assertThat(statement.getLong(0)).isEqualTo(1)
        statement.close()
        connection.close()
    }
}

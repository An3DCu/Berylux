package com.anadcurukovic.berylux.core.database

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeedCallbackTest {
    private val fileName = "seed-test.db"

    @After
    fun deleteFile() {
        context().deleteDatabase(fileName)
    }

    @Test
    fun `the first query already sees the whole seed`() = runTest {
        val db = seededInMemoryDatabase()
        assertThat(db.categoryDao().observeAll().first()).hasSize(8)
        val challenges = db.challengeDao().observeAll().first()
        assertThat(challenges).hasSize(56)
        assertThat(challenges.first().title).isEqualTo("Stretch break")
        assertThat(challenges.none { it.isCustom }).isTrue()
        db.close()
    }

    @Test
    fun `the seed is written once, not on every start`() = runTest {
        val context = context()
        fun open() = Room.databaseBuilder(context, BeryluxDatabase::class.java, fileName)
            .addCallback(seedCallback(context))
            .allowMainThreadQueries()
            .build()

        val first = open()
        assertThat(first.challengeDao().observeAll().first()).hasSize(56)
        first.close()

        val second = open()
        assertThat(second.challengeDao().observeAll().first()).hasSize(56)
        assertThat(second.categoryDao().observeAll().first()).hasSize(8)
        second.close()
    }
}

package com.anadcurukovic.berylux.core.database.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anadcurukovic.berylux.core.model.CategoryKind
import com.anadcurukovic.berylux.core.model.Difficulty
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

/** The starting categories and challenges, read from `assets/seed/challenges.json`. */
@Serializable
data class SeedData(
    val version: Int,
    val categories: List<SeedCategory>,
    val challenges: List<SeedChallenge>,
) {
    companion object {
        const val ASSET_PATH = "seed/challenges.json"

        private val json = Json { ignoreUnknownKeys = false }

        fun parse(input: InputStream): SeedData = json.decodeFromString(serializer(), input.bufferedReader().readText())
    }
}

@Serializable
data class SeedCategory(
    val id: Long,
    val name: String,
    val description: String,
    val kind: CategoryKind,
)

@Serializable
data class SeedChallenge(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
)

/**
 * Writes the seed when the database file is first created. It runs inside Room's create
 * transaction, before any query can see the database. The first screen never finds it empty.
 */
class SeedCallback(private val loadSeed: () -> SeedData) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        write(db, loadSeed())
    }

    internal companion object {
        fun write(db: SupportSQLiteDatabase, seed: SeedData) {
            seed.categories.forEach { category ->
                val values = ContentValues().apply {
                    put("id", category.id)
                    put("name", category.name)
                    put("description", category.description)
                    put("kind", category.kind.name)
                }
                db.insert("categories", SQLiteDatabase.CONFLICT_ABORT, values)
            }
            seed.challenges.forEach { challenge ->
                val values = ContentValues().apply {
                    put("id", challenge.id)
                    put("category_id", challenge.categoryId)
                    put("title", challenge.title)
                    put("description", challenge.description)
                    put("difficulty", challenge.difficulty.name)
                    put("is_custom", 0)
                }
                db.insert("challenges", SQLiteDatabase.CONFLICT_ABORT, values)
            }
        }
    }
}

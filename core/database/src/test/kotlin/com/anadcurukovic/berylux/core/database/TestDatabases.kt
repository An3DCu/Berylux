package com.anadcurukovic.berylux.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anadcurukovic.berylux.core.database.seed.SeedCallback
import com.anadcurukovic.berylux.core.database.seed.SeedData

internal fun context(): Context = ApplicationProvider.getApplicationContext()

internal fun seedCallback(context: Context) =
    SeedCallback { context.assets.open(SeedData.ASSET_PATH).use(SeedData::parse) }

/** A fresh database in memory, seeded like the real one. */
internal fun seededInMemoryDatabase(): BeryluxDatabase {
    val context = context()
    return Room.inMemoryDatabaseBuilder(context, BeryluxDatabase::class.java)
        .addCallback(seedCallback(context))
        .allowMainThreadQueries()
        .build()
}

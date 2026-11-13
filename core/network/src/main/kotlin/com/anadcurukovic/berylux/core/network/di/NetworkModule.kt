package com.anadcurukovic.berylux.core.network.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.anadcurukovic.berylux.core.network.BuildConfig
import com.anadcurukovic.berylux.core.network.Connection
import com.anadcurukovic.berylux.core.network.KeystoreTokenCipher
import com.anadcurukovic.berylux.core.network.TokenCipher
import com.anadcurukovic.berylux.core.network.TokenFile
import com.anadcurukovic.berylux.core.network.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    /** The tokens' own file, `tokens.preferences_pb`, left out of backups (see the app's backup rules). */
    @Provides
    @Singleton
    @TokenFile
    fun tokenDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { context.preferencesDataStoreFile("tokens") },
        )

    @Provides
    @Singleton
    fun tokenCipher(): TokenCipher = KeystoreTokenCipher()

    @Provides
    @Singleton
    fun connection(tokens: TokenStore): Connection = Connection.to(BuildConfig.API_URL, tokens)
}

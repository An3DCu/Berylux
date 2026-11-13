package com.anadcurukovic.berylux.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/** A DataStore kept in memory, as in core:datastore's tests. */
internal class InMemoryPreferencesDataStore : DataStore<Preferences> {
    val state = MutableStateFlow(emptyPreferences())
    private val lock = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        lock.withLock { transform(state.value).also { state.value = it } }
}

/**
 * The same AES-256-GCM as KeystoreTokenCipher, with a key held in memory: unit tests run on the
 * JVM, which has no Android Keystore. KeystoreTokenCipherTest checks the Keystore on a device.
 */
internal class InMemoryTokenCipher : TokenCipher {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        val sealed = cipher.doFinal(plain.toByteArray())
        val ivAndSealed = ByteBuffer.allocate(cipher.iv.size + sealed.size).put(cipher.iv).put(sealed).array()
        return Base64.getEncoder().encodeToString(ivAndSealed)
    }

    // The contract KeystoreTokenCipher keeps too: anything that won't open is a GeneralSecurityException.
    override fun decrypt(sealed: String): String {
        val bytes = try {
            Base64.getDecoder().decode(sealed)
        } catch (e: IllegalArgumentException) {
            throw GeneralSecurityException("Not a sealed token", e)
        }
        if (bytes.size <= 12) throw GeneralSecurityException("Not a sealed token")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes, 0, 12))
        }
        return String(cipher.doFinal(bytes, 12, bytes.size - 12))
    }
}

package com.anadcurukovic.berylux.core.network

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/** The pair the API hands out. Never logged, never outside this module. */
internal data class Tokens(val access: String, val refresh: String)

/** Seals a token before it's stored, and opens it again. */
interface TokenCipher {
    fun encrypt(plain: String): String

    /** Throws GeneralSecurityException when the text was changed or the key is gone. */
    fun decrypt(sealed: String): String
}

/**
 * AES-256-GCM with a key that lives in the Android Keystore and never leaves it. Each token gets
 * a fresh 12-byte IV, stored in front of the ciphertext; GCM's tag catches any change to either.
 */
class KeystoreTokenCipher : TokenCipher {
    private val key: SecretKey by lazy { existingKey() ?: newKey() }

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
        val sealed = cipher.doFinal(plain.toByteArray())
        val ivAndSealed = ByteBuffer.allocate(cipher.iv.size + sealed.size).put(cipher.iv).put(sealed).array()
        return Base64.getEncoder().encodeToString(ivAndSealed)
    }

    override fun decrypt(sealed: String): String {
        val bytes = try {
            Base64.getDecoder().decode(sealed)
        } catch (e: IllegalArgumentException) {
            throw GeneralSecurityException("Not a sealed token", e)
        }
        if (bytes.size <= IV_BYTES) throw GeneralSecurityException("Not a sealed token")
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        }
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES))
    }

    private fun existingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    private fun newKey(): SecretKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
        init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "berylux.tokens"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

/** The DataStore file that holds the sealed tokens, apart from the settings. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TokenFile

/**
 * The tokens, sealed by [TokenCipher] and kept in their own DataStore file. A token that won't
 * open, after a restore to a new phone or a changed file, signs the phone out instead of failing.
 */
@Singleton
internal class TokenStore @Inject constructor(
    @param:TokenFile private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) {
    val signedIn: Flow<Boolean> = dataStore.data.map { it[REFRESH] != null }.distinctUntilChanged()

    suspend fun current(): Tokens? {
        val stored = dataStore.data.first()
        val access = stored[ACCESS]
        val refresh = stored[REFRESH]
        if (access == null || refresh == null) return null
        return try {
            Tokens(cipher.decrypt(access), cipher.decrypt(refresh))
        } catch (_: GeneralSecurityException) {
            clear()
            null
        }
    }

    suspend fun save(tokens: Tokens) {
        val access = cipher.encrypt(tokens.access)
        val refresh = cipher.encrypt(tokens.refresh)
        dataStore.edit {
            it[ACCESS] = access
            it[REFRESH] = refresh
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(ACCESS)
            it.remove(REFRESH)
        }
    }

    private companion object {
        val ACCESS = stringPreferencesKey("access")
        val REFRESH = stringPreferencesKey("refresh")
    }
}

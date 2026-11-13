package com.anadcurukovic.berylux.core.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.security.GeneralSecurityException
import java.util.Base64

/** The Keystore exists only on a device: these run on an emulator or a phone. */
@RunWith(AndroidJUnit4::class)
class KeystoreTokenCipherTest {
    private val token = "eyJhbGciOiJIUzI1NiJ9.a-token-the-api-gave"

    @Test
    fun aSealedTokenOpensAgain() {
        val cipher = KeystoreTokenCipher()
        assertThat(cipher.decrypt(cipher.encrypt(token))).isEqualTo(token)
    }

    @Test
    fun theKeyOutlivesTheCipher() {
        val sealed = KeystoreTokenCipher().encrypt(token)
        // A new instance, as after the app restarts, finds the same key in the Keystore.
        assertThat(KeystoreTokenCipher().decrypt(sealed)).isEqualTo(token)
    }

    @Test
    fun theSameTokenSealsDifferentlyEachTime() {
        val cipher = KeystoreTokenCipher()
        assertThat(cipher.encrypt(token)).isNotEqualTo(cipher.encrypt(token))
    }

    @Test
    fun theSealedTextDoesNotHoldTheToken() {
        val sealed = Base64.getDecoder().decode(KeystoreTokenCipher().encrypt(token))
        assertThat(String(sealed, Charsets.ISO_8859_1)).doesNotContain("a-token-the-api-gave")
    }

    @Test
    fun aChangedByteWontOpen() {
        val cipher = KeystoreTokenCipher()
        val bytes = Base64.getDecoder().decode(cipher.encrypt(token))
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 1).toByte()
        assertThrows(GeneralSecurityException::class.java) {
            cipher.decrypt(Base64.getEncoder().encodeToString(bytes))
        }
    }

    @Test
    fun somethingThatWasNeverSealedWontOpen() {
        val cipher = KeystoreTokenCipher()
        assertThrows(GeneralSecurityException::class.java) { cipher.decrypt("not base64 at all!") }
        assertThrows(GeneralSecurityException::class.java) { cipher.decrypt("c2hvcnQ=") }
    }
}

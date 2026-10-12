package com.rendy.customlockscreen

import com.rendy.customlockscreen.net.RemoteAuthStore
import com.rendy.customlockscreen.net.RemoteAuthStore.Companion.hexToBytesOrNull
import com.rendy.customlockscreen.net.RemoteAuthStore.Companion.toHex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteAuthStoreTest {

    private val iterations = 1_000

    @Test
    fun pbkdf2_is_deterministic_for_same_pin_and_salt() {
        val salt = "0123456789abcdef0123456789abcdef".hexToBytesOrNull()!!

        val first = RemoteAuthStore.pbkdf2("4321", salt, iterations)
        val second = RemoteAuthStore.pbkdf2("4321", salt, iterations)

        assertArrayEquals(first, second)
    }

    @Test
    fun pbkdf2_differs_for_wrong_pin_or_salt() {
        val salt = RemoteAuthStore.generateSalt()
        val otherSalt = RemoteAuthStore.generateSalt()

        val correct = RemoteAuthStore.pbkdf2("4321", salt, iterations)

        assertFalse(correct.contentEquals(RemoteAuthStore.pbkdf2("1234", salt, iterations)))
        assertFalse(correct.contentEquals(RemoteAuthStore.pbkdf2("4321", otherSalt, iterations)))
    }

    @Test
    fun salt_is_16_random_bytes() {
        val salt = RemoteAuthStore.generateSalt()

        assertEquals(16, salt.size)
        assertFalse(salt.contentEquals(RemoteAuthStore.generateSalt()))
    }

    @Test
    fun pairing_code_is_six_digits() {
        repeat(50) {
            val code = RemoteAuthStore.generatePairingCode()

            assertEquals(6, code.length)
            assertTrue("code must be numeric: $code", code.all { it.isDigit() })
        }
    }

    @Test
    fun token_is_256_bits_of_hex() {
        val token = RemoteAuthStore.generateToken()

        assertEquals(RemoteAuthStore.TOKEN_BYTES * 2, token.length)
        assertNotEquals(token, RemoteAuthStore.generateToken())
        assertTrue(token.all { it in "0123456789abcdef" })
    }

    @Test
    fun sha256_matches_known_vector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            RemoteAuthStore.sha256Hex("abc")
        )
    }

    @Test
    fun hex_round_trip() {
        val bytes = RemoteAuthStore.generateSalt()

        assertArrayEquals(bytes, bytes.toHex().hexToBytesOrNull())
    }

    @Test
    fun invalid_hex_returns_null() {
        assertEquals(null, "abc".hexToBytesOrNull())
        assertEquals(null, "zz".hexToBytesOrNull())
    }
}

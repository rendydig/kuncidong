package com.rendy.customlockscreen.net

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rendy.customlockscreen.data.PairedParent
import com.rendy.customlockscreen.data.QuestionDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** A Parent device that has completed pairing with this Kids device. */
data class StoredParent(
    val id: String,
    val name: String,
    val tokenHash: String,
    val pairedAt: Long
)

/**
 * Stores the Kids device secrets: the parent PIN (PBKDF2 hash + salt), the
 * short-lived pairing code and the per-parent auth tokens (stored hashed).
 *
 * All values live in a dedicated [SharedPreferences] file so they are not
 * exposed by the general app settings.
 */
class RemoteAuthStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Room mirror of the paired parents, used by the Kids UI for display. */
    private val remoteDao by lazy { QuestionDatabase.getDatabase(appContext).remoteDao() }
    private val mirrorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // region PIN

    fun hasPin(): Boolean = prefs.contains(KEY_PIN_HASH)

    /** Sets (or replaces) the parent PIN. */
    fun setPin(pin: String) {
        val salt = generateSalt()
        val hash = pbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt.toHex())
            .putString(KEY_PIN_HASH, hash.toHex())
            .apply()
    }

    /** Verifies [pin] against the stored PBKDF2 hash using a constant-time compare. */
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null)?.hexToBytesOrNull() ?: return false
        val salt = prefs.getString(KEY_PIN_SALT, null)?.hexToBytesOrNull() ?: return false
        val candidate = pbkdf2(pin, salt)
        return MessageDigest.isEqual(storedHash, candidate)
    }

    // endregion

    // region pairing code

    /**
     * Starts a pairing window: generates the first 6 digit code.
     * Call from `PairingActivity.onCreate`.
     */
    fun startPairingSession() {
        refreshPairingCode()
    }

    /** Rotates the pairing code and extends its validity window. */
    fun refreshPairingCode() {
        prefs.edit()
            .putString(KEY_PAIR_CODE, generatePairingCode())
            .putLong(KEY_PAIR_CODE_EXPIRES, System.currentTimeMillis() + PAIRING_CODE_TTL_MS)
            .apply()
    }

    /** Ends the pairing window; the code stops being accepted. */
    fun endPairingSession() {
        prefs.edit()
            .remove(KEY_PAIR_CODE)
            .remove(KEY_PAIR_CODE_EXPIRES)
            .apply()
    }

    /** The current code, or `null` when the pairing window is closed/expired. */
    fun currentPairingCode(): String? {
        val code = prefs.getString(KEY_PAIR_CODE, null) ?: return null
        val expires = prefs.getLong(KEY_PAIR_CODE_EXPIRES, 0L)
        return if (System.currentTimeMillis() < expires) code else null
    }

    /** Seconds remaining before the code rotates; 0 when no session is open. */
    fun pairingCodeSecondsLeft(): Long {
        val expires = prefs.getLong(KEY_PAIR_CODE_EXPIRES, 0L)
        val left = expires - System.currentTimeMillis()
        return if (left > 0) left / 1000 else 0
    }

    /**
     * Validates [code] against the active pairing window, sets the PIN when none
     * exists yet, creates an auth token and stores its hash.
     *
     * @return the freshly generated token, or `null` when the code is invalid.
     */
    fun authorizePair(code: String?, pin: String?, parentName: String?): String? {
        val expected = currentPairingCode() ?: return null
        if (code.isNullOrBlank() || code != expected) return null

        val pinValue = pin.orEmpty()
        if (pinValue.isBlank()) return null
        if (!hasPin()) {
            setPin(pinValue)
        }

        val name = parentName?.takeIf { it.isNotBlank() } ?: "Orang Tua"
        val token = generateToken()
        val parents = loadParents().toMutableList()
        parents.removeAll { it.name == name }
        parents.add(
            StoredParent(
                id = randomId(),
                name = name,
                tokenHash = sha256Hex(token),
                pairedAt = System.currentTimeMillis()
            )
        )
        saveParents(parents)
        return token
    }

    /** @return the name of the parent owning [token], or `null` when invalid. */
    fun verifyToken(token: String?): String? {
        if (token.isNullOrBlank()) return null
        val hash = sha256Hex(token)
        return loadParents().firstOrNull { MessageDigest.isEqual(it.tokenHash.toByteArray(), hash.toByteArray()) }?.name
    }

    fun listParents(): List<StoredParent> = loadParents()

    fun unpair(token: String?) {
        if (token.isNullOrBlank()) return
        val hash = sha256Hex(token)
        saveParents(loadParents().filterNot { it.tokenHash == hash })
    }

    fun unpairParent(id: String) {
        saveParents(loadParents().filterNot { it.id == id })
    }

    fun unpairAll() {
        saveParents(emptyList())
    }

    // endregion

    // region device identity

    /** A friendly, stable name for this Kids device (e.g. `Anak-Pixel-3a`). */
    fun deviceName(): String {
        val stored = prefs.getString(KEY_DEVICE_NAME, null)
        if (!stored.isNullOrBlank()) return stored
        val model = android.os.Build.MODEL?.replace(" ", "-") ?: "Android"
        val name = "Anak-$model"
        prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
        return name
    }

    // endregion

    private fun loadParents(): List<StoredParent> {
        val json = prefs.getString(KEY_PARENTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<StoredParent>>() {}.type
            gson.fromJson<List<StoredParent>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveParents(parents: List<StoredParent>) {
        prefs.edit().putString(KEY_PARENTS, gson.toJson(parents)).apply()
        // Fire-and-forget mirror into Room so the Kids UI can observe the list.
        mirrorScope.launch {
            try {
                remoteDao.deleteAllParents()
                remoteDao.insertParents(
                    parents.map { PairedParent(it.id, it.name, it.tokenHash, it.pairedAt) }
                )
            } catch (e: Exception) {
                // Mirroring is best-effort; SharedPreferences stays authoritative.
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "kuncidong_remote_auth"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PAIR_CODE = "pair_code"
        private const val KEY_PAIR_CODE_EXPIRES = "pair_code_expires"
        private const val KEY_PARENTS = "paired_parents"
        private const val KEY_DEVICE_NAME = "device_name"

        /** Pairing code is only valid while [startPairingSession] is active. */
        const val PAIRING_CODE_TTL_MS = 60_000L

        private const val PIN_ITERATIONS = 120_000
        private const val PIN_KEY_LENGTH_BITS = 256
        const val SALT_BYTES = 16
        const val TOKEN_BYTES = 32

        private val gson = Gson()
        private val secureRandom = SecureRandom()

        /** Derives the PIN hash. Pure function so it can be unit tested on the JVM. */
        fun pbkdf2(pin: String, salt: ByteArray, iterations: Int = PIN_ITERATIONS): ByteArray {
            val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PIN_KEY_LENGTH_BITS)
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        }

        fun generateSalt(): ByteArray {
            val salt = ByteArray(SALT_BYTES)
            secureRandom.nextBytes(salt)
            return salt
        }

        /** Generates a 256-bit auth token as a hex string. */
        fun generateToken(): String {
            val bytes = ByteArray(TOKEN_BYTES)
            secureRandom.nextBytes(bytes)
            return bytes.toHex()
        }

        /** Generates a 6 digit pairing code (leading zeros preserved). */
        fun generatePairingCode(): String {
            val value = secureRandom.nextInt(1_000_000)
            return value.toString().padStart(6, '0')
        }

        fun randomId(): String {
            val bytes = ByteArray(8)
            secureRandom.nextBytes(bytes)
            return bytes.toHex()
        }

        fun sha256Hex(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).toHex()

        internal fun ByteArray.toHex(): String {
            val out = StringBuilder(size * 2)
            for (b in this) {
                val i = b.toInt() and 0xFF
                out.append(HEX_CHARS[i ushr 4])
                out.append(HEX_CHARS[i and 0x0F])
            }
            return out.toString()
        }

        internal fun String.hexToBytesOrNull(): ByteArray? {
            if (length % 2 != 0) return null
            val out = ByteArray(length / 2)
            for (i in out.indices) {
                val hi = Character.digit(this[i * 2], 16)
                val lo = Character.digit(this[i * 2 + 1], 16)
                if (hi < 0 || lo < 0) return null
                out[i] = ((hi shl 4) or lo).toByte()
            }
            return out
        }

        private const val HEX_CHARS = "0123456789abcdef"
    }
}

package com.rendy.customlockscreen.lock

import android.content.Context
import android.content.SharedPreferences
import com.rendy.customlockscreen.net.RemoteAuthStore
import com.rendy.customlockscreen.net.RemoteAuthStore.Companion.hexToBytesOrNull
import com.rendy.customlockscreen.net.RemoteAuthStore.Companion.toHex
import java.security.MessageDigest

/**
 * PIN khusus aplikasi, terpisah dari "PIN Rahasia" di [RemoteAuthStore].
 * Dipakai untuk membuka app itu sendiri (bukan membuka layar kunci edukatif).
 *
 * Disimpan sebagai hash PBKDF2 + salt di prefs sendiri, per-perangkat, sehingga
 * HP Anak dan HP Orang Tua punya PIN masing-masing.
 */
class AppPinStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True bila PIN aplikasi sudah pernah dibuat di perangkat ini. */
    fun hasPin(): Boolean = prefs.contains(KEY_PIN_HASH)

    /** Membuat atau mengganti PIN aplikasi. */
    fun setPin(pin: String) {
        val salt = RemoteAuthStore.generateSalt()
        val hash = RemoteAuthStore.pbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt.toHex())
            .putString(KEY_PIN_HASH, hash.toHex())
            .apply()
    }

    /** Memverifikasi [pin] terhadap hash tersimpan dengan perbandingan constant-time. */
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null)?.hexToBytesOrNull() ?: return false
        val salt = prefs.getString(KEY_PIN_SALT, null)?.hexToBytesOrNull() ?: return false
        val candidate = RemoteAuthStore.pbkdf2(pin, salt)
        return MessageDigest.isEqual(storedHash, candidate)
    }

    /** Menghapus PIN aplikasi; app kembali bisa dibuka bebas. */
    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "kuncidong_app_lock"
        private const val KEY_PIN_SALT = "app_pin_salt"
        private const val KEY_PIN_HASH = "app_pin_hash"
    }
}

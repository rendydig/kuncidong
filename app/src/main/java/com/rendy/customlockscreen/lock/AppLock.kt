package com.rendy.customlockscreen.lock

/**
 * Sesi buka-kunci app (in-memory).
 *
 * Default terkunci. Karena hanya disimpan di memori, proses yang mati otomatis
 * kembali terkunci. Selain itu [com.rendy.customlockscreen.SmartLockApplication]
 * memanggil [lock] setiap app ditinggalkan (pindah ke background) sehingga app
 * tetap terkunci walau proses masih hidup karena foreground service.
 */
object AppLock {

    @Volatile
    private var unlocked = false

    fun isUnlocked(): Boolean = unlocked

    fun unlock() {
        unlocked = true
    }

    fun lock() {
        unlocked = false
    }
}

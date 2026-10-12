package com.rendy.customlockscreen

import com.rendy.customlockscreen.lock.AppLock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {

    @Test
    fun app_lock_defaults_to_locked_after_lock() {
        AppLock.lock()

        assertFalse(AppLock.isUnlocked())
    }

    @Test
    fun unlock_then_lock_round_trip() {
        AppLock.unlock()
        assertTrue(AppLock.isUnlocked())

        AppLock.lock()
        assertFalse(AppLock.isUnlocked())
    }
}

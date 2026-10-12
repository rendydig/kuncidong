package com.rendy.customlockscreen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rendy.customlockscreen.kids.KidsHomeActivity
import com.rendy.customlockscreen.lock.AppLockGate
import com.rendy.customlockscreen.parent.ParentHomeActivity

/**
 * Launcher entry point. Routes to the role picker on first launch, then to the
 * Kids or Parent home depending on the saved role.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val role = SmartLockApplication.instance.getRole()
        val target = when (role) {
            SmartLockApplication.ROLE_KIDS -> KidsHomeActivity::class.java
            SmartLockApplication.ROLE_PARENT -> ParentHomeActivity::class.java
            else -> RoleSelectionActivity::class.java
        }

        if (role != SmartLockApplication.ROLE_UNSET &&
            AppLockGate.requireUnlock(this, target)
        ) {
            return
        }

        startActivity(Intent(this, target))
        finish()
    }
}

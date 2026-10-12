package com.rendy.customlockscreen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.rendy.customlockscreen.kids.KidsHomeActivity
import com.rendy.customlockscreen.lock.AppLockGate
import com.rendy.customlockscreen.parent.ParentHomeActivity

/**
 * First launch screen where the user picks whether this phone is the Parent or
 * the Kids device. The choice can be changed later from settings.
 */
class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        findViewById<MaterialCardView>(R.id.cardParent).setOnClickListener {
            chooseRole(SmartLockApplication.ROLE_PARENT, ParentHomeActivity::class.java)
        }

        findViewById<MaterialCardView>(R.id.cardKids).setOnClickListener {
            chooseRole(SmartLockApplication.ROLE_KIDS, KidsHomeActivity::class.java)
        }
    }

    private fun chooseRole(role: String, target: Class<*>) {
        SmartLockApplication.instance.setRole(role)
        if (AppLockGate.requireUnlock(this, target)) return
        startActivity(Intent(this, target))
        finish()
    }
}

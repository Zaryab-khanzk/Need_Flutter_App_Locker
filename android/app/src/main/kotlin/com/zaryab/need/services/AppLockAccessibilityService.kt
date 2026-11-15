package com.zaryab.need.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.zaryab.need.state.LockSessionManager
import com.zaryab.need.ui.LockActivity

/** Detects foreground app changes and launches the native lock screen. */
class AppLockAccessibilityService : AccessibilityService() {
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                LockSessionManager.clearSession()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        LockSessionManager.initialize(this)
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return
        LockSessionManager.onForegroundPackage(packageName)
        if (packageName == "com.google.android.apps.nexuslauncher" ||
            !LockSessionManager.isLocked(packageName) ||
            LockSessionManager.isUnlocked(packageName) ||
            LockSessionManager.isLockActivityVisible()
        ) {
            return
        }
        startActivity(
            Intent(this, LockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(LockActivity.EXTRA_TARGET_PACKAGE, packageName)
            },
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: IllegalArgumentException) {
            // The receiver may already be unregistered during service teardown.
        }
        LockSessionManager.clearSession()
        super.onDestroy()
    }
}

package com.zaryab.need.services

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.zaryab.need.state.LockSessionManager
import com.zaryab.need.ui.LockActivity

/** Detects foreground transitions and invokes native authentication when required. */
class AppLockAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var launcherOverviewActive = false
    private var lastUsageQuery = System.currentTimeMillis()
    private val usageStats by lazy {
        getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    }
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                LockSessionManager.clearSession()
                launcherOverviewActive = false
            }
        }
    }
    private val usagePoll = object : Runnable {
        override fun run() {
            purgeExplicitRecentsRemoval()
            handler.postDelayed(this, 750L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        LockSessionManager.initialize(this)
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        handler.post(usagePoll)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null ||
                event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            ) return
            val packageName = event.packageName?.toString() ?: return
            if (packageName == "com.google.android.apps.nexuslauncher") {
                launcherOverviewActive = event.className?.toString()?.contains(
                    "Recents",
                    ignoreCase = true,
                ) == true
            } else if (packageName != this.packageName &&
                packageName != "com.android.systemui" &&
                !packageName.contains("inputmethod", ignoreCase = true) &&
                !packageName.contains("keyboard", ignoreCase = true)
            ) {
                launcherOverviewActive = false
            }
            if (packageName == this.packageName ||
                packageName == "com.android.systemui" ||
                packageName.contains("inputmethod", ignoreCase = true) ||
                packageName.contains("keyboard", ignoreCase = true)
            ) return
            val shouldLock =
                LockSessionManager.onForegroundPackageChanged(this, packageName)
            val isAppLocked = LockSessionManager.isAppLocked(this, packageName)
            val overlayGranted = Settings.canDrawOverlays(this)
            Log.d(
                "NeedLock",
                "Detected package=$packageName isAppLocked=$isAppLocked " +
                    "shouldLock=$shouldLock overlayGranted=$overlayGranted",
            )
            if (!shouldLock ||
                LockSessionManager.isLockActivityVisible() ||
                LockSessionManager.shouldDebounceLockLaunch(packageName)
            ) return
            launchLockActivity(packageName)
        } catch (throwable: Throwable) {
            Log.e("NeedLock", "Accessibility error", throwable)
        }
    }

    private fun launchLockActivity(packageName: String) {
        try {
            LockSessionManager.recordLockLaunch(packageName)
            val lockIntent = Intent(this, LockActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION,
                )
                putExtra(LockActivity.EXTRA_TARGET_PACKAGE, packageName)
            }
            Log.d("NeedLock", "Executing startActivity(LockActivity) for $packageName")
            startActivity(
                lockIntent,
                ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle(),
            )
        } catch (throwable: Throwable) {
            Log.e("NeedLock", "Unable to launch LockActivity", throwable)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(usagePoll)
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: IllegalArgumentException) {
            // The receiver may already be unregistered during service teardown.
        }
        LockSessionManager.clearSession()
        super.onDestroy()
    }

    private fun purgeExplicitRecentsRemoval() {
        if (!launcherOverviewActive) return
        val now = System.currentTimeMillis()
        val events = usageStats.queryEvents(lastUsageQuery, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == TASK_REMOVED_EVENT &&
                event.packageName != packageName &&
                event.packageName != "com.google.android.apps.nexuslauncher"
            ) {
                Log.d("NeedLock", "Purging recents-removed task ${event.packageName}")
                LockSessionManager.purgePackage(event.packageName)
            }
        }
        lastUsageQuery = now
    }

    private companion object {
        const val TASK_REMOVED_EVENT = 23
    }
}

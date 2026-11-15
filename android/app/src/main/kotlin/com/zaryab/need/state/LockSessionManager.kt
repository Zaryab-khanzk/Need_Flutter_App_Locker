package com.zaryab.need.state

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Base64
import org.json.JSONArray
import org.json.JSONException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/** Manages policies and the deterministic active-app 60-second grace state machine. */
object LockSessionManager {
    private const val PREFERENCES_NAME = "FlutterSharedPreferences"
    private const val LOCKED_KEY = "flutter.locked_packages"
    private const val MASKED_KEY = "flutter.masked_packages"
    private const val LOCKED_CSV_KEY = "flutter.locked_packages_csv"
    private const val MASKED_CSV_KEY = "flutter.masked_packages_csv"
    private const val NEED_PACKAGE = "com.zaryab.need"
    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    private const val LAUNCHER_PACKAGE = "com.google.android.apps.nexuslauncher"
    private const val GRACE_PERIOD_MS = 30_000L
    private const val LOCK_LAUNCH_DEBOUNCE_MS = 800L

    private var preferences: SharedPreferences? = null
    private val stateLock = Any()
    private var lastLockLaunchPackage: String? = null
    private var lastLockLaunchTime = 0L

    /** The locked package currently in the foreground, if any. */
    @Volatile
    var activeUnlockedPackage: String? = null
        private set

    /** Exit times used to allow a protected app to resume within 30 seconds. */
    val graceExitTimestamps = ConcurrentHashMap<String, Long>()

    @Volatile
    private var lockActivityVisible = false

    /** Initializes the shared preference source used by native services. */
    fun initialize(context: Context) {
        synchronized(stateLock) {
            if (preferences == null) {
                preferences = context.applicationContext.getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                )
            }
        }
    }

    /** Returns whether [packageName] is configured for app locking. */
    fun isAppLocked(context: Context, packageName: String): Boolean {
        initialize(context)
        val normalized = packageName.trim().lowercase()
        return readCsv(LOCKED_CSV_KEY).contains(normalized) ||
            readStringSet(LOCKED_KEY).contains(normalized)
    }

    /** Returns whether [packageName] is configured for notification masking. */
    fun isPackageMasked(context: Context, packageName: String): Boolean {
        initialize(context)
        return readCsv(MASKED_CSV_KEY).contains(packageName) ||
            readStringSet(MASKED_KEY).contains(packageName)
    }

    /** Marks a package authenticated and makes it the active protected app. */
    fun markUnlocked(packageName: String) {
        val now = SystemClock.elapsedRealtime()
        activeUnlockedPackage = packageName
        graceExitTimestamps[packageName] = now
    }

    /**
     * Applies the foreground transition state machine.
     *
     * A normal Home/app switch only records an exit timestamp. A return inside
     * the 30-second grace window
     * [GRACE_PERIOD_MS] restores the active package without prompting.
     *
     * @return true when the caller must launch [LockActivity].
     */
    fun onForegroundPackageChanged(context: Context, newPackage: String): Boolean {
        if (isIgnoredPackage(newPackage)) return false
        val now = SystemClock.elapsedRealtime()
        val previous = activeUnlockedPackage
        if (previous != null && previous != newPackage) {
            graceExitTimestamps[previous] = now
            activeUnlockedPackage = null
        }
        if (!isAppLocked(context, newPackage)) return false
        if (newPackage == activeUnlockedPackage) return false

        val exitTime = graceExitTimestamps[newPackage]
        if (exitTime != null && now - exitTime < GRACE_PERIOD_MS) {
            activeUnlockedPackage = newPackage
            graceExitTimestamps[newPackage] = now
            return false
        }
        graceExitTimestamps.remove(newPackage)
        return true
    }

    /** Returns true when a duplicate lock launch should be suppressed briefly. */
    fun shouldDebounceLockLaunch(packageName: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        return synchronized(stateLock) {
            lastLockLaunchPackage == packageName &&
                now - lastLockLaunchTime < LOCK_LAUNCH_DEBOUNCE_MS
        }
    }

    /** Records a lock activity launch for duplicate-event suppression. */
    fun recordLockLaunch(packageName: String) {
        synchronized(stateLock) {
            lastLockLaunchPackage = packageName
            lastLockLaunchTime = SystemClock.elapsedRealtime()
        }
    }

    /** Removes only a package explicitly destroyed from the launcher recents UI. */
    fun purgePackage(packageName: String) {
        graceExitTimestamps.remove(packageName)
        if (activeUnlockedPackage == packageName) activeUnlockedPackage = null
    }

    /** Clears all active and grace state when the device screen turns off. */
    fun clearSession() {
        activeUnlockedPackage = null
        graceExitTimestamps.clear()
        synchronized(stateLock) {
            lastLockLaunchPackage = null
            lastLockLaunchTime = 0L
        }
    }

    /** Prevents duplicate lock activities while a biometric prompt is visible. */
    fun setLockActivityVisible(visible: Boolean) {
        lockActivityVisible = visible
    }

    /** Returns whether the native lock activity is visible. */
    fun isLockActivityVisible(): Boolean = lockActivityVisible

    private fun isIgnoredPackage(packageName: String): Boolean {
        return packageName == NEED_PACKAGE ||
            packageName == SYSTEM_UI_PACKAGE ||
            packageName.contains("inputmethod", ignoreCase = true) ||
            packageName.contains("keyboard", ignoreCase = true)
    }

    private fun readCsv(key: String): Set<String> {
        val value = preferences?.getString(key, null) ?: return emptySet()
        return value.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun readStringSet(key: String): Set<String> {
        val value = preferences?.all?.get(key) ?: return emptySet()
        if (value is Set<*>) {
            return value.filterIsInstance<String>()
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .toSet()
        }
        if (value !is String) return emptySet()
        val candidates = buildList {
            add(value)
            decodeBase64(value)?.let(::add)
            if (value.startsWith("base64:")) {
                add(value.removePrefix("base64:"))
                decodeBase64(value.removePrefix("base64:"))?.let(::add)
            }
        }
        candidates.forEach { candidate ->
            parseJson(candidate)?.let {
                return it.map { item -> item.trim().lowercase() }
                    .filter { item -> item.isNotBlank() }
                    .toSet()
            }
        }
        return emptySet()
    }

    private fun parseJson(value: String): List<String>? {
        return try {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) {
                    if (!array.isNull(index)) add(array.getString(index))
                }
            }
        } catch (_: JSONException) {
            null
        }
    }

    private fun decodeBase64(value: String): String? {
        return try {
            String(Base64.decode(value, Base64.DEFAULT), StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

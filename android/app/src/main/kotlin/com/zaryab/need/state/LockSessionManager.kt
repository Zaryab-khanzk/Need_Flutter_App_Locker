package com.zaryab.need.state

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONArray
import org.json.JSONException
import java.nio.charset.StandardCharsets

/** Thread-safe process state and Flutter preference reader for app policies. */
object LockSessionManager {
    private const val PREFERENCES_NAME = "FlutterSharedPreferences"
    private const val LOCKED_KEY = "flutter.locked_packages"
    private const val MASKED_KEY = "flutter.masked_packages"

    private val lock = Any()
    private var preferences: SharedPreferences? = null
    private val unlockedPackages = mutableSetOf<String>()
    private var foregroundPackage: String? = null
    private var lockActivityVisible = false

    /** Initializes the preference source used by the native services. */
    fun initialize(context: Context) {
        synchronized(lock) {
            if (preferences == null) {
                preferences = context.applicationContext.getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                )
            }
        }
    }

    /** Returns true when [packageName] is configured for launch protection. */
    fun isLocked(packageName: String): Boolean = synchronized(lock) {
        readStringSet(LOCKED_KEY).contains(packageName)
    }

    /** Returns true when [packageName] is configured for notification masking. */
    fun isMasked(packageName: String): Boolean = synchronized(lock) {
        readStringSet(MASKED_KEY).contains(packageName)
    }

    /** Returns true when this process has authenticated [packageName]. */
    fun isUnlocked(packageName: String): Boolean = synchronized(lock) {
        unlockedPackages.contains(packageName)
    }

    /** Marks [packageName] as authenticated for the current foreground session. */
    fun markUnlocked(packageName: String) {
        synchronized(lock) {
            unlockedPackages.add(packageName)
            foregroundPackage = packageName
        }
    }

    /** Records the foreground package and clears sessions on app switches. */
    fun onForegroundPackage(packageName: String) {
        synchronized(lock) {
            if (packageName == foregroundPackage) return
            if (packageName == "com.google.android.apps.nexuslauncher" ||
                foregroundPackage != null
            ) {
                unlockedPackages.clear()
            }
            foregroundPackage = packageName
        }
    }

    /** Clears all authenticated packages, normally on screen-off. */
    fun clearSession() {
        synchronized(lock) {
            unlockedPackages.clear()
            foregroundPackage = null
        }
    }

    /** Prevents duplicate lock activities while a prompt is visible. */
    fun setLockActivityVisible(visible: Boolean) {
        synchronized(lock) {
            lockActivityVisible = visible
        }
    }

    /** Returns whether the native authentication screen is currently visible. */
    fun isLockActivityVisible(): Boolean = synchronized(lock) {
        lockActivityVisible
    }

    private fun readStringSet(key: String): Set<String> {
        val value = preferences?.all?.get(key) ?: return emptySet()
        if (value is Set<*>) return value.filterIsInstance<String>().toSet()
        if (value !is String) return emptySet()
        return decodeStringList(value)
    }

    private fun decodeStringList(value: String): Set<String> {
        val candidates = buildList {
            add(value)
            if (value.startsWith("base64:")) add(value.removePrefix("base64:"))
            decodeBase64(value)?.let(::add)
            if (value.startsWith("base64:")) {
                decodeBase64(value.removePrefix("base64:"))?.let(::add)
            }
        }
        candidates.forEach { candidate ->
            parseJsonArray(candidate)?.let { return it.toSet() }
        }
        return emptySet()
    }

    private fun parseJsonArray(value: String): List<String>? {
        return try {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) {
                    if (!array.isNull(index)) {
                        val item = array.getString(index)
                        add(
                            if (item.startsWith("base64:")) {
                                decodeBase64(item.removePrefix("base64:")) ?: item
                            } else {
                                item
                            },
                        )
                    }
                }
            }
        } catch (_: JSONException) {
            null
        }
    }

    private fun decodeBase64(value: String): String? {
        return try {
            String(
                Base64.decode(value, Base64.DEFAULT),
                StandardCharsets.UTF_8,
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

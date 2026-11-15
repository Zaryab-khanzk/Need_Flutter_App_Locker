package com.zaryab.need.bridge

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.zaryab.need.services.AppLockAccessibilityService
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/** Implements the Flutter settings channel using live Android state. */
class SettingsMethodHandler(
    private val context: Context,
) : MethodChannel.MethodCallHandler {
    override fun onMethodCall(
        call: MethodCall,
        result: MethodChannel.Result,
    ) {
        when (call.method) {
            GET_INSTALLED_APPS -> result.success(installedApps())
            GET_PERMISSION_STATUS -> result.success(permissionStatus())
            OPEN_USAGE_ACCESS -> openSettings(
                result,
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            )
            OPEN_NOTIFICATION_ACCESS -> openSettings(
                result,
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            )
            OPEN_OVERLAY -> openSettings(
                result,
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
            OPEN_ACCESSIBILITY -> openSettings(
                result,
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            )
            REQUEST_NOTIFICATION_PERMISSION -> requestNotificationPermission(result)
            else -> result.notImplemented()
        }
    }

    private fun installedApps(): List<Map<String, Any>> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return context.packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.loadLabel(context.packageManager).toString().lowercase() }
            .map {
                mapOf(
                    "name" to it.loadLabel(context.packageManager).toString(),
                    "packageName" to it.packageName,
                    "iconBytes" to drawableToPng(it.loadIcon(context.packageManager)),
                )
            }
            .toList()
    }

    private fun permissionStatus(): Map<String, Boolean> = mapOf(
        "usageAccess" to hasUsageAccess(),
        "notificationAccess" to hasNotificationAccess(),
        "overlay" to Settings.canDrawOverlays(context),
        "accessibility" to hasAccessibilityAccess(),
        "postNotifications" to hasPostNotificationsPermission(),
    )

    private fun hasPostNotificationsPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermission(result: MethodChannel.Result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            result.success(null)
            return
        }
        if (context !is android.app.Activity) {
            result.error(
                "permission_unavailable",
                "Notification permission requires an active Need activity.",
                null,
            )
            return
        }
        context.requestPermissions(
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE,
        )
        result.success(null)
    }

    private fun drawableToPng(drawable: android.graphics.drawable.Drawable): ByteArray {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return java.io.ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        return enabled.split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it.packageName == context.packageName }
    }

    private fun hasAccessibilityAccess(): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as AccessibilityManager
        return manager.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
        ).any {
            it.resolveInfo?.serviceInfo?.let { service ->
                service.packageName == context.packageName &&
                    service.name == AppLockAccessibilityService::class.java.name
            } == true
        }
    }

    private fun openSettings(result: MethodChannel.Result, intent: Intent) {
        try {
            context.startActivity(intent)
            result.success(null)
        } catch (exception: Exception) {
            result.error(
                "settings_unavailable",
                "Unable to open Android settings.",
                exception,
            )
        }
    }

    companion object {
        const val CHANNEL_NAME = "com.zaryab.need/settings"
        private const val GET_INSTALLED_APPS = "getInstalledApps"
        private const val GET_PERMISSION_STATUS = "getPermissionStatus"
        private const val OPEN_USAGE_ACCESS = "openUsageAccessSettings"
        private const val OPEN_NOTIFICATION_ACCESS = "openNotificationAccessSettings"
        private const val OPEN_OVERLAY = "openOverlaySettings"
        private const val OPEN_ACCESSIBILITY = "openAccessibilitySettings"
        private const val REQUEST_NOTIFICATION_PERMISSION =
            "requestNotificationPermission"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}

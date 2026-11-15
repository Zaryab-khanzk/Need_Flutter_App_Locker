package com.zaryab.need.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zaryab.need.R
import com.zaryab.need.state.LockSessionManager

/** Cancels sensitive notifications and posts sanitized Need replacements. */
class AppLockNotificationListener : NotificationListenerService() {
    private val counts = mutableMapOf<String, Int>()
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return
            counts.remove(packageName)
            cancel(packageName)
        }
    }

    override fun onCreate() {
        super.onCreate()
        LockSessionManager.initialize(this)
        createChannel()
        registerReceiver(
            unlockReceiver,
            IntentFilter(ACTION_APP_UNLOCKED),
            RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName == this.packageName ||
            !LockSessionManager.isPackageMasked(this, packageName) ||
            sbn.isOngoing ||
            (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        ) {
            return
        }
        cancelNotification(sbn.key)
        val count = (counts[packageName] ?: 0) + 1
        counts[packageName] = count
        postReplacement(packageName, count)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == this.packageName) {
            counts.keys.firstOrNull { it.hashCode() == sbn.id }?.let {
                counts.remove(it)
            }
            return
        }
        // The source notification is cancelled as part of masking. The
        // sanitized Need notification is the dismissal signal.
    }

    private fun postReplacement(packageName: String, count: Int) {
        val label = packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(packageName, 0),
        ).toString()
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Color.WHITE)
            .setContentTitle(label)
            .setContentText(
                if (count == 1) "1 new message received"
                else "$count new messages received",
            )
            .setAutoCancel(true)
            .setGroup("need.masked.$packageName")
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (launchIntent != null) {
            builder.setContentIntent(
                android.app.PendingIntent.getActivity(
                    this,
                    packageName.hashCode(),
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        android.app.PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this)
                .notify(packageName.hashCode(), builder.build())
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.masked_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.masked_notification_channel_description)
            },
        )
    }

    private fun cancel(packageName: String) {
        NotificationManagerCompat.from(this).cancel(packageName.hashCode())
    }

    override fun onDestroy() {
        unregisterReceiver(unlockReceiver)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "need_masked_notifications"
        const val ACTION_APP_UNLOCKED = "com.zaryab.need.APP_UNLOCKED"
        const val EXTRA_PACKAGE = "package_name"
    }
}

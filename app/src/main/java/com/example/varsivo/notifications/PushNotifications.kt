package com.example.varsivo.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.varsivo.MainActivity
import com.example.varsivo.R
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Push notifications with Firebase Cloud Messaging (FCM).
 *  - Every phone subscribes to the "bursary-alerts" topic while notifications are switched on in
 *    Settings, so one message sent from the Firebase console reaches every student in real time.
 *  - Messages are shown on the "Varsivo alerts" notification channel.
 *
 * Reference: Firebase (2026) *Send messages to topics on Android*
 * https://firebase.google.com/docs/cloud-messaging/android/topic-messaging
 */
object PushNotifications {

    const val CHANNEL_ID = "varsivo_alerts"
    const val TOPIC = "bursary-alerts"
    private const val TAG = "PushNotifications"

    /** Android 8+ needs a channel before any notification can be shown. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.notification_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Subscribes to (or leaves) the alerts topic to match the Settings switch. */
    fun applyPreference(enabled: Boolean) {
        try {
            val messaging = FirebaseMessaging.getInstance()
            val task = if (enabled) messaging.subscribeToTopic(TOPIC) else messaging.unsubscribeFromTopic(TOPIC)
            task.addOnCompleteListener { result ->
                Log.i(TAG, "Topic $TOPIC subscribed=$enabled, success=${result.isSuccessful}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not change the topic subscription: ${e.message}")
        }
    }

    /** True when the app may show notifications (Android 13+ asks the user first). */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(context: Context, title: String, body: String) {
        if (!hasPermission(context)) {
            Log.w(TAG, "Notification not shown: permission not granted")
            return
        }
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
            Log.i(TAG, "Notification shown: $title")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification blocked: ${e.message}")
        }
    }
}

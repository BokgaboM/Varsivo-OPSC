package com.example.varsivo.notifications

import android.util.Log
import com.example.varsivo.R
import com.example.varsivo.VarsivoApplication
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives push messages from Firebase Cloud Messaging while the app is open and shows them.
 * (When the app is closed, Android shows FCM notification messages by itself.)
 *
 * Reference: Firebase (2026) *Receive messages in an Android app*
 * https://firebase.google.com/docs/cloud-messaging/android/receive
 */
class VarsivoMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val userStore = (application as VarsivoApplication).userStore
        if (!userStore.notificationsEnabled) {
            Log.i(TAG, "Message ignored: notifications are switched off in Settings")
            return
        }
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        Log.i(TAG, "Push message received: $title")
        PushNotifications.show(this, title, body)
    }

    /** A new device token: stored on the student's Firestore profile so messages can target this phone. */
    override fun onNewToken(token: String) {
        Log.i(TAG, "New FCM token received")
        val uid = (application as VarsivoApplication).userStore.remoteId ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "Could not save the FCM token: ${e.message}") }
    }

    private companion object {
        const val TAG = "VarsivoMessaging"
    }
}

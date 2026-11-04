package com.example.varsivo.notifications

import android.content.Context
import android.util.Log
import com.example.varsivo.R
import com.example.varsivo.VarsivoApplication
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Automatic real-time alerts, on top of the Firebase Cloud Messaging topic in [PushNotifications].
 *
 * Firestore snapshot listeners fire within a second of a change in the database, so the student
 * is alerted the moment something happens, without anyone sending a message by hand:
 *  - **New bursary:** a document added to the "bursaries" collection.
 *  - **Application update:** an application's status changed in the database from somewhere other
 *    than this phone (for example the university updating it, or the student's other device).
 * Nothing is shown while notifications are switched off in Settings.
 *
 * Reference: Firebase (2026) *Get realtime updates with Cloud Firestore*
 * https://firebase.google.com/docs/firestore/query-data/listen (Accessed: 4 November 2026).
 */
object RealtimeAlerts {

    private const val TAG = "RealtimeAlerts"

    private var bursaryListener: ListenerRegistration? = null
    private var applicationListener: ListenerRegistration? = null
    private var listeningUid: String? = null

    /** Last status seen for each application, so a real change can be told apart from a re-read. */
    private val knownStatuses = mutableMapOf<String, String>()

    /** Starts listening (safe to call every time Home opens). [uid] is the signed-in student. */
    fun start(context: Context, uid: String?) {
        val app = context.applicationContext
        val db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore not available, real-time alerts off: ${e.message}")
            return
        }

        if (bursaryListener == null) {
            var firstSnapshot = true
            bursaryListener = db.collection("bursaries").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Bursary listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                // The first snapshot is the existing list, not new bursaries.
                if (firstSnapshot) {
                    firstSnapshot = false
                    return@addSnapshotListener
                }
                // Written by this phone (e.g. the first-run seed): not news for this student.
                if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener

                snapshot.documentChanges
                    .filter { it.type == DocumentChange.Type.ADDED }
                    .forEach { change ->
                        val name = change.document.getString("name").orEmpty()
                        if (name.isBlank() || !enabled(app)) return@forEach
                        val closing = change.document.getString("closingDate").orEmpty()
                        Log.i(TAG, "New bursary in Firestore: $name")
                        PushNotifications.show(
                            app,
                            app.getString(R.string.alert_new_bursary_title, name),
                            if (closing.isBlank()) app.getString(R.string.alert_new_bursary_body)
                            else app.getString(R.string.alert_new_bursary_body_closing, closing)
                        )
                    }
            }
            Log.i(TAG, "Listening for new bursaries")
        }

        if (uid != null && uid != listeningUid) {
            applicationListener?.remove()
            knownStatuses.clear()
            listeningUid = uid
            applicationListener = db.collection("users").document(uid).collection("applications")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Application listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    snapshot?.documentChanges?.forEach { change ->
                        val id = change.document.id
                        val status = change.document.getString("status")
                        val previous = knownStatuses[id]
                        if (status != null) knownStatuses[id] = status

                        val changedElsewhere = !change.document.metadata.hasPendingWrites()
                        if (change.type == DocumentChange.Type.MODIFIED && changedElsewhere &&
                            RealtimeAlertRules.isNewStatus(previous, status) && enabled(app)
                        ) {
                            val institution = change.document.getString("institution").orEmpty()
                            val programme = change.document.getString("programme").orEmpty()
                            Log.i(TAG, "Application $id status $previous -> $status")
                            PushNotifications.show(
                                app,
                                app.getString(R.string.alert_application_title),
                                app.getString(R.string.alert_application_body, institution, programme, statusLabel(app, status))
                            )
                        }
                    }
                }
            Log.i(TAG, "Listening for application updates")
        }
    }

    /** Stops every listener (on logout). */
    fun stop() {
        bursaryListener?.remove()
        applicationListener?.remove()
        bursaryListener = null
        applicationListener = null
        listeningUid = null
        knownStatuses.clear()
        Log.i(TAG, "Real-time alerts stopped")
    }

    private fun enabled(context: Context): Boolean =
        (context as? VarsivoApplication)?.userStore?.notificationsEnabled ?: true

    private fun statusLabel(context: Context, status: String?): String = when (status) {
        "PENDING" -> context.getString(R.string.status_pending)
        "SUBMITTED" -> context.getString(R.string.status_submitted)
        "OFFER" -> context.getString(R.string.status_offer)
        else -> status.orEmpty()
    }
}

/** The decision rules, kept free of Android/Firebase classes so they can be unit tested. */
object RealtimeAlertRules {
    /** True only for a real change between two known statuses (not the first read). */
    fun isNewStatus(previous: String?, current: String?): Boolean =
        previous != null && current != null && previous != current
}

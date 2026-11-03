package com.example.varsivo.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** The online copy of the tracker. Firestore is the real one; tests can use a fake. */
interface ApplicationRemote {
    suspend fun upsert(uid: String, app: TrackedApplication)
    suspend fun delete(uid: String, remoteId: String)
}

/**
 * Stores each application in Cloud Firestore at "users/{uid}/applications/{remoteId}".
 * set() with the same id is safe to repeat, so a retry never creates duplicates.
 *
 * Reference: Firebase (2026) *Add data to Cloud Firestore*
 * https://firebase.google.com/docs/firestore/manage-data/add-data
 */
class FirestoreApplicationRemote : ApplicationRemote {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun doc(uid: String, remoteId: String) =
        db.collection("users").document(uid).collection("applications").document(remoteId)

    override suspend fun upsert(uid: String, app: TrackedApplication) {
        val data = mapOf(
            "type" to app.type.name,
            "institution" to app.institution,
            "programme" to app.programme,
            "status" to app.status.name,
            "applicantName" to app.applicantName,
            "email" to app.email,
            "createdAt" to app.createdAt,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc(uid, app.remoteId).set(data).await()
    }

    override suspend fun delete(uid: String, remoteId: String) {
        doc(uid, remoteId).delete().await()
    }
}

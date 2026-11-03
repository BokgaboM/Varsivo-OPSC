package com.example.varsivo.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * UniAssist (premium tier) is locked until the student verifies a university email (.ac.za).
 * The result is saved on the phone and on the student's Firestore profile (users/{uid}), and is
 * read back from Firestore so it also unlocks on a new phone.
 */
class UniAssistRepository(private val userStore: UserStore) {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** Saves the verified email. Returns true when Firestore confirmed it. */
    suspend fun saveVerified(studentEmail: String): Boolean {
        userStore.studentEmail = studentEmail
        userStore.uniAssistVerified = true
        val uid = userStore.remoteId ?: return false
        return try {
            withTimeoutOrNull(TIMEOUT_MS) {
                db.collection("users").document(uid).set(
                    mapOf(
                        "uniAssistVerified" to true,
                        "studentEmail" to studentEmail,
                        "verifiedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
                true
            } ?: false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Verification not synced yet: ${e.message}")
            false
        }
    }

    /** Reads the verification flag from Firestore (falls back to the phone's copy). */
    suspend fun isVerified(): Boolean {
        if (userStore.uniAssistVerified) return true
        val uid = userStore.remoteId ?: return false
        return try {
            val doc = withTimeoutOrNull(TIMEOUT_MS) { db.collection("users").document(uid).get().await() }
            val verified = doc?.getBoolean("uniAssistVerified") == true
            if (verified) {
                userStore.uniAssistVerified = true
                userStore.studentEmail = doc?.getString("studentEmail").orEmpty()
            }
            verified
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not read verification: ${e.message}")
            false
        }
    }

    private companion object {
        const val TAG = "UniAssistRepository"
        const val TIMEOUT_MS = 6_000L
    }
}

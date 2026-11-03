package com.example.varsivo.data

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Real [AuthService]:
 *  - Firebase Authentication keeps the accounts. Firebase stores passwords as salted scrypt hashes,
 *    so the plain password is never saved online.
 *  - Cloud Firestore (a NoSQL document database) keeps each profile in "users/{uid}".
 *
 * References:
 *  Firebase (2026) *Authenticate with Firebase using password-based accounts on Android*
 *  https://firebase.google.com/docs/auth/android/password-auth
 *  Firebase (2026) *Add data to Cloud Firestore* https://firebase.google.com/docs/firestore/manage-data/add-data
 */
class FirebaseAuthService : AuthService {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override suspend fun createAccount(email: String, password: String): String = mapErrors {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user?.uid ?: throw AuthFailure(AuthFailure.Reason.REJECTED)
    }

    override suspend fun signIn(email: String, password: String): String = mapErrors {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user?.uid ?: throw AuthFailure(AuthFailure.Reason.WRONG_CREDENTIALS)
    }

    override suspend fun signInWithGoogle(idToken: String): Pair<String, String> = mapErrors {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential).await().user
            ?: throw AuthFailure(AuthFailure.Reason.REJECTED)
        Pair(user.uid, user.email.orEmpty())
    }

    override suspend fun saveProfile(profile: RemoteProfile) {
        val data = mapOf(
            "email" to profile.email,
            "fullName" to profile.fullName,
            "schoolName" to profile.schoolName,
            "role" to profile.role,
            "language" to profile.language,
            "notificationsEnabled" to profile.notificationsEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        // merge = only these fields change; other fields (e.g. UniAssist verification) are kept.
        db.collection(USERS).document(profile.uid).set(data, SetOptions.merge()).await()
        Log.i(TAG, "Profile saved to Firestore for ${profile.uid}")
    }

    override suspend fun loadProfile(uid: String): RemoteProfile? {
        val doc = db.collection(USERS).document(uid).get().await()
        if (!doc.exists()) return null
        return RemoteProfile(
            uid = uid,
            email = doc.getString("email").orEmpty(),
            fullName = doc.getString("fullName").orEmpty(),
            schoolName = doc.getString("schoolName").orEmpty(),
            role = doc.getString("role") ?: UserStore.DEFAULT_ROLE,
            language = doc.getString("language") ?: UserStore.DEFAULT_LANGUAGE,
            notificationsEnabled = doc.getBoolean("notificationsEnabled") ?: true
        )
    }

    override suspend fun changePassword(email: String, currentPassword: String, newPassword: String) {
        mapErrors {
            // Signing in again proves the current password is right (Firebase also needs a recent login).
            val user = auth.signInWithEmailAndPassword(email, currentPassword).await().user
                ?: throw AuthFailure(AuthFailure.Reason.WRONG_CREDENTIALS)
            user.updatePassword(newPassword).await()
        }
        Log.i(TAG, "Password changed in Firebase Authentication")
    }

    override suspend fun sendPasswordReset(email: String) {
        mapErrors { auth.sendPasswordResetEmail(email).await() }
        Log.i(TAG, "Password reset email requested")
    }

    override fun signOut() {
        auth.signOut()
    }

    /** Turns Firebase's exceptions into the app's [AuthFailure] reasons. */
    private inline fun <T> mapErrors(block: () -> T): T {
        try {
            return block()
        } catch (e: FirebaseAuthUserCollisionException) {
            throw AuthFailure(AuthFailure.Reason.EMAIL_TAKEN, e)
        } catch (e: FirebaseAuthWeakPasswordException) {
            throw AuthFailure(AuthFailure.Reason.REJECTED, e)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw AuthFailure(AuthFailure.Reason.WRONG_CREDENTIALS, e)
        } catch (e: FirebaseAuthInvalidUserException) {
            throw AuthFailure(AuthFailure.Reason.WRONG_CREDENTIALS, e)
        } catch (e: FirebaseNetworkException) {
            throw AuthFailure(AuthFailure.Reason.NETWORK, e)
        } catch (e: FirebaseTooManyRequestsException) {
            throw AuthFailure(AuthFailure.Reason.NETWORK, e)
        }
    }

    private companion object {
        const val TAG = "FirebaseAuthService"
        const val USERS = "users"
    }
}

package com.example.varsivo.data

/**
 * A student's profile and settings as stored online (Cloud Firestore document "users/{uid}").
 * Plain Kotlin, so it can be used in JVM unit tests.
 */
data class RemoteProfile(
    val uid: String,
    val email: String,
    val fullName: String,
    val schoolName: String,
    val role: String,
    val language: String,
    val notificationsEnabled: Boolean
)

/** Why an online auth call failed. */
class AuthFailure(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason { EMAIL_TAKEN, WRONG_CREDENTIALS, REJECTED, NETWORK }
}

/**
 * The online side of sign-in. [FirebaseAuthService] is the real one (Firebase Authentication +
 * Cloud Firestore); unit tests use an in-memory fake so they run without a phone or internet.
 * Methods throw [AuthFailure] when the online service refuses the request or cannot be reached.
 */
interface AuthService {
    /** Creates the account and returns its uid. */
    suspend fun createAccount(email: String, password: String): String

    /** Signs in and returns the uid. */
    suspend fun signIn(email: String, password: String): String

    /** Signs in with a Google ID token (single sign-on). Returns the uid and the Google email. */
    suspend fun signInWithGoogle(idToken: String): Pair<String, String>

    /** Creates or updates the profile document. */
    suspend fun saveProfile(profile: RemoteProfile)

    /** Reads the profile document, or null when there is none yet. */
    suspend fun loadProfile(uid: String): RemoteProfile?

    /** Checks the current password, then replaces it. */
    suspend fun changePassword(email: String, currentPassword: String, newPassword: String)

    /** Emails a password-reset link (Firebase sends it). */
    suspend fun sendPasswordReset(email: String)

    fun signOut()
}

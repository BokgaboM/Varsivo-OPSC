package com.example.varsivo.data

import android.util.Log
import com.example.varsivo.SecurityUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class RegisterOutcome { SAVED_AND_SYNCED, ALREADY_EXISTS, REJECTED, NO_CONNECTION }

enum class LoginOutcome { SUCCESS, INVALID_CREDENTIALS, NO_CONNECTION }

enum class ChangePasswordOutcome { SUCCESS, WRONG_CURRENT_PASSWORD, NO_ACCOUNT, NO_CONNECTION }

/**
 * Registration, login, Google sign-in, settings, password change and logout.
 *
 * Online: [AuthService] (Firebase Authentication for accounts, Cloud Firestore for profiles).
 * On the phone: [AccountStore] keeps the profile, settings and a PBKDF2 hash of the password
 * ([SecurityUtils]) so the student can still log in without internet. The plain password is never
 * stored anywhere.
 *
 * Reference: OWASP (2024) *Password Storage Cheat Sheet*
 * https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
 */
class AuthRepository(
    private val userStore: AccountStore,
    private val service: AuthService
) {

    /** Creates the account online (needs internet). The student then logs in on the next screen. */
    suspend fun register(
        name: String,
        schoolName: String,
        email: String,
        password: String
    ): RegisterOutcome = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val uid = try {
            service.createAccount(cleanEmail, password)
        } catch (e: AuthFailure) {
            Log.w(TAG, "Register refused: ${e.reason}")
            return@withContext when (e.reason) {
                AuthFailure.Reason.EMAIL_TAKEN -> RegisterOutcome.ALREADY_EXISTS
                AuthFailure.Reason.NETWORK -> RegisterOutcome.NO_CONNECTION
                else -> RegisterOutcome.REJECTED
            }
        }

        val profile = RemoteProfile(
            uid = uid,
            email = cleanEmail,
            fullName = name.trim(),
            schoolName = schoolName.trim(),
            role = userStore.role,
            language = userStore.language,
            notificationsEnabled = userStore.notificationsEnabled
        )
        syncSafely("save new profile") { service.saveProfile(profile) }
        saveLocalAccount(profile.fullName, profile.schoolName, cleanEmail, password, uid)

        // Firebase signs a new account in straight away; we sign out so the student logs in themselves.
        userStore.isLoggedIn = false
        service.signOut()
        Log.i(TAG, "Account $cleanEmail created (uid $uid)")
        RegisterOutcome.SAVED_AND_SYNCED
    }

    /**
     * Logs in online and loads the profile. Without internet, the student can still log in with
     * the account last used on this phone (checked against the local PBKDF2 hash).
     */
    suspend fun login(email: String, password: String): LoginOutcome = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val uid = try {
            service.signIn(cleanEmail, password)
        } catch (e: AuthFailure) {
            Log.w(TAG, "Online login refused: ${e.reason}")
            return@withContext if (e.reason == AuthFailure.Reason.NETWORK) {
                loginOffline(cleanEmail, password)
            } else {
                finishLogin(false, cleanEmail)
            }
        }

        val profile = loadProfileSafely(uid)
        val sameAccount = userStore.studentId.equals(cleanEmail, ignoreCase = true)
        saveLocalAccount(
            name = profile?.fullName ?: if (sameAccount) userStore.name else "",
            schoolName = profile?.schoolName ?: if (sameAccount) userStore.schoolName else "",
            email = cleanEmail,
            password = password,
            uid = uid
        )
        if (profile != null) applySettings(profile)
        finishLogin(true, cleanEmail)
    }

    /**
     * Single sign-on: [idToken] comes from "Sign in with Google" (Credential Manager).
     * A profile is created the first time this Google account is used.
     */
    suspend fun loginWithGoogle(idToken: String, displayName: String?): LoginOutcome = withContext(Dispatchers.IO) {
        val (uid, email) = try {
            service.signInWithGoogle(idToken)
        } catch (e: AuthFailure) {
            Log.w(TAG, "Google sign-in refused: ${e.reason}")
            return@withContext if (e.reason == AuthFailure.Reason.NETWORK) {
                LoginOutcome.NO_CONNECTION
            } else {
                LoginOutcome.INVALID_CREDENTIALS
            }
        }

        val existing = loadProfileSafely(uid)
        val profile = existing ?: RemoteProfile(
            uid = uid,
            email = email,
            fullName = displayName.orEmpty(),
            schoolName = "",
            role = userStore.role,
            language = userStore.language,
            notificationsEnabled = userStore.notificationsEnabled
        )
        if (existing == null) syncSafely("save Google profile") { service.saveProfile(profile) }

        // Google accounts have no Varsivo password, so no local hash is kept (offline login needs email).
        userStore.saveUser(profile.fullName, profile.schoolName, email.lowercase(), "", "", uid)
        applySettings(profile)
        finishLogin(true, email)
    }

    /** Changes the password online (Firebase) and then on the phone. */
    suspend fun changePassword(current: String, new: String): ChangePasswordOutcome = withContext(Dispatchers.IO) {
        val local = userStore.getCredentials() ?: return@withContext ChangePasswordOutcome.NO_ACCOUNT
        try {
            service.changePassword(local.studentId, current, new)
        } catch (e: AuthFailure) {
            Log.w(TAG, "Password change refused: ${e.reason}")
            return@withContext when (e.reason) {
                AuthFailure.Reason.NETWORK -> ChangePasswordOutcome.NO_CONNECTION
                else -> ChangePasswordOutcome.WRONG_CURRENT_PASSWORD
            }
        }
        val salt = SecurityUtils.generateSalt()
        val hash = withContext(Dispatchers.Default) { SecurityUtils.hashPassword(new, salt) }
        userStore.updatePassword(hash, salt)
        Log.i(TAG, "Password changed for ${local.studentId}")
        ChangePasswordOutcome.SUCCESS
    }

    /**
     * Settings -> "Save changes". Saved on the phone first, then in Firestore.
     * Returns true when Firestore confirmed the save. When offline, Firestore keeps the change
     * in its local queue and uploads it automatically when the phone is back online.
     */
    suspend fun updateSettings(
        name: String,
        schoolName: String,
        language: String,
        notificationsEnabled: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        userStore.name = name.trim()
        userStore.schoolName = schoolName.trim()
        userStore.language = language
        userStore.notificationsEnabled = notificationsEnabled
        Log.i(TAG, "Settings saved on the phone: language=$language, notifications=$notificationsEnabled")

        val uid = userStore.remoteId ?: return@withContext false
        syncSafely("save settings") {
            service.saveProfile(
                RemoteProfile(
                    uid = uid,
                    email = userStore.studentId,
                    fullName = userStore.name,
                    schoolName = userStore.schoolName,
                    role = userStore.role,
                    language = language,
                    notificationsEnabled = notificationsEnabled
                )
            )
        }
    }

    /** Asks Firebase to email a reset link. Returns false when it could not be sent. */
    suspend fun sendPasswordReset(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            service.sendPasswordReset(email.trim().lowercase())
            true
        } catch (e: AuthFailure) {
            Log.w(TAG, "Password reset not sent: ${e.reason}")
            false
        }
    }

    /** Ends the session. The account stays on the phone so the student can log in again. */
    fun logout() {
        userStore.isLoggedIn = false
        service.signOut()
        Log.i(TAG, "Logged out")
    }

    /** Keeps the account on the phone with a PBKDF2 hash of the password (never the password). */
    private suspend fun saveLocalAccount(name: String, schoolName: String, email: String, password: String, uid: String) {
        val salt = SecurityUtils.generateSalt()
        val hash = withContext(Dispatchers.Default) { SecurityUtils.hashPassword(password, salt) }
        userStore.saveUser(name, schoolName, email, hash, salt, uid)
    }

    private fun applySettings(profile: RemoteProfile) {
        userStore.language = profile.language
        userStore.role = profile.role
        userStore.notificationsEnabled = profile.notificationsEnabled
    }

    private suspend fun loadProfileSafely(uid: String): RemoteProfile? = try {
        withTimeoutOrNull(SYNC_TIMEOUT_MS) { service.loadProfile(uid) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Could not load profile: ${e.message}")
        null
    }

    /** Runs an online save; never throws. False when it failed or did not finish in time. */
    private suspend fun syncSafely(what: String, block: suspend () -> Unit): Boolean = try {
        val done = withTimeoutOrNull(SYNC_TIMEOUT_MS) {
            block()
            true
        } ?: false
        if (!done) Log.w(TAG, "$what: not confirmed yet, will sync when online")
        done
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "$what failed: ${e.message}")
        false
    }

    private suspend fun loginOffline(email: String, password: String): LoginOutcome {
        val local = userStore.getCredentials()
        if (local == null || !local.studentId.equals(email, ignoreCase = true)) {
            return LoginOutcome.NO_CONNECTION
        }
        val ok = withContext(Dispatchers.Default) {
            SecurityUtils.verifyPassword(password, local.passwordHash, local.salt)
        }
        Log.i(TAG, "Offline login attempt for $email: ok=$ok")
        return finishLogin(ok, email)
    }

    private fun finishLogin(ok: Boolean, email: String): LoginOutcome {
        return if (ok) {
            userStore.isLoggedIn = true
            Log.i(TAG, "Login success for $email")
            LoginOutcome.SUCCESS
        } else {
            Log.w(TAG, "Login failed: wrong email or password for $email")
            LoginOutcome.INVALID_CREDENTIALS
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
        const val SYNC_TIMEOUT_MS = 8_000L
    }
}

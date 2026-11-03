package com.example.varsivo.data

/**
 * What [AuthRepository] needs from local storage. [UserStore] (SharedPreferences) is the real
 * implementation; the unit tests use a simple in-memory fake, so login / registration logic can be
 * tested on the JVM without an Android device.
 *
 * [studentId] holds the account's login email (the name is kept so older code still compiles).
 */
interface AccountStore {
    val studentId: String
    var name: String
    var schoolName: String
    var remoteId: String?
    var isLoggedIn: Boolean

    /** "English", "isiZulu" or "Afrikaans". */
    var language: String

    var notificationsEnabled: Boolean

    /** Chosen at onboarding, e.g. "Grade 12 learner". */
    var role: String

    fun saveUser(
        name: String,
        schoolName: String,
        studentId: String,
        passwordHash: String,
        salt: String,
        remoteId: String?
    )

    fun getCredentials(): Credentials?

    fun updatePassword(passwordHash: String, salt: String)
}

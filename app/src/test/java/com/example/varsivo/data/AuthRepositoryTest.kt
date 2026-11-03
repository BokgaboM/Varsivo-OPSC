package com.example.varsivo.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests registration, login (online, offline and Google), password change, settings and logout
 * using in-memory fakes, so no phone or internet connection is needed (GitHub Actions runs these
 * on every push).
 */
class AuthRepositoryTest {

    private lateinit var store: FakeAccountStore
    private lateinit var service: FakeAuthService
    private lateinit var repo: AuthRepository

    @Before fun setUp() {
        store = FakeAccountStore()
        service = FakeAuthService()
        repo = AuthRepository(store, service)
    }

    // ---------- registration ----------

    @Test fun register_createsTheAccountOnlineAndOnlyAHashOnThePhone() = runTest {
        val outcome = repo.register("Thabo Mokoena", "Rosebank High", "thabo@mail.com", "Password1")

        assertEquals(RegisterOutcome.SAVED_AND_SYNCED, outcome)
        val local = store.getCredentials()
        assertNotNull(local)
        assertNotEquals("Password1", local!!.passwordHash)   // never stored in plain text
        assertEquals(64, local.passwordHash.length)           // 256-bit PBKDF2 hash as hex
        assertTrue(service.accounts.containsKey("thabo@mail.com"))
        assertEquals("Thabo Mokoena", service.profiles["uid1"]?.fullName)
        assertEquals("uid1", store.remoteId)
    }

    @Test fun register_sendsTheOnboardingRoleAndLanguage() = runTest {
        store.role = "Grade 11 learner"
        store.language = "isiZulu"

        repo.register("Thabo", "", "thabo@mail.com", "Password1")

        assertEquals("Grade 11 learner", service.profiles["uid1"]?.role)
        assertEquals("isiZulu", service.profiles["uid1"]?.language)
    }

    @Test fun register_doesNotLeaveTheStudentSignedIn() = runTest {
        repo.register("Thabo", "", "thabo@mail.com", "Password1")

        assertFalse(store.isLoggedIn)
        assertTrue(service.signedOut)
    }

    @Test fun register_refusesAnEmailThatIsAlreadyRegistered() = runTest {
        repo.register("Thabo", "", "thabo@mail.com", "Password1")

        assertEquals(RegisterOutcome.ALREADY_EXISTS, repo.register("Other", "", "THABO@mail.com", "Password1"))
    }

    @Test fun register_reportsNoConnectionWhenOffline() = runTest {
        service.offline = true

        assertEquals(RegisterOutcome.NO_CONNECTION, repo.register("Thabo", "", "thabo@mail.com", "Password1"))
        assertNull(store.getCredentials())
    }

    // ---------- login ----------

    @Test fun login_succeedsWithTheCorrectPassword() = runTest {
        repo.register("Thabo", "", "thabo@mail.com", "Password1")

        assertEquals(LoginOutcome.SUCCESS, repo.login("thabo@mail.com", "Password1"))
        assertTrue(store.isLoggedIn)
    }

    @Test fun login_ignoresCaseAndSpacesInTheEmail() = runTest {
        repo.register("Thabo", "", "thabo@mail.com", "Password1")

        assertEquals(LoginOutcome.SUCCESS, repo.login("  Thabo@Mail.com ", "Password1"))
    }

    @Test fun login_failsWithTheWrongPassword() = runTest {
        repo.register("Thabo", "", "thabo@mail.com", "Password1")

        assertEquals(LoginOutcome.INVALID_CREDENTIALS, repo.login("thabo@mail.com", "WrongPass9"))
        assertFalse(store.isLoggedIn)
    }

    @Test fun login_failsForAnUnknownEmail() = runTest {
        assertEquals(LoginOutcome.INVALID_CREDENTIALS, repo.login("nobody@mail.com", "Password1"))
    }

    @Test fun login_onAnotherPhoneLoadsTheProfileAndSettings() = runTest {
        store.language = "Afrikaans"
        repo.register("Thabo", "Rosebank High", "thabo@mail.com", "Password1")

        val otherStore = FakeAccountStore()
        val otherRepo = AuthRepository(otherStore, service)

        assertEquals(LoginOutcome.SUCCESS, otherRepo.login("thabo@mail.com", "Password1"))
        assertEquals("Thabo", otherStore.name)
        assertEquals("Rosebank High", otherStore.schoolName)
        assertEquals("Afrikaans", otherStore.language)
    }

    @Test fun login_worksOfflineForTheAccountOnThisPhone() = runTest {
        repo.register("Thabo", "", "thabo@mail.com", "Password1")
        service.offline = true

        assertEquals(LoginOutcome.SUCCESS, repo.login("thabo@mail.com", "Password1"))
        assertEquals(LoginOutcome.INVALID_CREDENTIALS, repo.login("thabo@mail.com", "WrongPass9"))
    }

    @Test fun login_offlineWithoutALocalAccountReportsNoConnection() = runTest {
        service.offline = true

        assertEquals(LoginOutcome.NO_CONNECTION, repo.login("thabo@mail.com", "Password1"))
    }

    @Test fun loginWithGoogle_createsAProfileTheFirstTime() = runTest {
        assertEquals(LoginOutcome.SUCCESS, repo.loginWithGoogle("google-token", "Lerato Dlamini"))

        assertTrue(store.isLoggedIn)
        assertEquals("Lerato Dlamini", store.name)
        assertEquals("Lerato Dlamini", service.profiles["google-uid"]?.fullName)
    }

    @Test fun logout_endsTheSessionButKeepsTheAccount() = runTest {
        repo.register("Thabo", "", "thabo@mail.com", "Password1")
        repo.login("thabo@mail.com", "Password1")

        repo.logout()

        assertFalse(store.isLoggedIn)
        assertNotNull(store.getCredentials())
    }

    @Test fun sendPasswordReset_asksTheServiceToEmailALink() = runTest {
        assertTrue(repo.sendPasswordReset(" Thabo@Mail.com "))
        assertEquals(listOf("thabo@mail.com"), service.resetEmails)
    }

    // ---------- change password ----------

    @Test fun changePassword_rejectsAWrongCurrentPassword() = runTest {
        repo.register("Thabo", "", "thabo@mail.com", "Password1")

        assertEquals(ChangePasswordOutcome.WRONG_CURRENT_PASSWORD, repo.changePassword("nope", "NewPass22"))
    }

    @Test fun changePassword_updatesOnlineAndOnThePhone() = runTest {
        repo.register("Thabo", "", "thabo@mail.com", "Password1")

        assertEquals(ChangePasswordOutcome.SUCCESS, repo.changePassword("Password1", "NewPass22"))
        assertEquals(LoginOutcome.SUCCESS, repo.login("thabo@mail.com", "NewPass22"))
        assertEquals(LoginOutcome.INVALID_CREDENTIALS, repo.login("thabo@mail.com", "Password1"))
    }

    @Test fun changePassword_withoutAnAccountReportsNoAccount() = runTest {
        assertEquals(ChangePasswordOutcome.NO_ACCOUNT, repo.changePassword("a", "b"))
    }

    // ---------- settings ----------

    @Test fun updateSettings_savesOnThePhoneAndOnline() = runTest {
        repo.register("Thabo", "Old School", "thabo@mail.com", "Password1")

        val synced = repo.updateSettings("  Thabo M  ", "New School", "isiZulu", false)

        assertTrue(synced)
        assertEquals("Thabo M", store.name)
        assertEquals("isiZulu", store.language)
        assertFalse(store.notificationsEnabled)
        assertEquals("isiZulu", service.profiles["uid1"]?.language)
        assertEquals(false, service.profiles["uid1"]?.notificationsEnabled)
    }

    @Test fun updateSettings_stillSavesOnThePhoneWhenOffline() = runTest {
        repo.register("Thabo", "Old School", "thabo@mail.com", "Password1")
        service.offline = true

        val synced = repo.updateSettings("Thabo M", "New School", "Afrikaans", true)

        assertFalse(synced)
        assertEquals("Thabo M", store.name)
        assertEquals("Afrikaans", store.language)
    }
}

/** In-memory stand-in for [UserStore] (which needs an Android Context). */
class FakeAccountStore : AccountStore {
    private var hash: String? = null
    private var salt: String? = null
    private var id: String = ""

    override val studentId: String get() = id
    override var name: String = ""
    override var schoolName: String = ""
    override var remoteId: String? = null
    override var isLoggedIn: Boolean = false
    override var language: String = UserStore.DEFAULT_LANGUAGE
    override var notificationsEnabled: Boolean = true
    override var role: String = UserStore.DEFAULT_ROLE

    override fun saveUser(
        name: String,
        schoolName: String,
        studentId: String,
        passwordHash: String,
        salt: String,
        remoteId: String?
    ) {
        this.name = name
        this.schoolName = schoolName
        this.id = studentId
        this.hash = passwordHash
        this.salt = salt
        this.remoteId = remoteId
        this.isLoggedIn = false
    }

    override fun getCredentials(): Credentials? {
        val h = hash
        val s = salt
        return if (id.isEmpty() || h.isNullOrEmpty() || s.isNullOrEmpty()) null else Credentials(id, h, s)
    }

    override fun updatePassword(passwordHash: String, salt: String) {
        this.hash = passwordHash
        this.salt = salt
    }
}

/** In-memory stand-in for Firebase Authentication + the Firestore "users" collection. */
class FakeAuthService : AuthService {
    /** email -> (password, uid) */
    val accounts = mutableMapOf<String, Pair<String, String>>()
    val profiles = mutableMapOf<String, RemoteProfile>()
    var offline = false
    var signedOut = false

    private fun checkOnline() {
        if (offline) throw AuthFailure(AuthFailure.Reason.NETWORK)
    }

    override suspend fun createAccount(email: String, password: String): String {
        checkOnline()
        if (accounts.containsKey(email)) throw AuthFailure(AuthFailure.Reason.EMAIL_TAKEN)
        val uid = "uid${accounts.size + 1}"
        accounts[email] = Pair(password, uid)
        return uid
    }

    override suspend fun signIn(email: String, password: String): String {
        checkOnline()
        val account = accounts[email]
        if (account == null || account.first != password) throw AuthFailure(AuthFailure.Reason.WRONG_CREDENTIALS)
        return account.second
    }

    override suspend fun signInWithGoogle(idToken: String): Pair<String, String> {
        checkOnline()
        return Pair("google-uid", "lerato@gmail.com")
    }

    override suspend fun saveProfile(profile: RemoteProfile) {
        checkOnline()
        profiles[profile.uid] = profile
    }

    override suspend fun loadProfile(uid: String): RemoteProfile? {
        checkOnline()
        return profiles[uid]
    }

    override suspend fun changePassword(email: String, currentPassword: String, newPassword: String) {
        val uid = signIn(email, currentPassword)
        accounts[email] = Pair(newPassword, uid)
    }

    val resetEmails = mutableListOf<String>()

    override suspend fun sendPasswordReset(email: String) {
        checkOnline()
        resetEmails.add(email)
    }

    override fun signOut() {
        signedOut = true
    }
}

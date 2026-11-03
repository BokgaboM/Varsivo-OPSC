package com.example.varsivo

import android.app.Application
import android.util.Log
import com.example.varsivo.data.ApplicationRepository
import com.example.varsivo.data.ApplicationsDb
import com.example.varsivo.data.AuthRepository
import com.example.varsivo.data.FirebaseAuthService
import com.example.varsivo.data.FirestoreApplicationRemote
import com.example.varsivo.data.UserStore
import com.example.varsivo.notifications.PushNotifications
import com.example.varsivo.util.AppLanguage

/**
 * Application class: creates the shared data objects once so every Fragment uses the same
 * instances (simple manual dependency injection).
 */
class VarsivoApplication : Application() {

    val userStore: UserStore by lazy { UserStore(this) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(userStore, FirebaseAuthService())
    }

    val applicationRepository: ApplicationRepository by lazy {
        ApplicationRepository(ApplicationsDb(this), FirestoreApplicationRemote(), userStore)
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("VarsivoApplication", "Varsivo started")
        // Use the language the student chose last time (English, isiZulu or Afrikaans).
        AppLanguage.apply(userStore.language)
        // Push notifications: create the channel and follow the Settings switch.
        PushNotifications.createChannel(this)
        PushNotifications.applyPreference(userStore.notificationsEnabled)
    }
}

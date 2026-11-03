import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Signing key (app/varsivo-release.jks). Its passwords live in keystore.properties, which is NOT on GitHub.
// Debug and release builds use the same key, so the SHA-1 registered in Firebase (needed for Google
// sign-in) always matches. On a machine without the file (e.g. GitHub Actions) the default debug key is used.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
// Private settings that must never reach GitHub (secrets.properties is in .gitignore).
val secretProperties = Properties().apply {
    val file = rootProject.file("secrets.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val hasVarsivoKey = keystoreProperties.getProperty("storeFile")?.let { rootProject.file(it).exists() } == true

android {
    namespace = "com.example.varsivo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Play Store rejects "com.example" ids. The Kotlin package (namespace) can stay as it is.
        applicationId = "za.co.varsivo.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Azure Blob Storage container SAS URL (profile photos). Empty when secrets.properties is missing.
        val azureSasUrl = secretProperties.getProperty("azurePhotosSasUrl", "")
        buildConfigField("String", "AZURE_PHOTOS_SAS_URL", "\"$azureSasUrl\"")
    }

    signingConfigs {
        if (hasVarsivoKey) {
            create("varsivo") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasVarsivoKey) signingConfig = signingConfigs.getByName("varsivo")
        }
        release {
            if (hasVarsivoKey) signingConfig = signingConfigs.getByName("varsivo")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // XML layouts + View Binding (no Jetpack Compose anywhere in this project).
    buildFeatures {
        viewBinding = true
        buildConfig = true // BuildConfig.VERSION_NAME is shown on the Settings screen
    }

    testOptions {
        unitTests {
            // Lets plain JVM unit tests call android.util.Log without crashing.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Android Views, Material components and navigation
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // REST API client (Retrofit + Gson) and the map SDK (MapLibre, OpenStreetMap based)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp) // direct HTTP calls to the Azure Blob Storage REST API
    implementation(libs.maplibre.android)

    // Firebase: Authentication (email + Google sign-in), Cloud Firestore (NoSQL database)
    // and Cloud Messaging (push notifications). The BoM keeps all Firebase versions in step.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services) // Task.await() for Firebase calls

    // Sign in with Google through Android Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Local unit tests (run on the JVM, also in GitHub Actions)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests (need an emulator / device)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

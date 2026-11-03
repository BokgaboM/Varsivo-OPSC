# Varsivo — Apply. Fund. Succeed.

[![Android CI](https://github.com/ST10440136/Varsivo-OPSC/actions/workflows/android.yml/badge.svg)](https://github.com/ST10440136/Varsivo-OPSC/actions/workflows/android.yml)

Varsivo is a South African Android app that helps **Grade 11 and 12 learners** work out which universities they
qualify for, find bursaries, keep track of their applications, and get first-year support once they are enrolled.

OPSC6312 Portfolio of Evidence (Part 2: App Prototype, Part 3: Final App).
Language: **Kotlin** · UI: **XML layouts + Android Views** (no Jetpack Compose anywhere in the project).

> 🎥 **Demo video:** `<!-- TODO: paste the unlisted YouTube link here -->`
>
> 👤 **Author:** Bokgabo Mohlala (ST10440136) `<!-- TODO: add team members if this is a group submission -->`

---

## 1. Purpose of the app

Matric learners have to juggle university applications, bursary deadlines and subject requirements across many
websites. Varsivo puts the essentials in one app:

| Learner need | Varsivo feature |
|---|---|
| Do I qualify? | **Explore**: APS calculator that converts marks to the 7-point scale and matches institutions |
| How do I pay for it? | **Bursaries**: searchable list from the Cloud Firestore database, filtered by field and province |
| Where did I apply? | **Tracker**: offline-first application tracker that syncs to the cloud |
| Which universities exist? | **Find your university**: public universities API with a map |
| How do I survive first year? | **UniAssist** (premium): timetable and deadlines, unlocked with a verified `.ac.za` student email |

## 2. Screenshots

<!-- TODO: add phone screenshots to docs/screenshots/ and link them here, e.g. -->
<!-- ![Login](docs/screenshots/login.png) ![Home](docs/screenshots/home.png) ![Bursaries](docs/screenshots/bursaries.png) -->

## 3. Features and where they are in the code

| Feature | What it does | Code |
|---|---|---|
| **Register and log in** | Email + password with Firebase Authentication. Firebase stores passwords as salted scrypt hashes; the phone keeps only a salted **PBKDF2-HMAC-SHA256** hash (120 000 iterations) for offline login. Clear errors for a wrong password, an email already in use and no connection. | `LoginFragment`, `RegisterFragment`, `AuthRepository`, `FirebaseAuthService`, `SecurityUtils` |
| **Onboarding choices** | Role (Grade 11 learner / Grade 12 learner / University student) and language, picked on the login screen and saved to the profile. | `LoginFragment`, `UserStore` |
| **Forgot password** | Firebase emails a real reset link. | `LoginFragment`, `AuthRepository.sendPasswordReset` |
| **Settings** | Name, school, language, notifications and password change. Saved on the phone (SharedPreferences) and in Firestore `users/{uid}`; offline changes upload automatically later. | `SettingsFragment`, `AuthRepository.updateSettings` |
| **Bursary database** | Bursaries from the Firestore `bursaries` collection (seeded from the app the first time it is empty). Search by name or provider, filter by study field and province. | `BursariesFragment`, `BursaryRepository`, `BursarySeed`, `BursaryFilter` |
| **APS calculator** | Validates each mark (0–100), converts to APS points and shows which institutions the learner qualifies for. | `ExploreFragment`, `ApsCalculator` |
| **Application tracker** | Applications, status changes and deletes are saved to SQLite first, then synced to Firestore `users/{uid}/applications`. Offline changes show "Offline – will sync…" and upload when the tracker opens again. | `ApplicationsTrackerFragment`, `ApplicationRepository`, `ApplicationsDb`, `FirestoreApplicationRemote` |
| **UniAssist (premium)** | Locked until the student enters a university email ending in `.ac.za`; the flag is stored on the phone and in Firestore. | `FirstYearSupportFragment`, `UniAssistRepository` |
| **Universities + map** | Hipo Labs Universities API (Retrofit), OpenStreetMap Nominatim for coordinates, shown on a MapLibre map. | `UniversitiesFragment`, `UniversityMapFragment` |
| **Three languages** | English, isiZulu and Afrikaans for every screen; switch on the login screen or in Settings. | `res/values`, `res/values-zu`, `res/values-af`, `AppLanguage` |

## 4. Design considerations

* **Views and XML only, single Activity.** `MainActivity` hosts a `NavHostFragment`; every screen is a Fragment in
  `res/navigation/nav_graph.xml`. View Binding replaces `findViewById`, and `BaseFragment` clears the binding in
  `onDestroyView` so screens never leak views.
* **Bottom navigation:** Home | Explore | Bursaries | Tracker | Profile, defined once in `activity_main.xml`; the
  current tab is highlighted in gold.
* **Brand:** navy `#053E7D` and gold `#F8A100` Material 3 theme, shared styles in `styles.xml`, matching the Figma
  design.
* **Invalid input never crashes the app.** Every form validates with the pure functions in `Validators`, shows the
  error on the exact field and clears it while the user types. Every network call has a timeout and a friendly message.
* **Offline first.** The last account can log in without internet (local PBKDF2 hash), the tracker works fully
  offline, and Firestore queues profile and settings writes until the phone reconnects.
* **Separation of concerns:** Fragments (UI) → repositories → data sources (SharedPreferences, SQLite, Firebase,
  Retrofit). The online sign-in sits behind the `AuthService` interface so the logic is unit-tested with a fake.
* **Security:** passwords are never stored in plain text; Firestore security rules only let a signed-in user read
  and write their own `users/{uid}` documents.

```
com.example.varsivo
├── MainActivity · VarsivoApplication · SecurityUtils
├── api/   Retrofit clients and models (bursary model, universities, geocoding)
├── data/  AuthRepository, AuthService + FirebaseAuthService, BursaryRepository, ApplicationRepository,
│          ApplicationsDb (SQLite), UserStore (SharedPreferences), UniAssistRepository
├── ui/    one Fragment per screen, BaseFragment, adapters
└── util/  Validators, ApsCalculator, BursaryFilter, AppLanguage, PartOfDay
```

## 5. Online services and database

| Service | Used for |
|---|---|
| **Firebase Authentication** | Accounts (email + password) and password-reset emails |
| **Cloud Firestore** (NoSQL) | `users/{uid}`: profile, role, language, notifications, UniAssist flag · `users/{uid}/applications`: tracker · `bursaries`: bursary database |
| **Hipo Labs Universities API** | List of South African universities |
| **OpenStreetMap Nominatim** | University name → map coordinates |

Firebase project settings are in `app/src/main/res/values/firebase.xml` (copied from `google-services.json`).

## 6. Automated testing

JUnit tests run on the JVM, with no phone needed:

| Test class | What it proves |
|---|---|
| `AuthRepositoryTest` | Register (hash on phone, profile online, role and language sent), duplicate email, offline register; login online, offline, wrong password, unknown email, on a second phone; Google sign-in profile; password reset; change password; settings online and offline; logout |
| `ValidatorsTest` | Name, email, password, mark and `.ac.za` student-email rules |
| `ApsCalculatorTest` | Mark → points, APS scaling, eligibility |
| `BursaryFilterTest` | Search, field filter, province filter, "View more" |
| `BursaryTest`, `SecurityUtilsTest`, `PartOfDayTest`, `ApplicationStatusTest` | JSON nulls, hashing, greeting, status cycling |

Run locally: right-click `com.example.varsivo (test)` → **Run Tests**, or `./gradlew testDebugUnitTest`.

## 7. GitHub and GitHub Actions

* The project is version-controlled with Git and pushed to GitHub after each feature.
* `.github/workflows/android.yml` runs on every push and pull request: it sets up JDK 21, runs
  `./gradlew testDebugUnitTest`, builds `./gradlew assembleDebug`, and uploads the test report and the debug APK as
  build artifacts. The badge at the top shows the latest result.

<!-- TODO: add a screenshot of a green Actions run: ![Actions](docs/screenshots/actions.png) -->

## 8. Logging

`android.util.Log` is used throughout (tags such as `AuthRepository`, `FirebaseAuthService`, `BursaryRepository`,
`ApplicationRepository`, `UniAssist`, `AppLanguage`, `MainActivity`) to record logins, sync results, offline
fall-backs and navigation. Filter Logcat by `package:za.co.varsivo.app` to follow the app.

## 9. How to run

1. Open the project folder (the one containing `app/`) in Android Studio and let Gradle sync.
2. Connect an Android phone (USB debugging on) and press **Run**. Minimum Android 7.0 (API 24).
3. Register with an email and password, then log in.

## 10. Release notes

### Part 2 rebuild (November 2026)
* Rebuilt the whole UI with **XML layouts and Android Views**; Jetpack Compose removed.
* **Firebase Authentication** for register, login, logout and password reset; **Cloud Firestore** for profiles and
  settings (replacing the earlier MockAPI users table).
* Role and language chosen at onboarding.
* Bursaries moved to **Firestore**, with **field and province filters** and search by provider.
* Tracker status changes and deletes now **sync to the cloud**, with offline queueing.
* **UniAssist** locked behind `.ac.za` student-email verification.
* New bottom navigation: Home | Explore | Bursaries | Tracker | Profile.
* **isiZulu and Afrikaans** translations for every screen.
* App ID changed to `za.co.varsivo.app` (Play Store ready).
* Unit tests rewritten for Firebase; GitHub Actions builds and tests every push.

### Part 3 (final PoE)
<!-- TODO: filled in when Part 3 is done (Google SSO, push notifications, blob storage, app icon, signed APK). -->

## 11. Use of AI tools

See [docs/AI_USE.md](docs/AI_USE.md).

## 12. References

Android Developers (2024) *Navigation component*. Available at: https://developer.android.com/guide/navigation (Accessed: 3 November 2026).
Android Developers (2024) *View binding*. Available at: https://developer.android.com/topic/libraries/view-binding (Accessed: 3 November 2026).
Android Developers (2024) *Save data using SQLite*. Available at: https://developer.android.com/training/data-storage/sqlite (Accessed: 3 November 2026).
Android Developers (2024) *Per-app language preferences*. Available at: https://developer.android.com/guide/topics/resources/app-languages (Accessed: 3 November 2026).
Anthropic (2026) *Claude* [Large language model]. Available at: https://claude.ai (Accessed: 3 November 2026).
Firebase (2026) *Authenticate with Firebase using password-based accounts on Android*. Available at: https://firebase.google.com/docs/auth/android/password-auth (Accessed: 3 November 2026).
Firebase (2026) *Add data to Cloud Firestore*. Available at: https://firebase.google.com/docs/firestore/manage-data/add-data (Accessed: 3 November 2026).
Firebase (2026) *Get data with Cloud Firestore*. Available at: https://firebase.google.com/docs/firestore/query-data/get-data (Accessed: 3 November 2026).
GitHub (2025) *Automated build Android app with GitHub Action*. Available at: https://github.com/marketplace/actions/automated-build-android-app-with-github-action (Accessed: 3 November 2026).
Hipo Labs (2024) *Universities API*. Available at: http://universities.hipolabs.com/ (Accessed: 3 November 2026).
MapLibre (2025) *MapLibre Native for Android*. Available at: https://maplibre.org/maplibre-native/android/ (Accessed: 3 November 2026).
OpenStreetMap Foundation (2025) *Nominatim*. Available at: https://nominatim.org/ (Accessed: 3 November 2026).
OWASP (2024) *Password Storage Cheat Sheet*. Available at: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html (Accessed: 3 November 2026).
Square (2024) *Retrofit*. Available at: https://square.github.io/retrofit/ (Accessed: 3 November 2026).

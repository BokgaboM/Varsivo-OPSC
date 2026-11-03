# Varsivo – demo video script (Part 3 final PoE)

Record **on the phone** (screen recorder) with a **voice-over**. Aim for **8–10 minutes**.
For the "cut to" scenes, record the computer screen separately (e.g. Xbox Game Bar: Win + G) and join the clips.
Upload **unlisted** to YouTube and paste the link at the top of the README.

---

## Before you press record (10 minutes)

**On the phone**
- [ ] Install the latest `app-release.apk` (signed). If an older Varsivo is installed, uninstall it first.
- [ ] Phone language English, volume up, Do Not Disturb **off** (so the push notification shows).
- [ ] A Google account signed in on the phone (for Google sign-in).
- [ ] One photo in the gallery to use as a profile picture.
- [ ] Decide on a test email you can open (for the password-reset email), e.g. your own Gmail.

**On the computer, open these tabs in this order**
1. Firebase console → **Authentication → Users**
2. Firebase console → **Firestore Database → Data**
3. Firebase console → **Messaging** → a new **Notifications** campaign typed in but **not published yet**:
   title `NSFAS closes in 3 days`, text `Finish your application in Varsivo today.`, target **App: za.co.varsivo.app**
4. Azure portal → Storage account → **Containers → profile-photos**
5. GitHub → **BokgaboM/Varsivo-OPSC** (README) and the **Actions** tab

---

## Scenes

| # | Show | What to do | Say (in your own words) |
|---|---|---|---|
| 1 | App icon on the home screen → open | Tap the Varsivo icon. Splash → Login. | Introduce yourself, Varsivo, and the problem: Grade 11/12 learners juggling universities, APS and bursaries. Built in Kotlin with XML views, Firebase and Azure. |
| 2 | Login → **onboarding pills** | Tap **"I am a"** → Grade 12 learner. Tap **Language** → isiZulu, show the screen change, then back to **English**. | "Role and language are chosen at onboarding. The whole app supports English, isiZulu and Afrikaans." |
| 3 | **Register – invalid input** | Tap Sign up. Submit empty → errors. Type a bad email and `abc` as the password → errors. Mismatched confirm password → error. | "Every field is validated, and invalid input never crashes the app." |
| 4 | **Register – success** | Fill in a real name, school, email, a strong password, tick the terms → Create account. | "The password goes to Firebase Authentication, which stores it as a salted hash. The phone keeps only a PBKDF2 hash for offline login." |
| 5 | **Cut to Firebase → Authentication → Users** | Point at the new email row. | "This is the online authentication service. There is no plain-text password anywhere." |
| 6 | **Login** | Wrong password first → error. Then the correct password → Home. | "Clear error messages, then a successful login." |
| 7 | **Home** + **bottom navigation** | Allow notifications when asked. Tap through Home, Explore, Bursaries, Tracker, Profile. | "A single-activity app with the Navigation component and a bottom bar." |
| 8 | **Explore – APS calculator** | Enter marks, include **150** once to show the error, fix it → Calculate. Show matched institutions. | "User-defined feature 1: APS calculator with validation and matching." |
| 9 | **Bursaries** | Search "sasol". Clear. Field → Engineering. Province → Gauteng. Open a bursary's details. | "User-defined feature 2: bursary database from Cloud Firestore, with search and field and province filters." |
| 10 | **Cut to Firestore → bursaries** | Open the `bursaries` collection and one document. | "The NoSQL database: the app seeded this collection and reads from it." |
| 11 | **Tracker – online** | From Tracker → Browse universities → pick one → Apply now → fill the form (try a bad email first) → submit. Back in Tracker, tap the status chip to change it. | "User-defined feature 3: the application tracker. Changes are saved locally first, then synced." |
| 12 | **Tracker – offline mode with sync** | Turn on **airplane mode**. Add another application → it shows **"Offline – will sync…"**. Turn airplane mode **off**, leave and reopen Tracker → it changes to **Synced**. Delete one application. | "Offline mode with sync: SQLite on the phone, uploaded to Firestore when the phone reconnects." |
| 13 | **Cut to Firestore → users → (your uid) → applications** | Show the synced applications and their status. | "Here is the tracker data in the cloud, per user." |
| 14 | **UniAssist** | Home → UniAssist → locked. Type `name@gmail.com` → error. Type `name@student.uj.ac.za` → Verify and unlock → timetable and deadlines. | "Premium UniAssist is locked until a university .ac.za email is verified." |
| 15 | **Profile / Settings** | Change the name, switch language to **Afrikaans**, toggle notifications → **Save changes** → the app redraws in Afrikaans. Switch back to English. | "Settings are saved on the phone and in Firestore." |
| 16 | **Profile photo (blob storage)** | Tap **Change photo** → pick a photo → "saved to the cloud". | "Profile photos are uploaded to Azure Blob Storage." |
| 17 | **Cut to Azure → profile-photos** | Refresh → show `<uid>.jpg`. Then **cut to Firestore → users → (uid)** and point at `language`, `notificationsEnabled`, `photoBlob`, `uniAssistVerified`. | "Blob storage on Azure, and the settings stored in the Firestore user document." |
| 18 | **Push notification** | On the computer, **Publish** the prepared Firebase Messaging campaign. Show the phone receiving it, tap it → app opens. | "Real-time push notifications with Firebase Cloud Messaging. Students who switch notifications off don't receive them." |
| 19 | **Change password + logout** | Profile → Change password → Update. Log out. | "Password change and logout." |
| 20 | **Google single sign-on** | Login → **Continue with Google** → pick the account → Home. Optionally show the Google user in Firebase → Authentication → Users. | "Single sign-on with Google through Credential Manager and Firebase." |
| 21 | **Forgot password** | Log out → Forgot password? → enter the test email → Send. Show the email in your inbox. | "Real password-reset emails from Firebase." |
| 22 | **GitHub** | README (badge, release notes), **Actions** tab with green runs, and the commit history. | "Automated tests and builds run with GitHub Actions on every push. Release notes list everything added since the prototype." |
| 23 | **Publication preparation** | Show the signed `app-release.apk`, the Play Store icon and feature graphic in `docs/play-store`, and the Play Console screenshot if you have one. | "Signed with our upload key, version 2.0, application ID za.co.varsivo.app: ready for the Play Store." |
| 24 | Close | Back to the app's Home screen. | Short summary and thank you. |

---

## If something goes wrong while recording

- **Google sign-in fails**: check that the phone has Google Play services and is online; it needs the signed build from this project (debug or release on the VM).
- **No push notification**: check Profile → notifications switch is on, Android notification permission is allowed, and Do Not Disturb is off. Wait up to 1 minute after publishing.
- **Photo says "not set up"**: the APK was built without `secrets.properties`. Rebuild from the project folder that contains it.
- **Bursaries empty**: log in first (seeding needs a signed-in user) and check the internet connection.

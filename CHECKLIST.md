# Varsivo – Part 2 + Part 3 Checklist

Rules: XML views only. No Jetpack Compose anywhere (Compose in Part 3 = automatic 0).
Test on a real phone (no emulator on the school VM).

Marks: Part 2 marks come from the Part 2 rubric. Only two rows are known so far (Sign-in + API = 30, User-defined features = 30).
Part 3 is out of 100 in total; the per-line marks are in the Part 3 rubric. Lines marked "see rubric" still need their marks filled in.

Tick a box by changing `[ ]` to `[x]`.

## Block 0 – Audit (0:00–0:30)
- [x] P2+P3 – No Compose in the project (checked: viewBinding only, no compose plugin or dependency)
- [ ] P2+P3 – App builds in Android Studio
- [ ] P2+P3 – App installs and opens on the phone
- [ ] P2+P3 – Azure API opens in the phone's browser
- [x] P2+P3 – This checklist saved in the repo

## Part 2 – Sign-in and REST API (30 marks)
- [ ] P2 – App calls the Azure Node/Express API, not MockAPI (RetrofitClient base URL)
- [ ] P2 – Register works against the live API
- [ ] P2 – Login works against the live API
- [ ] P2 – Password hashed before it is sent (SecurityUtils, add comment + OWASP reference)
- [ ] P2 – Role (Grade 11/12 learner) and language chosen at onboarding
- [ ] P2 – Token/session saved on the phone
- [ ] P2 – Logout clears the session
- [ ] P2 – Clear error messages (wrong password, duplicate account, no internet)
- [ ] P2 – "Continue with Google" does NOT skip login (real SSO is a Part 3 line)

## Part 2 – User-defined features (30 marks)
- [ ] P2 – Bursaries table in Azure seeded (run INSERTs)
- [ ] P2 – Bursary list with search + field filter + province filter
- [ ] P2 – APS calculator (validates marks, matches institutions)
- [ ] P2 – Application tracker works offline; status change + delete reach the API
- [ ] P2 – UniAssist locked until a student email (.ac.za) is verified

## Part 2 – Settings (marks: see rubric)
- [ ] P2 – Name, school, language, notifications saved on the phone
- [ ] P2 – Same settings saved through the API
- [ ] P2 – Settings survive an app restart

## Part 2 – GitHub, README, tests (marks: see rubric)
- [ ] P2 – Repo root is the inner Varsivo folder (so .github/workflows is found)
- [ ] P2 – Unit tests for APS calculator and login checks pass
- [ ] P2 – GitHub Actions run is green on main
- [ ] P2 – README placeholders replaced (badge, video, names, screenshots, access dates)
- [ ] P2 – README features labelled: APS calculator, Bursary database, Application tracker, UniAssist
- [ ] P2 – AI use write-up in its own file, linked from README

## Part 2 – UI (marks: see rubric)
- [ ] P2 – Bottom nav: Home | Explore | Bursaries | Tracker | Profile
- [ ] P2 – Figma colours (navy #053E7D, gold #F8A100)
- [ ] P2 – Invalid input never crashes the app

## Part 3 – App (marks: see rubric, out of 100 total)
- [ ] P3 – Runs on a real phone, no errors, compiles
- [ ] P3 – Lecturer feedback from Part 1 and Part 2 applied
- [ ] P3 – Register + login, password encrypted
- [ ] P3 – Single sign-on (Google) works
- [ ] P3 – Change settings
- [ ] P3 – REST API connected to a database (Azure API + Azure SQL)
- [ ] P3 – Offline mode with sync (tracker, SQLite) – syncs on reconnect
- [ ] P3 – Real-time push notifications (Firebase Cloud Messaging)
- [ ] P3 – At least two SA languages (isiZulu + Afrikaans) – switch works on every screen
- [ ] P3 – Part 1 features present
- [ ] P3 – App icon + final image assets
- [ ] P3 – Blob storage used (learning outcome)
- [ ] P3 – Data stored in a NoSQL database (learning outcome)
- [ ] P3 – Data read from a NoSQL database (learning outcome)

## Part 3 – Video
- [ ] P3 – Recorded on the phone, with voice-over, professional
- [ ] P3 – Shows every feature above
- [ ] P3 – Shows data in the auth service, the API and the database
- [ ] P3 – Uploaded unlisted to YouTube, link in README

## Part 3 – GitHub
- [ ] P3 – Regular commits and pushes
- [ ] P3 – GitHub Actions tests + build green
- [ ] P3 – Final commit tagged "final PoE"

## Part 3 – README documentation
- [ ] P3 – Report: purpose, design considerations, GitHub + Actions use, with images
- [ ] P3 – Release notes: every update since the prototype, innovative features explained

## Part 3 – Publishing
- [ ] P3 – applicationId changed from com.example.varsivo (Play Store rejects com.example)
- [ ] P3 – Signed release APK
- [ ] P3 – Screenshots
- [ ] P3 – Screenshot of upload to Google Play Console (if applicable)

## Part 3 – Submission
- [ ] P3 – Full Kotlin source on GitHub (no zip)
- [ ] P3 – Code comments with references
- [ ] P3 – Logging in the code
- [ ] P3 – README, video link, release notes
- [ ] P3 – AI use write-up (max 500 words, cited)

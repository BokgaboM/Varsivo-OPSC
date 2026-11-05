## Use of AI Tools

### Tool used
Claude (Anthropic, 2026), as a coding assistant during Part 2 and Part 3.

### Bokgabo Mohlala's use of AI
AI supported me in four areas:

- **Debugging:** identifying errors, explaining error messages and suggesting fixes during development and testing.
- **Learning unfamiliar concepts:** explaining technical concepts and approaches I had not used before.
- **Firebase and Cloud Firestore:** connecting Firebase to the app and implementing Firebase Authentication and Firestore features, including sign-in, profiles, settings, bursaries, application tracking and UniAssist verification.
- **Moving from Jetpack Compose to XML:** finding a practical way to convert the Compose prototype to XML layouts and Android Views without rebuilding the project from scratch, which mattered given our limited time.

**How I checked the output:** I reviewed, built and tested every change in Android Studio and ran the unit tests locally, with GitHub Actions building and testing each push. I configured the Firebase project, authentication provider and Firestore security rules myself, and modified suggestions where needed before including them.

**What I learned:** Firebase and Cloud Firestore were new to me. I learned how to connect Firebase to an Android app, work with authentication and cloud storage, and how Firestore differs from traditional databases. I also learned that an existing Compose project could be moved to XML without recreating the whole app. AI gave suggestions, but I was responsible for reviewing the code, applying the changes, configuring the project and testing the features.

### Emihle Zifo's use of AI
My role was reviewing, testing and applying our lecturer's feedback. I found that our first group repository had been uploaded through the GitHub website without its `app/` folder, tested the features on an Android emulator (Pixel 8, Android 17) and reported the results to the team.

I used Claude to help draft the corrected APS calculation (six subjects, Life Orientation not counted), the Explore career recommendations, the automatic real-time alerts and the live bursary API, with their unit tests, and to plan the test checklist.

I checked the APS rule against a published source (ZA Bursaries, 2026), ran the unit tests in Android Studio and confirmed on the emulator that the source's example gives an APS of 32. I fixed problems I hit (Java setup, an emulator crash, Google sign-in in an unsigned build), committed each fix separately and merged them through pull requests after GitHub Actions passed.

### Testing and video
The release APK was tested feature by feature on an Android emulator by team members, and the demo video was recorded on a physical Android phone.

### References
Anthropic (2026) *Claude* [Large language model]. Available at: https://claude.ai (Accessed: 3 November 2026).

ZA Bursaries (2026) *Calculate your APS score for university entrance*. Available at: https://www.zabursaries.co.za/aps-score-for-university-entrance/ (Accessed: 4 November 2026).

## Use of AI Tools

### Tool Used
Claude (Anthropic, 2026), 

### How AI Was Used
AI was used as a coding assistant throughout the development of the Part 2 and Part 3 prototypes. It supported the team in the following areas:

- **Error fixing and debugging:** Assisted in identifying errors, understanding error messages and suggesting solutions to problems encountered during development and testing.
- **Learning unfamiliar concepts:** Helped explain technical concepts and development approaches that the team was not yet familiar with, allowing us to better understand and implement new features.
- **Firebase and Cloud Firestore:** Assisted us in understanding the Firebase model, connecting Firebase to the Android application and implementing features using Firebase Authentication and Cloud Firestore, including sign-in, user profiles, settings, bursaries, application tracking and UniAssist verification.
- **Converting Jetpack Compose to XML layouts:** Helped us identify a practical way to transition the existing Compose-based prototype to XML layouts and Android Views without having to convert every file individually or rebuild the entire project from scratch. This was particularly valuable given the limited time available and the requirement to use XML layouts.


### How the AI Output Was Checked
- Changes were reviewed, built and tested in Android Studio.
- Unit tests were run locally, with GitHub Actions also building and testing each push.
- The Firebase project, authentication provider and Firestore security rules were configured by me.
- The release APK was installed and tested feature by feature on an Android emulator (Pixel 8, Android 17) by team members.
- The demo video was recorded on a physical Android phone.
- AI-generated suggestions were reviewed, modified where necessary and tested before being included in the project.

### What I Learned and My Own Contribution
This project introduced me to Firebase and Cloud Firestore, which were unfamiliar technologies to me. Through the development process, I learned how to connect Firebase to an Android application, work with authentication and cloud-based data storage, and understand how Firestore differs from traditional database models.

I also gained experience debugging errors, learning unfamiliar programming concepts and working with XML layouts and Android Views. One of the most important lessons was discovering that an existing Jetpack Compose project could be transitioned to XML without having to recreate the entire application, which helped us manage our limited development time.

AI provided guidance and suggestions, but I was responsible for reviewing the code, applying the changes, configuring the project, testing the functionality and understanding how the implemented features worked.

### Emihle Zifo's Use of AI
My role was reviewing, testing and applying our lecturer's feedback. I discovered that our first group repository had been uploaded through the GitHub website without its `app/` folder, and I tested the features on an Android emulator (Pixel 8, Android 17), reporting the results to the team.

I used Claude (Anthropic, 2026) as a coding assistant to help fix errors and implement the corrected APS calculation (six subjects, excluding Life Orientation), Explore career recommendations and automatic real-time alerts. It also assisted with unit tests and planning the test checklist.

I verified the APS calculation against a published source (ZA Bursaries, 2026), ran the unit tests in Android Studio and confirmed on the emulator that the source's example produced an APS of 32. I addressed issues encountered during development, including Java setup, emulator crashes and Google sign-in in an unsigned build. I committed fixes separately and merged them through a pull request after GitHub Actions passed.

### References
Anthropic (2026) *Claude* [Large language model]. Available at: https://claude.ai (Accessed: 3 November 2026).

ZA Bursaries (2026) *Calculate your APS score for university entrance*. Available at: https://www.zabursaries.co.za/aps-score-for-university-entrance/ (Accessed: 4 November 2026).

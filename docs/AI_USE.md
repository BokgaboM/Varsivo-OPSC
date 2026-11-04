# Use of AI tools


## Tool used

Claude (Anthropic, 2026), a large language model, used through the Claude app.

## How it was used

The Part 2 prototype originally used Jetpack Compose, which the brief does not allow. I used Claude as a coding
assistant while rebuilding and extending the app with XML layouts and Android Views. Specifically, it was used to:

* audit the project against the Part 2 and Part 3 briefs and turn them into a checklist (`CHECKLIST.md`);
* draft the Firebase Authentication and Cloud Firestore code (sign-in, profiles, settings, bursaries, tracker sync,
  UniAssist verification) after the planned Azure API could not be hosted in time;
* draft the bottom navigation, the bursary field/province filters and the language-switching code;
* produce first drafts of the isiZulu and Afrikaans string translations;
* rewrite the unit tests for the new Firebase design, and draft this README.

## How the output was checked

* Every change was built in Android Studio ("Assemble Project with Tests") and the full unit-test suite was run
  locally; GitHub Actions also builds and tests every push.
* I created and configured the Firebase project, authentication provider and Firestore security rules myself.
* The release APK was installed and tested feature by feature on an Android emulator (Pixel 8, Android 17) by
  team members, and the demo video was recorded on a physical phone.


## What I learned / my own contribution

This was my first experience with the Firebase model , I not only learnt how to use it but also how different it is compared to other models. How to connect the Firebase to the code and push it to git. Along with adding different langauges froms of  authentication etc. 

## Emihle Zifo's use of AI

My role was reviewing, testing and applying our lecturer's feedback. I found that our first group repository had
been uploaded through the GitHub website without its `app/` folder, and I tested every feature on an Android
emulator (Pixel 8, Android 17) and reported the results to the team.

I used Claude (Anthropic, 2026) as a coding assistant. It drafted the corrected APS calculation (six subjects, Life
Orientation not counted), the Explore career recommendations and the automatic real-time alerts, with unit tests,
and helped me plan the test checklist.

I checked the APS rule against a published source (ZA Bursaries, 2026), ran the unit tests in Android Studio and
confirmed on the emulator that the source's example gives an APS of 32. I fixed the problems I hit (Java setup, an
emulator crash, Google sign-in in an unsigned build), committed each fix separately and merged them through a pull
request after GitHub Actions passed.

## Reference

Anthropic (2026) *Claude* [Large language model]. Available at: https://claude.ai (Accessed: 3 November 2026).

ZA Bursaries (2026) *Calculate your APS score for university entrance*. Available at: https://www.zabursaries.co.za/aps-score-for-university-entrance/ (Accessed: 4 November 2026).

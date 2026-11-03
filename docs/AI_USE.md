# Use of AI tools

> **Before submitting:** read this, change anything that is not accurate for your own work, and keep it under 500
> words. If this is a group submission, add a short paragraph from each member.

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
* The app was run on a physical phone to confirm each feature before recording the demo video.
* Translations were reviewed by a first-language speaker. <!-- TODO: confirm, or remove this line -->

## What I learned / my own contribution

<!-- TODO: in your own words (2–4 sentences): which decisions you made, what you changed from the AI's suggestions,
and what you learned (e.g. offline-first sync, Firebase security rules, Android per-app languages). -->

## Reference

Anthropic (2026) *Claude* [Large language model]. Available at: https://claude.ai (Accessed: 3 November 2026).

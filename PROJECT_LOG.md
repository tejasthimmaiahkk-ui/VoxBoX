# VoxBox Project Log

This file is append-only. New entries go at the end. Existing entries may be corrected only to fix an objective factual error, and the correction must be noted in a later entry.

## 2026-07-19 — Project reset accepted

### Context

- The previous college-project contents were deliberately removed by the user.
- A new empty Android Compose project named `VoxBox` is present under `D:\College Project\VoxBox`.
- The selected concept is an AI-powered voice-to-structured-notes assistant with a native no-key baseline.

### Decisions

- Working full title: **VoxBox: A Hybrid Voice-Command and AI-Assisted System for Creating Structured Visual Notes on Android**.
- The deterministic command language is named **VoxScript**.
- The wake word is configurable; `Tejas` is a valid example rather than a hard-coded requirement.
- The first implementation uses native Android speech recognition and deterministic parsing. AI summarization/reorganization is optional and scheduled after the offline core is reliable.
- Version-1 charts are pie, bar and progress. Version-1 diagrams are flowcharts and relationship maps.
- Speech sessions are visible and bounded; no always-on background microphone is planned.

### Environment observed

- Android namespace/application ID: `me.thimmaiah.voxbox`.
- Minimum SDK: 26.
- Target SDK: 36.
- Compose starter displays `Hello Android!` and has not yet been modified.
- ADB started successfully but reported no connected/authorized device. Physical-device verification is therefore **pending**, not passed.

## 2026-07-19 — Review Meeting 1 presentation completed

### Output

- Created `outputs/VoxBox_Review_Meeting_1.pptx` with 19 slides.
- The deck covers the problem, marks fit, solution, VoxScript example/grammar, bounded scope, Android speech constraints, architecture, data model, live-demo story, evaluation, two-pass verification, risks, eight-week roadmap, deliverables and official sources.

### Verification pass 1 — structural

- Export completed successfully.
- External slide overflow test passed with no overflow detected.

### Verification pass 2 — visual

- Every rendered slide was inspected at full size.
- The first visual pass found a clipped title on slide 4 and a table overlay on slide 11.
- Both were corrected; the deck was regenerated and the affected slides were re-inspected at full size.
- Corrected deck passed the overflow test again.

### Claims policy

- The deck makes no invented accuracy or latency claim.
- Device verification remains pending because ADB did not detect a phone during this session.

## 2026-07-19 — Project documentation baseline created

### Files

- Added `PROJECT_GUIDE.md`.
- Added this append-only `PROJECT_LOG.md`.
- Added `docs/VOXSCRIPT_SPEC.md`.
- Added `docs/TEST_PLAN.md`.
- Added `docs/REPORT_DRAFT.md`.
- Added `docs/VIVA_NOTES.md`.
- Added a root `.gitignore` protecting local configuration, credentials, keystores and build artifacts.
- Updated the existing daily automation in place to continue VoxBox at 7:00 PM Asia/Calcutta without creating a duplicate.

### Implementation status

- No application feature code has been implemented yet.
- The next ordered milestone is to verify the untouched starter, initialize Git, commit this verified planning baseline, and then build the note/block foundation before the native speech gateway.

## 2026-07-19 — Android starter baseline verified

### Configuration defect found and corrected

- The generated starter used `androidx.core:core-ktx:1.19.0` and lifecycle `2.11.0`, which require compile SDK 37.
- The project compiles against installed Android platform 36.1; platform 37 is not installed.
- Verified local AAR metadata showed that `core-ktx 1.18.0` requires compile SDK 36 and lifecycle `2.9.4` requires compile SDK 34.
- Pinned `core-ktx` to `1.18.0` and lifecycle runtime KTX to `2.9.4`. Target SDK, minimum SDK and application behavior were not changed.

### Verification pass 1 — build and unit test

- Command: `gradlew.bat testDebugUnitTest assembleDebug`.
- Result: **BUILD SUCCESSFUL**; debug APK assembled and starter unit tests passed.

### Verification pass 2 — physical device

- Device: `2411DRN47I`.
- Android version: 16; API level: 36.
- Debug APK installed and `me.thimmaiah.voxbox/.MainActivity` became the top resumed activity.
- UI hierarchy contains `Hello Android!` under the correct application package.
- Screenshot and hierarchy stored in `evidence/baseline/`.

### Status after verification

- The connected device is now available for subsequent feature checks.
- The application still contains only the starter greeting. No VoxBox feature has been implemented yet.

## 2026-07-19 — Native speech and starter VoxScript milestone completed

### Implemented

- Added `RECORD_AUDIO` permission and Android speech-service query.
- Added `SpeechRecognitionController` using `SpeechRecognizer` on the main thread with partial/final results, stop, cancel, error mapping and `destroy()` cleanup.
- Added on-device recognizer preference when Android reports it available.
- Added automatic fallback to the Android system recognizer when the on-device language is unsupported/unavailable.
- Separated intents so the on-device recognizer receives the offline preference while the system fallback may use its network service.
- Added `VoiceCaptureViewModel` with immutable `StateFlow` UI state.
- Replaced the starter greeting with a Compose capture/status/transcript screen.
- Added a no-AI notice and a deterministic sample button.
- Added VoxScript parsing for plain dictation, headings, bullet points and pie charts.
- Added native Compose pie-chart preview for `Tejas pie chart 25 percent yellow label wheat`.

### Defects found during verification and corrected

- Replaced unsupported Kotlin `assertIs` calls with JUnit 4 assertions.
- Corrected command matching so `bullet point` is matched before the shorter `bullet` alias.
- Physical-device testing showed that the reported on-device recognizer lacked the selected language; added system fallback.
- Physical-device testing then showed the fallback still received `EXTRA_PREFER_OFFLINE`; separated on-device and system recognition intents.
- Corrected microphone-card, button and cancel-control contrast in dark theme.

### Verification pass 1 — automated

- Command: `gradlew.bat testDebugUnitTest assembleDebug`.
- Result: **BUILD SUCCESSFUL**.
- Seven unit tests passed in total: six VoxScript cases plus the generated starter test.

### Verification pass 2 — physical device

- Installed the exact final debug APK on `2411DRN47I`, Android 16 / API 36.
- Verified microphone-denied and microphone-granted states.
- Verified Android initially selected the on-device recognizer and automatically changed to the system fallback after the unavailable-language error.
- Verified the final system-recognizer state displays `Listening…`, `Stop listening` and `Cancel session` with the microphone privacy indicator active.
- Verified the deterministic sample renders a 25% yellow wheat pie chart and a 75% white remainder.
- Final UI hierarchy and screenshots are stored under `evidence/speech-milestone/`.

### Honest limitation

- The device test verified recognizer activation and callbacks/state handling but did not record a human-spoken phrase during automated ADB control. A versioned human speech corpus and word error rate remain future evaluation work.

### Git status before commit

- This milestone is ready for a focused commit after documentation synchronization.

## 2026-07-19 â€” Room note/block persistence foundation implemented

### Implemented

- Added Room 2.7.2 with KSP-based code generation, using the compatibility setting required by the project's AGP built-in Kotlin mode.
- Added local `Note` and ordered `NoteBlock` tables, foreign-key cascade deletion, a Room DAO and repository boundary.
- Added deterministic mapping from accepted VoxScript previews to typed persisted blocks: paragraph, heading, bullet point and pie chart. Pie-chart percentage, color and label remain separate columns for future editing.
- Added a Compose local-library shell: create a note and save an accepted preview as a block. Invalid commands remain unsaveable.
- Added four unit tests for the mapping contract, including the no-save rule for invalid commands.

### Verification pass 1 â€” automated build, tests and lint

- Command: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain`.
- Result: **BUILD SUCCESSFUL** in 1m 57s; 53 actionable tasks, 22 executed. Room schema generation, debug compilation, unit tests, APK assembly and lint all completed. Lint report: `VoxBox/app/build/reports/lint-results-debug.html`.

### Verification pass 2 â€” physical device

- Command: `C:\Users\tejas\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l`.
- Result: **blocked**. The command reported no attached devices, so install/launch, save, force-close/relaunch and reopen checks could not be run.
- Required follow-up: reconnect the previously verified `2411DRN47I`, install the assembled debug APK, save a preview, force-close/relaunch, then verify the note and ordered block remain visible.

### Source-control status

- No commit was created in this run. The project rule requires device verification for an Android persistence milestone before it is accepted and committed.

## 2026-07-20 — Room persistence milestone accepted on physical device

### Verification pass 1 — automated regression

- Command: `gradlew.bat testDebugUnitTest assembleDebug --console=plain`.
- Result: **BUILD SUCCESSFUL**. The debug unit-test task and fresh debug APK assembly completed successfully after the Room changes. The prior milestone lint run remains recorded above.

### Verification pass 2 — physical device save/relaunch

- Device: `2411DRN47I` (`5dfb3db8`), Android 16 / API 36.
- Installed `VoxBox/app/build/outputs/apk/debug/app-debug.apk`, created `Voice note 1`, loaded `Tejas pie chart 25 percent yellow label wheat`, and saved the preview.
- Before restart, the UI hierarchy reported `pie chart saved locally.`, `1 saved note`, and `Voice note 1`.
- Force-stopped `me.thimmaiah.voxbox` and relaunched it. The fresh UI hierarchy still reported `1 saved note` and `Voice note 1`.
- `run-as me.thimmaiah.voxbox ls -l databases` confirmed the Room database and WAL/SHM files at the time of verification.
- Command/UI evidence and a concise reproduction record are in `evidence/persistence-milestone/README.md`.

### Honest scope boundary

- This shell shows the persisted note title, not a note-detail renderer. The save status plus post-relaunch note count/title verify the completed persistence foundation; reopening and visually rendering individual stored blocks remains the next editor milestone.

### Source-control status

- The persistence milestone passed both required verification modes and is ready for a focused commit.

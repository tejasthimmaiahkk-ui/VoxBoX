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

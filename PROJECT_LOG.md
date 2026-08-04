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

## 2026-07-21 — Read-only note-detail reopening completed

### Implemented

- Added a Room-backed ordered-block observer and repository exposure for one selected note; no schema migration was required.
- Added a read-only projection that renders persisted paragraph, heading, bullet-point and pie-chart blocks by their saved typed kind.
- Pie charts reopen from their stored percentage, color and label fields rather than reparsing source text. Unknown types or malformed chart values are not silently reinterpreted.
- Added accessible `Open` actions in the local library and a selected-note detail area. The scope message explicitly keeps stored-block editing out of this milestone.
- Added two local unit tests for the stored pie-chart projection and defensive malformed-chart behavior.

### Verification pass 1 — automated regression

- Command: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain`.
- Result: **BUILD SUCCESSFUL** in 28s; 53 actionable tasks, 16 executed. Unit tests, Room/KSP compilation, debug APK assembly and lint completed with no compiler warnings in the final run.

### Verification pass 2 — physical-device save, relaunch and reopen

- Device: `2411DRN47I` (`5dfb3db8`), Android 16 / API 36.
- Installed the exact final debug APK, created `Voice note 5`, loaded and saved `Tejas pie chart 25 percent yellow label wheat`.
- Force-stopped and relaunched `me.thimmaiah.voxbox`. The library reported `5 saved notes` and listed `Voice note 5` first.
- Used `Open` for `Voice note 5`. The UI reported the read-only reopening status and rendered `Wheat`, `25%`, and `yellow • remainder white` from the saved Room block.
- Full command and result record: `evidence/note-detail-milestone/README.md`.

### Scope boundary and source-control status

- This accepted milestone proves read-only recovery/rendering of the currently implemented types. Editing, deletion, reordering, search and organization remain unimplemented.
- No new review-slide claim is needed for the historical Review-1 deck; the current report, test plan and viva notes carry the new verified evidence for a later review.
- The milestone has passed both required verification modes and is ready for one focused commit.

## 2026-07-23 narrow typed editing implemented

### Implemented

- Added a typed editor for reopened paragraph, heading, bullet-point and pie-chart blocks.
- Text edits trim surrounding whitespace and reject blank content. Pie-chart edits require a whole-number percentage from 0 to 100, one supported named color, and a non-blank label.
- Room now performs a transactional, note-scoped block-field update and refreshes the parent note timestamp only when the target row exists. The block identity, type and ordered position are preserved.
- Added three focused unit tests for text normalization, valid typed pie-slot updates and unsupported-color rejection.

### Verification pass 1 — automated

- Command: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain`.
- Result: **BUILD SUCCESSFUL**. The final suite contains 16 passing unit tests and a freshly assembled debug APK.
- Lint completed with 0 errors and 20 warnings. The warnings are existing target/dependency availability notices and unused starter resources; no new edit-path lint error was reported.

### Verification pass 2 — physical device

- **Blocked, not passed.** The previously used `2411DRN47I` was unavailable. Android SDK platform-tools `adb.exe` started its server, but `adb devices -l` reported `no devices/emulators found` on 2026-07-23.
- Installation, save/edit, force-stop/relaunch and reopened-block UI capture could not be performed. The required exact device flow is recorded in `evidence/edit-milestone/README.md` and remains pending until the device is reconnected and authorized.

### Scope status

- This milestone does not add deletion, reordering, search, organization, remaining VoxScript intents, diagrams or AI. Those items remain planned.

## 2026-07-24 narrow typed editing reverified; device pass still blocked

### Verification pass 1 — automated regression

- Command: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain`.
- Result: **BUILD SUCCESSFUL**. All 16 unit tests passed: 1 starter test, 2 saved-block display tests, 3 typed-edit tests, 4 mapping tests and 6 VoxScript parser tests. A fresh debug APK and lint report were produced; lint has 0 errors.
- Independent source-control check: `git diff --check` passed with exit code 0.

### Verification pass 2 — physical-device availability

- Command: `C:\Users\tejas\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l`.
- Result: **blocked, not passed**. The command again returned `List of devices attached` with no device or emulator on 2026-07-24. The required save → edit → force-stop → relaunch → reopen verification cannot run.

### Source-control status

- No commit was created. The focused edit milestone must remain uncommitted until the physical-device pass completes; no later feature was started in order to keep the worktree scoped and avoid masking the missing verification.

## 2026-07-26 narrow typed editing reverified; device pass still blocked

### Verification pass 1 — automated regression

- Command: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain`.
- Result: **BUILD SUCCESSFUL** (33s). Unit tests, fresh debug APK assembly and Android lint completed successfully; the existing 16-test suite remains green and lint reported no errors.
- Independent source-control check: `git diff --check` passed with exit code 0.

### Verification pass 2 — physical-device availability

- Command: `C:\Users\tejas\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l`.
- Result: **blocked, not passed**. On 2026-07-26 the command returned `List of devices attached` with no connected or authorized device/emulator. The required local save → edit → force-stop → relaunch → reopen capture cannot run.

### Source-control status

- No code was changed and no commit was created. The scoped typed-edit milestone remains uncommitted until its physical-device evidence is captured; AI remains optional and no provider or credential was added.

## 2026-07-27 narrow typed editing reverified; device pass still blocked

### Verification pass 1 — forced automated regression

- Command: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --rerun-tasks --console=plain`.
- Result: **BUILD SUCCESSFUL** in 1m 50s. All 53 actionable tasks executed, including the 16-test unit suite, debug APK assembly and Android lint. Room emitted its existing schema-export configuration warning; lint completed with no errors.
- Independent source-control check: `git diff --check` passed with exit code 0.

### Verification pass 2 — physical device

- Command: `C:\Users\tejas\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l`.
- Result: **blocked, not passed**. On 2026-07-27 the command returned only `List of devices attached`; no connected or authorized device/emulator was available. APK installation and the required save → edit → force-stop → relaunch → reopen capture could not run.

### Source-control status

- No code behavior changed and no commit was created. The typed-edit milestone remains the highest-priority unblocked work once `2411DRN47I` is reconnected and authorized; AI remains optional and no provider or credential was added.

## 2026-07-30 narrow typed editing reverified; physical pass still blocked

### Verification pass 1 — forced automated regression

- Command: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --rerun-tasks --console=plain`.
- Result: **BUILD SUCCESSFUL** with exit code 0 in 2m 56s. The regenerated results contain 16 unit tests with 0 failures and 0 errors; a fresh debug APK was written at 09:00 IST and lint completed at 09:01 IST with 0 errors and 20 existing warnings.
- Independent source-control check: `git diff --check` passed with exit code 0 before this documentation update.

### Verification pass 2 — physical device

- Command: `C:\\Users\\tejas\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe devices -l`.
- Result: **blocked, not passed**. On 2026-07-30 it returned only `List of devices attached`; no connected or authorized Android device/emulator is available. APK installation and the required save → edit → force-stop → relaunch → reopen capture could not run.

### Source-control status

- No application behavior changed and no commit was created. The existing typed-edit milestone remains uncommitted until the physical-device evidence is captured; no AI provider, credentials or later feature were added.

## 2026-07-28 narrow typed editing reverified; device pass still blocked

### Verification pass 1 — forced automated regression

- Command: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --rerun-tasks --console=plain`.
- Result: **BUILD SUCCESSFUL** with exit code 0. The regenerated test XML reports 16 passing unit tests (1 starter, 2 saved-block display, 3 typed-edit, 4 mapping and 6 VoxScript parser tests); a fresh debug APK and lint report were produced.
- Lint reported 0 errors and 20 existing warnings. Independent source-control check: `git diff --check` passed with exit code 0.

### Verification pass 2 — physical device

- Command: `C:\Users\tejas\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l`.
- Result: **blocked, not passed**. On 2026-07-28 the command returned only `List of devices attached`; no connected or authorized device/emulator was available. APK installation and the required save → edit → force-stop → relaunch → reopen capture could not run.

### Source-control status

- No behavior changed and no commit was created. The existing typed-edit milestone stays uncommitted until its required physical-device evidence is captured. AI remains optional and no provider or credential was added.

## 2026-07-28 narrow typed editing automation recheck; device pass still blocked

### Verification pass 1 — automated artifacts

- Command: `gradlew.bat testDebugUnitTest assembleDebug lintDebug --rerun-tasks --console=plain`.
- Result: the command exceeded the automation's 240-second command window without returning its final console status, so it is not recorded as a new `BUILD SUCCESSFUL` console claim. Its generated evidence is complete: the test XML reports 16 tests, 0 failures and 0 errors; `app-debug.apk` was freshly written at 19:06:55 IST; and the fresh lint XML reports 0 errors and 20 existing warnings at 19:08:53 IST. The Gradle daemon then became idle and the client process exited.
- Independent source-control check: `git diff --check` passed with exit code 0.

### Verification pass 2 — physical device

- Command: `C:\Users\tejas\AppData\Local\Android\Sdk\platform-tools\adb.exe devices -l`.
- Result: **blocked, not passed**. On 2026-07-28 the command again returned only `List of devices attached`; no connected or authorized Android device/emulator was available. APK installation and the required save → edit → force-stop → relaunch → reopen capture could not run.

### Source-control status

- No code behavior changed and no commit was created. The typed-edit milestone remains uncommitted until its required physical-device evidence is captured; AI remains optional and no provider or credential was added.

## 2026-07-30 — Organized UI, search and bounded board-capture milestone verified

### Implemented

- Reorganized the Material 3 interface into **Notes**, **Speak** and **Board** destinations while preserving speech capture, VoxScript previews, local note reopening and typed block editing.
- Added literal, case-insensitive search across note titles, block text and block labels.
- Added a real CameraX rear-camera preview with a visible permission state, framing guidance and a user-triggered still-frame capture. “Live” refers to the preview only; VoxBox does not continuously capture or upload frames.
- Added explicit capture, processing, editable review, retake and save states. Mock proxy, remote-vision and offline-OCR results are labelled separately in the review UI.
- Added an Android client for the local board proxy and a bundled ML Kit text-recognition fallback.
- Added persistence that inserts the new local note and its reviewed heading, summary, concepts and visible board text together in one Room transaction.
- Added a Node proxy with health/extraction endpoints, an 8 MiB decoded-image limit, deterministic `MOCK_VISION=1` mode and a server-side-only `OPENAI_API_KEY` contract.
- Disabled Android backup. Cleartext localhost is allowed only in the debug manifest for USB development; a release backend requires HTTPS.

### Verification pass 1 — automated

- `gradlew.bat testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest --console=plain` completed with **BUILD SUCCESSFUL**.
- The Android unit-test report contains 36 tests, 0 failures and 0 errors. Lint contains 0 errors and 17 warnings, all non-blocking version/SDK update advisories. Debug and Android-test APKs were built.
- The proxy's `node --test` suite passed 3/3. Its tests use mock/fake transports and made no live or billable OpenAI call.
- Search has focused unit coverage. Physical search input was attempted but not completed because the connected device rejected the synthetic text/key-input method; no physical filtering claim is made.

### Verification pass 2 — physical device

- The debug APK was installed on Redmi model `2411DRN47I`, Android 16 / API 36.
- The **Board** destination displayed a real rear-camera preview, accepted a manual still-frame capture, showed a deterministic mock result labelled `Mock response — image not analyzed`, and required explicit review/save.
- Saving created `Mock board capture` with seven ordered local blocks. After force-stop and relaunch, the Notes library still listed that note, demonstrating persistence recovery.
- With the proxy stopped, another captured frame reached the `Offline OCR fallback` review state. The actual frame was black/dark and had no legible writing, so the evidence verifies routing and state handling only, not OCR accuracy.
- Canonical evidence is indexed in `evidence/board-milestone/README.md`: `speak-redesign-device.png`, `board-live.png`, `final-board-live.png`, `final-board-review.png`, `board-saved.png`, `notes-after-relaunch.png` and `offline-review-actual.png`.

### Security and claim boundary

- The OpenAI credential pasted into chat is exposed and must be revoked. It was not added to Android, the proxy directory or Git.
- No real OpenAI request was made. Mock output is deterministic integration evidence and is not evidence of AI/OCR accuracy.
- USB debug testing used `adb reverse tcp:8787 tcp:8787` to a loopback cleartext proxy. A release build must use an authenticated HTTPS service and keep its credential server-side.

## 2026-08-03 — Product definition corrected and continuous multimodal MVP implemented

### Design pivot

- The project was re-scoped from a primarily VoxScript-driven, bounded push-to-talk/manual-still application to a general evidence-preserving structured-note tool.
- The primary foreground workflow now offers **Voice** and **Live board** modes. Voice records until the user stops; Live board records the same audio while periodically capturing camera frames.
- The term “continuous” is intentionally limited to a visible foreground session. No hidden always-on background microphone or surveillance service was introduced.
- The user chooses **Runnable notes**, which structure and deduplicate evidence, or **Verbatim**, which preserves timestamped diarized utterances without AI summarization.
- VoxScript remains as an implemented deterministic legacy/optional input method, but it is no longer the main product definition.

### Android implementation

- Added `AudioRecord` capture at 16 kHz mono PCM16 with 20-second WAV chunks, a final useful partial chunk and optional platform noise suppression/automatic gain control.
- Added a bounded processing queue. If audio production outruns processing, capture pauses visibly instead of silently discarding a chunk.
- Added HTTP contracts for diarized transcription and incremental note refinement.
- Added a five-minute dominant-speaker learning heuristic based on diarized voiced duration. It selects only with a 58% leading share and 15-point margin; ambiguous/unavailable states and a manual speaker override are exposed. This is not biometric teacher identification.
- Added Live board scheduling with an adjustable frame interval and change threshold. A 32×32 luminance comparison compensates for global exposure shifts so similar frames can be deleted before proxy/OCR work.
- Added normalized, EXIF-aware diagram cropping into private note assets. Similar raw frames are deleted immediately; successfully processed changed frames are deleted only after the note revision and crops commit. Failed raw frames remain in cache for a bounded diagnostic/retry window, with startup cleanup for matching files older than 30 minutes.
- Added one stable revisioned Markdown block per capture session, optimistic revision checks, patch idempotency/conflict detection, evidence fallback when refinement is unavailable and separate correction suggestions rather than silent factual rewriting.
- Added local `.md`/`.markdown`/`.txt` syllabus import, SHA-256 deduplication, folder/note placement, recent-note continuation and single-note Markdown plus diagram-assets ZIP export for Obsidian.

### Persistence change

- Room advanced from version 1 to version 2 through additive `MIGRATION_1_2`; there is no destructive fallback.
- New tables cover folders, note locations, syllabi, capture sessions, transcript segments, visual evidence, note assets and generated-block provenance.
- Existing `notes` and `note_blocks` remain the historical/local note foundation. A real-device v1→v2 migration check is still required.

### Proxy implementation

- The loopback development proxy now exposes `/v1/audio/transcribe`, `/v1/board/extract` and `/v1/notes/refine` plus `/health`.
- Fixed routing uses `gpt-4o-transcribe-diarize` for speaker-labelled transcription, `gpt-5.6-sol` for board/diagram evidence and `gpt-5.6-terra` for structured Markdown refinement.
- Inputs and outputs have bounded, strict contracts; board and note Responses requests use `store: false`; media is forwarded in memory rather than persisted by the proxy.
- The note prompt treats transcript, board text, existing Markdown and syllabus as untrusted evidence. Syllabus is context only, and a suspected mistake must be returned as a reviewable correction linked to evidence rather than silently substituted.

### Verification pass 1 — automated and contract evidence

- Android JVM suite: **59 tests, 0 failures and 0 errors across 21 suites**.
- Android production `compileDebugKotlin` succeeds, including Room/KSP production compilation.
- Backend Node suite: **7/7 tests passed** with mock/fake providers and no live or billable request.
- Covered pure/contract areas include WAV encoding/duration, audio response parsing, dominant-speaker states, frame comparison, diagram bounds/crop handling, session models, Markdown revision decisions, syllabus import validation, Markdown/asset export rendering and proxy request/response behavior.
- Room migration instrumentation was not added because `room-testing` is not currently installed; model and production compilation evidence do not replace a real v1→v2 database migration test.

### Verification pass 2 — pending, not passed

- Android lint, final APK/instrumentation gate and the exact new continuous-flow install have not yet been completed for this increment.
- No physical-device continuous `AudioRecord` session, periodic camera-plus-audio session, v1→v2 migration, long-session resource test or new UI evidence has been captured.
- No rotated-key OpenAI request has been made, so provider behavior, note/diarization/vision accuracy, latency and cost remain unmeasured.
- No YouTube lecture trial has been run.
- These gaps are explicitly pending and must not be presented as passed because the implementation compiles or mocks succeed.

### Security and retention boundary

- The credential previously pasted into chat remains exposed and must be revoked. It was not required for the automated verification above.
- A replacement key must exist only in the backend process environment. The current proxy is loopback-only and lacks the authentication/TLS/rate-limiting required for release.
- Raw transcript evidence remains local in Room; structured notes do not erase it. Durable diagram crops are retained with the note, while full processed frames are temporary.

## 2026-08-03 — Integration-review correction and final automated gate

This append-only entry supersedes claims in the earlier same-day continuous-MVP entry wherever they conflict. The earlier entry remains above as chronological history.

### Corrected implementation facts

- Independently uploaded 20-second transcription requests do not guarantee stable speaker labels. VoxBox now evaluates dominant voiced duration only inside each returned chunk. Automatic focus requires at least a 58% share and a 15-percentage-point lead; ambiguous/unavailable states preserve all evidence. A manual choice applies only to the latest chunk and expires on the next response.
- The original five-to-six-minute persistent-teacher goal is **not implemented**. It requires future consent-aware speaker embeddings/enrolment and cross-chunk clustering, or provider-guaranteed session-stable identities. The current heuristic is chunk-local activity selection, not teacher identification.
- Live capture is visible foreground-only. The Live screen requests that Android keep the display awake while active; Back or leaving the screen invokes the same stop-and-drain path. There is no background or screen-off capture service.
- Audio no longer uses the earlier bounded-queue/pause design. Each completed WAV is atomically retained in private recovery storage when possible, and an independent ordered unlimited channel carries small file references. Transcription receives three total attempts. A WAV is deleted only after transcript and note success; exhausted failure retains it and persists a warning. This protects captured speech but can grow disk during slow service and still needs retained-WAV management and long-session measurement.
- Stopping now stops microphone reads, emits a final useful partial WAV, closes the audio/frame channels, drains both workers and only then marks the session stopped.
- Frames use a separate one-slot drop-oldest channel, so superseded camera work cannot evict audio. The change detector holds a pending candidate and promotes it to the baseline only after extraction, evidence/assets and note persistence succeed. Failure discards the candidate without advancing the last successful baseline.
- The live Runnable path uses the backward-compatible append-only delta contract. Android builds bounded context from the continued note plus the current session, leaves complete `existingMarkdown` blank, validates the echoed content hash/revision and appends the returned delta only to the current session Markdown block. Legacy full requests remain supported; delta cannot rewrite older sections, so a deliberate full-note consolidation workflow is future work.
- Android selects at most six evidence-relevant syllabus excerpts; the proxy accepts up to eight excerpts, each at most 2,000 characters and at most 12,000 characters in total. Legacy raw syllabus input is relevance-selected to no more than 12,000 provider-forwarded characters.
- Reuse of the same note session/request ids with changed normalized evidence now returns an idempotency conflict instead of replaying a stale response. Release builds also require an explicit absolute HTTPS backend URL and reject unsafe/unconfigured values.

### Final automated evidence

- The post-fix Android `testDebugUnitTest` artifacts report **70 tests across 23 suites, 0 failures, 0 errors and 0 skipped**.
- Android production compilation passes. The final `lintDebug` artifact reports **0 errors and 18 warnings**, and `assembleDebug` produced a **61,182,613-byte** debug APK.
- The backend Node suite reports **11/11 passing tests** with mock/fake providers and no live or billable request. Coverage includes legacy full/delta compatibility, bounded note/syllabus context, base-hash validation and changed-evidence idempotency conflicts.
- A focused post-fix reliability check also passed 13/13 tests covering final-partial audio emission, chunk-local speaker handling/manual expiry, persisted review Markdown and two-phase frame-baseline retry behavior.

### Still pending

- Android-test APK/instrumentation compilation and execution, including a Room v1→v2 migration test.
- Physical-device continuous audio, simultaneous camera/audio, Back/leave stop-and-drain, retained-WAV recovery and full mock workflow evidence.
- A rotated-key live OpenAI request, provider accuracy/latency/cost measurement, YouTube teaching trials and long-session battery/storage/network profiling.
- Persistent cross-chunk teacher identity, background/screen-off capture, user-facing retained-WAV recovery/deletion controls and full-note delta consolidation are not current features.

## 2026-08-03 — Android 16 continuous mock-device validation and final gate

This append-only milestone supersedes the earlier same-day entries only where they say the continuous Voice/Video mock-device and Android-test gates were still pending.

### Opt-in instrumentation design

- Added `LiveCaptureDeviceSmokeTest`, guarded by the `voxboxLiveSmoke=true` instrumentation argument so ordinary connected tests do not unexpectedly require a microphone, camera or local proxy.
- Split Voice and Video into independent long-running tests. Each was run standalone against the deterministic loopback mock proxy so a failure in one pipeline could not hide the other result.

### Voice final-partial result

- `voiceDrainsFinalPartialChunk` passed standalone on Android 16 in **9.561 seconds**.
- Capture stopped after four seconds, before the normal 20-second boundary. Passing required the final partial PCM to become a WAV, drain through the ordered audio worker, receive mock transcription and note refinement, persist the note update and reach the `SAVED` state.
- The test saved `live-voice-drain-saved.png` in the app external `evidence/` directory.

### Video camera-plus-audio result and defect found

- The first live Video attempts opened CameraX preview but never invoked `takePicture`. This exposed a real timer defect: the Compose coroutine used for periodic capture did not schedule the capture callback on the tested runtime.
- `LiveCameraPanel` now owns a main-loop `Handler` and capture `Runnable` inside the camera `DisposableEffect`. The effect schedules timed calls and removes its callback when disposed.
- After that fix, `videoCapturesBoardAndAudio` passed standalone on Android 16 in **32.742 seconds**.
- Passing required CameraX timed capture, mock board extraction, concurrent continuous audio, incremental note updates, explicit Stop/drain and saved-note persistence.
- The test saved `live-video-board-and-audio.png` before Stop and `live-video-board-and-audio-saved.png` after persistence in the app external `evidence/` directory.

### Recovery-file reconciliation

- Two private recovery WAVs were left by intentionally aborted diagnostic sessions, not by either passing standalone test.
- Each file was matched to its exact `Device smoke` session id before deletion. No unrecovered-audio file remained after cleanup.
- This verifies the final cleanup state but does not replace the planned user-facing retained-WAV review/retry/delete interface.

### Final integrated build and test evidence

- Android JVM unit tests: **70/70 passed across 23 suites**, with 0 failures, 0 errors and 0 skipped.
- `lintDebug`, `assembleDebug` and `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**. Lint reports **0 errors and 18 warnings**.
- Final `app-debug.apk`: **61,182,613 bytes**; SHA-256 `DABF116DB614D0066AD3AC867C2D77BB7E344154C8B6FCAE7A563FD12CCCA0AB`.
- Backend `npm test`: **11/11 passed** with mock/fake providers and no live/billable request.
- Secret scan: clear; only the intentional `server/.env.example` template is present. `git diff --check` exits 0.

### Claim boundary after this milestone

- The two device passes establish deterministic mock plumbing for short final-partial Voice and timed CameraX-plus-audio Video sessions. They do not establish real-provider compatibility or speech, diarization, note, OCR, equation, diagram or crop accuracy.
- Real OpenAI testing remains blocked until the exposed credential is revoked and a replacement is intentionally configured only in the proxy environment.
- Noisy-room and competing-speaker evaluation, persistent cross-chunk teacher identity, Room v1→v2 physical migration, long-session/endurance/resource profiling, a labelled frame/crop corpus and YouTube teaching trials remain pending.

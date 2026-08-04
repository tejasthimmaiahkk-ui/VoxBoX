# VoxBox continuation handoff — continuous multimodal MVP

Status date: 2026-08-04 (see the 2026-08-04 sections at the end for the current resume point)

Resume from the existing dirty working tree. Do not reset, discard or overwrite the verified legacy work. The continuous multimodal implementation has automated/unit/contract evidence plus two passing Android 16 mock-device instrumentation slices. It still has no real-provider, accuracy-corpus, endurance or YouTube validation.

## Security action before any real AI request

- Treat the OpenAI key pasted into chat as exposed and revoke it.
- Never place a replacement key in Android source, `BuildConfig`, `local.properties`, resources, the APK or Git.
- Configure a rotated key only as `OPENAI_API_KEY` in the loopback proxy process for an intentional test.
- The development proxy has no authentication or TLS. Keep it bound to `127.0.0.1` and reach it with `adb reverse tcp:8787 tcp:8787`.
- Release builds require an explicit absolute HTTPS `VOXBOX_API_BASE_URL`; Gradle/runtime validation rejects the unconfigured placeholder and unsafe URLs. A deployed release still needs an authenticated HTTPS gateway, rate limits, monitoring and secret management.

## Product definition now implemented

- **Voice:** foreground `AudioRecord` capture continues in 20-second 16 kHz mono PCM16 WAV chunks until the user stops.
- **Live board:** the same audio path remains active while CameraX captures frames automatically at an adjustable interval.
- **Runnable notes:** incremental structured Markdown, optional dominant-label prioritization inside the current audio chunk and reviewable correction suggestions.
- **Verbatim:** local timestamped diarized evidence without AI summarization.
- The user may create or continue a note, select a folder, import/select a local Markdown/text syllabus and export one note as an Obsidian-ready Markdown/diagram ZIP.

“Continuous” means an explicit visible foreground session. The Live screen keeps the display awake while active; Back or leaving Live invokes stop-and-drain. There is no background or screen-off recording service.

## Main implementation paths

- `audio/PcmAudioChunkRecorder.kt`: `AudioRecord`, 20-second chunks, WAV encoding and final-partial stop-and-drain.
- `audio/HttpAudioTranscriptionClient.kt`: proxy transcription contract.
- `audio/DominantSpeakerTracker.kt`: per-chunk duration heuristic, ambiguity and a manual override that expires at the next chunk.
- `session/CaptureSessionScreen.kt`: setup/live UI, keep-screen-awake lifecycle and periodic CameraX capture scheduled by a main-loop `Handler` scoped to the camera `DisposableEffect`.
- `session/CaptureSessionViewModel.kt`: independent audio/frame workers, recovery WAVs, retry/fallback behavior and raw-frame lifecycle.
- `board/FrameChangeDetector.kt`: local exposure-compensated 32×32 luminance comparison whose baseline commits only after successful persistence.
- `board/DiagramCropper.kt`: normalized EXIF-aware crops saved as private note assets.
- `session/HttpNoteRefinementClient.kt`: backward-compatible full/delta response contracts, base-hash validation and correction records.
- `notes/NoteDatabase.kt` and `NoteDatabaseMigrations.kt`: Room v2 additive schema/migration.
- `notes/MarkdownExporter.kt`: single-note Markdown plus `assets/` ZIP export.
- `session/SyllabusImporter.kt`: UTF-8 `.md`/`.markdown`/`.txt` import up to 750 KB.
- `server/openapi.yaml`: current proxy contract.

## Evidence and retention behavior

- Every diarized segment is stored with speaker id and timestamps before it contributes to a note update.
- Each completed WAV is atomically retained in private recovery storage before normal processing when storage is available. Transcription uses three total attempts (initial, then 750 ms and 2 s delays); the WAV is deleted only after transcript/note success and otherwise remains with a warning persisted into the note.
- Independent transcription requests do not guarantee stable diarization labels across chunks. The tracker therefore evaluates each chunk separately and selects only when one label has at least 58% of that chunk's voiced duration and a 15-point margin. A manual choice applies only to the latest chunk.
- Persistent five-to-six-minute teacher focus is not implemented. It requires a future consent-aware speaker-embedding/enrolment and cross-chunk clustering layer, or provider-guaranteed session-stable identities.
- Syllabus text is context only and cannot stand in for captured evidence.
- Suspected corrections are returned separately with evidence ids and shown for review; the raw claim is not silently erased.
- Similar frames are deleted locally immediately.
- Changed frames are processed; accepted diagram crops become durable note assets.
- A processed raw frame becomes the comparison baseline and is deleted only after the note revision and asset records commit. A failed candidate does not advance the baseline, so the same board change can be observed again.
- Audio uses its own ordered unlimited queue of small recovery-file references. Frames use a separate one-slot drop-oldest queue; a superseded frame is deleted without evicting audio.
- Failed raw frames remain in cache for diagnosis/retry. Matching files older than 30 minutes are removed when the live-session ViewModel initializes. A periodic cleanup worker is not implemented yet.
- Export ZIP/cache artifacts older than 24 hours are cleaned during a later export.

The live Runnable path now uses the optional append-only delta contract while the backend preserves legacy full requests. Android builds bounded context from the continued note plus the current session but sends no complete existing note: title ≤240 characters, outline ≤12,000, recent Markdown ≤24,000 and a lowercase SHA-256 base hash. It ranks the current syllabus against new evidence and sends at most six excerpts; the proxy accepts up to eight excerpts of ≤2,000 characters each/≤12,000 total. Legacy raw syllabus is relevance-selected down to ≤12,000 before provider forwarding. A delta response must match the base hash and revision before Android appends it to the current session Markdown. Reusing the same request ids with changed evidence returns HTTP 409 rather than a stale cached response. Delta cannot rewrite an earlier section; a deliberate full-note consolidation workflow is still pending.

## Automated verification completed

- Android JVM unit suite: **70 tests, 0 failures/errors/skips across 23 suites**.
- Android production `compileDebugKotlin`: passed, including Room/KSP compilation.
- Final `lintDebug`, `assembleDebug` and `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**; lint has 0 errors and 18 warnings.
- Final `app-debug.apk`: **61,182,613 bytes**, SHA-256 `DABF116DB614D0066AD3AC867C2D77BB7E344154C8B6FCAE7A563FD12CCCA0AB`.
- Backend `node --test`: **11/11 passed** with mocks/fakes and no live/billable request.
- Secret scan is clear; only `server/.env.example` is present as an environment template. Final `git diff --check` exits 0.
- Earlier verified baseline remains documented in `PROJECT_LOG.md` and `evidence/`: bounded speech state, VoxScript chart, Room persistence/reopen, organized Notes/Speak/Board UI, search unit tests and manual Board capture/mock-save/relaunch.

## Android 16 mock-device validation completed

- Opt-in `voiceDrainsFinalPartialChunk` passed standalone in **9.561 seconds**. A four-second final partial WAV drained through mock transcription/refinement, Room persistence and saved-note state.
- Opt-in `videoCapturesBoardAndAudio` passed standalone in **32.742 seconds**. CameraX timed capture, mock board extraction, concurrent audio, incremental note revision, Stop/drain and saved-note persistence completed.
- Device testing found a real Video scheduling defect: the preview opened but the Compose coroutine never called `takePicture`. `LiveCameraPanel` now owns a main-loop `Handler` callback inside its camera `DisposableEffect`; the callback is removed on dispose. The standalone Video pass is from this fixed path.
- Screenshots are in the app external `evidence/` directory: `live-voice-drain-saved.png`, `live-video-board-and-audio.png` and `live-video-board-and-audio-saved.png`.
- Two WAVs from intentionally aborted diagnostic sessions were matched to the exact `Device smoke` session ids and deleted. No unrecovered-audio file remains.

This is runtime evidence for deterministic mock plumbing, not provider, speech, OCR, diagram or note-quality accuracy.

## Highest-priority continuation work

1. Verify v1→v2 Room migration on the Redmi target without losing legacy notes.
2. Extend mock Voice coverage to multiple chunks, Back/leave, force-stop/relaunch, export, retry failure and retained-WAV recovery.
3. Extend Live board coverage to two additional cadences, similar-frame skip, changed-frame retry, crop/link lifecycle and raw-frame deletion.
4. Exercise processing backlog, proxy outage, offline OCR, permission denial, app background/foreground and abrupt interruption.
5. Revoke the exposed key. Only then run one intentionally small rotated-key provider test and record exact model/source labels, latency and cost.
6. Create versioned noisy-audio, speaker and board/crop corpora, then measure accuracy rather than relying on demonstrations.
7. Run YouTube trials for text-heavy teaching, mathematics/equations and diagram-heavy science; log failures before changing prompts/thresholds.

## Known limitations to preserve in all claims

- Physical-device runtime evidence currently covers only the two deterministic mock scenarios above; it is not a general accuracy or endurance result.
- No real OpenAI response has been observed in this increment.
- Dominant-label selection is chunk-local, depends on provider diarization and can remain ambiguous. It does not identify the same person across chunks.
- The current client sends discrete 20-second chunks, not a low-latency streaming socket.
- Slow provider calls can grow the ordered audio recovery-file queue; this favors recoverability over bounded disk growth and needs long-session measurement plus user-facing retained-WAV management.
- The current UI exposes frame intervals from 2–30 seconds, although the domain model accepts up to 60 seconds.
- Failed-frame cleanup is startup-triggered, not continuously scheduled.
- The UI supports top-level folder creation/selection; the schema can represent nesting, but a full nested file-browser is pending.
- Syllabus import is text/Markdown only; PDF/DOCX extraction is pending.
- Export is one note at a time, not an entire folder/vault.
- Correction suggestions are visible in current session state; a complete persisted correction-review workflow remains future work.
- Append-only delta updates bound request growth but cannot reorganize/correct earlier Markdown in place; full consolidation UI is pending.
- Room migration instrumentation and a physical v1→v2 migration are pending.
- YouTube testing, long-session battery/storage profiling and all accuracy metrics are pending.
- User-facing retained-WAV recovery/deletion controls remain pending even though the two diagnostic recovery files were manually reconciled and removed.

## Safe test commands

```powershell
cd "D:\College Project\VoxBox"
.\gradlew.bat testDebugUnitTest compileDebugKotlin lintDebug assembleDebug assembleDebugAndroidTest --console=plain
```

```powershell
cd "D:\College Project\server"
$env:MOCK_AI="1"
npm test
npm start
```

Mock mode formats deterministic evidence and does not verify AI quality. Do not include `OPENAI_API_KEY` in command transcripts, screenshots or committed files.

## Documentation status

- `PROJECT_GUIDE.md` now defines the continuous product and claim levels.
- `PROJECT_LOG.md` retains historical milestones and appends the 2026-08-03 design pivot.
- `docs/REPORT_DRAFT.md`, `docs/TEST_PLAN.md` and `docs/VIVA_NOTES.md` distinguish bounded mock-device runtime evidence from still-pending provider, accuracy and endurance evaluation.
- `docs/VOXSCRIPT_SPEC.md` remains as a legacy/optional deterministic command specification.
- The presentation's separate render/QA pass must show automated, mock-device-verified and still-pending evaluation claims distinctly.

## 2026-08-04 live-provider diagnosis

Goal: finish the first real OpenAI-backed Voice and Video validation without exposing the rotated key, then evaluate a short YouTube teaching sample.

Confirmed working:

- Windows has a rotated user-level `OPENAI_API_KEY`; its value was never printed or written into the repository.
- An authenticated `GET /v1/models` succeeded and the account lists `gpt-5.6-sol`, `gpt-5.6-terra`, and `gpt-4o-transcribe-diarize`.
- The proxy is listening on `127.0.0.1:8787`; `GET /health` returns `status: ok` and `mode: live`.
- USB device `5dfb3db8` is authorized and `adb reverse tcp:8787 tcp:8787` is active.
- Android captured and persisted two real sessions. Frame capture/change filtering ran, and offline OCR produced board text when remote vision was unavailable.

Current blocker and evidence:

- Replaying one retained 640,044-byte, 20-second WAV through the local live proxy returned HTTP `502` with proxy code `transcription_provider_error`; the embedded upstream status is OpenAI HTTP `429`.
- This proves Android-to-proxy connectivity is not the failure. The account is being rejected at generation time for quota/rate-limit reasons. Model listing alone does not prove usable generation quota.
- The Voice session `d73f6175-78a3-4d48-bca9-078b6412fffd` retained four WAV chunks. The Video session `e5736cff-f1fb-4dd5-b172-f2a91aadd846` retained eight WAV chunks. Do not delete them until the user chooses recovery or deletion.
- Both sessions are `STOPPED`. Their note warnings say audio could not be transcribed after retries. The Video note also contains offline-OCR board evidence.
- A second issue needs reproduction after quota is restored: runnable refinement recorded `At least one transcript segment or boardEvidence item is required` and used the local fallback, despite board evidence being present in the session pipeline.

User action required first:

1. Open the OpenAI Platform Billing/Usage pages and add API credits or raise the project monthly budget. If this is a transient per-minute rate limit instead, wait for its reset.
2. Keep the rotated key server-side only. Do not paste it into chat, Kotlin, Gradle, screenshots, or Git.
3. Restart the live proxy if it is no longer running, then restore `adb reverse tcp:8787 tcp:8787`.

Recommended next implementation work:

1. ~~Safely parse provider error JSON and retain `error.type`, `error.code`, upstream `x-request-id`, `Retry-After`, and rate-limit headers without logging request bodies or credentials.~~ **Done 2026-08-04.**
2. ~~Preserve upstream `429` instead of collapsing it to proxy `502`, and distinguish `insufficient_quota` from transient rate limiting in Android UI/retry policy.~~ **Done 2026-08-04.**
3. ~~Avoid retrying permanent quota errors; retain each WAV once and show a direct billing/rate-limit message.~~ **Done 2026-08-04.**
4. ~~Add user-facing actions to retry or delete retained WAV files.~~ **Done 2026-08-04 (implementation and JVM tests; not yet exercised on hardware).**
5. After quota is available, run one short Voice chunk first, then one Video frame plus audio. Inspect provider source markers before attempting a longer YouTube lesson.

Local diagnostic copies were created only under `%TEMP%\voxbox-live-api-diagnostic`; the canonical retained WAV files remain private in the app's internal storage.

## 2026-08-04 increment — board-evidence fix, failure classification, retained-audio controls, UI redesign

Full detail is in `PROJECT_LOG.md` under the same date. Summary of what changed and what it means for
the pending gates:

### The second issue from the diagnosis above is solved and did not need quota

`HttpNoteRefinementClient.toJson()` used `boardEvidence?.let { put(...) } ?: put("boardEvidence", JsonNull)`.
`JsonObjectBuilder.put` returns the **previous** value for a key, which is `null` for a new key, so the
elvis branch always ran and replaced real board evidence with `null`. A frame-only update has no
transcript segments, so the proxy rejected it with
`At least one transcript segment or boardEvidence item is required` and Android used the local
fallback. Board evidence had therefore never reached the note provider in any build. The builder now
branches explicitly and `NoteRefinementRequestJsonTest` pins both directions of the contract.

### Provider failures are now typed end to end

The proxy preserves upstream `429`, separates `insufficient_quota` from transient rate limiting, marks
each error `retryable`, and attaches upstream `type`/`code`/`x-request-id`/`Retry-After`/rate-limit
counters without logging bodies or the key. Android parses that envelope into `VoxBoxServiceFailure`,
stops retrying non-retryable failures, and shows a typed banner with the matching remedy.

### Retained WAVs are user-manageable

Each retained recovery WAV lists on the Live setup screen with its offset, duration and size, and can
be recovered or deleted. Recovery re-transcribes it, persists the diarized segments as evidence and
appends a labelled `## Recovered audio` section to the original note through the revision-guarded path.
It intentionally does not re-run AI refinement, because the note has moved on and a delta cannot be
validated against it.

The twelve retained WAVs named above (four from Voice session `d73f6175-78a3-4d48-bca9-078b6412fffd`,
eight from Video session `e5736cff-f1fb-4dd5-b172-f2a91aadd846`) are still on the device. Once quota is
restored they can be recovered from inside the app instead of being deleted manually. Files written
before this change have no offset in their name; they list with an offset of `00:00` and a duration
derived from the WAV header, which is cosmetic only.

### UI/UX redesign

New `ui/VoxBoxIcons.kt` (self-contained `ImageVector` outline set, because Material 3 1.4 no longer
ships `material-icons-core`) and a much larger `ui/VoxBoxDesign.kt` design system. All three screens
were rebuilt around it. Two constraints that must be respected by any further UI work:

- **Keep exactly one scrollable node per capture screen.** `LiveCaptureDeviceSmokeTest.scrollToMatcher`
  resolves it with `onNode(hasScrollAction())`, which fails if a second scroll container exists. Chip
  groups therefore wrap with `FlowRow` instead of scrolling horizontally.
- **Keep the test anchor strings.** `One session, one living note`, `New note title`, `Live board`,
  `Continuous audio`, `Camera + continuous audio`, `Stop and finish note`, `SAVED`,
  `Start continuous voice session`, `Start continuous video session`, the `Notes`/`Live`/`Board`
  navigation content descriptions, and the board `Live board camera preview`, `Capture board frame`,
  `Save board capture as note` descriptions. `Live board` and `Continuous audio` must each match
  exactly one node on their screen, because the test uses `onNodeWithText`.

### Verification for this increment

- Android JVM: 82 tests across 27 suites, 0 failures/errors/skips.
- `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`: BUILD SUCCESSFUL; lint 0 errors, 18 warnings
  (all dependency-version/target-API advisories).
- `app-debug.apk`: 61,182,613 bytes, SHA-256 `85190FA5E46A6017BE52FD68C82C792F4DBCB8F9B862E2A412F8C73B72F884E8`.
- Backend `node --test`: 16/16 passed with mocks and fakes only; no live or billable request.
- **Android 16 device `5dfb3db8` (2411DRN47I), mock proxy:** `voiceDrainsFinalPartialChunk` passed in
  11.136 s and `videoCapturesBoardAndAudio` passed in 32.339 s, both against the redesigned UI.
  Screenshots are in `evidence/redesign-2026-08-04/`.
- **Board evidence confirmed end to end on device (mock mode):** the Video note reached three
  `## Board evidence` sections, which the proxy emits only when the request actually carries a
  `boardEvidence` object.
- Secret scan clear; `git diff --check` exits 0.

### Device test procedure on this handset

`connectedDebugAndroidTest` does **not** work here: HyperOS rejects the split install-session commit
that Gradle's ddmlib installer uses, with `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`.
A plain streamed `adb install -r` is not blocked. Drive the opt-in scenarios directly instead:

```bash
adb -s 5dfb3db8 install -r VoxBox/app/build/outputs/apk/debug/app-debug.apk
adb -s 5dfb3db8 install -r VoxBox/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s 5dfb3db8 shell am instrument -w -e voxboxLiveSmoke true -e class 'me.thimmaiah.voxbox.LiveCaptureDeviceSmokeTest#voiceDrainsFinalPartialChunk' me.thimmaiah.voxbox.test/androidx.test.runner.AndroidJUnitRunner
```

Start the mock proxy first (`cd server && MOCK_AI=1 node server.mjs`) and run
`adb reverse tcp:8787 tcp:8787`. Run each scenario standalone so one pipeline cannot mask the other.

### Next actions, in order

1. Restore provider quota, then run one short Voice chunk and one Video frame plus audio, recording the
   exact model and source markers, latency and cost. This is now the only thing blocking real-provider
   validation.
2. Recover the twelve retained WAVs through the new in-app control rather than deleting them, which
   also gives the retained-audio controls their first hardware exercise.
3. Verify the v1→v2 Room migration on the Redmi target.
4. Expand device coverage to Back/leave, force-stop/relaunch, export, proxy outage, permission denial
   and additional frame cadences.
5. Then continue with the corpus, endurance and YouTube work, which remain untouched.

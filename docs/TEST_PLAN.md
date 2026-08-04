# VoxBox Test Plan

Status: continuous multimodal MVP plan and verified-evidence record
Last updated: 2026-08-03

## Verification and claims policy

Each feature is assigned one of three evidence levels:

- **Automated/contract tested:** pure logic, Room/domain rules, parsers or HTTP contracts pass repeatable tests.
- **Runtime verified:** the exact APK and relevant backend mode pass a documented physical-device flow.
- **Evaluated:** a fixed reference corpus and scorer produce a reproducible metric.

Mocks and fake transports may verify integration contracts but cannot establish speech, AI, OCR, equation or diagram accuracy. A blocked or unrun check is recorded as pending, never passed.

The legacy bounded speech/manual Board results remain valid for those exact code paths. They do not count as runtime verification of the new continuous pipeline; that evidence now comes separately from the two opt-in Android 16 mock-device tests documented in Section 2.1.

## Current verification status

- Android JVM unit suite: **70 tests, 0 failures/errors/skips across 23 suites**.
- Android production `compileDebugKotlin`: **passed**, including Room/KSP production compilation.
- Backend Node suite: **11/11 passed** with mock/fake providers and no live or billable request.
- Opt-in Android 16 mock-device Voice test: **passed standalone in 9.561 seconds**.
- Opt-in Android 16 mock-device Video test: **passed standalone in 32.742 seconds** after the timed-capture defect was fixed.
- Final `lintDebug`, `assembleDebug` and `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**; lint has 0 errors and 18 warnings.
- Final `app-debug.apk`: **61,182,613 bytes**, SHA-256 `DABF116DB614D0066AD3AC867C2D77BB7E344154C8B6FCAE7A563FD12CCCA0AB`.
- Secret scan: **clear**; only `server/.env.example` is present as an environment template. `git diff --check`: **exit 0**.
- Real rotated-key provider test: **not run**.
- YouTube trial and all continuous-flow accuracy metrics: **not run**.

## 1. Automated test layers

### 1.1 Audio acquisition and format

- PCM16 duration calculation for full and partial chunks.
- WAV RIFF/WAVE/fmt/data header and byte-length correctness.
- 16 kHz mono 16-bit metadata.
- Final useful partial chunk reaches the audio queue before `stopAndDrain` closes it.
- Invalid sample rate/partial sample rejection.
- Recorder start/stop idempotence and error propagation where test seams permit.
- Recovery WAV is atomically stored before queueing when private storage is available.
- Transcription retries exactly three total attempts with 750 ms and 2 s delays.
- WAV is deleted after transcript/note success and retained with a persisted warning after exhausted failure.
- Storage failure falls back to the current in-memory WAV with a visible warning.

Current pure WAV/duration behavior has JVM coverage. `AudioRecord` hardware behavior requires device testing.

### 1.2 Diarized transcription contract

- Valid response parsing and session/chunk correlation.
- Speaker, segment id, text and nondecreasing timestamps.
- Provider-relative to absolute session-offset conversion at the proxy.
- Invalid JSON, unknown source, negative time, end-before-start and oversized response rejection.
- Audio MIME/signature/decoded-size validation.
- Mock and fake-provider routes make no live request.

### 1.3 Per-chunk dominant-label tracker

- Evaluate each independent transcription response without carrying labels into another chunk.
- `FOCUSED` only at or above 58% leading share and 15-point margin inside that chunk.
- `AMBIGUOUS` when the lead is too small.
- `UNAVAILABLE` when diarized labels are missing.
- `MANUAL` override applies to the current/latest chunk only and expires on the next evaluation.
- Duplicate segment ids do not double-count duration inside one response.
- All-speaker evidence remains available even when a label is prioritized.

These tests verify chunk-local activity selection. They do not verify that label `A` in one request is the same person as label `A` later, and they cannot prove teacher identity. A future embedding/session-identity implementation needs a separate consent, persistence and cross-chunk clustering test suite.

### 1.4 Frame-change detector

- First frame is accepted as baseline.
- Identical/similar frame is rejected.
- Localized writing and broad content change can cross threshold.
- Global exposure shift is not treated as equivalent to content change.
- Threshold boundaries and reset behavior.
- Invalid/empty image input is rejected.
- Fingerprint is stable for the same sampled image.

### 1.5 Diagram crop and raw-frame lifecycle

- Normalized coordinates convert to in-bounds integer crop rectangles.
- Edge coordinates, rounding and minimum one-pixel width/height.
- Invalid/out-of-range rectangles are rejected.
- EXIF orientations produce correctly oriented crops.
- Safe private asset paths prevent traversal.
- Crop and recovery-WAV writes use temporary file then atomic rename.
- Asset insert failure removes the new crop.
- Similar frame deletion occurs before evidence/API work.
- Accepted detector candidate becomes the baseline only after note/evidence/crop success.
- Failed candidate is discarded without advancing the last successful baseline.
- Processed raw path clears only after note/crop/evidence commit.
- One-slot drop-oldest frame queue deletes superseded frames without affecting the independent ordered audio queue.
- Failed evidence retains its temporary-path metadata.
- Startup cleanup removes matching cache frames older than 30 minutes.

Pure crop bounds and file behavior have focused tests. Complete lifecycle timing requires integration/device verification.

### 1.6 Note-refinement and revision contract

- Legacy full request/response remains accepted when `responseMode` is omitted.
- Delta request leaves complete `existingMarkdown` blank and carries only title ≤240, outline ≤12,000, recent Markdown ≤24,000 and a lowercase SHA-256 base hash.
- Delta response returns non-blank append-only Markdown, echoes the base hash and matches the expected revision before local materialization.
- Delta does not restate bounded context or mutate earlier Markdown; an intentional full consolidation remains a distinct operation.
- The live Runnable call path builds context from the continued note plus current-session Markdown, then appends a validated delta only to the current session block.
- Runnable request carries mode, policy, an optional current-chunk primary label, bounded note/syllabus context and new evidence only.
- Verbatim formatting preserves timestamp and speaker order without remote note refinement.
- Provider failure appends an explicit needs-review fallback.
- Response request/session/base/next revision must match the request.
- Blank generated Markdown cannot erase a note.
- Expected-revision mismatch returns conflict.
- Identical request replay is idempotent; reuse of the same ids with changed normalized evidence returns an idempotency conflict.
- Reused patch id with different Markdown is rejected.
- Correction severity and evidence-id lists validate.
- Unknown/invalid source and oversized response are rejected.

The bounded-context builder, Android response parser/materializer and proxy contract have focused automated coverage. The exact live ViewModel-to-proxy-to-Room sequence still requires the mock/device integration run in Section 2.

### 1.7 Room v2 and repositories

- Additive schema includes folders, locations, syllabi, sessions, transcripts, visual evidence, assets and provenance.
- Session creation atomically inserts one stable Markdown block and provenance row.
- Transcript positions remain unique and ordered.
- Session stop/resume transitions are guarded.
- Revision update changes the Markdown block and session revision atomically.
- Folder placement and hash-deduplicated syllabi.
- Asset/evidence foreign-key behavior.
- Existing note/block display and edit/search tests remain passing.
- Schema export is generated.

Pending: add `room-testing` and a migration test that creates a v1 database with known notes/blocks, runs `MIGRATION_1_2`, checks content preservation and validates the v2 schema.

### 1.8 Syllabus import

- Accept `.md`, `.markdown` and `.txt`.
- Reject unsupported extension, empty input, NUL-containing input and files over 750 KB.
- SHA-256 is stable and duplicate content selects the existing row.
- Store only a safe hash-derived private relative path.
- Android relevance selection emits at most six explicit excerpts; the proxy contract accepts at most eight, with 2,000 text characters each and 12,000 characters total.
- Legacy raw context may validate up to 120,000 characters, but server relevance selection forwards no more than 12,000.
- Prompt/contract marks syllabus as context rather than captured evidence.

### 1.9 Markdown and Obsidian export

- Note title, heading, bullet, chart and generated Markdown rendering.
- Diagram links are rewritten to `assets/<file>`.
- Missing/unreferenced available assets appear under Captured diagrams.
- Unsafe/traversal asset paths are rejected.
- ZIP contains one UTF-8 Markdown file plus existing assets.
- Export filename is sanitized.
- Old export cache cleanup behavior.

### 1.10 UI/ViewModel state

- Mode, policy, existing-note, folder, syllabus, interval and threshold setup.
- Required permission sets for Voice and Live board.
- Setup → starting → running → stopping → stopped/error transitions.
- Active Live screen requests keep-screen-awake; leaving/back uses the stop-and-drain path rather than background capture.
- Stop waits for the final partial WAV, closes both queues and drains both workers before marking the session stopped.
- Audio error and proxy error remain visible while preserving evidence.
- Frame counters and speaker-focus state are reflected accurately.
- Manual speaker override controls current-chunk Runnable prioritization and expires at the next chunk.
- Export share intent uses `FileProvider` and ZIP MIME type.
- State recreation/interruption behavior.

Instrumentation now covers the exact short final-partial Voice path and one timed CameraX-plus-audio Video path through saved-note state. Permission denial, Back/leave, process death, retries, multiple cadences, relaunch/export and long-session behavior still require device coverage.

### 1.11 Backend contract/security

- `/health` reports mock/live mode, fixed models and in-memory-forwarding retention.
- `/v1/audio/transcribe`, `/v1/board/extract` and `/v1/notes/refine` validate JSON, MIME/signature, sizes and fields.
- Provider errors produce bounded non-secret error responses.
- Note idempotency cache fingerprints and replays one logical request, rejects changed evidence under the same ids, and expires/bounds entries.
- Full/delta strict schemas, 5,000/2,000 output-token caps and base-content hash binding.
- `OPENAI_API_KEY` is required only when mock mode is off.
- Board/note Responses requests use `store: false`.
- No test reads the exposed key or makes a billable request.
- Debug URL remains loopback; release pre-build rejects missing/non-HTTPS base URL, credentials, query and fragment.
- Runtime endpoint validation rejects malformed URLs and embedded credentials and requires HTTPS outside debug.

The current Node suite passes 11/11. Production authentication, TLS and rate limiting do not exist in the loopback development server and require a separate deployment/security test plan.

## 2. Physical-device matrix for continuous MVP

Target: Redmi model `2411DRN47I`, Android 16/API 36, plus one lower-resource Android target if available.

| Area | Required cases | Current status |
| --- | --- | --- |
| Upgrade | Install legacy v1 DB/APK state, upgrade to v2, reopen prior note | Pending |
| Permissions | Grant microphone/camera for opt-in smoke; denial/settings recovery | Grant path passed; denial/recovery pending |
| Voice start/stop | Short final partial plus at least three full chunks | Four-second final-partial path passed; multi-chunk pending |
| Voice interruption | Stop, Back/leave, screen-off, force-stop/process death | Explicit Stop/drain passed; other interruption paths pending |
| Audio reliability | Successful partial drain plus slow/unavailable proxy/retry retention | Mock-success drain passed; failure/retry cases pending |
| Video concurrency | Preview + timed frame + continuous audio + saved note | Passed for one 32.742-second mock session |
| Video cadence | 2 s, 8 s and 30 s intervals | Timed scheduling exercised once; full cadence matrix pending |
| Frame filter | First accepted frame, unchanged board, writing, slide change, exposure and failed-candidate retry | First frame/extraction passed; variation corpus pending |
| Crop lifecycle | Diagram crop saved; processed raw deleted; failed raw bounded | Pending |
| Speaker focus | Per-chunk dominance/ambiguity, label resets and chunk-local manual override | Pending |
| Cross-chunk identity | Confirm current labels cannot support a five-to-six-minute teacher claim; evaluate future embeddings separately | Pending/future |
| Syllabus | Import/select/reuse text syllabus; no unsupported UI claim | Pending |
| Note policy | Runnable mock path, fallback and Verbatim ordering | Runnable mock success passed; fallback/Verbatim pending |
| Revision | Board and audio update one block without lost content | Passed in the isolated Video mock session; stress/replay pending |
| Persistence | Saved state plus force-close/relaunch/reopen transcript/note/assets | Saved-note state passed; relaunch/reopen pending |
| Organization | Create folder, continue note, filter recents | Pending |
| Export | Share ZIP, unzip externally, open Markdown/assets in Obsidian | Pending |
| Accessibility | TalkBack labels, touch targets, contrast, dynamic font | Pending |
| Long session | 30–60 minutes with battery/storage/network sampling | Pending |

### 2.1 Completed opt-in smoke evidence

`voiceDrainsFinalPartialChunk` stopped capture after four seconds. Passing required the final partial PCM to become a WAV, drain through the audio worker, receive deterministic mock transcription/refinement, persist the Markdown update and show `SAVED`. Standalone instrumentation time was 9.561 seconds.

`videoCapturesBoardAndAudio` required a live CameraX preview, scheduled capture, deterministic board extraction, a normal audio chunk, incremental note updates, explicit Stop/drain and saved-note persistence. Standalone instrumentation time was 32.742 seconds.

The initial Video attempt exposed a defect rather than passing by timeout relaxation: preview opened, but the Compose coroutine never called `takePicture`. `LiveCameraPanel` now schedules on a main-loop `Handler` inside the camera `DisposableEffect` and removes the callback on disposal. The fixed test wrote `live-video-board-and-audio.png` before Stop and `live-video-board-and-audio-saved.png` afterward. Voice wrote `live-voice-drain-saved.png`; all are in the app external `evidence/` directory.

Two WAVs from intentionally aborted diagnostics were associated with their exact `Device smoke` session ids before deletion. No unrecovered-audio file remained. This is cleanup evidence, not a substitute for testing the future user-facing recovery manager.

## 3. Live-provider validation

Run only after the exposed credential is revoked and a replacement is configured in the backend process environment.

### 3.1 Small contract smoke test

- Keep the proxy on loopback and Android connected by `adb reverse`.
- Record the `/health` mode and model names without logging the key.
- Submit one short WAV, one readable board image and one note refinement request.
- Record source labels, HTTP status, latency, payload size and billed usage/cost if available.
- Compare the actual provider response against the Android parser contract.
- Stop immediately on schema mismatch, unexpected storage/logging or excessive cost.

Passing this smoke test establishes compatibility only, not accuracy.

### 3.2 Accuracy test

Use the fixed corpora below. Do not tune a threshold/prompt on an example and report the same example as evaluation.

## 4. Evaluation datasets and metrics

### 4.1 Speech and diarization corpus

Conditions:

- one speaker in quiet room;
- teacher plus moderate background noise;
- teacher plus student questions;
- two speakers with similar duration;
- accented/technical vocabulary; and
- at least one session longer than six minutes.

Metrics:

- word error rate;
- diarization error rate;
- speaker-label instability/reset rate across chunk boundaries;
- missing/duplicate segment count;
- chunk and end-to-note latency;
- per-chunk dominant-label precision/recall and ambiguous/unavailable rate;
- chunk-local manual-override success and expiry behavior;
- future cross-chunk embedding/identity clustering error before any five-to-six-minute teacher-focus claim;
- ambiguous/unavailable rate; and
- manual-override success rate.

### 4.2 Structured-note corpus

For every source session, create reference key points, headings, equations, examples and unrelated chatter labels.

Metrics:

- key-point precision, recall and F1;
- coverage of required concepts;
- duplicate/repetition rate;
- unsupported statement/hallucination rate;
- hierarchy/Markdown validity;
- Runnable versus Verbatim faithfulness;
- correction suggestion precision and unsupported-correction rate;
- syllabus-assisted improvement; and
- syllabus leakage, defined as unsupported content attributable only to the syllabus.

### 4.3 Frame-change corpus

Sequence labels:

- exact duplicate;
- camera noise only;
- global exposure/projector flicker;
- small new text/equation;
- erased content;
- new slide/board;
- moving person occlusion; and
- diagram addition.

Metrics:

- meaningful-change precision/recall/F1;
- duplicate rejection rate;
- false acceptance from exposure/occlusion;
- API requests, decoded bytes and temporary storage avoided; and
- end-to-note latency per accepted frame.

### 4.4 Board/equation/diagram corpus

Metrics:

- visible text character/word error rate;
- equation exact match and symbol error analysis;
- concept faithfulness and unsupported concept rate;
- diagram-region intersection-over-union;
- crop legibility acceptance;
- correct caption and Markdown link rate; and
- asset survival after relaunch/export.

### 4.5 Resource and retention metrics

- CPU, memory and battery delta over 30/60 minutes.
- Audio bytes, camera bytes, network bytes and proxy requests.
- Audio recovery-file queue high-water mark, retry count and retained/deleted WAV count.
- Frame supersession/drop count and confirmation that it does not remove audio.
- Raw frame count by accepted/skipped/processed/failed/deleted state.
- Storage after session, after 30-minute cleanup opportunity and after export cleanup.
- Crash-free scripted demo repetitions.

## 5. YouTube teaching-video test matrix

This is an ordinary capture test: play a permitted teaching video and let the phone hear/see it. No direct YouTube download or API integration is part of VoxBox.

| Trial | Content | Expected stress |
| --- | --- | --- |
| YT-1 | Text-heavy conceptual lecture | Continuous coverage and deduplication |
| YT-2 | Mathematics with progressive equations | Symbols, sequencing and small frame changes |
| YT-3 | Science/engineering diagrams | Region crop, caption and spoken/visual merge |
| YT-4 | Multiple speakers/Q&A | Diarization, ambiguity and manual override |

For each trial record source/title, duration, permission to use, phone placement, volume/lighting, mode/policy, interval/threshold, syllabus, expected concepts, produced note/assets, latency/resource data, errors and corrective change. Re-run after a fix on both the failing case and a regression set.

## 6. Historical verified evidence

The following remains valid for its original bounded paths:

- Review-1 PPT structural/visual export checks passed on 2026-07-19.
- Starter build and physical baseline launch passed on Redmi `2411DRN47I`, Android 16/API 36.
- Bounded native speech permission/fallback/listening UI passed on device.
- Deterministic 25% yellow wheat chart preview passed.
- Room v1 note/chart save, force-stop/relaunch and read-only reopen passed on device.
- Typed-edit logic passed automated tests, but its exact edit→force-stop→reopen device regression remained pending.
- Organized Notes/Speak/Board UI, search unit tests and the manual Board mock-save/relaunch flow passed their documented 2026-07-30 gates.
- Offline OCR fallback routing reached device review, but a dark frame prevented any OCR accuracy conclusion.
- No real OpenAI call or accuracy result exists in those milestones.

Canonical detail is retained in `PROJECT_LOG.md` and the `evidence/` README files.

## 7. Next verification commands

```powershell
cd "D:\College Project\VoxBox"
.\gradlew.bat testDebugUnitTest compileDebugKotlin lintDebug assembleDebug assembleDebugAndroidTest --console=plain
```

```powershell
cd "D:\College Project\server"
$env:MOCK_AI="1"
npm test
```

The final post-device-fix build is recorded above. Use that exact artifact for the remaining matrix. Do not configure a live key until the exposed key has been revoked; the two mock smoke paths are stable, but failure, migration and endurance cases remain.

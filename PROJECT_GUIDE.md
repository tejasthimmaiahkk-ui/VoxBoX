# VoxBox Project Guide

Status: continuous multimodal MVP implemented with automated and bounded Android 16 mock-device evidence
Last updated: 2026-08-03

## Working title

**VoxBox: An Evidence-Preserving Multimodal System for Continuous Structured Note Creation on Android**

Short title: **VoxBox — Continuous Voice and Board-to-Notes**

## Product definition

VoxBox is a local-first Android application that turns a foreground voice or camera-plus-voice session into an incrementally revised Markdown note. The main use case is a lecture, but the same workflow can summarize a meeting, interview, conversation or recorded explanation.

The user chooses one of two live modes:

- **Voice:** continuously records the active foreground session in bounded audio chunks until the user stops it.
- **Live board:** records the same audio while CameraX captures a frame at an adjustable interval. Similar frames are rejected locally; accepted frames may contribute visible text, equations, concepts and diagram crops.

The user also chooses one of two note policies:

- **Runnable notes:** create concise, structured notes, optionally prioritize the dominant diarization label inside the current audio chunk, remove repetition and unrelated chatter, and surface likely conflicts as review annotations.
- **Verbatim:** append timestamped diarized speech and board evidence without AI summarization.

“Continuous” means a visible foreground session controlled by the user. While capture is starting/running/stopping, the Live screen requests that Android keep the display awake. Back navigation or leaving that screen triggers stop-and-drain. VoxBox does not provide a hidden background or screen-off recording service.

## Claim levels used by this project

Every report, deck and viva answer must distinguish these levels:

1. **Verified legacy baseline:** the earlier bounded `SpeechRecognizer`, VoxScript, Room note/block, search and manual Board-still flows passed their documented automated and physical-device checks.
2. **Implemented continuous MVP:** the new `AudioRecord`, periodic CameraX frame, frame-difference, diarized-transcription contract, speaker-focus, incremental Markdown, folder/syllabus, crop/retention and export paths exist in the shared tree. Android JVM tests and backend mock/fake contract tests pass; Kotlin production compilation passes.
3. **Runtime-verified mock slice:** two opt-in instrumentation tests passed independently on Android 16 against the deterministic loopback proxy. They establish the exact Voice final-partial and Video camera-plus-audio pipelines described below, not model or capture accuracy.
4. **Not yet evaluated:** no real rotated-key OpenAI run, noisy-room/speaker corpus, endurance/resource evaluation, diagram-crop corpus or YouTube lecture trial has passed. No claim may upgrade these items until evidence is recorded.

## Core user story

1. Open the live-session screen.
2. Select **Voice** or **Live board**.
3. Create a new note or continue a recent note.
4. Choose **Runnable notes** or **Verbatim**.
5. Optionally choose a folder and a locally imported Markdown/text syllabus.
6. In Live board mode, choose the frame interval and change threshold.
7. Grant the visible microphone permission and, for Live board, camera permission.
8. Start the foreground session.
9. Review the living Markdown note, evidence transcript, speaker-focus state, frame counters and warnings while capture continues.
10. Stop the session and reopen or export the saved note.

## Implemented continuous architecture

```text
Compose live-session UI
    ├── Voice mode
    │     └── AudioRecord: 16 kHz mono PCM16 → 20 s WAV chunks
    └── Live board mode
          ├── the same continuous audio path
          └── CameraX periodic JPEG capture (2–30 s in the current UI)
                 ↓
          32×32 luminance change detector
                 ├── similar → delete locally; no proxy request
                 └── changed → board extraction → diagram crop(s)

Android client → loopback debug proxy
    ├── /v1/audio/transcribe → diarized transcript segments
    ├── /v1/board/extract   → text, equations, concepts, diagram regions
    └── /v1/notes/refine    → legacy full note or validated append-only delta + review flags

Room v2
    ├── notes + ordered typed blocks
    ├── folders + note locations + syllabi
    ├── capture sessions + transcript evidence
    ├── visual evidence + note assets
    └── block provenance + optimistic Markdown revision

Private app files
    ├── hash-named syllabus copies
    ├── accepted diagram crops
    └── temporary export ZIPs
```

The legacy Notes and manual Board-still destinations remain available while the new live-session flow is evaluated. The bounded speech/VoxScript source and its historical evidence remain in the tree, although the current Speak destination now opens the new live-session UI. None of those older results substitutes for validating the continuous MVP.

## Audio and speaker-focus method

`PcmAudioChunkRecorder` uses Android `AudioRecord` with `MediaRecorder.AudioSource.VOICE_RECOGNITION`, 16 kHz mono PCM16 and 20-second target chunks. It enables platform noise suppression and automatic gain control when the device reports them available. A final useful partial chunk is emitted when the user stops.

Each completed chunk is atomically written to app-private recovery storage before normal processing when storage is available; the queue then carries a small file reference rather than the full WAV. Audio has its own ordered unlimited channel so frame work cannot evict speech. Transcription receives at most three attempts, with 750 ms and 2 s retry delays. After transcript evidence and its note revision commit, the recovery WAV is deleted. If transcription or the note commit still fails, the WAV remains private and an operational warning is persisted into the note. If durable storage fails, the current chunk remains in memory with a visible warning, which is less robust for a long session.

Stopping uses `stopAndDrain`: it stops microphone reads, waits for the recorder to emit a final useful partial WAV, closes both processing channels, drains their workers and only then marks the session stopped. Leaving the Live screen uses the same stop/save path.

The transcription contract returns speaker-labelled segments with session-relative timestamps. These diarization labels are scoped to one independently uploaded chunk: a label such as `A` in one request is not guaranteed to represent the same person as `A` in the next request. The current `PerChunkSpeakerTracker` therefore never accumulates a five-minute identity history.

For each chunk it sums unique voiced duration by label and marks a dominant label only when the leader has at least 58% of that chunk's voiced time and a margin of at least 15 percentage points over the runner-up. Otherwise it reports `AMBIGUOUS` or `UNAVAILABLE`. A manual choice also applies only to the latest chunk and expires when the next transcription response arrives. Raw segments from every returned label remain stored.

This is chunk-local activity filtering, not teacher identification. Meeting the original five-to-six-minute persistent-teacher goal requires a future consent-aware speaker-embedding/enrolment and cross-chunk clustering layer, or a provider that guarantees session-stable diarization identities. Until then, VoxBox must not claim that it has learned the teacher across the lecture.

## Frame-efficiency and diagram lifecycle

The current setup model accepts a 2–60 second interval; the visible slider exposes 2–30 seconds. The default is eight seconds. `LiveCameraPanel` schedules `takePicture` on a main-loop `Handler` owned by the camera `DisposableEffect`, which cancels its callback when the panel is disposed. This replaced a Compose-coroutine timer that opened CameraX preview but, on the tested device, never invoked `takePicture`. A local detector downsamples each JPEG to 32×32 luminance, compensates for global exposure changes and combines centered pixel difference with changed-pixel fraction. The first frame establishes the accepted baseline; later frames must meet the chosen threshold.

Lifecycle:

1. CameraX creates a temporary JPEG in app cache.
2. Similar frames are deleted immediately without AI, OCR or note work.
3. Changed frames receive a `visual_evidence` row and enter extraction.
4. Normalized diagram regions are orientation-corrected, cropped and stored under `note-assets/<note>/<asset>.jpg`.
5. The Markdown revision and asset records must commit before the detector promotes that frame to the comparison baseline or deletes the successfully processed raw frame.
6. If extraction/persistence fails, the candidate baseline is discarded so the next capture is still compared with the last successfully committed board state; the failed raw frame remains in cache for diagnosis/retry.
7. Frame processing has a separate one-slot drop-oldest queue. Superseded frames are deleted locally while the independent audio queue continues.
8. On live-session ViewModel creation, matching raw cache files older than 30 minutes are removed.

Only the accepted crop is a durable note asset. The full raw frame is not intended as long-term note content. The current retention cleanup is startup-triggered rather than a continuously scheduled cleanup job; that limitation must be tested and may be strengthened later.

## Structured-note and correction policy

Each capture session owns one stable `MARKDOWN` block. Every successful update supplies a patch id and expected revision. Room commits the materialized next Markdown only if the revision still matches, rejects blank erasure, detects conflicting patch reuse and treats an identical replay as idempotent.

The refinement contract is backward compatible. A request with no `responseMode` uses the original full-document contract. The new live Runnable path uses `delta` mode and does not upload the complete existing note. It builds context from the continued note plus the current session, then sends only a title of at most 240 characters, an outline of at most 12,000 characters, the most recent Markdown of at most 24,000 characters and a lowercase SHA-256 of the complete current content. The response must echo that base hash and return only non-blank `markdownDelta`; the Android client validates the hash/revision, appends the delta to the current session Markdown and asks Room to commit that materialized session block. Delta mode is deliberately append-only: it cannot rewrite an older section, so an intentional full consolidation/review operation remains a separate future workflow.

Runnable-note requests contain only bounded note context and new transcript/board evidence. Syllabus text is marked as context, not proof. The proxy prompt requires the model to preserve captured claims and return a separate correction record when evidence supports a likely conflict. The UI labels these as suggested annotations. It must never silently rewrite a teacher, transcript or OCR claim as established fact.

If the note-refinement service is unavailable, the Android client appends a clearly labelled “needs review” evidence section. Verbatim mode formats evidence locally and does not require AI note refinement.

## Local organization, continuation and export

Room v2 adds nested-folder-capable entities, note locations and hash-deduplicated syllabi. The current UI creates/selects top-level folders, filters the recent-note dashboard by folder, and can continue an existing note. A complete nested file-browser UI is not yet implemented.

The syllabus importer accepts UTF-8 `.md`, `.markdown` and `.txt` files up to 750 KB, stores a hash-named private copy and records its SHA-256 value. The live Android selector ranks the imported sections against the new transcript/board evidence and sends at most six excerpts. The proxy accepts at most eight explicit excerpts, each at most 2,000 characters and at most 12,000 characters in total. For backward compatibility, a legacy raw context field may contain up to 120,000 characters, but the proxy ranks it against current evidence and forwards no more than 12,000 characters. PDF/DOCX syllabus extraction is not implemented.

Single-note export creates:

- a UTF-8 Markdown file;
- an `assets/` directory containing available diagram crops; and
- a shareable ZIP whose relative links are suitable for import into an Obsidian vault.

Export cache items older than 24 hours are removed on a later export. Folder-wide vault export and cloud synchronization are not implemented.

## Data model — Room version 2

- `Note` and ordered `NoteBlock`: existing local note content; `MARKDOWN` is added for generated revisions.
- `Folder` and `NoteLocation`: hierarchy-capable folders and one current folder per note.
- `Syllabus`: local relative path, extracted text, SHA-256 and timestamps.
- `CaptureSession`: mode, policy, status, syllabus, generated block, revision, patch id, frame settings and a reserved speaker-focus field. Current automatic/manual focus is chunk-local rather than a persisted identity.
- `TranscriptSegment`: ordered timestamped speaker evidence.
- `VisualEvidence`: fingerprint, delta score, processing state, temporary path and error metadata.
- `NoteAsset`: durable diagram/board crop associated with a note and optional frame evidence.
- `BlockProvenance`: source and review metadata for the generated block.

`MIGRATION_1_2` is additive and preserves legacy `notes` and `note_blocks`. Production compilation and model/revision tests pass. A real-device migration from an existing v1 database and Room migration instrumentation test remain pending.

## Secure proxy and model routing

The Android APK never contains an OpenAI key. Debug builds use `http://127.0.0.1:8787` through `adb reverse`. Release builds must receive `VOXBOX_API_BASE_URL` as a Gradle property or environment variable; the pre-release build rejects a missing/non-HTTPS URL, credentials, query or fragment. Runtime URL validation also forbids embedded credentials and requires HTTPS outside debug. The proxy validates input sizes/signatures and exposes:

- `gpt-4o-transcribe-diarize` for diarized audio transcription;
- `gpt-5.6-sol` for image/board extraction; and
- `gpt-5.6-terra` for incremental note synthesis.

Responses use strict full/delta contracts, provider storage is disabled where supported, and the proxy forwards media in memory rather than writing it to disk. The delta contract binds output to the current content hash; the legacy full contract remains the default for older clients. Note-refinement responses have a small in-memory idempotency cache. Mock mode is deterministic and is integration evidence only.

The pasted project credential is exposed and must be revoked. A replacement belongs only in the proxy process environment. The loopback proxy has no end-user authentication or TLS; a production release requires an authenticated HTTPS backend, rate limits, monitoring and access control.

## Privacy and ethical baseline

- Microphone/camera use is visible and scoped to an active foreground session.
- Both modes require microphone permission; video also requires camera permission.
- Similar frames are rejected on-device.
- Raw transcript and frame evidence are not silently overwritten by a summary.
- Syllabus text is context, never source evidence.
- Speaker focus is chunk-local, activity-based and user-overridable for that chunk; it is not identity recognition.
- Android backup remains disabled for app-private notes and assets.
- Mock output must never be reported as AI, speech, OCR or diagram accuracy.

Before recording a class or conversation, the user is responsible for obtaining any consent required by the institution or applicable law.

## Evaluation plan

### Audio and speaker focus

- Word error rate in quiet, moderate-noise and competing-speaker conditions.
- Diarization error rate and the observed instability of labels across independent chunk requests.
- Per-chunk dominant-label precision/recall and the rate of ambiguous/unavailable chunks.
- Per-chunk manual-override behavior and expiry at the next chunk.
- Future evaluation of cross-chunk speaker embeddings/session-stable identities before any five-to-six-minute teacher-focus claim.
- Chunk latency, recovery-queue growth, retained-WAV count, final-partial drain and long-session stability.

### Structured notes

- Key-point precision/recall, coverage, duplicate rate and hierarchy/Markdown validity.
- Runnable-versus-verbatim faithfulness.
- Correction precision and rate of unsupported corrections.
- Syllabus-assisted coverage versus syllabus leakage/unsupported additions.
- Revision conflict/idempotency, persistence and recovery after relaunch.

### Live board

- Duplicate-frame rejection and accepted-change recall.
- API/storage savings relative to processing every scheduled frame.
- OCR/equation accuracy and concept faithfulness.
- Diagram-region intersection-over-union, crop legibility and link recovery after export.
- Raw-frame deletion/failed-retention behavior and resource use over a full lecture.

### Product flow

- Voice and video foreground sessions on the physical Redmi device.
- Concurrent camera/audio behavior, permission denial and lifecycle transitions.
- Create/continue/folder/syllabus/export flows.
- YouTube teaching-video trials covering text-heavy, mathematical and diagram-heavy lessons.
- Crash-free repeated demo runs, battery use, storage growth and network/API usage.

No percentage is reported until a versioned corpus, protocol and scorer exist.

## Current status — 2026-08-03

### Verified legacy baseline

- Physical-device permission/listening state, deterministic 25% yellow wheat chart, Room persistence/reopen and bounded manual Board capture/mock-save/relaunch flows are preserved in `PROJECT_LOG.md` and `evidence/`.
- The Board offline fallback route was reached on device, but the frame was dark and did not measure OCR accuracy.

### Implemented and automated-contract tested

- Foreground-only voice/video session UI with keep-screen-awake and leave/back stop-and-drain behavior.
- Continuous `AudioRecord` chunking, final-partial drain, private recovery WAVs, three-attempt transcription retry and persisted failure warnings.
- Independent ordered audio and one-slot drop-oldest frame queues so superseded camera work does not evict captured speech.
- Diarized-transcription client contract, per-chunk dominant-duration heuristic and per-chunk manual override.
- Periodic video capture, commit-after-success frame baseline, local change filtering, normalized diagram cropping and raw-frame lifecycle code.
- Runnable/verbatim note policies, fallback evidence formatting and revisioned Markdown updates.
- Room v2 schema/migration, folders, syllabi, sessions, transcripts, visual evidence, assets and provenance.
- Local syllabus import and single-note Markdown/asset ZIP export.
- Secure three-endpoint proxy with mock/fake provider tests.
- Android JVM suite: 70 tests, 0 failures/errors/skips across 23 suites; production `compileDebugKotlin` succeeds.
- Final `lintDebug`, `assembleDebug` and `assembleDebugAndroidTest` gate: BUILD SUCCESSFUL with 0 lint errors and 18 warnings.
- Final `app-debug.apk`: 61,182,613 bytes; SHA-256 `DABF116DB614D0066AD3AC867C2D77BB7E344154C8B6FCAE7A563FD12CCCA0AB`.
- Backend Node suite: 11/11 tests pass without a live or billable request, including full/delta compatibility, bounded context and idempotency-conflict cases.
- Secret scan is clear; the only environment template is `server/.env.example`. Final `git diff --check` exits 0.

### Android 16 mock-device validation

- `voiceDrainsFinalPartialChunk` passed standalone in 9.561 seconds. The user stopped after four seconds, before the 20-second target boundary; the final partial WAV drained, was mock-transcribed and refined, committed to Room, and reached the saved-note state.
- `videoCapturesBoardAndAudio` passed standalone in 32.742 seconds. Timed CameraX capture, mock board extraction, concurrent continuous audio, incremental note updates, Stop/drain and saved-note persistence all completed.
- The Video run first exposed a real timer defect: preview opened but no scheduled `takePicture` call occurred. Moving scheduling to a main-loop `Handler` scoped to the camera `DisposableEffect` fixed it; the passing result is from the corrected path.
- The tests wrote `live-voice-drain-saved.png`, `live-video-board-and-audio.png` and `live-video-board-and-audio-saved.png` into the app's external `evidence/` directory.
- Two recovery WAVs created by intentionally aborted diagnostic sessions were matched to their exact `Device smoke` session ids and deleted. No unrecovered-audio file remained after cleanup.

This proves deterministic mock plumbing for those two scenarios. It does not measure transcription, diarization, note, OCR, equation or diagram accuracy, and it does not establish long-session reliability.

### Pending before the continuous MVP can be called evaluated

- Physical-device v1→v2 migration and longer continuous voice/video sessions.
- Real noisy-room speech, competing-speaker behavior and long-session resource checks.
- A rotated-key OpenAI run and provider response/latency/cost validation.
- Measured diarization, note, frame-filter, OCR/equation and diagram/crop accuracy.
- YouTube lecture test matrix.
- Periodic failed-frame cleanup independent of screen recreation, user-facing retained-WAV recovery/deletion controls and recovery/resume UX for an interrupted active session.

## Immediate next gates

1. Exercise v1→v2 migration on the physical device without losing legacy notes.
2. Expand mock device coverage to Back/leave, relaunch/export, proxy outage, retained-WAV recovery and multiple frame cadences.
3. Revoke the exposed key; intentionally configure a rotated key only in the proxy environment.
4. Run one small live-provider test, record model/source labels, latency and cost, then fix contract issues.
5. Build fixed noisy-audio and board/video/crop corpora before claiming accuracy.
6. Execute text, maths and diagram YouTube trials and document failures.

## Project records

- `PROJECT_GUIDE.md`: current product and implementation map.
- `PROJECT_LOG.md`: append-only history, verification and blockers.
- `CONTINUATION_HANDOFF.md`: current resume point and exact pending gates.
- `docs/REPORT_DRAFT.md`: academic report written alongside verified work.
- `docs/TEST_PLAN.md`: corpus, automated and device verification plan.
- `docs/VIVA_NOTES.md`: short, defensible answers using current claim levels.
- `docs/VOXSCRIPT_SPEC.md`: legacy/optional deterministic command layer.
- `server/openapi.yaml`: current proxy contract.

## Source-control and claims policy

- Preserve the dirty shared tree and unrelated user work.
- Never commit secrets, `local.properties`, build folders or generated APK/AAB files.
- Keep historical evidence; document a design pivot instead of rewriting earlier milestones.
- Do not call code “device verified,” “AI verified” or “accurate” based only on unit tests, mocks or schemas.
- Preserve evidence and require explicit review for semantic corrections.

## Primary technical sources

- [Android AudioRecord](https://developer.android.com/reference/android/media/AudioRecord)
- [Android CameraX](https://developer.android.com/media/camera/camerax)
- [Android Room](https://developer.android.com/training/data-storage/room)
- [Android app architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [OpenAI model documentation](https://developers.openai.com/api/docs/models)
- [OpenAI Responses API](https://developers.openai.com/api/docs/guides/responses-vs-chat-completions)

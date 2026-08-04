# VoxBox Project Guide

Status: continuous multimodal MVP implemented with automated and bounded Android 16 mock-device evidence
Last updated: 2026-08-04

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
3. **Runtime-verified mock slice:** two opt-in instrumentation tests passed independently on Android 16 against the deterministic loopback proxy. They establish the exact Voice final-partial and Video camera-plus-audio pipelines described below, not model or capture accuracy. Both were re-run on 2026-08-04 against the redesigned UI and passed (Voice 11.136 s, Video 32.339 s).
4. **Live-provider contract verified:** on 2026-08-04 all three pipelines completed against the real OpenRouter API through the proxy, using synthetic inputs with known ground truth. This establishes that the request/response contracts work end to end with a real model. It is **not** an accuracy result.
5. **Not yet evaluated:** no noisy-room/speaker corpus, endurance/resource evaluation, diagram-crop corpus or YouTube lecture trial has passed, and nothing has been measured on real classroom material. No claim may upgrade these items until evidence is recorded.

The 2026-08-04 increment also found that board evidence had never reached the note provider: the note-refinement request builder overwrote it with `null` before sending. That is fixed, covered by request-contract tests, and confirmed on device in mock mode — the Video note now reaches `## Board evidence` sections, which the proxy emits only when the request actually carries a `boardEvidence` object. It is still unconfirmed against a real provider.

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

Each completed chunk is atomically written to app-private recovery storage before normal processing when storage is available; the queue then carries a small file reference rather than the full WAV. Audio has its own ordered unlimited channel so frame work cannot evict speech. Transcription receives at most three attempts, with 750 ms and 2 s retry delays, **unless the proxy reports a non-retryable failure** — an exhausted account quota, a rejected server credential or a rejected request — in which case the remaining attempts are skipped. After transcript evidence and its note revision commit, the recovery WAV is deleted. If transcription or the note commit still fails, the WAV remains private and an operational warning is persisted into the note that states whether retries were attempted. If durable storage fails, the current chunk remains in memory with a visible warning, which is less robust for a long session.

Retained recovery WAVs are user-manageable. Each file's name carries its session offset and duration, the Live setup screen lists every retained file with its size and the reason it was kept, and the user can recover or delete each one. Recovery re-transcribes the WAV, persists its diarized segments as evidence and appends a labelled `## Recovered audio` section to the original note through the same revision-guarded path. It deliberately does not re-run AI refinement: the note has moved on since the failure, so a delta cannot be validated against it and captured speech must not be silently reinterpreted. Deletion is explicit and states that the audio evidence is gone.

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

The Android APK never contains a provider key. Debug builds use `http://127.0.0.1:8787` through `adb reverse`. Release builds must receive `VOXBOX_API_BASE_URL` and `VOXBOX_CLIENT_TOKEN` as Gradle properties or environment variables; the pre-release build rejects a missing/non-HTTPS URL, credentials, query or fragment, and a missing, short or whitespace-bearing token. Runtime URL validation also forbids embedded credentials and requires HTTPS outside debug.

The provider is **OpenRouter**, reached through its OpenAI-compatible `POST /api/v1/chat/completions` with a `strict` `json_schema` response format on all three pipelines. Defaults, each chosen by measuring candidates against this project's own contracts rather than by list price:

- `google/gemini-3.1-flash-lite` for diarized audio transcription;
- `google/gemini-2.5-flash-lite` for image/board extraction; and
- `openai/gpt-oss-120b` for incremental note synthesis.

Each is overridable by environment variable. Measured cost is roughly **$0.18 per lecture-hour** across all three pipelines.

OpenRouter's dedicated transcription endpoint returns no speaker labels, so diarization comes from an audio-capable model constrained by a diarized transcript schema. That is **speaker segmentation inferred from the audio, not acoustic diarization with voice embeddings**. It suits how this project already treats labels — local to one chunk, never an identity claim — but its accuracy on real classroom audio is unmeasured.

Every request pins `provider: { require_parameters: true }`. OpenRouter load-balances one model id across several upstreams, and without the pin a request can land on one that ignores `response_format` and aborts mid-generation; that was observed live as `finish_reason: "error"` with half-written JSON. Abnormal completions now report `upstream_output_truncated` or `upstream_generation_failed` rather than surfacing as malformed JSON, and model output is normalized for a Markdown fence or short preamble before parsing.

Pipeline routes require `Authorization: Bearer <VOXBOX_CLIENT_TOKEN>`, compared in constant time. In live mode the server refuses to forward anything without that token configured, so a deployment can never be an open relay. `/health` stays unauthenticated for platform health checks. A per-caller rate limit and a hard daily budget of billable calls bound the damage from a leaked token; both are in-memory and per instance. The token is compiled into the APK and is therefore extractable — it deters casual abuse, and the daily budget is the real protection.

Responses use strict full/delta contracts, and the proxy forwards media in memory rather than writing it to disk. The delta contract binds output to the current content hash; the legacy full contract remains the default for older clients. Note-refinement responses have a small in-memory idempotency cache. Mock mode is deterministic, consumes no budget, and is integration evidence only.

Provider failures are classified rather than collapsed into one gateway error. Upstream `429` is preserved: `insufficient_quota` becomes `<kind>_quota_exhausted` with `retryable: false`, any other `429` becomes `<kind>_rate_limited` with `retryable: true`, `401`/`403` become a non-retryable `<kind>_auth_error`, other `4xx` a non-retryable `<kind>_request_rejected`, and `5xx` or unreadable bodies a retryable `<kind>_provider_error`. Each error carries `retryable` plus upstream status, type, code, request id, retry-after and rate-limit counters. The proxy logs only that classification; never request bodies, media or credentials. Android parses the same envelope, skips retries that cannot succeed, and shows the matching remedy.

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

### 2026-08-04 live-provider verification

All three pipelines were exercised against the real OpenRouter API through the proxy, with synthetic
inputs whose correct answer was known in advance:

- **Transcription** — a 16.7 s two-voice clip synthesized with two distinct TTS voices. Returned the
  correct A/B speaker split, an accurate transcript, and `durationMs` 16,420 against a true 16,700 ms.
  Session offsets were correctly rebased onto the session timeline.
- **Board vision** — a synthetic whiteboard with a known equation and a known diagram rectangle.
  Returned the equation `6CO2 + 6H2O --light--> C6H12O6 + 6O2` exactly, and a diagram crop of
  `{0.096, 0.472, 0.284, 0.318}` against a true `{0.09, 0.47, 0.30, 0.29}`.
- **Note refinement** — transcript containing a planted factual error plus contradicting board
  evidence. Returned a valid append-only delta, echoed the content hash correctly, preserved the
  captured claim in the note body, and raised the contradiction as a separate correction.

Two defects were found only by this live run and are fixed: OpenRouter routing to an upstream that
ignored `response_format` and aborted mid-generation (now pinned with `require_parameters`), and
abnormal completions surfacing as "invalid JSON" instead of a truncation or generation failure.

This is contract evidence with synthetic inputs. It measures neither transcription, diarization,
note, OCR, equation nor diagram accuracy on real classroom material.

### 2026-08-04 increment

- Fixed a request-serialization defect that had replaced every outgoing `boardEvidence` object with `null`, so board evidence never reached the note provider and frame-only updates were rejected outright. Covered by new request-contract tests.
- Added typed provider-failure classification across the proxy and the Android clients, and stopped retrying failures that cannot succeed.
- Added user-facing retained-WAV recovery and deletion, closing that pending gate at the implementation and JVM-test level.
- Rebuilt the UI on a real design system with a self-contained icon set: selectable rows and chips instead of stacked outlined buttons with `✓` prefixes, section headers, stat tiles, actionable banners and empty states across the Notes, Live and Board screens. Removed unreachable legacy speech composables; the VoxScript source and `VoiceCaptureViewModel` remain in the tree as legacy evidence.
- Android JVM suite: 82 tests across 27 suites, 0 failures/errors/skips. Backend Node suite: 16/16. Lint: 0 errors, 18 warnings. `app-debug.apk`: 61,182,613 bytes, SHA-256 `85190FA5E46A6017BE52FD68C82C792F4DBCB8F9B862E2A412F8C73B72F884E8`.
- None of this is device or provider evidence. The two mock-device scenarios have not been re-run since the redesign.

### Pending before the continuous MVP can be called evaluated

- Re-running both opt-in mock-device scenarios against the redesigned UI.
- Confirming on device that board evidence now reaches `/v1/notes/refine` and is used by the note provider.
- Physical-device v1→v2 migration and longer continuous voice/video sessions.
- Real noisy-room speech, competing-speaker behavior and long-session resource checks.
- A rotated-key OpenAI run and provider response/latency/cost validation.
- Measured diarization, note, frame-filter, OCR/equation and diagram/crop accuracy.
- YouTube lecture test matrix.
- Periodic failed-frame cleanup independent of screen recreation, hardware verification of the new retained-WAV recovery/deletion controls, and recovery/resume UX for an interrupted active session.

## Immediate next gates

1. Re-run both opt-in mock-device scenarios against the redesigned UI and capture fresh screenshots. This is the only gate the 2026-08-04 redesign could have regressed.
2. Confirm on device that a real changed frame now reaches `/v1/notes/refine` with `boardEvidence` present.
3. Exercise v1→v2 migration on the physical device without losing legacy notes.
4. Expand mock device coverage to Back/leave, relaunch/export, proxy outage, retained-WAV recovery and multiple frame cadences.
5. Restore provider quota, then run one small live-provider test, record model/source labels, latency and cost, and recover the retained WAVs through the new in-app control.
6. Build fixed noisy-audio and board/video/crop corpora before claiming accuracy.
7. Execute text, maths and diagram YouTube trials and document failures.

## Project records

- `PROJECT_GUIDE.md`: current product and implementation map.
- `PROJECT_LOG.md`: append-only history, verification and blockers.
- `CONTINUATION_HANDOFF.md`: current resume point and exact pending gates.
- `docs/REPORT_DRAFT.md`: academic report written alongside verified work.
- `docs/TEST_PLAN.md`: corpus, automated and device verification plan.
- `docs/VIVA_NOTES.md`: short, defensible answers using current claim levels.
- `docs/VOXSCRIPT_SPEC.md`: legacy/optional deterministic command layer.
- `server/openapi.yaml`: current proxy contract.
- `server/DEPLOYMENT.md`: first-time hosted deployment.
- `server/OPERATIONS.md`: token/key rotation, budgets, models, error codes, logs, rollback.

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

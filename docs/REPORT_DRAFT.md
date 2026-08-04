# VoxBox Project Report — Working Draft

Status: implementation- and evidence-aligned draft
Last updated: 2026-08-03

## Title

**VoxBox: An Evidence-Preserving Multimodal System for Continuous Structured Note Creation on Android**

## Abstract

Lectures, meetings and technical explanations contain more information than a listener can reliably type and organize in real time. Ordinary speech-to-text produces a flat transcript, while a photograph of a board loses the relationship between spoken explanation, equations, diagrams and the time at which they appeared. VoxBox is a local-first Android application that investigates whether continuous foreground audio and selectively sampled board imagery can be converted into a revisioned, transferable Markdown note without discarding the underlying evidence. The user chooses Voice mode or Live board mode. Both record 16 kHz mono audio in bounded 20-second chunks until the user stops the visible session. Live board also captures camera frames at an adjustable interval and rejects similar frames locally through an exposure-compensated luminance comparison. Accepted frames may contribute visible text, equations, concepts and orientation-corrected diagram crops. Diarized transcript segments are stored with timestamps. Because independently uploaded chunks do not guarantee stable speaker labels, the current heuristic may prioritize only the dominant label inside one chunk; manual choice also expires at the next chunk. Persistent teacher identity requires a future speaker-embedding or provider session-identity layer. Runnable-note mode incrementally synthesizes structured Markdown, while Verbatim mode preserves timestamped evidence without AI summarization. A locally stored syllabus can supply bounded topic context but is explicitly not treated as evidence. Suspected errors are returned as reviewable annotations instead of silently replacing a captured claim. Room version 2 stores notes, folders, syllabi, sessions, transcripts, visual evidence, assets and provenance; a single note can be exported as an Obsidian-ready Markdown/asset ZIP. The continuous MVP has automated Android/backend evidence and two passing Android 16 mock-device instrumentation slices: short final-partial Voice capture and timed CameraX-plus-audio Video capture. These tests verify deterministic plumbing and persistence, not real-provider, speech, diarization, note, OCR or diagram accuracy; those metrics and YouTube/endurance trials remain pending.

## 1. Introduction

Students and knowledge workers often need to listen, understand and record information simultaneously. Manual typing competes with attention. Speech transcription reduces typing effort but generally preserves the order of words rather than the hierarchy of ideas. Technical teaching also relies on boards, projectors and monitors containing equations, diagrams and short labels that may never be spoken in full. Saving only audio misses these visual elements; saving every camera frame wastes storage, bandwidth and model usage.

VoxBox treats note creation as an evidence-processing problem. It keeps the captured transcript and selected visual evidence separate from the generated note, then updates one structured Markdown representation in small revisions. The design supports a classroom lecture, but its scope is deliberately broader: a meeting, interview, conversation, tutorial or spoken explanation can use the Voice workflow, while any session with meaningful displayed material can use Live board.

The earlier project baseline focused on bounded Android `SpeechRecognizer` sessions, a deterministic formatting language named VoxScript and a user-triggered Board still. Those components produced useful verified milestones, but the refined requirement places continuous foreground capture and AI-assisted structure at the centre. The current implementation retains the legacy source/evidence and manual Board utility while the Speak destination now uses a new `AudioRecord`-based multimodal session pipeline.

## 2. Problem statement

A practical automatic note system must solve several problems together:

1. Capture a long explanation without requiring the user to restart after each phrase.
2. Reduce unrelated speech using honest chunk-local diarization labels, while recognizing that persistent main-speaker identity is a separate unsolved problem.
3. Preserve raw evidence while producing concise, well-structured notes.
4. Combine speech with changed board/projector content, equations and diagrams.
5. Avoid processing duplicate frames and avoid retaining unnecessary full images.
6. Use local syllabus context without inventing content that was not captured.
7. Detect possible transcription, OCR or factual conflicts without silently rewriting history.
8. Store, continue, search, organize and export notes in a durable open format.
9. Keep a provider secret outside the APK and degrade honestly when the provider is unavailable.

These requirements are interdependent. Aggressive summarization can remove useful evidence; conservative transcription can create unusable notes. Frequent frames improve temporal coverage but increase resource use. Speaker filtering can remove classroom noise but may also remove an important question. VoxBox therefore exposes policy, confidence and fallback states rather than treating one opaque model response as the source of truth.

## 3. Research questions

The evaluation will address the following questions after fixed corpora are prepared:

1. How accurately can the system preserve key concepts while reducing transcript repetition in Runnable-note mode?
2. How accurately can a per-chunk dominant-duration heuristic reduce unrelated speech, and what cross-chunk speaker-identity method would be required before claiming five-to-six-minute teacher focus?
3. How much model/storage work can local frame-change filtering avoid without missing meaningful board changes?
4. How accurately are board text, equations and diagram regions transferred into the note?
5. Does syllabus context improve coverage without causing unsupported additions?
6. Can revision, provenance and fallback rules prevent silent loss or silent correction of captured evidence?
7. Is the complete foreground voice/video workflow stable and usable for a realistic lecture duration on the target Android device?

## 4. Objectives

1. Provide user-controlled foreground Voice and Live board sessions that continue until stopped.
2. Record 16 kHz mono PCM16 audio in bounded chunks with explicit overload behavior.
3. Store diarized timestamped transcript segments as evidence.
4. Select a conservative dominant diarization label within each chunk, allow a chunk-local manual override, and preserve all returned speaker evidence.
5. Capture Live board frames at an adjustable cadence and reject similar frames locally.
6. Extract visible text, equations, concepts and diagram regions from accepted frames.
7. Retain durable diagram crops while deleting unnecessary or successfully processed raw frames.
8. Offer Runnable and Verbatim note policies.
9. Maintain one revisioned Markdown block whose updates are idempotent and conflict checked.
10. Use a selected local syllabus as context only and surface suspected corrections for review.
11. Support recent-note continuation, local folders, syllabus import, search and Obsidian-compatible export.
12. Keep provider credentials on a backend boundary and retain a deterministic no-key test mode.
13. Evaluate accuracy, faithfulness, resource use, persistence and physical-device stability without unmeasured claims.

## 5. Scope and limitations

### 5.1 Foreground continuous capture

In this report, continuous capture means a visible activity-bound foreground session. The user explicitly starts it, sees live status and stops it. The Live screen requests that Android keep the display awake while capture is starting, running or stopping. Back navigation or leaving Live initiates stop-and-drain. VoxBox does not implement a hidden background or screen-off recording service. Process destruction can still interrupt capture; interruption recovery is therefore an evaluation and future-hardening area.

The new path uses `AudioRecord`, not Android `SpeechRecognizer`, because `SpeechRecognizer` is not designed as a continuous raw-audio stream. The bounded recognizer source and its earlier verification record remain in the project, but the current Speak destination uses the new live-session path.

### 5.2 Current format and organization limits

The database can represent nested folders, but the current UI creates and selects top-level folders only. Syllabus import accepts UTF-8 Markdown or text up to 750 KB; PDF and Word extraction are not implemented. Export operates on one note at a time, not a complete folder or synchronized vault. Cloud collaboration and cross-device sync remain outside the MVP.

### 5.3 Speaker-focus limit

VoxBox does not recognize a teacher's identity or perform voice biometrics. Labels returned for separately uploaded chunks may reset or swap, so the current implementation never accumulates them into a five-minute identity. It estimates a dominant label within one response only. If labels are absent or speakers are close, it reports uncertainty. A user selection applies to the latest chunk and expires when the next response arrives. Persistent focus would require consent-aware speaker embeddings/enrolment and cross-chunk clustering, or a provider contract with session-stable identities.

### 5.4 AI and correction limit

Runnable notes depend on a configured note-refinement service for semantic structure. When it is unavailable, the app appends captured evidence under a visible needs-review heading. The model may still be wrong; correction entries and evidence retention reduce risk but do not create a guarantee of factual correctness. A syllabus is contextual reference, not proof of what occurred.

### 5.5 Retention limit

Similar frames are removed immediately and successfully processed frames are removed after commit. Failed raw frames can remain temporarily for retry/diagnosis. The current code removes matching cache frames older than 30 minutes when the live-session ViewModel is created; a continuously scheduled cleanup worker is not yet present.

## 6. System methodology

### 6.1 Session setup

The Compose setup screen collects capture mode, note destination, note policy, optional folder, optional syllabus, frame interval and change threshold. Voice requires microphone permission. Live board requires microphone and camera permissions. A user may create a note or continue a recent one; the repository appends a stable Markdown block for each new capture session.

### 6.2 Audio acquisition and chunking

`PcmAudioChunkRecorder` opens `AudioRecord` with `MediaRecorder.AudioSource.VOICE_RECOGNITION`, 16,000 samples per second, mono input and 16-bit PCM encoding. Hardware/software noise suppression and automatic gain control are enabled only when Android reports them available. Bytes are accumulated into a 20-second target chunk and encoded with a standard 44-byte WAV header.

When possible, each completed WAV is atomically written to app-private recovery storage before it is enqueued. The ordered audio channel is unlimited but carries small file references, not full audio byte arrays. Transcription uses three total attempts: the initial call followed by 750 ms and 2 s delays. A recovery WAV is deleted only after its transcript evidence and structured-note revision commit. After exhausted failure it remains private and a warning is persisted into the note. If recovery storage is unavailable, the current WAV remains in memory with a visible warning; this is less safe for a long session.

Frames use an independent one-slot drop-oldest channel. A newly captured frame may supersede an older unprocessed frame, which is deleted, while the audio queue continues. On stop, `stopAndDrain` ends microphone reads, emits a final partial WAV of at least approximately half a second, closes both channels, waits for their workers and then marks the session stopped. This prioritizes audio recoverability over bounded disk growth; retained-WAV management and long-session resource use require evaluation.

### 6.3 Diarized transcription and speaker focus

The Android client sends Base64 WAV data, session id, chunk id and chunk offset to `/v1/audio/transcribe`. The strict response contains speaker-labelled segments whose provider-relative timestamps are converted to session-relative values by the proxy. Segments are inserted into Room in stable order before note refinement.

`PerChunkSpeakerTracker` deduplicates segments by id and sums duration by label only within the current transcription response. Automatic focus requires a leading share of at least 0.58 and a margin of at least 0.15 over the runner-up. Otherwise the state is `AMBIGUOUS` or `UNAVAILABLE`. Manual selection changes prioritization for current-chunk evidence but is cleared by the next chunk. Other-speaker evidence is not deleted.

### 6.4 Local frame-change filtering

Live board uses CameraX `LifecycleCameraController` with preview and image capture. The current UI exposes a two-to-thirty-second cadence; the default is eight seconds. `LiveCameraPanel` schedules `takePicture` through a main-loop `Handler` scoped to the camera `DisposableEffect` and removes the callback on disposal. This design was introduced after Android 16 testing showed that preview could open while the earlier Compose-coroutine timer never invoked a capture. Each temporary JPEG is decoded into a 32×32 luminance sample. The detector subtracts each frame's mean brightness before comparing pixels, which reduces false changes caused by projector flicker or automatic exposure. It computes:

```text
score = 0.65 × normalized mean centred difference
      + 0.35 × changed-pixel fraction
```

The score is clamped to `[0, 1]` and compared with the selected threshold. An accepted frame becomes only a pending baseline candidate. It replaces the last committed baseline after extraction, note revision and evidence persistence all succeed; failure discards the candidate so the same board change can be observed again. A short perceptual bit pattern is SHA-256 hashed and truncated to provide an evidence fingerprint. Similar frames are deleted without using the proxy or local OCR.

### 6.5 Board extraction and diagram cropping

An accepted frame is recorded as visual evidence, then sent to `/v1/board/extract`. The remote result contains a title, summary, visible text, concepts, equations, warnings and zero or more normalized diagram rectangles. If the proxy is unavailable, the existing coordinator can use bundled ML Kit text recognition; this fallback does not invent diagram regions.

`DiagramCropper` reads EXIF orientation, validates normalized bounds, converts them into clipped pixel bounds and stores each accepted crop as a 92-quality JPEG in private note storage. The crop is registered as a `NoteAsset` and referenced by a relative Markdown image link. If the database insert fails, the newly written crop is deleted. After crops, Markdown revision and evidence state commit, the detector baseline advances and the full raw frame is deleted.

### 6.6 Incremental structured-note synthesis

Each session creates one `MARKDOWN` block and a `CaptureSession` row with revision zero. The refinement route is backward compatible. An omitted `responseMode` retains the original full-document request/response. The live Runnable path uses `delta` mode. It builds context from the continued note plus the current session, leaves `existingMarkdown` empty and sends a bounded `noteContext`: title up to 240 characters, outline Markdown up to 12,000 characters, recent Markdown up to 24,000 characters and a 64-character lowercase SHA-256 of the complete current content. The response returns only a non-blank append-only `markdownDelta` and must echo that base hash. The Android client validates response mode, revision and hash before appending the delta to the current session Markdown and committing that materialized session block. Delta responses are capped at 2,000 output tokens versus 5,000 for full responses. This bounds repeated request/output growth, but an append-only delta cannot rewrite or reorganize an older section; a deliberate full-note consolidation workflow remains future work.

The Room transaction checks request patch id, expected revision, non-blank materialized Markdown and the identity/type of the generated block. A repeated identical request is idempotent. The backend cache also fingerprints the normalized request: reusing the same session/request ids with changed evidence returns HTTP 409 instead of replaying stale content. A successful local update advances the revision and touches the parent note timestamp.

Verbatim mode formats transcript segments locally with timestamp and speaker label, and formats board evidence as quoted text/equations. It does not invoke semantic note refinement. If Runnable refinement fails, the local fallback appends a “Captured evidence (needs review)” section and a warning.

### 6.7 Evidence-preserving corrections

The backend instruction treats transcript, board content, existing Markdown and syllabus as untrusted evidence rather than instructions. It requires the model to preserve a captured claim and return a separate correction record containing captured text, suggested annotation, reason, severity and evidence ids. The Android UI displays these records under Review flags. The current correction list is session UI state; a complete persisted accept/reject correction workflow is still pending.

### 6.8 Local syllabus context

The importer accepts `.md`, `.markdown` and `.txt`, validates size and UTF-8-like text, calculates SHA-256 and stores a private hash-named copy. The database deduplicates by hash. For the live path, Android ranks syllabus sections against the current transcript/board evidence and selects at most six excerpts. The contract accepts at most eight explicit excerpts, each with at most 2,000 text characters and at most 12,000 characters in total. Legacy clients may still send a raw context field up to 120,000 characters; if it exceeds 12,000, the server ranks chunks against current transcript/board terms and forwards at most 12,000 characters to the provider. The prompt states that the syllabus may help interpret terminology but cannot establish that a topic was taught.

### 6.9 Markdown and Obsidian export

The exporter renders ordered blocks to UTF-8 Markdown. Generated Markdown remains unchanged; headings, bullets and the legacy pie-chart payload receive deterministic Markdown representations. Available diagram assets are copied into an `assets/` folder and note links are rewritten to the exported relative path. The Markdown file and assets are placed in a shareable ZIP. Export cache entries older than 24 hours are removed during a subsequent export.

## 7. Architecture

```text
Presentation layer
  Compose Notes library | Live session | Legacy Board/Speech
                  ↓ immutable state / explicit actions
ViewModels
  NoteLibraryViewModel | CaptureSessionViewModel | legacy capture view models
                  ↓
Domain/services
  audio chunk recorder | per-chunk speaker tracker | frame detector
  board coordinator | diagram cropper | revision policy | exporter
                  ↓
Data
  Room v2 repositories | app-private syllabus/assets | temporary cache
                  ↓
Development backend boundary
  diarized transcription | board vision | note refinement
```

The backend is intentionally stateless for durable user data. It forwards media in memory and keeps only a small ten-minute/128-entry in-memory idempotency cache for note responses. Cache entries include a normalized request fingerprint, so an identifier collision with changed evidence is rejected. Android remains the owner of note state and revision.

## 8. Data model

Room version 2 retains the original `Note` and ordered `NoteBlock` entities and adds:

- `Folder` with optional parent id;
- `NoteLocation` joining a note to one folder;
- `Syllabus` with local path, extracted text and SHA-256;
- `CaptureSession` with mode, policy, status, frame configuration, a reserved speaker-focus field, generated block and revision; current speaker focus is chunk-local rather than a persisted identity;
- `TranscriptSegment` with speaker and timestamps;
- `VisualEvidence` with fingerprint, change score, processing state and temporary path;
- `NoteAsset` with durable crop path and caption; and
- `BlockProvenance` linking generated content to its session/source.

`MIGRATION_1_2` creates the new tables and indices without a destructive fallback. Existing v1 notes/blocks are intended to remain intact. The schema compiles through Room/KSP, but a real-device migration and Room migration instrumentation test are still required.

## 9. Backend and model boundary

The Node development proxy exposes `/health`, `/v1/audio/transcribe`, `/v1/board/extract` and `/v1/notes/refine`. JSON bodies, decoded media and response sizes are bounded. Media signatures and normalized diagram boxes are validated. Mock mode produces deterministic integration output without inspecting supplied media. Debug Android builds use the loopback base URL. Release builds require an explicit absolute HTTPS `VOXBOX_API_BASE_URL`; the Gradle pre-release check rejects a missing URL, credentials, query or fragment, and the runtime client also validates the final endpoint.

The fixed current routing is:

- `gpt-4o-transcribe-diarize` for timestamped speaker-labelled transcription;
- `gpt-5.6-sol` for text/image board interpretation; and
- `gpt-5.6-terra` for lower-latency incremental Markdown synthesis.

The note and board Responses requests use `store: false`. The Android client never sends or stores an API key. The credential previously pasted into chat is exposed and must be revoked. The loopback server has no user authentication, rate limiting or TLS and is unsuitable for public release without an authenticated HTTPS gateway.

## 10. Evaluation plan

### 10.1 Audio corpus

Record fixed lecture excerpts in quiet, moderate-noise and competing-speaker conditions. Preserve reference words, speaker turns and session timestamps. Report word error rate, diarization error rate, chunk-boundary errors, transcription latency and missing/duplicated segment counts.

### 10.2 Speaker-focus corpus

Use chunks with known speaker-duration distribution across quiet, noisy and multi-speaker sessions. Measure per-chunk dominant-label precision/recall, ambiguous/unavailable rate and whether a manual override affects only current-chunk Runnable evidence without deleting other segments. Separately evaluate future speaker-embedding/session-stable identity approaches before making any five-to-six-minute teacher-focus claim.

### 10.3 Note-quality protocol

Create reference key points and structure labels. Compare Verbatim evidence, Runnable notes without syllabus and Runnable notes with syllabus for coverage, key-point precision/recall, duplicate rate, Markdown validity, unsupported statements, correction precision and human acceptance. Every evaluated sentence should be traceable to transcript/frame/syllabus context, with syllabus-only additions counted separately.

### 10.4 Frame and diagram corpus

Use a versioned sequence containing unchanged frames, small writing additions, slide changes, projector exposure changes, equations and diagrams. Measure duplicate rejection, meaningful-change recall, requests/storage avoided, visible-text/equation accuracy, diagram-region intersection-over-union and crop legibility.

### 10.5 System/device protocol

On the target Redmi device, test microphone/camera permission states, concurrent audio/camera, multiple chunks, stop-and-drain behavior, force-stop/relaunch, v1→v2 migration, note continuation, folder/syllabus selection, export/share, proxy outage, local OCR fallback, queue saturation and interruption. Record battery, storage, memory, network bytes and crash-free lecture duration.

### 10.6 YouTube teaching trials

Use at least three permitted videos through ordinary microphone/camera capture rather than direct platform integration:

1. text-heavy conceptual teaching;
2. mathematics with equations and progressive working; and
3. science/engineering with diagrams.

Record video identity/duration, capture settings, environment, expected concepts, produced transcript/note/assets, errors and fixes. A demonstration is not an accuracy corpus unless the reference and scorer are fixed first.

## 11. Ethical, privacy and security considerations

Capture is visible, user-started and user-stopped. The user must obtain institutional/legal consent before recording a class or conversation. Speaker labels describe chunk-local diarization clusters, not personal identity, and may change between requests. The system should not be marketed as identifying or monitoring a teacher. A future embedding/enrolment design would require additional consent, biometric-risk review and retention controls.

Similar frames are rejected locally. Durable storage retains notes, evidence metadata, transcript segments, syllabi and accepted crops; successfully processed full raw frames are deleted. Android backup is disabled. Syllabus context and provider-generated suggestions remain distinguishable from captured evidence.

An APK cannot protect a reusable provider secret, so the key belongs only in a backend environment. The exposed development key must be revoked. Public deployment requires authenticated HTTPS, rate limits, authorization, audit logging and a documented retention policy.

## 12. Results to date

### 12.1 Verified legacy native-speech/VoxScript milestone

The bounded Android speech implementation passed its contemporary unit/build checks and was exercised on Redmi model `2411DRN47I`, Android 16/API 36. Permission states, system-recognizer fallback and the visible listening state were verified. The deterministic phrase `Tejas pie chart 25 percent yellow label wheat` rendered the requested editable chart on device. Human-spoken word error rate was not measured.

### 12.2 Verified legacy persistence and note-detail milestones

The original Room v1 schema stored `Note` and ordered typed `NoteBlock` rows. On the physical device, a saved 25% yellow wheat chart survived force-stop/relaunch and reopened with its stored label, percentage and color. Narrow typed-edit operations later gained automated coverage. The exact typed-edit → force-stop → reopen device regression remained pending and must not be inferred from the earlier read-only reopen test.

### 12.3 Verified organized UI/search/manual Board milestone

The Notes, Speak and Board Material 3 interface, literal case-insensitive search and bounded CameraX Board-still workflow passed 36 Android unit tests and the contemporary build/lint gate; the proxy passed 3/3 tests. On the physical Redmi device, a real preview, manual still, clearly labelled mock review, explicit save and relaunch recovery were demonstrated. Offline OCR routing also reached review, but the captured image was dark and contained no readable text. This was routing evidence, not OCR accuracy. Physical search typing remained pending.

### 12.4 Continuous multimodal MVP — automated evidence

The current shared tree implements the new foreground session, audio chunking, diarized client contract, speaker tracker, periodic frame path, local change detector, diagram cropper, Room v2 model, revisioned Markdown, folders, syllabus import and Markdown/asset export.

Current automated result:

- Android JVM unit suite: **70 tests, 0 failures/errors/skips across 23 suites**.
- Android production `compileDebugKotlin`: **passed**, including Room/KSP production compilation.
- Android `lintDebug`, `assembleDebug` and `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**; lint reports 0 errors and 18 warnings.
- Final `app-debug.apk`: **61,182,613 bytes**, SHA-256 `DABF116DB614D0066AD3AC867C2D77BB7E344154C8B6FCAE7A563FD12CCCA0AB`.
- Backend Node suite: **11/11 passed** with mock/fake providers, including delta/full compatibility, bounded context and idempotency-conflict cases.

The JVM/Node tests cover pure/domain and protocol behavior, not physical microphone/camera accuracy. Room migration instrumentation is absent because `room-testing` is not installed. The final secret scan is clear (only `server/.env.example` is present as an environment template), and `git diff --check` exits 0.

### 12.5 Android 16 mock-device evidence

The opt-in `LiveCaptureDeviceSmokeTest` was split so each long-running path could be isolated and timed against the deterministic loopback mock proxy:

- `voiceDrainsFinalPartialChunk` passed standalone in **9.561 seconds**. Capture stopped after four seconds, before the target chunk boundary. The final partial WAV drained, received mock transcription and refinement, committed its note update, and reached the saved-note state.
- `videoCapturesBoardAndAudio` passed standalone in **32.742 seconds**. The corrected scheduler produced timed CameraX captures; mock board extraction, continuous audio, incremental note updates, Stop/drain and saved-note persistence all completed.

The first Video attempts produced a visible CameraX preview but no `takePicture` call, exposing a real scheduler defect. Replacing the Compose-coroutine timer with a main-loop `Handler` owned by the camera `DisposableEffect` fixed the failure. The passing Video result is therefore regression evidence for the corrected implementation, rather than a claim based only on code inspection. Screenshots named `live-voice-drain-saved.png`, `live-video-board-and-audio.png` and `live-video-board-and-audio-saved.png` were written to the app external `evidence/` directory.

Two recovery WAVs came from intentionally aborted diagnostic sessions. They were matched to their exact `Device smoke` session ids before deletion, and no unrecovered-audio file remained after cleanup. This manual reconciliation does not replace the planned user-facing retained-WAV recovery/deletion interface.

### 12.6 Unverified work

No real OpenAI request was made for the continuous increment. Therefore provider compatibility, latency, cost, diarization quality, structured-note quality, factual correction quality, vision accuracy and diagram extraction remain unmeasured. No noisy-room/competing-speaker corpus, endurance run, diagram-crop corpus or YouTube teaching-video trial has run. The physical Room v1→v2 migration is also pending. No accuracy percentage is available.

## 13. Discussion

The new architecture improves alignment with the actual problem by treating notes as an evolving evidence-linked artifact rather than a series of spoken formatting commands. Recovery WAVs, separate audio/frame queues, commit-after-success frame baseline, revision guards and fallback text make resource loss and service failure visible. Separating Verbatim from Runnable also gives the user control over semantic compression.

The design still has important risks. Discrete 20-second requests are simpler than a streaming socket but create delay and may split words or speaker turns. Independent diarization labels cannot establish one teacher across requests; even within a chunk, dominant duration can select a talkative audience member. Slow service can grow the private recovery-file queue and consume disk. Camera frame difference does not itself know whether a change is pedagogically useful. A language model can over-summarize, misuse syllabus context or suggest an unsupported correction despite prompt constraints. These are evaluation targets, not solved claims.

## 14. Conclusion

VoxBox now has an implemented vertical slice for evidence-preserving continuous voice and board-to-Markdown notes: local audio chunks, chunk-local diarized evidence handling, locally filtered frames, diagram assets, revisioned Room storage, local context and portable export. Automated tests establish the domain rules and contracts, while the two Android 16 mock-device tests establish a short final-partial Voice path and a timed CameraX-plus-audio Video path through saved persistence. Device testing also found and verified the fix for a real camera scheduler defect, and the final Android build/lint/APK gate passes. Persistent teacher identity remains future work. The next decisive milestones are a deliberately small rotated-key provider test, physical migration and fixed noisy-audio/board/crop corpora. VoxBox may now be presented as an implemented MVP with bounded mock-device runtime evidence, but not as an accurate or provider-verified AI note taker.

## References — working list

1. Android Developers. “AudioRecord.” https://developer.android.com/reference/android/media/AudioRecord
2. Android Developers. “MediaRecorder.AudioSource.” https://developer.android.com/reference/android/media/MediaRecorder.AudioSource
3. Android Developers. “CameraX architecture.” https://developer.android.com/media/camera/camerax/architecture
4. Android Developers. “Room persistence library.” https://developer.android.com/training/data-storage/room
5. Android Developers. “Recommendations for Android architecture.” https://developer.android.com/topic/architecture/recommendations
6. Android Developers. “SpeechRecognizer.” https://developer.android.com/reference/android/speech/SpeechRecognizer
7. Google ML Kit. “Text recognition v2.” https://developers.google.com/ml-kit/vision/text-recognition/v2
8. OpenAI Developers. “Models.” https://developers.openai.com/api/docs/models
9. OpenAI Developers. “GPT-5.6 sol.” https://developers.openai.com/api/docs/models/gpt-5.6-sol
10. OpenAI Developers. “GPT-4o Transcribe Diarize.” https://developers.openai.com/api/docs/models/gpt-4o-transcribe-diarize
11. Markdown Guide. “Basic Syntax.” https://www.markdownguide.org/basic-syntax/
12. Obsidian Help. “Import notes.” https://help.obsidian.md/import-notes

## Evidence still required

- Updated component, sequence, state-machine, ERD and frame-retention diagrams.
- Screenshots of setup, speaker-focus and export UI beyond the three saved mock-device smoke screenshots.
- Physical-device v1→v2 migration and extended/interrupted-session reproduction log.
- Versioned audio, board/frame and note-reference corpora.
- Rotated-key live-provider model/source/latency/cost record.
- YouTube text/maths/diagram test records and error analysis.
- Long-session battery, storage, memory and network measurements.

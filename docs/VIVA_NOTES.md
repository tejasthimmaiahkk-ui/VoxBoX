# VoxBox Viva Notes

Last updated: 2026-08-04

## One-sentence answer

VoxBox is a local-first Android tool that turns a user-controlled foreground voice or camera-plus-voice session into an evidence-preserving, revisioned Markdown note with optional syllabus context and portable diagram assets.

## What problem does it solve?

Ordinary dictation produces a flat transcript, while a board photograph loses the spoken explanation and temporal context. VoxBox combines timestamped speech with selected changed board frames so the user receives a structured note without discarding the underlying evidence.

## Is it only for college lectures?

No. A lecture is the primary evaluation scenario, but Voice mode can structure a meeting, interview, conversation, tutorial or spoken explanation. Live board applies whenever a board, projector or monitor contains important visual information.

## What are the two modes?

- **Voice:** continuous foreground audio in bounded 20-second chunks until the user stops.
- **Live board:** the same audio path plus automatic CameraX frames at an adjustable interval. Similar frames are rejected locally.

The older manual Board-still screen remains as a bounded fallback/legacy tool.

## What does “continuous” mean?

It means one visible foreground session controlled by the user. The Live screen keeps the display awake while active. Back or leaving Live starts stop-and-drain. It is not a hidden background or screen-off recorder. Process destruction can still interrupt capture; robust interruption recovery remains testing/future-hardening work.

## Why not use SpeechRecognizer for the new continuous path?

Android documents that `SpeechRecognizer` is not intended for continuous recognition. The new path uses `AudioRecord` to acquire raw 16 kHz mono PCM16 audio and emits 20-second WAV chunks. The bounded recognizer source and its earlier verification record remain, but the current Speak destination uses the new live-session path.

## Is 20 seconds truly streaming?

No. It is continuous acquisition with discrete chunked requests, not a low-latency WebSocket stream. Chunking is simpler and restartable, but it adds latency and may split words or speaker turns. Those effects must be measured.

## What happens if processing is too slow?

Audio and frames have separate queues. A completed WAV is first written to private recovery storage when possible, then its small file reference enters an ordered audio queue. Transcription receives three total attempts; the WAV is deleted after transcript/note success and retained with a warning after failure. Frames use a one-slot drop-oldest queue, so a stale frame can be deleted without evicting audio. Slow service can grow recovery storage and must be measured.

## How does it find the teacher?

It does not identify the teacher. The transcription provider returns anonymous labels for each independently uploaded chunk, and those labels may reset or swap on the next request. VoxBox totals voiced duration only inside the current chunk and marks a dominant label when it has at least 58% of that chunk's voiced time and a 15-percentage-point margin. Otherwise it reports ambiguous/unavailable. A manual selection also applies only to the latest chunk. All returned speakers remain in the evidence transcript.

## Does it satisfy the requirement to learn the teacher within five to six minutes?

Not yet. Accumulating provider labels across independent chunks would create a false identity claim. Persistent teacher focus requires consent-aware speaker embeddings or enrolment plus cross-chunk clustering, or a provider that guarantees session-stable diarization identities. That future layer needs biometric/privacy review and its own evaluation before VoxBox can claim five-to-six-minute teacher focus.

## What are Runnable and Verbatim notes?

- **Runnable:** concise structured Markdown that may prioritize the selected/dominant label inside the current chunk, removes repetition and unrelated chatter, and returns possible corrections as review flags.
- **Verbatim:** timestamped diarized utterances and board evidence in order, formatted locally without AI summarization.

Raw transcript evidence is stored in both cases.

## Can the AI silently correct a teacher?

No. Transcript/board content remains evidence. The note service must return a separate correction record with captured text, suggested annotation, reason, severity and evidence ids. The UI displays it for review. The current session displays these flags; a full persisted accept/reject correction workflow is still pending.

## How is the syllabus used?

The user imports a local UTF-8 Markdown/text syllabus and explicitly selects it for a session. The live Android path ranks sections against new evidence and sends at most six excerpts; the proxy contract permits up to eight. The provider receives at most 12,000 syllabus characters, with each explicit excerpt limited to 2,000 characters. A backward-compatible raw context is relevance-selected to the same provider limit. The prompt says that syllabus content is not proof that something was taught and must not overwrite captured evidence.

## What syllabus formats work?

`.md`, `.markdown` and `.txt`, up to 750 KB. The app hashes and stores a private copy. PDF/DOCX extraction is not implemented.

## How does Live board save resources?

CameraX captures at the chosen cadence. Before any remote/OCR work, a 32×32 luminance sample is compared with the last accepted frame. Mean brightness is removed so projector flicker or auto-exposure is less likely to appear as new content. Similar frames are deleted locally.

## How do you know timed camera capture really runs?

The Android 16 smoke test initially showed the value of runtime testing: CameraX preview opened, but the earlier Compose coroutine never called `takePicture`. `LiveCameraPanel` now schedules capture with a main-loop `Handler` scoped to the camera `DisposableEffect` and cancels the callback on disposal. The corrected Video test then passed timed capture, board extraction, concurrent audio, incremental note updates, Stop/drain and save.

## Can frame filtering miss small writing?

Yes. Threshold and sampling trade recall for fewer requests. Small local changes, occlusion and camera motion can create false decisions. That is why meaningful-change recall and duplicate rejection must be measured on a labelled frame sequence.

## How are diagrams added?

Board extraction may return normalized diagram rectangles. Android validates them, applies EXIF orientation, crops the corresponding pixels and stores a private JPEG. The generated Markdown links to that durable crop. Offline OCR does not invent a diagram box.

## When are frames deleted?

- Similar frames: immediately.
- Successfully processed changed frames: only after the note revision and diagram assets commit.
- Failed changed frames: temporarily retained for diagnosis/retry. Matching cache frames older than 30 minutes are removed when the live-session ViewModel is created.

The frame-difference baseline advances only after successful extraction and persistence. A failed candidate is discarded, so a later frame is still compared with the last successful board state.

A periodic cleanup worker independent of screen recreation is still pending.

## Why keep one generated Markdown block?

It provides a stable exportable note while preserving all evidence in separate tables. Every update uses an expected revision and patch id, so stale updates conflict, blank output cannot erase the note and identical retries are idempotent.

## Does every update resend the complete growing note?

The legacy full-document contract remains supported, but the live Runnable path uses delta mode and does not send the complete note. It builds bounded context from the continued note plus this session and sends a title, outline, recent tail and SHA-256 of the complete current content. The service returns only append-only Markdown tied to that hash and revision. Android validates the response before appending it to and committing the current session block. The trade-off is that delta mode cannot rewrite an older section; a deliberate full-note consolidation operation remains future work.

## Why use Room version 2?

Room v2 stores notes/blocks plus folders, note locations, syllabi, capture sessions, transcript segments, visual evidence, note assets and provenance. The additive migration preserves v1 notes/blocks in design. Production compilation passes, but a real-device v1→v2 migration and `room-testing` instrumentation check remain pending.

## How are notes organized?

The dashboard sorts recent notes, supports search and can filter by selected folder. A live session may create a note or continue a recent one. The schema supports nested folders, but the current UI creates/selects top-level folders only.

## How does Obsidian export work?

One note is rendered as UTF-8 Markdown. Available diagram crops are copied into an `assets/` directory, links are rewritten and both are zipped for Android sharing. This is single-note export, not full-vault synchronization.

## Why use a backend?

An APK cannot keep a reusable API key secret. Debug Android sends bounded evidence to a loopback development proxy; only the proxy reads `OPENAI_API_KEY`. A release build requires an explicit HTTPS base URL and fails its validation task for a missing/insecure/malformed URL. Public deployment still needs authentication, rate limits and proper secret management.

## Which models are routed?

- `gpt-4o-transcribe-diarize` for speaker-labelled transcription.
- `gpt-5.6-sol` for board/image evidence.
- `gpt-5.6-terra` for incremental Markdown refinement.

The model split matches the modality and latency/cost role. It is a current implementation choice, not an accuracy claim.

## Is the pasted API key safe?

No. It is exposed and must be revoked. It was not needed for the automated tests and must never be embedded in Android. A rotated replacement belongs only in the proxy process environment.

## What happens without the proxy or AI?

Verbatim note formatting works locally after transcript evidence is available. If Runnable refinement fails, captured evidence is appended under a visible needs-review section. Board extraction can fall back to bundled ML Kit OCR. The deterministic mock mode tests plumbing but does not analyze media.

## What is VoxScript’s role now?

VoxScript remains a deterministic optional/legacy command layer for exact formatting or an offline fallback. It is no longer the main product definition; ordinary continuous speech should not require the teacher to speak formatting commands.

## What is novel?

The contribution is the combination of evidence preservation, two note policies, honest chunk-local speaker handling, local exposure-aware frame filtering, durable diagram crops, context-without-proof syllabus handling, revision-safe Markdown and open-format export in one Android workflow.

## How will it be evaluated?

- Speech word error rate and diarization error rate.
- Per-chunk dominant-label accuracy, ambiguity and manual-override expiry.
- A separate future evaluation of speaker embeddings/session-stable identities before any five-to-six-minute teacher-focus claim.
- Key-point precision/recall, duplication, faithfulness and unsupported-note rate.
- Correction precision and syllabus leakage.
- Frame-change precision/recall and API/storage savings.
- OCR/equation accuracy, diagram-region overlap and crop legibility.
- Revision/persistence/export recovery.
- Latency, queue pressure, battery, memory, storage and network usage.
- Additional physical-device failure/endurance cases and YouTube text/maths/diagram teaching trials.

No accuracy percentage exists yet.

## What is verified today?

### Verified legacy baseline

- Bounded speech permission/fallback/listening state and deterministic chart on the Redmi device.
- Room v1 save/relaunch/reopen for the chart note.
- Organized UI and manual CameraX Board mock-save/relaunch flow.
- Offline OCR routing only; the test frame was dark, so no OCR accuracy result.

### New continuous MVP — automated evidence

- 70 Android JVM tests pass with 0 failures/errors/skips across 23 suites.
- Production `compileDebugKotlin` succeeds, including Room/KSP compilation.
- `lintDebug` reports 0 errors and 18 warnings; `assembleDebug` and `assembleDebugAndroidTest` both pass.
- The final debug APK is 61,182,613 bytes with SHA-256 `DABF116DB614D0066AD3AC867C2D77BB7E344154C8B6FCAE7A563FD12CCCA0AB`.
- 11/11 backend tests pass with mock/fake providers and no live request.
- Code exists for the full foreground voice/video vertical slice, evidence, Room v2, syllabus, revision and export paths.

### New continuous MVP — Android 16 mock-device evidence

- Voice passed standalone in 9.561 seconds: a four-second final partial WAV drained, was mock-transcribed/refined, persisted and saved.
- Video passed standalone in 32.742 seconds: timed CameraX capture, mock board extraction, concurrent audio, incremental note updates, Stop/drain and saved-note persistence passed.
- Three screenshots were saved in the app external `evidence/` folder.
- Two WAVs from intentionally aborted diagnostics were matched to their exact `Device smoke` session ids and deleted; no unrecovered-audio file remained.

These results verify deterministic mock plumbing, not real transcription, AI, OCR or diagram accuracy.

### 2026-08-04 increment

- The board-evidence serialization defect is fixed and covered by request-contract tests.
- Provider failures are classified end to end; retries that cannot succeed are skipped.
- Retained WAVs can be recovered or deleted from the app.
- The interface was rebuilt on a design system with its own icon set.
- Android JVM: 80 tests across 26 suites, 0 failures. Backend: 16/16. Lint: 0 errors, 18 warnings.

### Not yet verified/evaluated

- Both mock-device scenarios re-run against the redesigned interface.
- End-to-end confirmation that board evidence now reaches the note provider.
- Real-device Room v1→v2 migration.
- Any rotated-key OpenAI request or model accuracy/latency/cost.
- Noisy-room and competing-speaker evaluation.
- Multiple-cadence frame/crop corpus, YouTube tests and long-session resource results.
- Hardware verification of the new retained-WAV recovery and deletion controls.

## Did you find any real defect by testing, and how?

Two, and both are worth stating plainly.

The first was found on the device. Live board opened the CameraX preview but never captured a frame: the Compose coroutine used for periodic capture never invoked `takePicture` on the tested runtime. Capture scheduling now uses a main-loop `Handler` owned by the camera `DisposableEffect`. The passing Video run is from the corrected path.

The second was found by reading the request contract on 2026-08-04. The note-refinement client built its JSON with `boardEvidence?.let { put(...) } ?: put("boardEvidence", JsonNull)`. In kotlinx.serialization, `JsonObjectBuilder.put` returns the **previous** value for that key, which is `null` for a new key, so the elvis branch always ran and replaced real board evidence with `null`. Because a frame-only update carries no transcript segments, the proxy correctly rejected the request with "At least one transcript segment or boardEvidence item is required" and the app fell back to unrefined local evidence. Board evidence had therefore never reached the note provider in any build. The existing suite only tested response parsing, which is exactly why the defect was invisible; there are now request-serialization tests for both the frame-only and audio-only directions.

## What happens now when the AI provider refuses the request?

The proxy classifies the failure instead of collapsing everything into one gateway error. It reads a bounded provider error body and the response headers, preserves an upstream `429`, and separates an exhausted account quota from a transient rate limit. Every error carries a `retryable` flag plus the upstream status, type, code, request id and retry-after seconds. It never logs or forwards request bodies, media or the key.

Android acts on that. A non-retryable failure such as an exhausted quota or a rejected credential stops the retry loop immediately rather than burning two more attempts and retaining more audio, the note warning records whether retries were attempted, and the live screen shows the specific remedy.

## If transcription fails, is the speech lost?

No. Each completed chunk is written to app-private recovery storage before processing, and the WAV is deleted only after both its transcript evidence and the note revision commit. If either fails the file stays, and a warning is written into the note.

Those files are now manageable from inside the app. The Live setup screen lists every retained WAV with its session offset, duration, size and the reason it was kept, and each one can be recovered or deleted. Recovery re-transcribes it, persists the diarized segments as evidence, and appends a labelled "Recovered audio" section to the original note through the same revision-guarded path. It deliberately does not re-run AI refinement: the note has moved on since the failure, so a delta could not be validated against it, and captured speech must not be quietly reinterpreted. Deleting is explicit and says the audio evidence is gone.

## Why does the interface look the way it does?

The earlier build encoded selection as a "✓" prefix inside a button label, which meant a screen reader announced the tick as part of the text, and it stacked full-width outlined buttons for every folder, note and syllabus, so the setup screen was several screens of near-identical controls.

The current build has a small design system: tone-mapped status pills, section cards with numbered headers, radio-style selectable rows, chips for folders and syllabi, stat tiles for the live counters, and banners that carry a problem and its remedy together. Selection state now lives in semantics rather than in the label text. Icons are a self-contained set drawn as vector path data, because Material 3 1.4 no longer ships an icon dependency and the extended artifact would add a large amount of unused drawable data to an already 61 MB debug APK.

One constraint is worth knowing: the device smoke test resolves the scrollable list with a matcher that requires exactly one scrollable node per screen, so chip groups wrap rather than scroll horizontally.

## Why is the project still feasible?

The implementation uses bounded, independently testable parts: 20-second audio chunks, one frame at a time, one revisioned Markdown block and a stateless proxy. Mock and local fallbacks allow plumbing tests without API cost, and two exact Android 16 paths now pass. The remaining work is broader failure/endurance coverage, real-provider validation, corpus evaluation and refinement rather than an unimplemented architecture.

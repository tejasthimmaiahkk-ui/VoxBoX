/** Technical chapters: architecture, algorithms, implementation, interface, backend, security. */

const CH_DESIGN = [
  { h1: "6. System Design and Architecture" },
  { h2: "6.1 Architectural Overview" },
  {
    p: "The system is a three-tier arrangement: an Android application that captures and stores, " +
      "an authenticated HTTPS proxy that holds the provider credential and enforces policy, and a " +
      "set of interchangeable AI models reached through that proxy. The division is not arbitrary. " +
      "Everything that must not leave the device stays in tier one; the one secret that must not " +
      "reach the device lives in tier two; tier three is replaceable.",
  },
  {
    code:
      "  CAPTURE            ON DEVICE                VIA THE PROXY           THE NOTE\n" +
      "                     (project code)           (model calls)           (project code)\n" +
      "\n" +
      "  Microphone   --->  Ordered audio queue --->  Transcription      --+\n" +
      "  16 kHz mono        unbounded, ordered        text + per-chunk     |\n" +
      "  20 s chunks        nothing dropped           dominant speaker     |\n" +
      "  2 s overlap                                                       +--> Note builder\n" +
      "                                                                    |    append-only delta\n" +
      "  Camera       --->  32x32 change filter --->  Board extraction  --+    SHA-256 binding\n" +
      "  every 2-30 s       ~450/hr -> ~40 sent       text, equations,          revision check\n" +
      "  adaptive           rest deleted on phone     concepts, crops                |\n" +
      "                                                                             v\n" +
      "                                                                    Live Markdown note\n" +
      "\n" +
      "  Everything captured is written to the local database as-is.\n" +
      "  No model output overwrites it. At session end a second model\n" +
      "  re-reads the note against the evidence and returns findings.",
    caption: "Figure 6.1 — End-to-end data flow",
  },
  { h2: "6.2 Two Independent Lanes" },
  {
    p: "Audio and vision are processed on separate queues with deliberately different policies, " +
      "because the consequences of dropping an item differ.",
  },
  {
    table: {
      head: ["Property", "Audio lane", "Vision lane"],
      widths: [2400, 3700, 3700],
      rows: [
        ["Queue depth", "Unbounded", "One slot"],
        ["Overflow policy", "Never drops", "Drop oldest"],
        ["Ordering", "Strict", "Latest wins"],
        ["Rationale", "Speech cannot be re-recorded. A dropped chunk is evidence destroyed.", "A superseded frame shows a board that has already been replaced. Keeping it wastes a call."],
        ["Failure handling", "WAV retained on disk for later recovery", "Frame deleted; the same change is detected again"],
      ],
    },
  },
  { h2: "6.3 Package Structure" },
  {
    table: {
      head: ["Package", "Responsibility"],
      widths: [2600, 7200],
      rows: [
        ["audio", "PCM capture, chunk accumulation with overlap, transcription client, speaker tracking, overlap de-duplication"],
        ["board", "Frame-change detection, diagram cropping, board extraction clients, speech-pace adaptation"],
        ["camera", "CameraX zoom and focus control"],
        ["session", "Capture orchestration, note refinement, verification, the session view model"],
        ["notes", "Room entities, DAOs, repositories, Markdown export"],
        ["reader", "Markdown parsing and rendering, review-flag parsing, diagram display"],
        ["ui", "Design system, navigation shell, shared components, motion"],
        ["home / library / settings / onboarding", "Screen implementations"],
        ["data", "Durable application settings on DataStore"],
        ["network", "Endpoint configuration, failure classification, health probing"],
        ["debug", "Temporary diagnostic log (scheduled for removal)"],
      ],
    },
  },
  { h2: "6.4 Data Model" },
  {
    table: {
      head: ["Entity", "Purpose", "Key relationships"],
      widths: [2600, 4000, 3200],
      rows: [
        ["NoteEntity", "A note document", "Parent of blocks and assets"],
        ["NoteBlockEntity", "One block of note content", "Belongs to a note; ordered by position"],
        ["FolderEntity", "A library folder", "Self-referencing via parentId for nesting"],
        ["NoteLocationEntity", "Placement of a note in a folder", "Links note to folder"],
        ["CaptureSessionEntity", "One recording session", "Belongs to a note"],
        ["TranscriptSegmentEntity", "One captured utterance", "Belongs to a session; ordered by position"],
        ["VisualEvidenceEntity", "One accepted camera frame", "Belongs to a session; carries fingerprint and score"],
        ["NoteAssetEntity", "A stored diagram crop", "Belongs to a note; references evidence"],
        ["BlockProvenanceEntity", "Which evidence produced a block", "Links block to source"],
        ["SyllabusEntity", "Imported syllabus text", "Optional context for refinement"],
      ],
    },
  },
  {
    p: "The schema is versioned and migrated rather than recreated, so a note captured under an " +
      "earlier build survives an application update. Migration from version 1 to version 2 is " +
      "implemented explicitly in NoteDatabaseMigrations.kt.",
  },
];

const CH_ALGORITHMS = [
  { h1: "7. Algorithms Implemented in this Project" },
  {
    p: "This chapter documents the logic that is the project's own work as distinct from calls to " +
      "a model. It is the answer to the question of whether the system is a wrapper. The AI " +
      "provider has already been migrated once during development, and none of the algorithms " +
      "below changed as a result.",
  },
  { h2: "7.1 On-Device Frame-Change Detection" },
  {
    p: "The cost of board capture is dominated by how many frames reach the vision model. At an " +
      "eight-second interval a one-hour lecture produces approximately 450 frames, while a " +
      "lecturer writes perhaps 30 to 50 distinct board states. The filter's job is to send only " +
      "the latter.",
  },
  { h3: "7.1.1 Procedure" },
  {
    n: [
      "Downsample the captured JPEG to a 32x32 luminance grid.",
      "Compute the mean luminance of both the candidate and the last committed board state.",
      "Subtract the global brightness shift from every pixel difference, so a lighting change or an auto-exposure adjustment does not read as new board content.",
      "Combine two signals: the mean centred pixel difference, and the fraction of pixels whose centred difference exceeds a threshold.",
      "Score the frame as 0.65 times the mean difference plus 0.35 times the changed fraction.",
      "If the score is below the sensitivity threshold, delete the frame; the board has not changed.",
      "If the score is above the threshold, hold the frame and require the next sample to confirm it (the settle test, 7.1.2).",
      "Only when a change has settled is the frame sent for extraction.",
    ],
  },
  { h3: "7.1.2 The settle test and why it is relative" },
  {
    p: "A change that appears and then disappears is a person walking past the board, not new " +
      "content. Requiring a change to persist across two consecutive samples removes that entire " +
      "class of false positive without any object detection. Writing stays put and a projector " +
      "slide stays put, so both settle within one interval; a person keeps moving and then leaves " +
      "the board as it was.",
  },
  {
    p: "The first implementation compared consecutive frames against an absolute threshold of " +
      "0.035. Instrumentation of a real lecture showed this was wrong. In the captured log a " +
      "genuinely static board scored between 0.09 and 0.17 against its own baseline, purely from " +
      "handheld shake and sensor noise — far above the settle threshold. The consequence was that " +
      "a diagram sat on screen for twenty seconds across nine consecutive rejected frames before " +
      "one was finally accepted by chance.",
  },
  {
    code:
      "05:29:22  score 0.3297  \"Board changed; waiting for the next frame to confirm\"\n" +
      "05:29:24  score 0.3272  \"Scene is still moving, so no board state is committed yet\"\n" +
      "05:29:26  score 0.3515  \"Scene is still moving, so no board state is committed yet\"\n" +
      "        ... six further frames, all rejected ...\n" +
      "05:29:42  score 0.3472  \"Settled board change detected\"",
    caption: "Figure 7.1 — Field log showing the twenty-second delay caused by an absolute settle threshold",
  },
  {
    p: "The corrected test is relative: a change is settled when the drift between two consecutive " +
      "samples is small compared with the change from the committed baseline. Formally, the frame " +
      "is accepted when compare(pending, current) is less than 0.45 times compare(baseline, " +
      "current). This compares like with like and is indifferent to how noisy the camera is.",
  },
  { h3: "7.1.3 A rejected refinement" },
  {
    p: "An attempt was made to add a cap so that a scene which never settles — a video playing, or " +
      "continuous writing — would be captured after a fixed number of unsettled frames. The " +
      "existing walk-past test failed immediately and correctly. A scene that never settles and a " +
      "person crossing the board are the same signal, and no threshold separates them. The cap was " +
      "removed and the case is covered instead by an explicit manual capture control, on the " +
      "principle that when the student can see something matters, their judgement outranks a cost " +
      "heuristic.",
  },
  { h3: "7.1.4 Two-phase baseline commit" },
  {
    p: "The comparison baseline advances only after the note update and the diagram crops have " +
      "been persisted. If extraction fails, the baseline stays where it was, so the same board " +
      "change is detected again on the next sample instead of being lost. A raw frame is deleted " +
      "only after its derived evidence has committed.",
  },
  { h2: "7.2 Adaptive Sampling from Speech Pace" },
  {
    p: "Sampling settings chosen before a lecture are wrong within minutes. A lecturer who speeds " +
      "up covers more ground per minute, so the board changes more often and each state is on " +
      "screen for less time. SpeechPaceTracker measures words per minute from the transcript the " +
      "system already produces — no extra model call, no extra permission — and scales both knobs " +
      "together.",
  },
  {
    b: [
      "Interval shortens as pace rises, so a fast speaker is sampled more often.",
      "Threshold falls as pace rises, so smaller board changes still register, because a speaker in a hurry adds a line at a time rather than rewriting the board.",
      "The pace is an exponential moving average, so a single fast sentence cannot swing the settings.",
      "Both values are clamped to a usable range so an outlier cannot drive the interval to zero.",
      "No suggestion is made until at least two chunks have been observed.",
    ],
  },
  {
    p: "A device trial exposed a defect in the first version. Each suggestion divided the current " +
      "setting, so adjustments compounded chunk after chunk: the interval walked to its ceiling " +
      "while the threshold walked to its floor, and the two knobs ended up describing opposite " +
      "paces — an interval of thirteen seconds saying \"slow\" beside a threshold of thirteen per " +
      "cent saying \"fast\". Both are now scaled from the settings the session started with, so a " +
      "given pace always maps to the same values and a speaker who slows down gets the original " +
      "settings back.",
  },
  { h2: "7.3 Per-Chunk Dominant Speaker Selection" },
  {
    p: "Within each twenty-second chunk the system computes voiced duration per returned speaker " +
      "label and selects a dominant speaker only where that label holds at least 58 per cent of " +
      "the voiced time with a margin of at least 15 percentage points over the runner-up. " +
      "Otherwise it reports AMBIGUOUS rather than guessing.",
  },
  {
    note: "This is speaker segmentation inferred per request, not acoustic diarization with voice " +
      "embeddings. Label \"A\" in one chunk is not guaranteed to be the same person as label \"A\" " +
      "in the next. The system therefore makes no identity claim, and stores no voiceprint.",
  },
  { h2: "7.4 Append-Only Delta Protocol with Content Binding" },
  {
    p: "A naive implementation sends the whole note with every update so the model can rewrite it. " +
      "Cost then grows with the square of lecture length, and every update is an opportunity for " +
      "the model to silently alter earlier content. The protocol used here does neither.",
  },
  {
    n: [
      "The client sends a bounded context: the note's heading outline, a short tail of recent text, and a SHA-256 hash of the note as it currently stands.",
      "The model returns only new Markdown to append, never a rewrite.",
      "The server binds the response to the hash it was generated against.",
      "The client commits the append only if the note's revision still matches the one the request was based on.",
      "A blank erasure is rejected outright; an identical replay is treated as idempotent.",
    ],
  },
  { h2: "7.5 Transcript Overlap and De-duplication" },
  {
    p: "Audio capture is a single continuous read loop, so the PCM stream has no gap. Nevertheless " +
      "speech was being lost. Each twenty-second chunk was transcribed independently, and a " +
      "sentence spanning a boundary reached the model as a fragment at the edge of a clip, which " +
      "it discarded. Comparison against a known source showed whole clauses missing.",
  },
  {
    p: "Chunks now repeat the last two seconds of the previous chunk, so no word is only ever seen " +
      "as a fragment. Offsets remain true to the recording clock: a chunk's offset is where its " +
      "new audio begins, not where its overlap does. The seam consequently arrives transcribed " +
      "twice, and the repeat is removed before it reaches the note.",
  },
  {
    b: [
      "Matching is on words rather than characters, because two transcriptions of the same audio rarely agree on punctuation and almost never split segments identically.",
      "At least three consecutive words must match before anything is trimmed. Short coincidental matches such as \"and so\" occur constantly, and a duplicated clause is a blemish while a deleted one is lost evidence.",
      "Trimming stops after the seam, so a lecturer genuinely repeating themselves later in the chunk is preserved.",
    ],
  },
  { h2: "7.6 Grounding Guard against Fabricated Content" },
  {
    p: "The note prompt forbids inventing content, but a prompt instruction is a request rather " +
      "than a guarantee. A live session whose only speech was \"Is this working?\" returned a " +
      "confident section headed \"Chemical Reactions\". The server therefore scores every " +
      "substantial delta against the evidence it claims to summarise.",
  },
  {
    n: [
      "Extract content words from the supplied transcript and board evidence, discarding stop words and Markdown furniture.",
      "Extract content words from the returned delta.",
      "Compute the fraction of the delta's vocabulary that appears in the evidence.",
      "If the delta exceeds 120 characters and its grounding ratio falls below 0.25, discard it and report the discard as a visible warning.",
    ],
  },
  {
    p: "The threshold is a floor rather than a plagiarism test: a genuine summary rephrases " +
      "heavily but still shares most of its vocabulary with the source, whereas text about a " +
      "subject nobody mentioned shares almost none. Short deltas are exempt because they cannot be " +
      "scored fairly. Discarding costs one chunk of notes; keeping would put something in a study " +
      "note that nobody said.",
  },
  { h2: "7.7 Syllabus Relevance Ranking" },
  {
    p: "Where a syllabus has been supplied, forwarding all of it with every request would be " +
      "expensive and would encourage the model to write about topics that were not taught. " +
      "Instead the syllabus is split into chunks, each chunk is scored by overlap with the current " +
      "evidence, and only the highest-scoring excerpts are forwarded, in their original order and " +
      "within a strict character budget. Syllabus text is marked as context and never as proof.",
  },
  { h2: "7.8 Dual-Queue Scheduling Policy" },
  {
    p: "Described in 6.2. The policy is itself an algorithm: it encodes the judgement that " +
      "irreplaceable data must never be dropped under load while superseded data should be, and it " +
      "is what allows the system to survive a slow network without either stalling capture or " +
      "losing speech.",
  },
];

const CH_IMPLEMENTATION = [
  { h1: "8. Implementation" },
  { h2: "8.1 Capture Pipeline" },
  {
    p: "PcmAudioChunkRecorder opens a single AudioRecord at 16 kHz mono PCM16 with hardware noise " +
      "suppression and automatic gain control where the device provides them. A read loop " +
      "accumulates bytes into a chunk accumulator, which emits a WAV every twenty seconds and " +
      "retains the trailing two seconds as the head of the next chunk. On stop, the recorder " +
      "drains: the final partial chunk is emitted before the coroutine completes, and a chunk " +
      "consisting only of carried overlap is suppressed because that audio has already been " +
      "transcribed once.",
  },
  { h2: "8.2 Note Construction" },
  {
    p: "Each transcribed chunk, and each accepted board frame, triggers a note update. The update " +
      "is guarded by a mutex so that concurrent audio and vision events cannot interleave a " +
      "revision. The refinement request carries the bounded note context described in 7.4, the " +
      "new evidence, the selected detail level and any custom instruction. On success the delta is " +
      "appended and the revision advances. On failure the captured evidence is appended verbatim " +
      "under a clearly labelled heading and a warning is recorded — the session continues.",
  },
  { h2: "8.3 Persistence and Recovery" },
  {
    p: "Transcript segments and visual evidence are written before refinement is attempted, so a " +
      "provider failure cannot destroy what was captured. Where a chunk's note revision fails to " +
      "commit, its WAV is retained in private storage and surfaced in Settings with the reason, " +
      "the duration and the size, offering recovery or deletion. Recovery appends a labelled " +
      "section and deliberately does not re-run refinement, so nothing already written is rewritten.",
  },
  { h2: "8.4 Markdown Rendering" },
  {
    p: "The reader originally displayed block content as plain text, so notes arrived full of " +
      "literal hash and asterisk characters and were harder to read than the transcript they were " +
      "made from. A parser and renderer were written for the subset the note pipeline emits: " +
      "headings, nested bullets, numbered lists, emphasis, inline code, block quotes with callout " +
      "kinds, tables, horizontal rules and mathematics.",
  },
  {
    b: [
      "Fenced code and display equations are consumed whole and never wrapped, because a wrapped equation is a wrong equation.",
      "Inline parsing tests two-character markers before one-character markers, or bold is misread as two empty italics.",
      "Content inside inline code and mathematics is taken verbatim, so an asterisk within an equation is not emphasis.",
      "Callout quotes retain their kind, because an accepted AI suggestion is recorded as a note callout and must not look like the lecturer's own words.",
    ],
  },
  { h2: "8.5 Export" },
  {
    p: "Export produces two documents in one archive. The refined note is what the model wrote. " +
      "The captured-evidence note is the verbatim transcript with timestamps and chunk-local " +
      "speaker labels, plus the diagram crops. Keeping them apart is what allows one to be checked " +
      "against the other. Diagram links are rewritten to relative paths inside the archive so the " +
      "note opens correctly in an external Markdown reader.",
  },
];

const CH_UI = [
  { h1: "9. User Interface Design" },
  { h2: "9.1 Design Principles" },
  {
    b: [
      "Four destinations only: Home, Capture, Library, Settings. Everything durable lives in Settings so the capture path stays short.",
      "One primary action per screen.",
      "Minimum 44 dp touch targets throughout; icon buttons carry a 48 dp hit box regardless of glyph size.",
      "The live capture surface is always dark in both themes, because a white page behind a photograph of a lecture hall is unreadable.",
      "Status colour meanings are fixed and never follow the user's accent choice.",
    ],
  },
  { h2: "9.2 Fixed Status Semantics" },
  {
    table: {
      head: ["Colour", "Meaning", "Never used for"],
      widths: [2000, 4000, 3800],
      rows: [
        ["Green", "Saved, verified, permission granted", "Anything not yet written to disk"],
        ["Orange", "Needs the student's review", "Anything the application changed by itself"],
        ["Red", "Recording, destructive, or failed", "Ordinary primary actions"],
        ["Accent", "Primary action and selection; user-chosen from four hues", "Any status meaning"],
      ],
    },
  },
  {
    p: "Dynamic colour is deliberately not supported. If green followed the accent, a user on the " +
      "green accent could not distinguish \"saved\" from \"primary action\", and a user on the red " +
      "accent would see every button look like a warning.",
  },
  { h2: "9.3 Screen Inventory" },
  {
    table: {
      head: ["Screen", "Purpose"],
      widths: [3000, 6800],
      rows: [
        ["Onboarding", "Three panes: what the app does, permissions with reasons stated before the system dialog, and the consent acknowledgement"],
        ["Home", "Mode switch, record button, review-flag banner, recent notes"],
        ["Capture setup", "The four decisions: mode, destination, note style, and board sampling behind a disclosure"],
        ["Live session", "Always dark. Preview with zoom, live controls, living note, evidence transcript, stop"],
        ["Library", "Search, folder breadcrumb, subfolders, notes grouped by recency, long-press actions"],
        ["Note reader", "Rendered Markdown, outline, reading controls, find, evidence expansion, review flags, diagram lightbox, share"],
        ["Settings", "Three groups leading to six sub-pages"],
      ],
    },
  },
  { h2: "9.4 Motion" },
  {
    p: "A single motion vocabulary is defined centrally so the whole application decelerates the " +
      "same way. Every entry animation and every looping animation consults the system animator " +
      "scale: when the user has turned animations off, entries collapse to a short fade and loops " +
      "hold still. A blinking indicator that cannot be switched off is a problem for people with " +
      "vestibular or attention conditions, and a lecture application is used for an hour at a time.",
  },
];

const CH_BACKEND = [
  { h1: "10. Backend Service and Deployment" },
  { h2: "10.1 Purpose" },
  {
    p: "The proxy exists for one reason: an APK is a distributable archive, so any credential " +
      "compiled into it can be recovered. The provider key therefore lives in the server's secret " +
      "store and never reaches the device. A secondary benefit is that all prompt and schema logic " +
      "lives in one place and can be changed without rebuilding and redistributing the application.",
  },
  { h2: "10.2 Endpoints" },
  {
    table: {
      head: ["Endpoint", "Function"],
      widths: [3000, 6800],
      rows: [
        ["GET /health", "Liveness, active mode, the model identifiers actually in use, and the day's budget consumption"],
        ["POST /v1/audio/transcribe", "Diarized transcription of one audio chunk with offset applied"],
        ["POST /v1/board/extract", "Board text, equations, concepts and validated diagram regions"],
        ["POST /v1/notes/refine", "One note update, full or delta, with corrections and warnings"],
        ["POST /v1/notes/verify", "End-of-session review of the finished note"],
      ],
    },
  },
  { h2: "10.3 Controls" },
  {
    b: [
      "Bearer token authentication with a constant-time comparison.",
      "Per-caller rate limiting, in memory and per instance.",
      "A hard daily request budget that mock calls never consume.",
      "Strict JSON schema enforcement on every model response, with provider parameter requirements asserted upstream.",
      "One retry for transient generation failures, and no retry for permanent ones.",
      "Typed failure classification returned to the client so the application can decide whether retrying is sensible.",
      "In-memory forwarding only: no request body is written to disk.",
    ],
  },
  { h2: "10.4 The /health Endpoint as a Deployment Check" },
  {
    p: "The health response echoes the model identifiers the server is actually running. This is " +
      "not decoration. During development three separately reported defects turned out to have a " +
      "single cause: the corrective commit had never been pushed, so the deployed service was " +
      "running older code while the repository looked correct. The application's Connection screen " +
      "now reads this endpoint and displays the live model routing, which makes that class of " +
      "failure visible from the device.",
  },
];

const CH_SECURITY = [
  { h1: "11. Security, Privacy and Consent" },
  { h2: "11.1 Threat Model" },
  {
    table: {
      head: ["Concern", "Mitigation"],
      widths: [3400, 6400],
      rows: [
        ["Provider key extracted from the APK", "The key is never in the APK. The device holds only a client token, which is rotatable independently and bounded by rate limits and a daily budget."],
        ["Client token extracted from the APK", "Accepted as unavoidable and bounded rather than denied. The token authorises only this proxy, is rate limited per caller, and is capped by a daily request budget."],
        ["Note content leaking off the device", "Local-first storage, system backup disabled, export user-initiated only."],
        ["Prompt injection from captured content", "Transcript, board text, existing note and syllabus are all declared to the model as untrusted evidence, never as instructions."],
        ["Path traversal via model-supplied asset paths", "Asset paths are resolved against private storage with absolute paths, drive prefixes and parent segments rejected."],
        ["Over-broad file sharing", "The FileProvider grants access per directory, so the diagnostic log lives in its own subdirectory and cannot expose note assets."],
        ["Covert recording", "Capture is foreground-only and visible; the application cannot record with the screen off."],
      ],
    },
  },
  { h2: "11.2 Consent" },
  {
    p: "Recording a lecture may require permission from the institution or the speaker. The " +
      "onboarding flow states this before the student begins and requires acknowledgement, and the " +
      "About screen repeats it. The foreground-only constraint exists so that the fact of " +
      "recording is observable by people in the room.",
  },
  { h2: "11.3 Claims Discipline" },
  {
    p: "The About screen separates three levels of confidence — device-verified, live-contract " +
      "verified, and not yet evaluated — rather than presenting a single indicator. Accuracy is " +
      "listed under \"not yet evaluated\" because no labelled corpus and scorer exist yet. " +
      "Publishing a figure derived from a handful of runs would be misleading, and the project " +
      "declines to do so.",
  },
];

module.exports = { CH_DESIGN, CH_ALGORITHMS, CH_IMPLEMENTATION, CH_UI, CH_BACKEND, CH_SECURITY };

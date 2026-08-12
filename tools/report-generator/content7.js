/**
 * Additional sections appended to existing chapters. Each export continues the chapter named
 * in its identifier; build.js concatenates them onto the corresponding array.
 */

// ---------------------------------------------------------------- chapter 2

const PROBLEM_EXTRA = [
  { h2: "2.5 The problem as it is actually experienced" },
  {
    p: "The failures in section 2.2 are stated abstractly. It is worth walking through them once " +
      "concretely, because the ordering in which they arrive is part of the problem and is not " +
      "visible from a list.",
  },
  {
    p: "A student sits down for a fifty-minute lecture on integration by parts. The lecturer begins " +
      "with a motivation — why the technique exists, what it does to a product of functions — and " +
      "the student listens, because listening is what the first two minutes are for. Nothing is " +
      "written down. That decision is correct and it is also the first loss: the motivation is the " +
      "part that will be hardest to reconstruct in three months, and it was never notated because " +
      "notating it would have meant missing the rest of it.",
  },
  {
    p: "At minute six the derivation starts. The lecturer writes, speaks, points at what they have " +
      "written, and says \"this term here cancels\". The student photographs the board, which " +
      "captures \"this term\" and discards \"cancels because the boundary condition is zero\". At " +
      "minute eleven the board is erased. The photograph is now the only record of a state that no " +
      "longer exists anywhere, and it has no caption, no timestamp the student will remember, and " +
      "no connection to the eight sentences that made it meaningful.",
  },
  {
    p: "By minute thirty the student has four photographs and a page of handwriting that stops " +
      "mid-sentence in the second topic, because at minute nineteen something was confusing and " +
      "the choice was between writing and understanding. At minute fifty they leave with an " +
      "artefact that will take longer to decode than the lecture took to attend, and they will " +
      "know that, and they will therefore not decode it.",
  },
  {
    p: "Three months later, revising, they open the camera roll. Four photographs of a whiteboard " +
      "among two hundred photographs of everything else. No indication of which lecture they belong " +
      "to. The derivation is legible and the reason for it is gone.",
  },
  {
    note: "Every mechanism in this system is aimed at one point in that account: the moment where " +
      "capturing and understanding compete. The system's job is to make that competition " +
      "unnecessary, not to produce a better artefact from a student who is already losing.",
  },

  { h2: "2.6 Why the obvious solutions do not close it" },
  {
    table: {
      head: ["Obvious solution", "What it fixes", "What it leaves"],
      widths: [2600, 3400, 3406],
      rows: [
        ["Record the audio", "Nothing is lost", "Fifty minutes of unsearchable audio, and no board"],
        ["Photograph the board more", "More board states preserved", "More unstructured photographs, and the attention cost rises rather than falls"],
        ["Transcribe the recording afterwards", "The speech becomes searchable", "A transcript is not a note; still no board; and the work happens after the student has stopped caring"],
        ["Ask a general AI assistant to summarise it", "A structured artefact appears", "No board; produced after the fact; and no way to tell which sentences are supported"],
        ["Write faster", "More is captured", "Less is understood, which is the trade the problem is about"],
      ],
    },
    tnum: "2.2",
    caption: "Why each obvious solution leaves the problem open",
  },
  {
    p: "The last row is the important one, and it is why this is an engineering problem rather than " +
      "a discipline problem. A student who takes better notes by working harder during the lecture " +
      "has moved the cost, not removed it. The only solutions that help are the ones that reduce " +
      "what the student has to do while the lecture is happening.",
  },
];

// ---------------------------------------------------------------- chapter 4

function uc(id, name, rows) {
  return [
    { h3: id + "  " + name },
    { table: { head: ["", ""], widths: [1900, 7506], rows } },
  ];
}

const REQ_EXTRA = [
  { h2: "4.6 Use-case model" },
  {
    p: "Nineteen use cases in four packages. The diagram gives the structure; the specifications " +
      "that follow give the eight that carry the system's function, in the standard template — " +
      "actor, precondition, main flow, alternatives, postcondition.",
  },
  { fig: "docs/diagrams/fig-usecase.png", num: "4.1", caption: "UML use-case diagram", opts: { maxH: 700 } },

  ...uc("UC-01", "Run a live board session", [
    ["Primary actor", "Student"],
    ["Supporting actors", "AI proxy; Android OS (permissions, camera, microphone)"],
    ["Precondition", "Onboarding complete; RECORD_AUDIO and CAMERA granted; a note policy and mode chosen"],
    ["Trigger", "The student presses Start on the capture setup screen"],
    ["Main flow",
      "1. The system creates a note, a living block and a capture_sessions row at PREPARING.  " +
      "2. The system acquires the microphone and camera and moves to RUNNING.  " +
      "3. Every 20 seconds the audio pipeline emits a chunk carrying a 2-second overlap.  " +
      "4. At the configured interval the camera pipeline samples a frame and applies the change filter.  " +
      "5. Accepted frames are held, settle-tested, fingerprinted and uploaded for extraction.  " +
      "6. New evidence produces a bounded composition request; an accepted delta is appended and the revision is incremented.  " +
      "7. The screen shows elapsed time, revision, and frames kept and skipped."],
    ["Alternative: network failure",
      "At any step, a transient failure is retried twice with backoff. A permanent failure moves the session to DEGRADED: raw audio is retained to local storage, the student is warned, and capture continues. The lecture is never lost; only the AI work is."],
    ["Alternative: revision moved",
      "If another append landed while a delta was being composed, the revision check fails, the delta is discarded, and the request is rebuilt against the new state."],
    ["Alternative: back gesture",
      "The camera preview collapses. The session does not end."],
    ["Postcondition", "A note exists containing structured Markdown, board crops and any review flags; transcript_segments and visual_evidence hold the evidence exactly as captured"],
    ["Requirements", "FR-01 to FR-12, FR-15 to FR-26"],
  ]),

  ...uc("UC-02", "Capture a frame now", [
    ["Primary actor", "Student"],
    ["Precondition", "A board-mode session is RUNNING"],
    ["Trigger", "The student presses Capture now"],
    ["Main flow",
      "1. The system sets a force flag on the frame detector.  " +
      "2. The next sampled frame bypasses the change threshold and the settle test.  " +
      "3. The frame is fingerprinted and, if not a duplicate, uploaded for extraction.  " +
      "4. The kept counter increments."],
    ["Alternative: duplicate", "If the fingerprint matches a frame already extracted, the frame is dropped and the student is told so rather than silently ignored"],
    ["Postcondition", "A visual_evidence row exists for the forced frame"],
    ["Design note", "This use case exists because the automatic filter is right about most frames and wrong about some, and the student can see which is which. It replaced a rejected 'capture anyway after N frames' rule that would have photographed a person crossing the board."],
    ["Requirements", "FR-09"],
  ]),

  ...uc("UC-03", "Tune sampling while running", [
    ["Primary actor", "Student"],
    ["Precondition", "A session is RUNNING"],
    ["Trigger", "The student opens the tuning panel"],
    ["Main flow",
      "1. The panel shows the current interval, the current threshold and whether adaptive sampling is on.  " +
      "2. The student moves a slider.  " +
      "3. The new value is written to capture_sessions immediately and applied to the running pipeline.  " +
      "4. The adaptive baseline is reset to the new value, so the loop anchors to the student's choice."],
    ["Alternative: adaptive off", "Turning Auto off stops the pace tracker from issuing suggestions; the manual values stand until changed"],
    ["Postcondition", "frameIntervalMs and changeThreshold are persisted and in force"],
    ["Design note", "Before this existed, a badly chosen sensitivity was locked in for fifty minutes"],
    ["Requirements", "FR-10, FR-11"],
  ]),

  ...uc("UC-04", "Append a note delta", [
    ["Primary actor", "System (no human actor)"],
    ["Supporting actor", "AI proxy"],
    ["Precondition", "New evidence exists that has not been composed; a session is RUNNING"],
    ["Main flow",
      "1. Read the current revision and compute the SHA-256 of the living block.  " +
      "2. Build a bounded request: syllabus (4k), outline (4k), recent Markdown (6k), new evidence, hash, revision.  " +
      "3. Call the proxy. The response is schema-validated or the call is a 502.  " +
      "4. The proxy scores the delta against the evidence and rejects it below a ratio of 0.25.  " +
      "5. Re-read the revision. If unchanged, append the delta and increment the revision in one transaction."],
    ["Alternative: empty delta", "A chunk that adds nothing produces an empty delta. This is accepted, not retried, and the revision does not move."],
    ["Alternative: below the grounding floor", "The delta is discarded; the evidence is kept; the next chunk retries"],
    ["Alternative: revision moved", "The delta is discarded and the request rebuilt against the new state"],
    ["Postcondition", "Either the living block has grown and the revision has increased, or nothing has changed and the evidence is intact"],
    ["Requirements", "FR-16 to FR-24"],
  ]),

  ...uc("UC-05", "Verify the note against its evidence", [
    ["Primary actor", "System, triggered by the student pressing Stop"],
    ["Precondition", "A session is moving from RUNNING to FINALISING"],
    ["Main flow",
      "1. The audio queue is flushed; no chunk is abandoned.  " +
      "2. The finished note and the evidence that produced it are sent to the verification route.  " +
      "3. Findings are returned as claim, evidence, offset and severity.  " +
      "4. Each finding becomes a review flag attached to the block it concerns.  " +
      "5. The session moves to COMPLETE and the reader opens."],
    ["Alternative: verification unavailable", "The session moves to COMPLETE without findings. The note is still complete; only the check did not run."],
    ["Postcondition", "The note is readable; block_provenance.reviewState is FLAGGED for any block with a finding"],
    ["Constraint", "The verification route has no field in its response schema through which a rewritten note could be returned. It reports; it cannot edit."],
    ["Requirements", "FR-25, FR-26, FR-27"],
  ]),

  ...uc("UC-06", "Resolve a review flag", [
    ["Primary actor", "Student"],
    ["Precondition", "A note contains at least one review flag"],
    ["Trigger", "The student opens a note and reaches a flag card"],
    ["Main flow",
      "1. The card shows the note's reading and the evidence's reading side by side, with the timestamp the evidence sits at.  " +
      "2. The student chooses Accept, which records the decision, or Annotate, which adds a student-owned block beside it.  " +
      "3. block_provenance.reviewState moves to ACCEPTED or ANNOTATED and the card collapses."],
    ["Explicitly absent", "There is no 'fix it for me' action. The system has no way to know which reading is correct, and offering to choose would be the most damaging thing it could do."],
    ["Postcondition", "The evidence is unchanged. The student's decision is recorded."],
    ["Requirements", "FR-26, FR-27"],
  ]),

  ...uc("UC-07", "Organise notes into folders", [
    ["Primary actor", "Student"],
    ["Precondition", "At least one note exists"],
    ["Main flow",
      "1. The student creates a folder, optionally inside another.  " +
      "2. The student moves a note into it.  " +
      "3. The library shows a breadcrumb and the folder's contents."],
    ["Alternative: duplicate name", "The database rejects a sibling folder with the same name through a unique index on (parentId, name), and the dialog reports it. The application does not check first, because a check followed by an insert is a race."],
    ["Alternative: delete a folder", "The folder and its subtree are removed. The notes inside are not deleted; they become unfiled."],
    ["Postcondition", "note_locations reflects the filing"],
    ["Requirements", "FR-36 to FR-39"],
  ]),

  ...uc("UC-08", "Export and share a note", [
    ["Primary actor", "Student"],
    ["Supporting actor", "Android OS share sheet"],
    ["Precondition", "A note exists"],
    ["Main flow",
      "1. The student presses share.  " +
      "2. The system writes the note as Markdown with every asset it references, using relative paths that resolve after extraction.  " +
      "3. The archive is handed to the system share sheet through a FileProvider."],
    ["Alternative: no assets", "A note with no board crops exports as a single Markdown file"],
    ["Postcondition", "An archive exists in a location another application can read"],
    ["Design note", "The first field trial reported that export 'did nothing'. The file was being written to application-private storage, where no other application can read it. FileProvider is the whole of the fix."],
    ["Requirements", "FR-40, FR-41, FR-42"],
  ]),

  { h2: "4.7 Use-case to requirement coverage" },
  {
    table: {
      head: ["Use case", "Requirements exercised", "Verified by"],
      widths: [3000, 3200, 3206],
      rows: [
        ["UC-01 Run a live board session", "FR-01 to FR-12, FR-15 to FR-26", "Field trials 1-4; ~90 unit tests"],
        ["UC-02 Capture a frame now", "FR-09", "Device test; field trial 4"],
        ["UC-03 Tune while running", "FR-10, FR-11", "Device test; SpeechPaceTrackerTest"],
        ["UC-04 Append a note delta", "FR-16 to FR-24", "GeneratedMarkdownRevisionTest; backend tests"],
        ["UC-05 Verify against evidence", "FR-25 to FR-27", "NoteVerificationTest; field log"],
        ["UC-06 Resolve a review flag", "FR-26, FR-27", "ReviewFlagParserTest; unproven in the field"],
        ["UC-07 Organise into folders", "FR-36 to FR-39", "Device test; schema constraints"],
        ["UC-08 Export and share", "FR-40 to FR-42", "MarkdownExporterTest; device test"],
      ],
    },
    tnum: "4.7",
    caption: "Use-case coverage",
  },
];

// ---------------------------------------------------------------- chapter 6

const DESIGN_EXTRA = [
  { h2: "6.10 Class model of the domain layer" },
  {
    p: "Only the classes that carry the project's own logic are shown. Compose screens and " +
      "Room-generated data-access code are omitted, because neither contains a decision.",
  },
  { fig: "docs/diagrams/fig-class.png", num: "6.10", caption: "UML class diagram of the domain layer", opts: { maxH: 780 } },
  {
    p: "The composition relationships in Figure 6.10 all point outward from " +
      "CaptureSessionController, which is the only class in the system that owns a running " +
      "session. Everything it composes is a pure function over plain data — FrameChangeDetector " +
      "takes two luminance arrays, SpeechPaceTracker takes text and a duration, TranscriptOverlap " +
      "takes two strings. None of them knows what an Android is, which is why 134 unit tests can " +
      "cover them without an emulator.",
  },
  {
    p: "AiClient is an interface rather than a class, and that is the seam along which the AI " +
      "provider was migrated mid-development. The migration replaced its implementation and " +
      "touched nothing beneath it, which is the practical evidence for the claim in Chapter 7 that " +
      "the algorithms are the project's own work rather than a wrapper.",
  },

  { h2: "6.11 Design patterns, and where each earns its place" },
  {
    table: {
      head: ["Pattern", "Where", "What it buys"],
      widths: [2200, 3200, 4006],
      rows: [
        ["Repository", "NoteRepository over Room DAOs", "The transactional append lives in one place, so the revision-and-hash check cannot be forgotten by a caller."],
        ["Strategy", "AiClient interface", "The provider was replaced during development without touching the domain layer."],
        ["Observer", "Kotlin Flow from the session controller to the UI", "The live screen re-renders from state changes rather than being pushed to, which is what makes a screen that changes several times a minute tractable."],
        ["Producer-consumer", "The audio chunk queue", "Transcription can be slower than capture without dropping a chunk."],
        ["State machine", "capture_sessions.status", "Six states, every transition persisted before it is displayed, so a killed process does not lose the session."],
        ["Value object", "PaceSuggestion, Decision, LumaComparison", "Algorithms return data rather than mutate state, which is what makes them testable as pure functions."],
        ["Optimistic locking", "Revision plus content hash on every append", "Two pipelines append to one note without a lock and without losing a write."],
      ],
    },
    tnum: "6.3",
    caption: "Patterns used, and the specific problem each solves here",
  },
  {
    p: "No pattern in that table was chosen for its own sake. Each is named because it is the " +
      "conventional name for a structure the problem forced, and naming it makes the code easier " +
      "to read for someone who has met the structure elsewhere.",
  },

  { h2: "6.12 Error-handling design" },
  {
    p: "The system's error policy follows from one rule: a lecture is delivered once, so nothing " +
      "may stop capture. Every failure path degrades rather than halts.",
  },
  {
    table: {
      head: ["Failure", "Response", "What is preserved"],
      widths: [2600, 3400, 3406],
      rows: [
        ["Transcription call fails transiently", "Retried twice with backoff; the chunk stays queued", "The audio, in the queue"],
        ["Transcription fails permanently", "Session moves to DEGRADED; raw audio retained to disk", "The lecture, as a WAV the student can recover"],
        ["Extraction call fails", "visual_evidence marked FAILED with a reason; the frame's crop is kept", "The board image"],
        ["Composition returns an invalid shape", "502 from the proxy; retried twice", "All evidence; the note simply does not grow this cycle"],
        ["Composition fails the grounding check", "Delta discarded; not retried", "All evidence; a fabrication does not reach the note"],
        ["Daily budget exhausted", "Session moves to DEGRADED; not retried", "The lecture; the AI work stops"],
        ["Revision moved", "Delta discarded; request rebuilt", "Both appends; neither is lost"],
        ["Storage exhausted", "Session moves to ABANDONED; evidence retained", "Everything already written"],
        ["Process killed", "Session remains RUNNING in the database and is offered for resumption from Home", "Everything already written"],
      ],
    },
    tnum: "6.4",
    caption: "Failure modes and responses",
  },
  {
    p: "The last row is the reason every state transition is persisted before it is displayed. A " +
      "phone running a camera, a microphone and a network client for fifty minutes is a phone the " +
      "operating system may decide to reclaim, and a session that exists only in memory is a " +
      "session that a low-memory kill silently deletes.",
  },
];

// ---------------------------------------------------------------- chapter 7

const ALGO_EXTRA = [
  { h2: "7.8 Complexity and resource analysis" },
  {
    p: "None of the six algorithms is asymptotically interesting; all of them are bounded by " +
      "design, and the point of this section is that the bounds are deliberate rather than " +
      "incidental.",
  },
  {
    table: {
      head: ["Algorithm", "Time", "Space", "Frequency", "Bound comes from"],
      widths: [2400, 1500, 1500, 1800, 2206],
      rows: [
        ["1. Frame change", "O(n), n = 1024", "O(n)", "Every 2 to 30 s", "Fixed grid size, chosen not derived"],
        ["2. Adaptive sampling", "O(w), w = 5", "O(w)", "Every 20 s", "Fixed rolling window"],
        ["3. Overlap repair", "O(k^2), k <= 40 words", "O(k)", "Every 20 s", "Lookback capped at 40 words"],
        ["4. Delta assembly", "O(c), c <= 14000 chars", "O(c)", "Every 20 s", "Explicit context caps"],
        ["5. Dominant speaker", "O(s), s = segments", "O(1)", "Every 20 s", "One chunk's segments"],
        ["6. Grounding guard", "O(d + e) set ops", "O(d + e)", "Per composition", "Bounded request size"],
      ],
    },
    tnum: "7.5",
    caption: "Complexity, and the source of each bound",
  },
  {
    p: "Algorithm 3 is quadratic in the lookback window, which would matter if the window could " +
      "grow. It cannot: the comparison looks back at most forty words, because an overlap longer " +
      "than two seconds of speech is not an overlap but a repetition, and treating it as one would " +
      "delete content. The cap is a correctness constraint that happens to also bound the cost.",
  },
  {
    p: "Algorithm 4 is the one whose bound was added rather than assumed. Without the context " +
      "caps its input grows linearly with lecture length and its cost therefore grows " +
      "quadratically over a session, which is exactly the behaviour measured before the caps " +
      "existed and described in Chapter 13.9.",
  },
  {
    table: {
      head: ["Resource", "Per lecture-hour", "Note"],
      widths: [3000, 2400, 4006],
      rows: [
        ["Frame analyses on device", "~450", "1,024 integer comparisons each; negligible"],
        ["Full-resolution frames held at once", "1 to 2", "The held frame and the incoming one; deleted after extraction"],
        ["Audio buffered at once", "22 s", "One chunk plus the overlap carry"],
        ["Database writes", "~350", "Evidence rows plus appends"],
        ["Network calls", "~190", "150 transcription, ~40 extraction, plus one verification"],
        ["Peak local storage", "~40 MB", "Board crops before extraction clears them"],
      ],
    },
    tnum: "7.6",
    caption: "Resource use over one lecture-hour",
  },
  {
    note: "Battery and thermal behaviour across a full fifty-minute board session have not been " +
      "measured. It is listed in Chapter 16 as planned work, and no claim about them is made here.",
  },
];

// ---------------------------------------------------------------- chapter 9

const UI_EXTRA = [
  { h2: "9.8 Visual design system" },
  {
    p: "One theme layer, four accent choices, and a small set of primitives that every screen is " +
      "built from. The system is deliberately small: sixteen destinations built from seven " +
      "primitives is easier to keep coherent than sixteen destinations each styled on its own.",
  },
  {
    table: {
      head: ["Primitive", "Used for", "Rule it enforces"],
      widths: [2200, 3400, 3806],
      rows: [
        ["VbScaffold", "Every destination", "Consistent top bar, back behaviour and content insets"],
        ["VbCard", "Grouped content", "One corner radius and one elevation across the application"],
        ["VbSectionHeader", "Section labels", "One type scale for headings inside a screen"],
        ["VbSlider", "Interval, threshold, zoom", "Value, unit and thumb position are always rendered from the same state"],
        ["VbConfirmDialog", "Delete, discard", "A destructive action always states what will be lost"],
        ["VbEmptyState", "Empty library, empty note", "An empty screen explains itself instead of being blank"],
        ["VbStatusPill", "LIVE, DEGRADED, QUEUED", "Session state is shown in one place, in one form"],
      ],
    },
    tnum: "9.1",
    caption: "UI primitives",
  },
  {
    p: "VbSlider exists because of a specific defect. The zoom slider was reported as not working " +
      "while pinch-to-zoom did, and the cause was that the slider held its own local state and " +
      "never reached the camera controller: the thumb moved and nothing happened. Rendering the " +
      "thumb from the same state the action writes to makes that failure unrepresentable, and the " +
      "same primitive now backs the interval and threshold sliders in the tuning panel.",
  },

  { h2: "9.9 Accessibility and readability" },
  {
    table: {
      head: ["Concern", "Treatment", "Status"],
      widths: [2600, 4200, 2606],
      rows: [
        ["Content descriptions", "Every icon-only control carries one", "Done"],
        ["Touch target size", "Minimum 48 dp on every interactive element", "Done"],
        ["Dynamic type", "Type scale respects the system font size setting", "Done"],
        ["Contrast", "Body text meets 4.5:1 against its background in all four accents", "Done"],
        ["Dark theme", "The application is dark by default and is legible in a darkened lecture theatre", "Done"],
        ["Screen reader navigation order", "Not verified with TalkBack", "Not done"],
        ["Colour-independent status", "LIVE, DEGRADED and QUEUED carry text as well as colour", "Done"],
        ["Reduced motion", "Not implemented", "Not done"],
      ],
    },
    tnum: "9.2",
    caption: "Accessibility, including what has not been done",
  },
  {
    p: "The two 'not done' rows are stated rather than omitted. A TalkBack pass and a " +
      "reduced-motion setting are both small pieces of work that have not been carried out, and an " +
      "accessibility table listing only the completed items would be misleading in exactly the way " +
      "this project has spent a chapter arguing against.",
  },

  { h2: "9.10 The dark default" },
  {
    p: "The application is dark by default rather than following the system setting on first " +
      "launch. This is unusual enough to justify: the screen is being held up in a lecture " +
      "theatre, often a darkened one, frequently pointed at a board while other students are " +
      "sitting behind it. A bright screen in that setting is both harder to read and more " +
      "conspicuous than it needs to be. The system setting is respected from the appearance page " +
      "onwards; only the default differs.",
  },
];

// ---------------------------------------------------------------- chapter 11

const SEC_EXTRA = [
  { h2: "11.7 Threat model, walked through" },
  {
    p: "Six scenarios, each traced from the attacker's position to what they actually obtain. The " +
      "purpose is to be specific about the size of each risk rather than to list controls.",
  },
  {
    table: {
      head: ["Scenario", "What the attacker obtains", "What bounds it"],
      widths: [2600, 3400, 3406],
      rows: [
        ["Decompiles the APK", "The client token and the proxy's base URL", "The token authorises the proxy only. Rate limit and daily budget cap the loss. Rotation is one environment variable plus a rebuild."],
        ["Obtains the client token and scripts against the proxy", "Some number of AI calls, up to the daily budget", "The daily budget. No access to any student's notes, because the proxy stores none."],
        ["Has physical access to an unlocked phone", "Everything the student has: notes, evidence, retained audio", "Nothing, beyond device lock. This is out of the application's control and is stated as such."],
        ["Installs a malicious application on the same phone", "Nothing from this application", "All captured media is in application-private storage. Nothing is written to shared media."],
        ["Intercepts network traffic", "Nothing readable", "TLS on both hops. No credential travels in a URL or a query string."],
        ["Compromises the deployment environment", "The provider key, and any traffic in flight", "Not bounded by this system. The response is rotation, which requires no application change."],
      ],
    },
    tnum: "11.3",
    caption: "Threat scenarios and their actual extent",
  },
  {
    p: "Two rows say the honest thing rather than the reassuring one. Physical access to an " +
      "unlocked device defeats every application-level control there is, and claiming otherwise " +
      "would be false. A compromised deployment environment yields the provider key, and the only " +
      "real mitigation is that rotating it is cheap and requires no rebuild — which is a recovery " +
      "property, not a prevention.",
  },

  { h2: "11.8 What is deliberately not collected" },
  {
    b: [
      "No account. The application has no sign-in, no user record and no identifier that persists off the device.",
      "No analytics. There is no telemetry, no crash reporter and no usage measurement of any kind.",
      "No voiceprints. Speaker labels are per chunk and are discarded with the session; nothing that could identify a person by voice is stored.",
      "No location. The application requests no location permission and records nothing about where a lecture happened.",
      "No contact or calendar access.",
      "No student identifier in any request to the proxy or the provider. The proxy has none to send.",
    ],
  },
  {
    p: "This list is short because the system was built without these things rather than having " +
      "them removed. The practical consequence is that the residual privacy risk reduces to one " +
      "item — lecture audio is processed by a third party — and that item is disclosed in the " +
      "consent screen in those words rather than buried in a policy.",
  },
];

// ---------------------------------------------------------------- chapter 12

const TEST_EXTRA = [
  { h2: "12.6 Test data strategy" },
  {
    p: "Where a test's input comes from turned out to matter more in this project than how many " +
      "tests there are. Three sources are used, and the ordering below is the order in which " +
      "confidence increases.",
  },
  {
    table: {
      head: ["Source", "Used for", "What it proved, and what it did not"],
      widths: [2200, 3000, 4206],
      rows: [
        ["Constructed fixtures", "Parser cases, schema cases, error mappings", "Proves that stated logic holds. Proved nothing about the frame filter, because a constructed frame has no hand tremor."],
        ["Generated inputs", "Overlap seams at every word boundary of a fixed paragraph; malformed Markdown", "Finds edge cases a hand-picked example misses. Still bounded by what the generator was told to vary."],
        ["Field-derived fixtures", "Luminance and settle scores replayed from the 218-line field log", "The only source that has ever caught a defect in this project's algorithms before a student did."],
      ],
    },
    tnum: "12.4",
    caption: "Test data sources",
  },
  {
    p: "The third row is the change this project would keep. Replaying real measurements makes the " +
      "properties of the real world — tremor, exposure drift, a provider that returns one segment " +
      "when the schema allows several — part of the specification instead of an assumption sitting " +
      "outside it.",
  },
  {
    p: "The limitation of that approach is equally clear and is worth stating: a field-derived " +
      "fixture only contains conditions that have actually occurred. A lecture theatre with a " +
      "projector screen instead of a whiteboard, a left-handed lecturer standing on the other " +
      "side, a phone on a tripod rather than in a hand — none of these is in any fixture, because " +
      "none has been recorded. The fixtures make the tests honest about four sessions, not about " +
      "lectures in general.",
  },
];

// ---------------------------------------------------------------- chapter 16

const FUTURE_EXTRA = [
  { h2: "16.6 Estimated effort" },
  {
    p: "Estimates for the work in this chapter, given as ranges because several of them cross " +
      "paths that are not fully understood yet. They are included so that the future-work chapter " +
      "can be read as a plan rather than as a wish list.",
  },
  {
    table: {
      head: ["Item", "Effort", "Risk", "Blocks"],
      widths: [3400, 1600, 1600, 2806],
      rows: [
        ["Confirm placement and dedup in a field session", "1 session", "Low", "Most of Table 15.1"],
        ["Rotate the client token", "10 minutes", "None", "Nothing"],
        ["Remove the diagnostic log", "1 hour", "Low", "Distribution"],
        ["Replace the debug signing config", "1 hour", "Low", "Distribution"],
        ["Lint rule for negative padding", "2 hours", "None", "Nothing"],
        ["Labelled corpus, 5 to 10 lectures", "2 to 3 weeks", "Low, but slow", "Every accuracy claim"],
        ["Scorer for transcription and extraction", "1 week", "Medium", "Every improvement claim"],
        ["Battery and thermal measurement", "2 days", "Low", "Nothing"],
        ["Sub-chunk timestamps", "1 to 2 weeks", "High — may need a different provider", "Known defect 5"],
        ["On-device diagram pre-filter", "1 week", "Medium", "Cost, and note quality"],
        ["Composition batching", "1 to 2 weeks", "High — crosses retained-audio recovery", "The largest remaining cost lever"],
        ["Full-note consolidation", "2 weeks", "Medium", "Topical rather than chronological notes"],
        ["Diagram structure recognition", "Open-ended", "Research", "A capability often assumed present"],
      ],
    },
    tnum: "16.3",
    caption: "Estimated effort, risk and what each item unblocks",
  },
  {
    p: "The two items marked high risk are high risk for the same reason: both touch code paths " +
      "whose failure mode is losing a student's lecture. Composition batching crosses the " +
      "retained-audio recovery path, and sub-chunk timestamps change where audio is cut. Neither " +
      "is difficult in isolation; both are difficult to do without introducing a failure that only " +
      "appears in a real lecture, which is the most expensive kind of failure this project has.",
  },
];

module.exports = {
  PROBLEM_EXTRA, REQ_EXTRA, DESIGN_EXTRA, ALGO_EXTRA, UI_EXTRA, SEC_EXTRA, TEST_EXTRA, FUTURE_EXTRA,
};

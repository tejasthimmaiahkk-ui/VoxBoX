/**
 * Narrative content for the VoxBox project report.
 *
 * Kept apart from the docx layout code so the writing can be read and corrected on its own.
 * Every factual claim here is drawn from the repository, its test suites, or the field logs
 * captured during real lecture trials — nothing is asserted that the project cannot show.
 */

// A block is one of:
//   { h1 | h2 | h3: "text" }        heading (h1 starts a new page)
//   { p: "text" }                   paragraph
//   { b: ["item", ...] }            bulleted list
//   { n: ["item", ...] }            numbered list
//   { table: { head: [...], rows: [[...]], widths: [...] } }
//   { code: "line\nline", caption } fixed-width listing
//   { note: "text" }                indented remark
//   { pagebreak: true }

const CH_INTRO = [
  { h1: "1. Introduction" },
  { h2: "1.1 Background" },
  {
    p: "A lecture is delivered once. The speech that carries the explanation exists only while " +
      "it is being spoken, and the board that carries the formulas is erased to make room for the " +
      "next one. A student sitting in that room has a single opportunity to convert both into " +
      "something they can revise from, and the conversion has to happen while they are also " +
      "trying to understand what is being said.",
  },
  {
    p: "This is not a new observation, and the usual responses to it are well known. Students " +
      "write by hand, which is slow and competes for the attention that understanding requires. " +
      "They photograph the board, which preserves the pixels but not the meaning and leaves a " +
      "camera roll nobody revisits. They record the audio, which preserves everything and " +
      "structures nothing: a fifty-minute recording is not a revision aid, because nobody listens " +
      "to fifty minutes of audio the night before an examination.",
  },
  {
    p: "The arrival of capable speech and vision models changes what is possible here, but it " +
      "also introduces a problem of its own. A model that turns speech into tidy notes is also a " +
      "model that can quietly rewrite what was said. If a student cannot tell which words came " +
      "from the lecturer and which came from a language model, the notes are worse than useless " +
      "for examination revision — they are confidently wrong in ways that are invisible.",
  },
  { h2: "1.2 Project Overview" },
  {
    p: "VoxBox is an Android application that turns a live lecture into a structured Markdown " +
      "note while the class is happening. It listens continuously and, in board mode, also watches " +
      "the board through the device camera. It writes the note incrementally as the lecture " +
      "proceeds, so that when the session ends the note is already finished.",
  },
  {
    p: "The design principle that shapes every part of the system is evidence preservation. The " +
      "raw transcript and the board text are written first and kept exactly as captured. Anything " +
      "the AI produces sits beside that evidence rather than replacing it, and where the model " +
      "disagrees with what was captured, the disagreement is surfaced to the student as a review " +
      "flag showing both readings. No AI output silently overwrites a captured line.",
  },
  { h2: "1.3 Objectives" },
  {
    n: [
      "Capture continuous classroom audio and produce a diarized transcript with per-chunk speaker labels.",
      "Capture board content through the camera without uploading every frame, using an on-device change filter.",
      "Produce a structured, readable Markdown note incrementally during the session rather than after it.",
      "Preserve all captured evidence so that any AI-generated statement can be traced back to its source.",
      "Keep the running cost low enough for a student to use daily, and bound it so it cannot run away.",
      "Keep the AI provider credential off the device entirely.",
      "Store everything locally by default, with export as the only route off the device.",
    ],
  },
  { h2: "1.4 Organisation of this Report" },
  {
    p: "Chapter 2 states the problem precisely and decomposes it into the sub-problems the system " +
      "must solve. Chapter 3 surveys existing approaches and explains why each was rejected. " +
      "Chapter 4 sets out functional and non-functional requirements. Chapter 5 defines the scope, " +
      "including what is deliberately excluded and why. Chapter 6 describes the system " +
      "architecture. Chapter 7 documents the algorithms implemented in this project, which are the " +
      "part of the work that is not simply a call to a model. Chapter 8 covers implementation " +
      "module by module. Chapters 9 to 11 cover the interface, the backend and the security " +
      "posture. Chapter 12 documents testing. Chapter 13 records the engineering challenges " +
      "encountered and how each was resolved — including several where the first solution was " +
      "wrong and had to be replaced. Chapter 14 presents results from field trials. Chapter 15 " +
      "states the limitations honestly. Chapter 16 is reserved for future implementation and is " +
      "expected to grow as the project continues. The appendices contain the complete source code.",
  },
];

const CH_PROBLEM = [
  { h1: "2. Problem Statement" },
  { h2: "2.1 Statement" },
  {
    p: "Design and implement a mobile system that converts a live spoken lecture, together with " +
      "the visual content of the board or projector, into a structured and revisable set of notes " +
      "in real time, without discarding the evidence from which those notes were derived, and at a " +
      "running cost that a student can sustain.",
  },
  { h2: "2.2 Decomposition" },
  {
    p: "The statement above conceals several independent problems, each of which has to be solved " +
      "for the system to be useful. They are listed here because the rest of this report is " +
      "organised around them.",
  },
  { h3: "2.2.1 The attention problem" },
  {
    p: "Note-taking competes with understanding for the same cognitive resource. A student writing " +
      "a definition is not listening to the sentence that follows it. Any system that requires " +
      "active operation during the lecture has failed at the outset, which constrains the " +
      "interface: the number of decisions taken before pressing start must be small, and the " +
      "number taken during the session must be close to zero.",
  },
  { h3: "2.2.2 The board problem" },
  {
    p: "A significant fraction of the examinable content of a technical lecture is never spoken " +
      "aloud in a form that can be transcribed. Formulas, diagrams, circuit schematics and " +
      "worked derivations are written, pointed at, and referred to as \"this\" and \"here\". An " +
      "audio-only system misses all of it. A system that photographs continuously produces " +
      "hundreds of near-identical images and an unusable bill.",
  },
  { h3: "2.2.3 The cost problem" },
  {
    p: "Commercial speech and vision models are priced per token. A naive implementation that " +
      "sends every audio chunk and every camera frame to a model, together with the entire note " +
      "so far for context, produces a per-lecture cost that scales with the square of the lecture " +
      "length. This report documents a measured instance of exactly that failure and the " +
      "correction applied to it.",
  },
  { h3: "2.2.4 The trust problem" },
  {
    p: "A language model asked to tidy a transcript will, given thin evidence, produce fluent and " +
      "entirely fabricated content. During development a capture session whose only speech was the " +
      "phrase \"Is this working?\" produced a confident section headed \"Chemical Reactions\" " +
      "containing a plausible definition of a chemical reaction. Nothing in that section was said " +
      "by anyone. A note-taking system that can do this without the student noticing is actively " +
      "dangerous for examination revision.",
  },
  { h3: "2.2.5 The credential problem" },
  {
    p: "The system must call a paid AI service. An API key compiled into an Android application " +
      "is recoverable by anyone who obtains the APK, since an APK is a distributable archive whose " +
      "contents can be extracted with ordinary tools. The credential therefore cannot live on the " +
      "device, which forces a server component into what would otherwise be a purely local system.",
  },
  { h2: "2.3 Why the Problem is Non-Trivial" },
  {
    p: "Each sub-problem above has an obvious solution that fails in contact with a real lecture. " +
      "The obvious solution to the board problem is to photograph on a timer, which produces " +
      "hundreds of duplicates. The obvious refinement is to compare frames and only send the " +
      "different ones, which then fires on every person who walks past. The obvious refinement to " +
      "that is to require the change to persist, which — as this project discovered from a field " +
      "log — silently blocks genuine captures for twenty seconds at a time if the persistence test " +
      "is written with an absolute threshold, because a handheld camera is never still. The " +
      "correct solution, arrived at only after instrumenting a real session, is a relative test. " +
      "This pattern recurs throughout the project and is documented in Chapter 13.",
  },
];

const CH_LITERATURE = [
  { h1: "3. Existing Systems and Related Work" },
  { h2: "3.1 Survey" },
  {
    p: "The following approaches were examined before the architecture of this project was fixed. " +
      "Each is in use somewhere, and each fails at least one requirement of a live technical lecture.",
  },
  {
    table: {
      head: ["Approach", "Representative use", "Why it is insufficient here"],
      widths: [2400, 2600, 4400],
      rows: [
        ["Manual typing or handwriting", "The default student behaviour", "Competes directly with understanding; cannot keep pace with a technical lecture; board diagrams are copied badly or not at all."],
        ["Audio recording only", "Voice memo applications", "Preserves everything, structures nothing. A fifty-minute file is not revisable and is never re-listened to before an examination."],
        ["Post-hoc transcription services", "Offline speech-to-text pipelines", "Produces an unstructured wall of text after the lecture has ended. Misses the board entirely. No incremental value during the class."],
        ["Meeting note assistants", "General-purpose meeting summarisers", "Optimised for conversational turn-taking and action items, not for definitions, derivations or formulas. No board capture. Typically cloud-first with no local storage guarantee."],
        ["Photographing the board", "Camera roll", "Preserves pixels, not meaning. Produces an unsearchable archive with no connection to what was being said at the time."],
        ["On-device OCR only", "ML Kit text recognition", "Weak on handwriting, produces no concept structure, cannot identify diagram regions. Retained in this project only as an offline fallback."],
        ["Slide-sharing platforms", "Institutional LMS", "Only helps where slides exist and are shared. Does not cover a whiteboard, and carries none of the spoken explanation."],
      ],
    },
  },
  { h2: "3.2 Position of this Project" },
  {
    p: "VoxBox occupies the intersection that none of the above covers: simultaneous audio and " +
      "board capture, structured output produced during the session, local-first storage, and an " +
      "explicit guarantee that the captured evidence survives whatever the AI does with it. The " +
      "last of these appears to be the least common property among comparable systems and is the " +
      "one this project treats as non-negotiable.",
  },
];

const CH_REQUIREMENTS = [
  { h1: "4. Requirements Specification" },
  { h2: "4.1 Functional Requirements" },
  {
    table: {
      head: ["ID", "Requirement", "Status"],
      widths: [900, 6600, 1900],
      rows: [
        ["FR-01", "Capture continuous microphone audio in bounded chunks while the session is running.", "Implemented"],
        ["FR-02", "Transcribe each chunk and return per-chunk speaker labels.", "Implemented"],
        ["FR-03", "Identify the dominant speaker within a chunk, or report ambiguity.", "Implemented"],
        ["FR-04", "Capture camera frames at a configurable interval in board mode.", "Implemented"],
        ["FR-05", "Discard visually unchanged frames on the device before any network call.", "Implemented"],
        ["FR-06", "Extract board text, equations, concepts and diagram regions from an accepted frame.", "Implemented"],
        ["FR-07", "Crop and store diagram regions as local image assets.", "Implemented"],
        ["FR-08", "Build a structured Markdown note incrementally during the session.", "Implemented"],
        ["FR-09", "Surface AI–evidence disagreements as review flags rather than applying them.", "Implemented"],
        ["FR-10", "Run one verification pass over the finished note at end of session.", "Implemented"],
        ["FR-11", "Store notes, transcripts and assets in a local database.", "Implemented"],
        ["FR-12", "Organise notes into nested folders.", "Implemented"],
        ["FR-13", "Search notes by title and body text.", "Implemented"],
        ["FR-14", "Rename and delete notes, with confirmation before destruction.", "Implemented"],
        ["FR-15", "Export a note as Markdown with its diagram assets.", "Implemented"],
        ["FR-16", "Export the captured evidence as a separate verbatim document.", "Implemented"],
        ["FR-17", "Allow the note detail level to be selected and a custom instruction supplied.", "Implemented"],
        ["FR-18", "Allow sampling interval and sensitivity to be changed during a running session.", "Implemented"],
        ["FR-19", "Adapt sampling to the speaker's pace automatically.", "Implemented"],
        ["FR-20", "Provide a manual capture control that overrides the change filter.", "Implemented"],
        ["FR-21", "Recover audio whose note revision failed to commit.", "Implemented"],
        ["FR-22", "Classify diagram type and extract diagram structure.", "Future work"],
        ["FR-23", "Import a syllabus from PDF or DOCX.", "Future work"],
        ["FR-24", "Consent-aware speaker identity across a whole lecture.", "Future work"],
      ],
    },
  },
  { h2: "4.2 Non-Functional Requirements" },
  {
    table: {
      head: ["ID", "Requirement", "How it is met"],
      widths: [900, 3900, 4600],
      rows: [
        ["NFR-01", "The AI provider credential must never reside on the device.", "All model calls are proxied through an authenticated HTTPS service that holds the key in its own secret store."],
        ["NFR-02", "Running cost must be bounded and predictable.", "Forwarded context is capped per request; the server enforces a per-caller rate limit and a hard daily request budget."],
        ["NFR-03", "Captured evidence must survive AI failure.", "Transcript and board text are persisted before refinement is attempted; failure appends a labelled fallback section."],
        ["NFR-04", "Speech must not be lost.", "The audio queue is unbounded and strictly ordered; chunks whose note revision fails are retained on disk for recovery."],
        ["NFR-05", "Capture must be visible to those present.", "Recording is foreground-only and cannot run with the screen off."],
        ["NFR-06", "Data must remain on the device by default.", "System backup is disabled; export is the only route off the device and is user-initiated."],
        ["NFR-07", "The interface must be usable one-handed during a lecture.", "Four destinations, minimum 44 dp touch targets, single primary action per screen."],
        ["NFR-08", "Status must never be ambiguous.", "Fixed status semantics: green saved, orange needs review, red recording or destructive. These never follow the user's accent choice."],
        ["NFR-09", "Motion must be suppressible.", "All entry and looping animations honour the system animator scale."],
        ["NFR-10", "The system must degrade rather than fail.", "Every provider failure path produces a labelled note section and a warning, never a lost session."],
      ],
    },
  },
  { h2: "4.3 Hardware Requirements" },
  {
    table: {
      head: ["Component", "Minimum", "Used in development"],
      widths: [3000, 3200, 3200],
      rows: [
        ["Android device", "API level 26 (Android 8.0)", "Redmi 2411DRN47I, Android 16"],
        ["Microphone", "Any device microphone", "Built-in"],
        ["Rear camera", "Required for board mode only", "Built-in"],
        ["Storage", "Approximately 100 MB plus note assets", "269 KB measured after several sessions"],
        ["Network", "Intermittent connectivity tolerated", "5G and Wi-Fi"],
        ["Development machine", "Any machine capable of running Android Studio", "Windows 10, 16 GB RAM"],
      ],
    },
  },
  { h2: "4.4 Software Requirements" },
  {
    table: {
      head: ["Layer", "Technology", "Purpose"],
      widths: [2400, 3400, 3600],
      rows: [
        ["Language", "Kotlin 2.2", "Application source"],
        ["UI toolkit", "Jetpack Compose, Material 3", "Declarative interface"],
        ["Persistence", "Room 2.7", "Local relational storage with migrations"],
        ["Preferences", "DataStore Preferences", "Durable application settings"],
        ["Navigation", "Navigation Compose", "Destination graph"],
        ["Camera", "CameraX 1.6", "Preview, capture and zoom control"],
        ["Serialisation", "kotlinx.serialization", "Request and response bodies"],
        ["On-device OCR", "ML Kit Text Recognition", "Offline board fallback"],
        ["Backend runtime", "Node.js 22", "Proxy service, dependency-free"],
        ["AI provider", "OpenRouter (OpenAI-compatible)", "Transcription, vision, note and verification models"],
        ["Hosting", "Render, Docker", "Deployment of the proxy"],
        ["Testing", "JUnit 4, Compose UI Test, node:test", "Unit, instrumentation and backend tests"],
      ],
    },
  },
];

const CH_SCOPE = [
  { h1: "5. Scope of the Project" },
  { h2: "5.1 In Scope" },
  {
    b: [
      "Continuous foreground voice capture in twenty-second chunks with a two-second overlap.",
      "Camera board capture at an adjustable interval between two and thirty seconds.",
      "On-device frame-change filtering, executed before any network call is made.",
      "Diarized transcription with per-chunk speaker labels and dominant-speaker selection.",
      "Incremental Markdown note construction during the lecture.",
      "Board text, equation, concept and diagram-region extraction.",
      "Detail control across three levels, plus a free-text custom instruction.",
      "End-of-session verification of formulas, units and named concepts.",
      "Local storage with nested folders, search, rename and delete.",
      "Export of both the refined note and the captured evidence, with diagram assets.",
      "A hosted, authenticated HTTPS proxy holding the provider credential.",
      "Adaptive sampling driven by measured speech pace, with manual override.",
    ],
  },
  { h2: "5.2 Explicitly Out of Scope" },
  {
    p: "The following are excluded deliberately. Each exclusion is a design decision with a " +
      "stated reason, not an omission.",
  },
  {
    table: {
      head: ["Excluded", "Reason"],
      widths: [3600, 6200],
      rows: [
        ["Background or screen-off recording", "Capture must be visible to people in the room. A system that records invisibly cannot honestly claim the consent posture described in Chapter 11."],
        ["Identifying who the teacher is", "True speaker identity requires voice enrolment, stored voiceprints and explicit consent. The project claims only chunk-local dominance, which is a weaker and honest claim."],
        ["Real-time word-by-word captioning", "A different latency problem with a different architecture. Chunking at twenty seconds is incompatible with live captioning."],
        ["Multi-user accounts, cloud sync, sharing", "Local-first by design. Export is the sharing mechanism."],
        ["Arbitrary handwriting recognition", "Bounded by what the vision model can read. Uncertainty is reported rather than hidden."],
        ["PDF and DOCX syllabus import", "Markdown and plain text only in the current scope. Listed as future work."],
        ["Diagram type classification", "No arrow, chart or entity-relationship understanding exists. The system crops regions the model identifies; it does not interpret their structure. Listed as future work."],
      ],
    },
  },
  { h2: "5.3 Scoping Rationale" },
  {
    p: "The project was deliberately scoped to one student, one device and one lecture at a time. " +
      "That constraint made it possible to go deep on evidence handling, failure recovery and cost " +
      "control rather than shallow on collaboration features. The parts of the system that took " +
      "the most engineering effort — the frame filter, the append-only note protocol, the recovery " +
      "paths — are all consequences of taking that narrow scope seriously.",
  },
];

module.exports = { CH_INTRO, CH_PROBLEM, CH_LITERATURE, CH_REQUIREMENTS, CH_SCOPE };

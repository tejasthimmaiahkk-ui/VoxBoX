/**
 * Chapters 1 to 5: introduction, problem statement, related work, requirements and scope.
 */

const CH_INTRO = [
  { h1: "1. Introduction" },

  { h2: "1.1 The situation this project addresses" },
  {
    p: "A lecture happens once. The spoken explanation exists only while it is being spoken; the " +
      "board that carries the derivation is erased to make room for the next one; the aside that " +
      "finally makes a definition click is never repeated in the same words. A student sitting in " +
      "that room has exactly one opportunity to convert an hour of transient information into " +
      "something that can be revised in three months' time, and must do so while simultaneously " +
      "trying to understand it.",
  },
  {
    p: "These two activities compete directly. Writing thoroughly means falling behind the " +
      "explanation. Listening properly means writing less. Most students resolve the conflict by " +
      "capturing the artefacts that are easiest to capture — the board, photographed on a phone — " +
      "and hoping that the spoken explanation will be reconstructable later from the notation " +
      "alone. It rarely is. A photograph of a derivation records the steps and discards the reason " +
      "each step was taken, which is the part the lecturer was actually contributing.",
  },
  {
    p: "The result is a familiar artefact: a camera roll containing forty near-identical " +
      "photographs of a whiteboard, in no particular order, with no indication of which lecture " +
      "each belongs to, alongside a page of handwritten notes that trail off in the middle of the " +
      "third topic. Neither is revisable. Both took effort to produce.",
  },

  { h2: "1.2 What VoxBox does" },
  {
    p: "VoxBox is an Android application that converts a live lecture into a structured Markdown " +
      "note while the lecture is still in progress. It listens continuously through the device " +
      "microphone and, in board mode, watches the board through the device camera. The note is " +
      "written incrementally as the session runs, so that when the student presses stop the note " +
      "is already complete rather than beginning to be produced.",
  },
  {
    p: "Two design decisions distinguish it from the obvious implementation of the same idea. The " +
      "first is that the phone does substantial work before it sends anything anywhere: an " +
      "on-device change filter reduces roughly 450 candidate camera frames per lecture-hour to " +
      "roughly 40 distinct board states, so that the expensive operation runs 40 times instead of " +
      "450. The second is that captured evidence and generated text are held separately and " +
      "permanently apart. The raw transcript and the raw board text are written to local storage " +
      "before any model is consulted, and nothing a model produces is permitted to overwrite them.",
  },
  { fig: "docs/diagrams/fig-context-dfd.png", num: "1.1", caption: "Context diagram: everything that crosses the system boundary" },
  {
    p: "Figure 1.1 shows the whole system as a single process with four external parties. Two are " +
      "human: the student, who configures and reads, and the lecturer, who is drawn as an actor " +
      "but never interacts with the application — their speech and board writing enter through the " +
      "student's own phone. Two are services: the project's own hosted proxy, which holds the AI " +
      "provider credential, and the platform's share sheet, through which a finished note leaves.",
  },

  { h2: "1.3 The principle that shapes every part of the system" },
  {
    p: "The organising principle of this project is evidence preservation, and it is worth stating " +
      "precisely because almost every design decision in the following chapters follows from it.",
  },
  {
    note: "What the student said and what the lecturer wrote are facts. What the model produced from " +
      "them is an interpretation. The system stores the facts first, keeps them exactly as " +
      "captured, and never allows an interpretation to replace one.",
  },
  {
    p: "This is not a stylistic preference. A language model asked to summarise a lecture will " +
      "produce fluent, plausible, confidently worded text whether or not it has anything to " +
      "summarise. During the third field trial this project's own system produced a section headed " +
      "\"Applications in Industry\" containing four well-written paragraphs, at a point in the " +
      "recording where the lecturer had said nothing for two minutes while drawing a diagram. The " +
      "text was entirely invented. It was also indistinguishable, on the page, from the text that " +
      "was not.",
  },
  {
    p: "A student revising from that note three months later has no way to tell the two apart. " +
      "That is the failure mode this project treats as unacceptable, and the reason the " +
      "architecture keeps a physical separation between what was captured and what was generated, " +
      "records which evidence produced which sentence, and runs a server-side check that discards " +
      "generated text whose vocabulary does not match the evidence it claims to be summarising.",
  },

  { h2: "1.4 Structure of this report" },
  {
    table: {
      head: ["Chapter", "Contents"],
      widths: [2600, 6806],
      rows: [
        ["2. Problem Statement", "The specific problems, each stated as an observable failure rather than a general need."],
        ["3. Existing Systems", "What already exists, what each class of tool does well, and the gap this project occupies."],
        ["4. Requirements", "Forty-two functional and eighteen non-functional requirements, each with a verification method."],
        ["5. Scope", "What is in scope, what is explicitly out of scope, and why each exclusion was made."],
        ["6. Design and Architecture", "Layers, components, data flow, deployment and the database schema."],
        ["7. Algorithms", "The six algorithms that are this project's own work, with the reasoning and the measurements behind each."],
        ["8. Implementation", "How the design became 13,337 lines of Kotlin, and the platform constraints that shaped it."],
        ["9. User Interface", "Sixteen destinations, the navigation rules, and the interface changes that came from field use."],
        ["10. Backend and Deployment", "The proxy, why it exists, and how it is deployed and operated."],
        ["11. Security and Privacy", "Trust boundaries, threats considered, controls in place and residual risk disclosed."],
        ["12. Testing", "180 automated tests, and an honest account of the defect classes they did not catch."],
        ["13. Challenges", "Twelve problems encountered during development, each with its diagnosis and its resolution."],
        ["14. Results", "What was measured, what the measurements say, and what is deliberately not claimed."],
        ["15. Limitations", "Known defects, unproven features and unmeasured properties."],
        ["16. Future Implementation", "Planned work, in priority order, with the reason each item is not yet done."],
        ["17. Conclusion", "What was built, what was learned and what the project is honestly worth."],
        ["18. References", "Annotated references, grouped by the part of the system each informed."],
        ["Appendices V to Y", "Complete source of the application, the backend, the test suites and the build configuration."],
        ["Appendices E to J", "Schema reference, API specification, test register, field trial records, user manual and glossary."],
      ],
    },
    tnum: "1.1",
    caption: "Structure of this report",
  },
  {
    p: "Chapter 13 deserves a note. It records twelve problems encountered during development, " +
      "including several where the initial approach was simply wrong and had to be discarded. It " +
      "is longer than a chapter of that kind usually is, deliberately: the most useful thing this " +
      "project produced was not the application but the set of specific ways in which a plausible " +
      "design turned out not to survive contact with a real classroom.",
  },

  { h2: "1.5 Summary of what exists" },
  {
    table: {
      head: ["Measure", "Value"],
      widths: [4700, 4706],
      rows: [
        ["Application source", "13,337 lines of Kotlin across 74 files"],
        ["Test source", "2,574 lines across 32 files"],
        ["Backend source and tests", "2,939 lines of JavaScript, no runtime dependencies"],
        ["Android unit tests", "134, all passing"],
        ["Backend contract tests", "43, all passing"],
        ["Device instrumentation tests", "3, all passing on a physical phone"],
        ["Lint", "Clean"],
        ["Database", "10 tables, schema version 5, 4 tested migrations"],
        ["Screens", "16 destinations"],
        ["Backend", "Deployed and live; verified against /health"],
        ["Field trials in real lectures", "4"],
        ["Algorithms that are this project's own work", "6"],
      ],
    },
    tnum: "1.2",
    caption: "The system at the time this report was generated",
  },
];

const CH_PROBLEM = [
  { h1: "2. Problem Statement" },
  {
    p: "This chapter states the problem as a set of specific, observable failures rather than as a " +
      "general need. Each is something that can be watched happening in a lecture theatre, and " +
      "each has a corresponding requirement in Chapter 4 and a corresponding mechanism in Chapters " +
      "6 and 7.",
  },

  { h2: "2.1 The core statement" },
  {
    note: "A lecture is delivered once, in two channels simultaneously — speech and board — and a " +
      "student cannot fully capture either while also understanding both. The information is " +
      "transient, the two channels are not independent, and the moment for capture does not recur.",
  },

  { h2: "2.2 The six failures this decomposes into" },

  { h3: "2.2.1 Speech is not durable and cannot be re-listened to" },
  {
    p: "The spoken explanation is the highest-value content in a lecture and the least likely to " +
      "be preserved. A student can photograph the board in a second; capturing the reason the " +
      "lecturer chose that substitution requires writing a sentence, which takes long enough that " +
      "the next two sentences are missed. Recording the audio solves durability and creates a new " +
      "problem: fifty minutes of audio is not revisable. Finding one explanation inside it requires " +
      "re-listening to it, which costs as much time as the lecture did.",
  },

  { h3: "2.2.2 The board is erased, and photographs of it are unstructured" },
  {
    p: "Board content is deleted by design — the lecturer needs the space. A photograph preserves " +
      "the pixels and preserves nothing else: not which lecture it belongs to, not what was being " +
      "said while it was written, not whether it supersedes the photograph taken four minutes " +
      "earlier. Forty photographs of a whiteboard in a camera roll are, in practice, forty items " +
      "the student will scroll past.",
  },

  { h3: "2.2.3 The two channels are only meaningful together" },
  {
    p: "This is the failure that makes the problem interesting rather than merely tedious. The " +
      "board carries notation; the speech carries meaning. A derivation photographed without its " +
      "commentary is a sequence of manipulations with no stated motivation. A transcript without " +
      "the board contains sentences such as \"and then this term cancels with that one\", where " +
      "both pronouns refer to something the student can no longer see.",
  },
  {
    p: "Any solution that captures one channel well and the other not at all has not solved the " +
      "problem; it has produced half of an artefact. The alignment between them — which board " +
      "state was on display while which sentence was spoken — is itself information that must be " +
      "preserved, and it is information that neither an audio recorder nor a camera captures.",
  },

  { h3: "2.2.4 Naive automation is prohibitively expensive" },
  {
    p: "The obvious implementation — sample the camera every few seconds, send every frame for " +
      "analysis, send every chunk of audio for transcription, and re-send the whole note on every " +
      "update so the model can rewrite it — is technically straightforward and economically " +
      "impossible for a student. A frame every eight seconds is 450 frames an hour. A note that is " +
      "re-sent in full on every update grows quadratically in cost with lecture length. Measured " +
      "on this project's own early build, a fifty-minute lecture cost roughly three times what the " +
      "final build costs, and almost all of the difference was in what was sent rather than what " +
      "was received.",
  },

  { h3: "2.2.5 Generated notes can be confidently wrong" },
  {
    p: "A language model given thin evidence produces fluent output regardless. It does not fail " +
      "loudly; it fills the gap. Chapter 1 described the specific instance from this project's " +
      "third field trial. The general problem is that generated text carries no visible marker " +
      "distinguishing it from grounded text, so a student cannot apply their own judgement to it, " +
      "and the error surfaces during an examination rather than during revision.",
  },
  {
    p: "A prompt instruction does not address this. The prompt in use at the time of that trial " +
      "explicitly instructed the model not to introduce material absent from the evidence. The " +
      "instruction was followed on every other call in that session and ignored on one. An " +
      "instruction changes the probability of an output; only a check on the output changes what " +
      "can reach the student.",
  },

  { h3: "2.2.6 Notes accumulate faster than they can be organised" },
  {
    p: "This failure has nothing to do with capture and was reported from the first real trial. A " +
      "student taking three lectures a day has fifteen notes by the end of a week and sixty by the " +
      "end of a month. A flat, reverse-chronological list is unusable at that point: finding last " +
      "Tuesday's session on integration by parts requires remembering when last Tuesday was. What " +
      "is needed is the arrangement students already use for their own files — a folder per " +
      "subject, and a folder per unit inside it.",
  },

  { h2: "2.3 Failures mapped to mechanisms" },
  {
    table: {
      head: ["Failure", "Mechanism", "Where it is described"],
      widths: [3000, 3400, 3006],
      rows: [
        ["Speech is transient", "Continuous 20-second chunked capture with a never-drop ordered queue", "Chapters 6, 8"],
        ["Words lost at chunk boundaries", "Two-second overlap plus word-level trimming", "Chapter 7, Algorithm 3"],
        ["Board erased", "Camera sampling with on-device change detection", "Chapter 7, Algorithm 1"],
        ["Channels must be aligned", "Both streams carry session-relative timestamps into one append-only note", "Chapters 6, 7"],
        ["Cost of naive automation", "Frame filter, bounded delta context, duplicate rejection, budget ceiling", "Chapters 7, 10, 14"],
        ["Sampling rate is a fixed guess", "Adaptive sampling driven by measured speech pace", "Chapter 7, Algorithm 2"],
        ["Confidently wrong output", "Grounding guard, review flags, block provenance, verification pass", "Chapters 7, 11"],
        ["Model rewriting captured text", "Append-only delta with SHA-256 binding and revision check", "Chapter 7, Algorithm 4"],
        ["Notes accumulate unorganised", "Nested folders, search, rename, delete with confirmation", "Chapter 9"],
      ],
    },
    tnum: "2.1",
    caption: "Each stated failure and the mechanism that addresses it",
  },

  { h2: "2.4 What this project does not claim to solve" },
  {
    p: "Three things are outside what the stated problem asks for, and are named here so that the " +
      "problem statement is not read as broader than it is.",
  },
  {
    b: [
      "Understanding. The system produces a structured record of a lecture. It does not teach, " +
        "explain, answer questions about the material or assess whether the student has learned it.",
      "Diagram interpretation. The system identifies and crops regions of the board that a vision " +
        "model reports as diagrams. It does not understand what an arrow means, what a chart plots " +
        "or what an entity-relationship structure describes. This is the most frequently " +
        "misunderstood limitation of the system and is stated again in Chapters 15 and 16.",
      "Accuracy guarantees. No accuracy figure is claimed anywhere in this report, because none " +
        "has been measured. Building a labelled corpus and a scorer is the first item of planned " +
        "work in Chapter 16.",
    ],
  },
];

const CH_LITERATURE = [
  { h1: "3. Existing Systems and Related Work" },
  {
    p: "This chapter surveys what already exists, in five classes of tool, and identifies precisely " +
      "where each stops. The purpose is not to argue that existing tools are bad — several are " +
      "excellent at what they do — but to locate the gap this project occupies, which is narrower " +
      "and more specific than \"note-taking\".",
  },

  { h2: "3.1 Class 1: audio recorders and voice memos" },
  {
    p: "Every phone ships with one. They solve durability completely and reliably: the audio is " +
      "captured, it is not lost, and it can be replayed. What they do not do is make the content " +
      "addressable. Fifty minutes of audio has no structure, no index and no search. Locating one " +
      "explanation within it requires knowing roughly when it occurred, which is precisely the " +
      "information a student who needs the explanation does not have.",
  },
  {
    p: "They also capture one channel only. A recording of a mathematics lecture consists largely " +
      "of deixis — \"this\", \"that\", \"here\" — pointing at a board the recording does not contain.",
  },

  { h2: "3.2 Class 2: automatic transcription services" },
  {
    p: "Transcription services convert audio to searchable text, and modern ones do so with " +
      "genuinely high accuracy on clear single-speaker audio. This is a real advance over raw " +
      "audio: the content becomes searchable, skimmable and quotable.",
  },
  {
    p: "Three limitations remain relevant to this problem. First, a transcript is a record of " +
      "speech, not a note: it has no headings, no hierarchy and no separation between a definition " +
      "and an aside about the examination. Fifty minutes of accurate transcript is more revisable " +
      "than fifty minutes of audio and considerably less revisable than two pages of structured " +
      "notes. Second, transcription is still single-channel; the board is absent. Third, " +
      "transcription services are priced per minute of audio and applied to every minute, which " +
      "for a full timetable is a recurring cost with no filtering step to reduce it.",
  },

  { h2: "3.3 Class 3: document scanning and OCR applications" },
  {
    p: "Scanning applications correct perspective, enhance contrast and extract text from " +
      "photographs of documents and boards, and they do this well. They are the right tool for " +
      "capturing a single board state accurately.",
  },
  {
    p: "They are manual and stateless. Each capture is an explicit user action, which means the " +
      "student must decide, during the lecture, which board states are worth photographing — the " +
      "attention cost the problem statement is trying to remove. And each capture is independent: " +
      "the application has no notion that this photograph supersedes the one four minutes ago, " +
      "that it belongs to the same lecture, or that anything was being said while it was written.",
  },

  { h2: "3.4 Class 4: general-purpose AI note assistants" },
  {
    p: "Assistants that accept a recording or a transcript and return structured notes are the " +
      "closest existing analogue to this project, and the comparison is the most instructive one " +
      "in this chapter.",
  },
  {
    p: "They are almost always post-hoc: the artefact is produced after the event, from a complete " +
      "recording. That is a legitimate design and it has a consequence — the student leaves the " +
      "lecture with nothing, and the note exists only once the processing has run. They are also " +
      "single-channel in the same way transcription is; a photograph of the board is not part of " +
      "the input.",
  },
  {
    p: "The more important difference is epistemic. A general assistant produces one artefact: the " +
      "generated note. The evidence it was generated from is either discarded or kept separately " +
      "and never referred to again, and the note carries no record of which sentence came from " +
      "where. When the model is wrong, nothing in the artefact indicates it. This project's entire " +
      "architecture — block provenance, review flags, the grounding guard, the append-only " +
      "protocol — exists to answer that specific weakness, and would be unnecessary if the " +
      "weakness did not matter.",
  },

  { h2: "3.5 Class 5: lecture capture systems installed by institutions" },
  {
    p: "Institutional systems record the room: a fixed camera, a microphone, and often a capture " +
      "of the presentation software's output. Where they are installed they are the most complete " +
      "record available, and they are also the least available: they require the institution to " +
      "have installed them, they typically do not capture a physical whiteboard usefully, and the " +
      "output is a video the student must still watch in full.",
  },

  { h2: "3.6 Comparison" },
  {
    table: {
      head: ["Capability", "Recorder", "Transcription", "Scanner", "AI assistant", "VoxBox"],
      widths: [3106, 1200, 1500, 1200, 1300, 1100],
      rows: [
        ["Speech captured", "yes", "yes", "no", "yes", "yes"],
        ["Board captured", "no", "no", "manual", "no", "automatic"],
        ["Two channels aligned in time", "no", "no", "no", "no", "yes"],
        ["Structured output", "no", "partial", "no", "yes", "yes"],
        ["Available during the lecture", "n/a", "rarely", "n/a", "no", "yes"],
        ["Cost reduced before the call", "n/a", "no", "n/a", "no", "yes"],
        ["Evidence kept separate from output", "n/a", "n/a", "n/a", "no", "yes"],
        ["Per-sentence provenance", "no", "no", "no", "no", "yes"],
        ["Output checked against evidence", "n/a", "n/a", "n/a", "no", "yes"],
        ["Disagreements surfaced to the user", "n/a", "n/a", "n/a", "no", "yes"],
      ],
    },
    tnum: "3.1",
    caption: "Capability comparison across the five classes of existing tool",
  },
  {
    p: "The bottom four rows are the ones that matter. Every tool in the first four columns either " +
      "does not generate text at all, in which case the question of grounding does not arise, or " +
      "generates text and presents it as the artefact with no way for the reader to interrogate " +
      "it. The gap this project occupies is not \"a better note-taking application\"; it is a " +
      "system where generated text is treated as a claim about evidence that the system still " +
      "holds, and where the student can see both.",
  },

  { h2: "3.7 Technical and conceptual background" },
  {
    p: "The following work informed specific mechanisms in this system. Full citations appear in " +
      "Chapter 18, grouped by the part of the system each informed.",
  },

  { h3: "3.7.1 Optimistic concurrency control" },
  {
    p: "The append protocol described in Chapter 7 is optimistic concurrency control applied to a " +
      "document rather than to a database row: read the version, compute the change, and commit " +
      "only if the version has not moved. The technique is standard in database systems and in " +
      "distributed version control, and it is the right shape here for the same reason it is right " +
      "there — the conflicting operations are independent, conflicts are rare, and a lock would " +
      "stall one pipeline behind another with no benefit when the conflict does not occur.",
  },
  {
    p: "This project adds a content hash to the version check. A version number alone is satisfied " +
      "by an edit that changes text without changing the count, and in this system such an edit is " +
      "routine: the student can correct a sentence while the session is still running. Binding the " +
      "delta to a SHA-256 of the content it was computed against closes that gap.",
  },

  { h3: "3.7.2 Frame differencing and perceptual hashing" },
  {
    p: "Deciding whether a video frame differs meaningfully from a reference is a long-standing " +
      "problem in video compression and surveillance, and the standard approaches — downsample to " +
      "a small grid, compare cell by cell, threshold the proportion of changed cells — are what " +
      "Algorithm 1 uses. What is specific to this application is the settle test, described in " +
      "Chapter 7, which exists because the interesting distinction in a lecture theatre is not " +
      "\"has the scene changed\" but \"has the scene finished changing\". A lecturer writing and a " +
      "person walking across the board produce the same initial signal and differ entirely in what " +
      "happens next.",
  },
  {
    p: "Perceptual hashing, used here to reject a board that has already been extracted, comes " +
      "from the image-retrieval literature. The property being relied on is that the hash is " +
      "stable under the small changes a handheld phone introduces — slight rotation, exposure " +
      "drift, sub-pixel translation — and unstable under the change that matters, which is new " +
      "writing appearing.",
  },

  { h3: "3.7.3 Control theory, applied badly and then correctly" },
  {
    p: "The adaptive sampling loop of Algorithm 2 is a feedback controller: measure the speaker's " +
      "pace, adjust the sampling parameters, measure again. Its first implementation contained a " +
      "defect that is textbook and was still not obvious while writing it — each adjustment was " +
      "applied to the previous adjusted value rather than to the baseline, which turned a " +
      "proportional controller into an integrating one and caused the parameters to drift " +
      "monotonically until they hit their clamps. Chapter 7 gives the measured drift. The " +
      "correction is to anchor every suggestion to the session baseline, so that the loop has no " +
      "memory of its own output.",
  },

  { h3: "3.7.4 Grounding and hallucination in generated text" },
  {
    p: "That language models produce fluent, well-formed, unsupported text is widely documented " +
      "and is not a novel observation. The response this project takes — score the generated text " +
      "against the evidence it claims to summarise and discard it below a threshold — is a simple " +
      "form of the automatic faithfulness checks studied in the summarisation literature. It is " +
      "deliberately simple: a lexical overlap ratio on content words, with a floor of 0.25 and a " +
      "minimum length of 120 characters. The thresholds were set from measurements on this " +
      "project's own field data, given in Chapter 7, and not taken from any external source.",
  },
  {
    p: "The important claim here is not that the check is sophisticated. It is that a check exists " +
      "at all, on the server, where it cannot be bypassed, in a place a prompt cannot reach.",
  },

  { h3: "3.7.5 Platform and library foundations" },
  {
    table: {
      head: ["Technology", "Used for", "Why this one"],
      widths: [2200, 3200, 4006],
      rows: [
        ["Kotlin 2.2", "The entire application", "Platform default; null safety and coroutines are directly relevant to a system with three concurrent pipelines."],
        ["Jetpack Compose + Material 3", "All sixteen screens", "Declarative UI makes the live session screen — which changes state several times a minute — tractable."],
        ["Room 2.7", "Persistence over SQLite", "Compile-time query verification and a migration framework that fails the build rather than the user."],
        ["DataStore Preferences", "Settings", "Replaces SharedPreferences; asynchronous and transactional, so a settings write cannot block the capture thread."],
        ["CameraX 1.6", "Board sampling and zoom", "LifecycleCameraController removes an entire class of camera lifecycle bugs; ZoomState is what the zoom slider binds to."],
        ["AudioRecord", "16 kHz mono PCM16 capture", "Direct buffer access, which chunking with a two-second overlap requires. Higher-level recorders do not expose the buffer."],
        ["ML Kit Text Recognition", "On-device text presence check", "Runs locally, so a frame with no legible text can be rejected without a network call."],
        ["Node.js 22", "The proxy", "No framework is used at all; the entire service is one file with no runtime dependencies, which makes its security surface auditable by reading it."],
      ],
    },
    tnum: "3.2",
    caption: "Platform and library choices, with the reason for each",
  },
];

const CH_REQUIREMENTS = [
  { h1: "4. Requirements Specification" },
  {
    p: "Requirements are stated here in the form used throughout development: an identifier, a " +
      "statement, a priority, and the method by which the requirement is verified. The " +
      "verification column is not decorative. A requirement with no stated verification method is " +
      "an aspiration, and several requirements in early drafts of this specification were " +
      "rewritten once it became clear that nothing in the system could demonstrate them.",
  },
  {
    table: {
      head: ["Priority", "Meaning"],
      widths: [1600, 7806],
      rows: [
        ["Must", "The system does not meet its problem statement without this."],
        ["Should", "Substantially degrades the system if absent, but the system remains useful."],
        ["Could", "Valuable, implemented where it was cheap, not defended if it conflicts with a Must."],
      ],
    },
    tnum: "4.1",
    caption: "Priority levels",
  },

  { h2: "4.1 Functional requirements: capture" },
  {
    table: {
      head: ["ID", "Requirement", "Pri.", "Verification"],
      widths: [800, 5000, 800, 2806],
      rows: [
        ["FR-01", "Record continuous audio for the duration of a session without gaps.", "Must", "AudioPipelineTest; field trial"],
        ["FR-02", "Divide audio into 20-second chunks that overlap the preceding chunk by 2 seconds.", "Must", "AudioPipelineTest"],
        ["FR-03", "Queue chunks in order and never drop one, even when transcription is slower than capture.", "Must", "AudioPipelineTest"],
        ["FR-04", "Remove text repeated across a chunk overlap without removing a genuine spoken repetition.", "Must", "TranscriptOverlapTest (9 cases)"],
        ["FR-05", "Sample the camera at a configurable interval in board mode.", "Must", "Field trial; device test"],
        ["FR-06", "Reject camera frames that do not differ materially from the last accepted board.", "Must", "FrameChangeDetectorTest"],
        ["FR-07", "Reject a frame whose scene has not yet settled, so a person crossing the board costs nothing.", "Must", "FrameChangeDetectorTest"],
        ["FR-08", "Compensate for global exposure change before scoring a frame.", "Should", "FrameChangeDetectorTest"],
        ["FR-09", "Allow the student to force a capture at any moment, overriding the filter.", "Should", "Device test; field trial 4"],
        ["FR-10", "Allow the sampling interval and sensitivity to be changed while a session is running.", "Must", "Device test; field trial 4"],
        ["FR-11", "Adapt the sampling interval and sensitivity to measured speech pace.", "Should", "SpeechPaceTrackerTest (10 cases)"],
        ["FR-12", "Reject a board image that has already been extracted, by perceptual fingerprint.", "Should", "BoardExtractionCoordinatorTest"],
        ["FR-13", "Assign each chunk a dominant speaker label, or mark it ambiguous.", "Could", "Unit test; field observation"],
        ["FR-14", "Retain raw audio when the network fails, and offer recovery afterwards.", "Should", "RetainedAudioRecoveryTest"],
        ["FR-15", "Continue capturing when AI calls fail, rather than ending the session.", "Must", "VoxBoxServiceFailureTest"],
      ],
    },
    tnum: "4.2",
    caption: "Functional requirements: capture",
  },

  { h2: "4.2 Functional requirements: note composition" },
  {
    table: {
      head: ["ID", "Requirement", "Pri.", "Verification"],
      widths: [800, 5000, 800, 2806],
      rows: [
        ["FR-16", "Write the note incrementally during the session, not after it.", "Must", "Field trial; revision counter visible on screen"],
        ["FR-17", "Send only bounded context to the composition model, regardless of note length.", "Must", "IncrementalNoteContextBuilderTest"],
        ["FR-18", "Accept only append-only deltas; never allow a model to rewrite existing text.", "Must", "GeneratedMarkdownRevisionTest"],
        ["FR-19", "Bind every delta to a SHA-256 of the content it was built against.", "Must", "GeneratedMarkdownRevisionTest"],
        ["FR-20", "Discard a delta whose revision no longer matches, and rebuild it.", "Must", "GeneratedMarkdownRevisionTest"],
        ["FR-21", "Accept an empty delta as a valid outcome when a chunk adds nothing.", "Must", "NoteRefinementParserTest; backend tests"],
        ["FR-22", "Discard generated text whose vocabulary does not match its evidence.", "Must", "Backend tests; grounding ratio"],
        ["FR-23", "Restrict AI writes to a single designated block per session.", "Must", "Schema constraint; unique index"],
        ["FR-24", "Record, for every generated block, which evidence produced it.", "Must", "block_provenance; NoteVerificationTest"],
        ["FR-25", "Run a verification pass against the evidence when the session ends.", "Should", "NoteVerificationTest; field log"],
        ["FR-26", "Present disagreements as review flags showing both readings.", "Must", "ReviewFlagParserTest"],
        ["FR-27", "Offer no action on a review flag that rewrites captured evidence.", "Must", "Design constraint; RemoveFlagTest"],
        ["FR-28", "Reject any timestamp the client did not itself emit.", "Must", "CaptureNoteFormattingTest"],
        ["FR-29", "Place board images near the text they relate to, not appended at the end.", "Should", "Unimplemented in the field; see Chapter 15"],
        ["FR-30", "Produce varied Markdown structure — headings, lists, tables, rules — not bullets alone.", "Should", "MarkdownTest; field trial 3"],
      ],
    },
    tnum: "4.3",
    caption: "Functional requirements: note composition",
  },

  { h2: "4.3 Functional requirements: reading, organising and export" },
  {
    table: {
      head: ["ID", "Requirement", "Pri.", "Verification"],
      widths: [800, 5000, 800, 2806],
      rows: [
        ["FR-31", "Render Markdown properly: headings, lists, quotes, code, tables and rules.", "Must", "MarkdownTest (15 cases)"],
        ["FR-32", "Display board crops inline with captions.", "Must", "DiagramLinkTest"],
        ["FR-33", "Open an image in a lightbox with both pinch and slider zoom.", "Should", "Device test; field trial 3"],
        ["FR-34", "Reject an asset path that escapes the note's asset directory.", "Must", "DiagramLinkTest"],
        ["FR-35", "Allow the student to edit any block the AI does not own.", "Should", "NoteBlockEditTest"],
        ["FR-36", "Rename a note.", "Must", "Device test"],
        ["FR-37", "Delete a note only after an explicit confirmation.", "Must", "Device test"],
        ["FR-38", "Support nested folders, at least two levels deep.", "Must", "Schema; device test"],
        ["FR-39", "Search notes by title and by block content.", "Should", "NoteSearchTest"],
        ["FR-40", "Export a note as Markdown with every asset it references.", "Must", "MarkdownExporterTest"],
        ["FR-41", "Deliver the export through the platform share sheet.", "Must", "Device test; field trial 1"],
        ["FR-42", "Keep exported evidence and exported refined note distinguishable.", "Should", "MarkdownExporterTest"],
      ],
    },
    tnum: "4.4",
    caption: "Functional requirements: reading, organising and export",
  },

  { h2: "4.4 Non-functional requirements" },
  {
    table: {
      head: ["ID", "Requirement", "Pri.", "Verification"],
      widths: [800, 5000, 800, 2806],
      rows: [
        ["NFR-01", "The AI provider credential must never exist on the device or in the APK.", "Must", "Architecture; secret scan on every commit"],
        ["NFR-02", "All network traffic must use HTTPS.", "Must", "Manifest; device test"],
        ["NFR-03", "Every pipeline route must require a bearer token, compared in constant time.", "Must", "Backend tests"],
        ["NFR-04", "The proxy must enforce a per-minute rate limit before any provider call.", "Must", "Backend tests"],
        ["NFR-05", "The proxy must enforce a daily budget ceiling.", "Must", "Backend tests"],
        ["NFR-06", "Captured media must be stored in application-private storage only.", "Must", "Code review; no MediaStore usage"],
        ["NFR-07", "The diagnostic log must never store speech in full.", "Must", "Code review; VbDebugLog"],
        ["NFR-08", "The diagnostic log must be absent from release builds.", "Must", "Build configuration"],
        ["NFR-09", "Cost per composition call must not grow with note length.", "Must", "Measured; Chapter 14"],
        ["NFR-10", "On-device filtering must reduce board calls by at least an order of magnitude.", "Must", "Measured: ~450 to ~40 per hour"],
        ["NFR-11", "A network failure must not end a session or lose captured audio.", "Must", "VoxBoxServiceFailureTest; field trial"],
        ["NFR-12", "A permanent failure must not be retried on a schedule.", "Must", "VoxBoxServiceFailureTest"],
        ["NFR-13", "The application must run on Android 8.0 and above.", "Must", "minSdk 26; device test"],
        ["NFR-14", "The live session screen must remain responsive while three pipelines run.", "Should", "Field trials 1 to 4"],
        ["NFR-15", "Every database schema change must have a tested migration.", "Must", "Migration tests; fallback disabled"],
        ["NFR-16", "Consent must be obtained before the first recording and must state what leaves the device.", "Must", "Onboarding; Figure I.3"],
        ["NFR-17", "No accuracy figure may be published that has not been measured.", "Must", "Editorial constraint on this report"],
        ["NFR-18", "The system must state, in the report, which of its features remain unproven in the field.", "Must", "Chapter 15; PROJECT_STATUS.md"],
      ],
    },
    tnum: "4.5",
    caption: "Non-functional requirements",
  },
  {
    note: "NFR-17 and NFR-18 are unusual requirements to place on a system, because they constrain " +
      "the report rather than the software. They are here because the most damaging thing this " +
      "project could produce is a confident claim it cannot support, and that failure mode is " +
      "exactly the one the software itself is built to prevent. Applying the standard to the " +
      "report as well as to the application seemed the minimum consistency.",
  },

  { h2: "4.5 Requirement status" },
  {
    table: {
      head: ["Status", "Count", "Notes"],
      widths: [3000, 1200, 5206],
      rows: [
        ["Implemented and verified on a device", "38", "Exercised on the physical phone, not merely compiled"],
        ["Implemented and unit-tested, unproven in the field", "9", "Listed individually in Chapter 15"],
        ["Partially implemented", "2", "FR-29 diagram placement; FR-13 speaker labelling at chunk granularity only"],
        ["Deferred to future work", "11", "Chapter 16, in priority order"],
      ],
    },
    tnum: "4.6",
    caption: "Requirement status at the time this report was generated",
  },
];

const CH_SCOPE = [
  { h1: "5. Scope of the Project" },

  { h2: "5.1 In scope" },
  {
    b: [
      "An Android application, minimum SDK 26, running on a student's own phone.",
      "Continuous audio capture with chunking, overlap and ordered delivery.",
      "Camera-based board capture with on-device change filtering and manual override.",
      "Incremental Markdown note composition during the session.",
      "Local persistence of all captured evidence, with provenance for every generated block.",
      "A hosted proxy service holding the AI provider credential and enforcing authentication, rate limits, budget and grounding.",
      "A reader that renders Markdown, displays board crops inline and surfaces review flags.",
      "Nested folders, search, rename, delete with confirmation, and export through the share sheet.",
      "A test suite covering the algorithms, the persistence layer, the network layer and the navigation graph.",
    ],
  },

  { h2: "5.2 Out of scope, and why" },
  {
    table: {
      head: ["Excluded", "Reason for the exclusion"],
      widths: [3000, 6406],
      rows: [
        ["iOS and web clients", "The capture pipeline is built directly on AudioRecord and CameraX. Porting means rewriting the half of the system that matters, and would not test any new idea."],
        ["Multi-device synchronisation", "Requires an account system, a server-side note store and conflict resolution across devices. Each is a project in itself, and none tests the hypothesis this project is about."],
        ["Collaborative or shared notes", "Same reason, with the addition of a permissions model."],
        ["Speaker identification by name", "Requires voice enrolment and stored voiceprints. Deliberately excluded on privacy grounds; the system reports that the voice changed and never who it is."],
        ["Diagram structure recognition", "Recognising that a shape is an entity, an arrow a relationship, a chart a plot, is a research problem. The system crops what a vision model calls a diagram and states plainly that it does not understand it."],
        ["Handwriting recognition beyond board text", "The vision model reads board text. Recognising a student's own handwritten annotations is a separate problem with a separate failure profile."],
        ["PDF and DOCX syllabus import", "Markdown and plain text only. Document parsing is well-solved elsewhere and adds no insight here."],
        ["Offline queueing of AI calls", "Loss of connectivity is handled by retaining audio and warning the student, not by deferring uploads. Deferred upload crosses the retained-audio recovery path and was judged too risky to add late."],
        ["Accuracy measurement", "Requires a labelled corpus and a scorer. Named as the first item of future work rather than quietly omitted."],
      ],
    },
    tnum: "5.1",
    caption: "Exclusions and the reason for each",
  },

  { h2: "5.3 Assumptions" },
  {
    n: [
      "The student has permission to record the lecture. The application requests consent and states what leaves the device; it cannot verify institutional policy.",
      "The phone is positioned so the board is within the camera frame for the duration of the session.",
      "Network connectivity is available for most of the session. Extended loss is handled by degradation, not by full offline operation.",
      "The lecture is delivered in a language the transcription model supports.",
      "One student, one device, one note. There is no shared session.",
    ],
  },

  { h2: "5.4 Constraints" },
  {
    table: {
      head: ["Constraint", "Consequence for the design"],
      widths: [3000, 6406],
      rows: [
        ["A student's budget", "Cost reduction is a first-class design concern, not an optimisation. The on-device frame filter exists because of this constraint and is the single largest saving in the system."],
        ["Phone battery and thermal limits", "Three concurrent pipelines over fifty minutes. Frame analysis runs on a 32 x 32 grid rather than a full frame specifically to keep this affordable."],
        ["The credential cannot ship in the APK", "Forces a proxy into the architecture. Everything in Chapter 10 follows from this one constraint."],
        ["The lecture cannot be paused or repeated", "No mechanism may block capture. Every failure path degrades to retaining evidence rather than stopping."],
        ["A single developer, a single term", "Scope is defended aggressively. The exclusions in Table 5.1 are the mechanism by which the parts that were built could be built properly."],
      ],
    },
    tnum: "5.2",
    caption: "Constraints and their design consequences",
  },

  { h2: "5.5 Deliverables" },
  {
    table: {
      head: ["Deliverable", "State"],
      widths: [4200, 5206],
      rows: [
        ["Android application (13,337 lines of Kotlin, 74 files)", "Built, installed and exercised on a physical device"],
        ["Backend proxy (dependency-free Node.js)", "Deployed, live, verified against /health"],
        ["Test suite (180 tests)", "Passing; lint clean"],
        ["Database schema (10 tables, 4 tested migrations)", "At version 5"],
        ["This report, with 21 generated diagrams and 21 device screenshots", "Complete"],
        ["Operations runbook (server/OPERATIONS.md)", "Complete: token rotation, error codes, rollback"],
        ["Honest status register (docs/PROJECT_STATUS.md)", "Maintained; the input to Chapters 15 and 16"],
      ],
    },
    tnum: "5.3",
    caption: "Deliverables",
  },
];

module.exports = { CH_INTRO, CH_PROBLEM, CH_LITERATURE, CH_REQUIREMENTS, CH_SCOPE };

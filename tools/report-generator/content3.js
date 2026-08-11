/** Testing, challenges, results, limitations, future work, conclusion, references. */

const CH_TESTING = [
  { h1: "12. Testing and Validation" },
  { h2: "12.1 Strategy" },
  {
    p: "Testing is organised by what each level can honestly establish. Unit tests prove logic; " +
      "instrumentation tests prove the application composes and navigates on a real device; " +
      "contract tests prove the server behaves correctly against a scripted provider; field trials " +
      "expose everything the other three cannot.",
  },
  {
    table: {
      head: ["Level", "Count", "What it establishes"],
      widths: [2600, 1600, 5600],
      rows: [
        ["Android unit tests", "134", "Algorithm correctness: frame filtering, pace adaptation, overlap trimming, Markdown parsing, flag parsing, export rendering, revision control"],
        ["Backend tests", "43", "Endpoint contracts, schema enforcement, authentication, rate limiting, budget, retry policy, grounding guard, delimiter normalisation"],
        ["Instrumentation tests", "3", "Every destination and every settings sub-page composes and returns on a physical device"],
        ["Static analysis", "Lint clean", "No outstanding lint errors in the debug variant"],
        ["Field trials", "4 sessions", "Behaviour against real speech, real boards and real network conditions"],
      ],
    },
  },
  { h2: "12.2 Representative Unit Tests" },
  {
    table: {
      head: ["Test", "Property established"],
      widths: [4200, 5600],
      rows: [
        ["globalExposureShiftIsDiscounted", "A uniform brightness change scores below 0.01 and is not treated as new board content"],
        ["personMovingThroughFrameIsNeverAccepted", "A figure crossing four consecutive frames and leaving never produces an upload"],
        ["aStableDiagramIsCapturedDespiteHandheldNoise", "A diagram seen through a shaking camera is accepted on the second frame"],
        ["repeatedSuggestionsDoNotCompound", "Repeated pace suggestions return identical settings rather than drifting"],
        ["bothKnobsAgreeAboutWhetherTheSpeakerIsFast", "Interval and threshold never describe opposite paces"],
        ["trimsTheRepeatedClauseAtASeam", "The duplicated clause at a chunk boundary is removed exactly"],
        ["aShortAccidentalMatchIsNotTrimmed", "A two-word coincidence does not delete real speech"],
        ["asterisksInsideMathAreNotEmphasis", "An asterisk inside an equation is preserved verbatim"],
        ["theCapturedLineInTheNoteBodyIsNeverTouched", "Resolving a review flag leaves the captured text unchanged"],
        ["aPathEscapingPrivateStorageIsRejected", "Traversal, absolute and scheme-prefixed asset paths all fail to resolve"],
        ["anInventedSectionIsDiscardedAndTheDiscardIsReported", "Ungrounded model output is dropped and the drop is visible"],
        ["anEmptyDeltaIsAValidAnswer", "A chunk that adds nothing produces an empty delta rather than an error"],
      ],
    },
  },
  { h2: "12.3 Field Trials" },
  {
    table: {
      head: ["Trial", "Material", "Outcome"],
      widths: [1800, 3400, 4600],
      rows: [
        ["1", "Recorded algebra lecture, three board states", "Complete note produced; four defects exposed"],
        ["2", "Live classroom, hand-drawn physics diagram", "Diagram captured with correct caption; duplicate crop and end-placement exposed"],
        ["3", "Spoken-word video, 2 minutes", "Transcription accurate against source; chunk-boundary loss identified"],
        ["4", "Instrumented session with diagnostic log", "Settle-gate defect localised precisely; adaptive tuning defect localised"],
      ],
    },
  },
  { h2: "12.4 Instrumentation as a Testing Tool" },
  {
    p: "The most productive testing instrument in this project was a temporary on-device " +
      "diagnostic log. Failures that matter here occur during a live lecture, on a phone, away " +
      "from a development machine, and reproducing them from a screenshot after the fact proved " +
      "consistently unsuccessful. The log records transcription outcomes per chunk, frame-filter " +
      "decisions with their scores and reasons, refinement failures with their cause, and " +
      "verification start and completion.",
  },
  {
    p: "It is written to be safe to share: transcribed speech is stored as a character count and " +
      "a sixty-character prefix, never in full, and no audio, image or note body is recorded. A " +
      "log that quietly accumulated a transcript would be a recording of a lecture by another " +
      "name and would fall outside every consent statement the application makes. The feature is " +
      "documented as temporary with an explicit removal checklist.",
  },
];

const CH_CHALLENGES = [
  { h1: "13. Challenges Encountered and Solutions" },
  {
    p: "This chapter records engineering problems that were genuinely difficult, including several " +
      "where the first solution was wrong and had to be replaced. They are recorded in full " +
      "because the corrections are the most instructive part of the work.",
  },
  { h2: "13.1 A Serialisation Defect that Silently Discarded Board Evidence" },
  {
    p: "Board evidence was being dropped from every refinement request. The cause was a single " +
      "Kotlin expression using an elvis operator on the result of a map put. Because put returns " +
      "the previous value for that key — null on first insertion — the elvis branch always " +
      "executed and overwrote the object just written with a JSON null.",
  },
  {
    p: "Resolution: an explicit conditional branch, plus a serialisation test asserting the field " +
      "is present. Confirmed end-to-end on a device by observing board evidence sections in the " +
      "produced note, which the server emits only when the field arrives populated.",
  },
  { h2: "13.2 A Model Timeout that Only Appeared at Realistic Scale" },
  {
    p: "The note model was selected on a small request that completed in 0.7 seconds. In a real " +
      "session, once the note had grown, the same model exceeded the 90-second timeout twice and " +
      "surfaced to the user as a generic service-unavailable message.",
  },
  {
    p: "Resolution: latency was re-measured at realistic note size rather than on a toy request, " +
      "and the model replaced with one approximately sixty times faster on the same task. The " +
      "wider lesson recorded in the project notes is that latency is a correctness property when a " +
      "timeout is involved, and benchmarks must use realistic inputs.",
  },
  { h2: "13.3 A Fix that Was Committed but Never Deployed" },
  {
    p: "Three separately reported defects — formulas rendering as raw text, notes that were far " +
      "too long, and a service error at the end of every session — persisted after being fixed. " +
      "Investigation against the live service showed it rejecting a request field that the current " +
      "code supported, and reporting a model identifier the current code no longer selected. The " +
      "repository was six commits ahead of the remote, and the hosting platform deploys from the " +
      "remote. The corrective work had never been deployed.",
  },
  {
    p: "Resolution: the commits were pushed and the deployment verified against /health before the " +
      "fix was reported as complete. The Connection screen was subsequently added so that the live " +
      "model routing is visible from the device, making this class of failure observable rather " +
      "than invisible. This is recorded in the project's persistent notes as a standing rule: a " +
      "green test suite says nothing about what is running.",
  },
  { h2: "13.4 Cost Located in the Wrong Place" },
  {
    p: "A measured running cost of approximately 0.18 US dollars per lecture-hour was investigated " +
      "on the assumption that verbose model output was responsible. It was not. A note update is " +
      "sent roughly every twenty seconds, so forwarded context is billed about 180 times an hour, " +
      "and the bounds in place allowed 48,000 characters per request — approximately 12,000 tokens " +
      "of input on each of 180 calls.",
  },
  {
    p: "Resolution: the outline was capped at 2,000 characters, the recent tail at 3,000 and " +
      "syllabus excerpts at 3,000, for approximately 8,000 in total, with server-side ceilings " +
      "above those as a backstop and a unit test asserting the bound. Delta output was capped at " +
      "1,200 tokens and the default detail level changed to concise. The lesson recorded is to " +
      "measure request size and multiply by call frequency before touching prompts.",
  },
  { h2: "13.5 An Instruction that Contradicted a Validator" },
  {
    p: "The concise instruction added during the cost work explicitly asks the model to return an " +
      "empty delta when a chunk adds nothing. The server treated an empty delta as a malformed " +
      "response, and so did the client. The result was a service-unavailable warning during live " +
      "sessions for behaviour that was entirely correct.",
  },
  {
    p: "Resolution: an empty delta is now a valid answer in delta mode and is passed through. An " +
      "empty full-note response is still rejected, because that would replace an entire note with " +
      "nothing. Both sides carry tests.",
  },
  { h2: "13.6 Fabricated Content from Thin Evidence" },
  {
    p: "Described in 2.2.4 and 7.6. The prompt already forbade invention. The resolution was to " +
      "stop relying on the prompt alone and add a server-side grounding check, on the principle " +
      "that an instruction is a request and a validator is a guarantee.",
  },
  { h2: "13.7 A Settle Test that Blocked Real Captures" },
  {
    p: "Described in detail in 7.1.2. The significant point for this chapter is that the defect " +
      "was invisible without instrumentation: the application appeared to be working, frames were " +
      "being evaluated, and the only symptom was that diagrams were sometimes missed. Only a " +
      "timestamped log of scores and decisions made the twenty-second stall obvious.",
  },
  { h2: "13.8 Compounding Adjustment in the Adaptive Controller" },
  {
    p: "Described in 7.2. Notable because the defect was visible only on a device and only as an " +
      "inconsistency between two displayed numbers. A controller that scales its own output " +
      "rather than a fixed baseline will drift, and the drift direction differed between the two " +
      "knobs because their clamps differ.",
  },
  { h2: "13.9 A Rendered Interface Element that Was Never Drawn" },
  {
    p: "Rename and delete were implemented, compiled, and reported as complete. They did not work. " +
      "The long-press handler set the state correctly, and the block that renders the dialogs had " +
      "been placed outside the composable during an automated edit. The code compiled, the test " +
      "suite passed, and the button silently did nothing.",
  },
  {
    p: "Resolution: the dialogs were moved inside the composable and rendered outside the scrolling " +
      "column so a sheet cannot be clipped by the list. The correction was reported explicitly " +
      "rather than quietly folded into the next change. The lesson is that neither compilation nor " +
      "unit tests can establish that a control is reachable, and only pressing it on a device can.",
  },
  { h2: "13.10 Platform Restrictions on Instrumentation" },
  {
    p: "Installing the instrumentation test package failed on the target device with a user " +
      "restriction error, blocking automated device testing entirely for several development " +
      "cycles. The cause was a vendor Android distribution which refuses test-package installation " +
      "over ADB unless USB installation is explicitly enabled and vendor optimisation disabled. " +
      "Once enabled the suite ran without modification.",
  },
  { h2: "13.11 Layout Defects Only Visible at Runtime" },
  {
    p: "A negative padding value passed to a Compose modifier compiles cleanly and throws at " +
      "runtime. This crashed every settings sub-page. The same mistake was made and corrected " +
      "twice in the same development session, which is recorded here as an argument for a lint " +
      "rule rather than vigilance.",
  },
];

const CH_RESULTS = [
  { h1: "14. Results" },
  { h2: "14.1 Functional Results" },
  {
    p: "The system captures a lecture end to end. In field trials it produced complete structured " +
      "notes from live speech, transcribed accurately against a known source, captured " +
      "hand-drawn board diagrams with descriptive captions, and completed an end-of-session " +
      "verification pass returning findings against the finished note.",
  },
  {
    table: {
      head: ["Measurement", "Value", "Source"],
      widths: [3600, 2400, 3800],
      rows: [
        ["Frames captured per lecture-hour at 8 s interval", "approximately 450", "Interval arithmetic"],
        ["Frames actually sent after filtering", "approximately 40", "Change-filter behaviour"],
        ["Transcription latency, per chunk", "1.3 – 2.1 s", "Live probe against the deployed service"],
        ["Note refinement latency, per chunk", "1.3 – 2.1 s", "Live probe"],
        ["Verification latency, whole note", "1.6 – 12.6 s", "Field log"],
        ["Forwarded context per note update, before", "up to 48,000 characters", "Code inspection"],
        ["Forwarded context per note update, after", "at most 8,000 characters", "Asserted by unit test"],
        ["Android unit tests", "134 passing", "Gradle test report"],
        ["Backend tests", "43 passing", "node:test"],
        ["Instrumentation tests", "3 passing", "Device run"],
        ["Application source", "13,337 lines across 74 files", "Repository"],
        ["Test source", "2,574 lines across 32 files", "Repository"],
        ["Backend source and tests", "2,939 lines", "Repository"],
      ],
    },
  },
  { h2: "14.2 Accuracy" },
  {
    p: "No accuracy percentage is claimed. Word error rate, diarization error, optical character " +
      "recognition accuracy and diagram-crop overlap have not been measured against a labelled " +
      "corpus, because no such corpus and scorer exist for this project yet. Constructing them is " +
      "the first item of future work. Quoting a figure derived from a small number of favourable " +
      "runs would misrepresent the state of the system.",
  },
  {
    p: "What can be stated is qualitative and was verified by direct comparison against source " +
      "material: in a two-minute spoken-word trial the transcript matched the source closely, and " +
      "the loss that did occur was localised precisely to chunk boundaries and subsequently " +
      "addressed by the overlap mechanism described in 7.5.",
  },
  { h2: "14.3 Cost" },
  {
    p: "The first measured cost was approximately 0.18 US dollars per lecture-hour. Following the " +
      "context reduction described in 13.4, forwarded context per note update fell by " +
      "approximately six times at the ceiling. A revised per-hour figure is deliberately not " +
      "stated: the payload reduction is measured and asserted by test, but the end-to-end cost has " +
      "not been re-measured across a full lecture, and stating a projection as a measurement would " +
      "be inconsistent with the claims discipline described in 11.3.",
  },
];

const CH_LIMITATIONS = [
  { h1: "15. Limitations and Known Issues" },
  {
    p: "Stated plainly, because a limitation that is known and documented is a different thing " +
      "from one that is discovered by a user.",
  },
  {
    table: {
      head: ["Limitation", "Detail", "Status"],
      widths: [2800, 4800, 2200],
      rows: [
        ["No accuracy measurement", "No labelled corpus or scorer exists; no accuracy figure is claimed anywhere in the system or this report", "Future work"],
        ["No diagram interpretation", "The system crops regions a vision model identifies. It has no understanding of arrows, chart types or entity-relationship structure", "Future work"],
        ["Diagram placement unverified", "Crops are attached to the note update generated from the same frame, but no field session has yet produced an accepted frame under the corrected filter", "Unverified"],
        ["Chunk-local speaker labels only", "No identity is carried across chunks; no voiceprint is stored", "By design"],
        ["Twenty-second granularity", "Not suitable for live captioning", "By design"],
        ["Single-device, single-user", "No accounts, no synchronisation, no sharing beyond export", "By design"],
        ["Free hosting tier cold start", "The proxy may take up to a minute to wake after inactivity, which appears as a slow first request", "Accepted"],
        ["Client token in the APK", "Recoverable by anyone holding the APK; bounded by rate limit and daily budget rather than prevented", "Accepted and bounded"],
        ["Temporary diagnostic log present", "Must be removed before any wider distribution; removal checklist documented", "Scheduled"],
        ["Platform fonts", "Downloadable fonts require certificates not present in the repository and fall back mid-layout on a cold offline start, so platform families are used", "Accepted"],
      ],
    },
  },
];

const CH_FUTURE = [
  { h1: "16. Future Implementation" },
  {
    p: "This chapter is deliberately structured as a live section of the report. The project is " +
      "not complete, and each item below is written so that it can be moved into the implemented " +
      "chapters as the work is done. Items are ordered by the value they would add.",
  },
  { h2: "16.1 Measured Evaluation" },
  {
    p: "Construct fixed corpora — noisy classroom audio, photographed board images, and " +
      "hand-labelled reference notes — together with a scorer, and publish word error rate, " +
      "diarization error, optical character recognition accuracy and diagram-crop overlap. This is " +
      "first because every other claim the project makes is currently qualitative, and because it " +
      "converts the honest \"not yet measured\" position into evidence.",
  },
  { h3: "Status" },
  { p: "Not started. Reserved for update." },
  { h2: "16.2 Diagram Structure Extraction" },
  {
    p: "Extend the board schema with a diagram kind — flowchart, graph, entity-relationship, " +
      "circuit, free-body, table — and per-kind structural extraction, so that a diagram enters " +
      "the note as content rather than only as a picture. For a flowchart that means nodes and " +
      "edges; for a graph, axes and series; for an entity-relationship diagram, entities and " +
      "cardinalities. A note that carries the structure of a diagram is searchable and revisable " +
      "in a way that an image is not.",
  },
  { h3: "Status" },
  { p: "Not started. Schema design drafted only. Reserved for update." },
  { h2: "16.3 On-Device Diagram Pre-Filter" },
  {
    p: "Add an edge-density and line-segment heuristic on the device so that a crop which is " +
      "clearly a wall, a face or empty space can be rejected before a network call is spent. This " +
      "addresses the weakest part of the current pipeline, which relies entirely on a prompt " +
      "instruction to define what a diagram is.",
  },
  { h3: "Status" },
  { p: "Not started. Reserved for update." },
  { h2: "16.4 Consent-Aware Speaker Identity" },
  {
    p: "Voice embeddings with an explicit enrolment step and cross-chunk clustering, so that a " +
      "speaker can be tracked across a whole lecture rather than per chunk. This must not ship " +
      "without the consent flow, since it converts a chunk-local heuristic into biometric data.",
  },
  { h3: "Status" },
  { p: "Not started. Reserved for update." },
  { h2: "16.5 Full-Note Consolidation Pass" },
  {
    p: "Updates today are append-only, which is what makes the cost flat and the evidence safe. " +
      "A deliberate end-of-lecture consolidation pass could reorganise and de-duplicate the whole " +
      "note as a separate, explicit step that the student initiates and can reject.",
  },
  { h3: "Status" },
  { p: "Not started. Reserved for update." },
  { h2: "16.6 Note Refinement Batching" },
  {
    p: "Accumulate several audio chunks into a single note update rather than refining every " +
      "twenty seconds. This is the largest remaining cost lever, estimated at a threefold " +
      "reduction on the note side. It was not implemented because it touches the retained-audio " +
      "recovery path, which is safety-critical, and the surrounding work did not leave room to do " +
      "it carefully.",
  },
  { h3: "Status" },
  { p: "Not started. Reserved for update." },
  { h2: "16.7 Additional Items" },
  {
    b: [
      "PDF and DOCX syllabus import.",
      "Whole-vault export.",
      "Battery and long-session profiling.",
      "Per-user sign-in so that backend access is revocable per device.",
      "Offline queueing so a session survives a total loss of connectivity and uploads later.",
      "Removal of the temporary diagnostic log.",
    ],
  },
  { h3: "Status" },
  { p: "Not started. Reserved for update." },
];

const CH_CONCLUSION = [
  { h1: "17. Conclusion" },
  {
    p: "The project set out to convert a live lecture into a structured, revisable note without " +
      "discarding the evidence behind it. That system exists, runs on a physical device against a " +
      "deployed backend, and has been exercised against real lectures.",
  },
  {
    p: "The parts of the work that took the most engineering are not the model calls. They are the " +
      "on-device frame filter that makes board capture affordable, the append-only delta protocol " +
      "that keeps cost flat and prevents silent rewriting, the recovery paths that ensure captured " +
      "speech survives a provider failure, the overlap mechanism that stopped words disappearing " +
      "at chunk boundaries, the adaptive controller that keeps sampling matched to the speaker, " +
      "and the grounding guard that stops fabricated content reaching a study note. The AI " +
      "provider has been migrated once during development and none of that logic changed.",
  },
  {
    p: "The project's most useful outcome may be methodological rather than technical. Repeatedly, " +
      "a defect was invisible to compilation, invisible to a passing test suite, and obvious " +
      "within seconds of instrumenting the running system on a real device. A commit is not a " +
      "release; a compiled control is not a reachable one; a benchmark on a small input is not a " +
      "measurement. Each of those was learned here by getting it wrong first, and each is recorded " +
      "in Chapter 13 rather than quietly corrected.",
  },
  {
    p: "What remains is stated without softening. Accuracy is unmeasured. Diagram interpretation " +
      "does not exist. The system is a working, tested, deployed foundation with a clearly " +
      "defined and honestly bounded set of things it cannot yet do.",
  },
];

const CH_REFERENCES = [
  { h1: "18. References" },
  {
    n: [
      "Android Developers. Jetpack Compose documentation. Google LLC.",
      "Android Developers. CameraX overview and ZoomState reference. Google LLC.",
      "Android Developers. Room persistence library and migration guide. Google LLC.",
      "Android Developers. DataStore Preferences guide. Google LLC.",
      "Android Developers. AudioRecord and audio effects reference. Google LLC.",
      "Android Developers. FileProvider and secure file sharing. Google LLC.",
      "Material Design 3 guidelines. Google LLC.",
      "Kotlin language documentation and coroutines guide. JetBrains.",
      "kotlinx.serialization documentation. JetBrains.",
      "Node.js documentation, HTTP module and node:test runner. OpenJS Foundation.",
      "OpenRouter API documentation, chat completions and structured outputs.",
      "JSON Schema specification, draft 2020-12.",
      "Render platform documentation, Docker services and blueprints.",
      "Google ML Kit Text Recognition documentation.",
      "CommonMark specification, referenced for Markdown parsing semantics.",
    ],
  },
];

module.exports = { CH_TESTING, CH_CHALLENGES, CH_RESULTS, CH_LIMITATIONS, CH_FUTURE, CH_CONCLUSION, CH_REFERENCES };

/**
 * Further sections appended to chapters 1, 3, 8, 10 and 14.
 */

const INTRO_EXTRA = [
  { h2: "1.6 Terminology used in this report" },
  {
    p: "Five terms are used throughout with a specific meaning that differs from their ordinary " +
      "one. They are defined here rather than only in the glossary because misreading any of them " +
      "changes what the following chapters appear to claim.",
  },
  {
    table: {
      head: ["Term", "Meaning here", "What it does not mean"],
      widths: [1800, 4200, 3406],
      rows: [
        ["Evidence", "Anything the system captured rather than generated: transcript segments, board text, board images.", "Not proof of anything. It is the record of what was heard and seen, which may itself be wrong."],
        ["Delta", "The new Markdown a composition call returns, to be appended to the note.", "Not a diff. Nothing is ever replaced, so there is no removal side."],
        ["Living block", "The single note block a session's AI output may extend.", "Not the whole note. Every other block is the student's and is unreachable by any model."],
        ["Grounded", "Sharing enough vocabulary with the evidence to pass a measured threshold.", "Not true, and not verified. A grounded claim can still be wrong; it simply is not invented from nothing."],
        ["Diagram", "A region of the board that a vision model reported as containing one, cropped and captioned.", "Not an understood diagram. Nothing in this system knows what an arrow means."],
      ],
    },
    tnum: "1.3",
    caption: "Terms with a specific meaning in this report",
  },
  {
    p: "The last two rows carry the most weight. \"Grounded\" is a measured lexical property and " +
      "not a claim of correctness, and \"diagram\" describes a crop rather than an interpretation. " +
      "Both are restated in Chapters 7 and 15 because both are easy to read as stronger claims " +
      "than they are.",
  },
];

const LIT_EXTRA = [
  { h2: "3.8 Evaluating the alternatives against this problem" },
  {
    p: "Section 3.6 compared capabilities. This section scores the same five classes against the " +
      "six failures of section 2.2, which is a stricter test: a tool can have a capability and " +
      "still not close the failure it appears to address.",
  },
  {
    table: {
      head: ["Failure (from 2.2)", "Recorder", "Transcription", "Scanner", "AI assistant", "VoxBox"],
      widths: [3106, 1200, 1500, 1200, 1300, 1100],
      rows: [
        ["Speech is transient", "closed", "closed", "open", "closed", "closed"],
        ["Speech is not revisable", "open", "partial", "n/a", "closed", "closed"],
        ["Board is erased", "open", "open", "partial", "open", "closed"],
        ["Board photos are unstructured", "n/a", "n/a", "open", "n/a", "closed"],
        ["Channels not aligned", "open", "open", "open", "open", "closed"],
        ["Naive automation is unaffordable", "n/a", "open", "n/a", "open", "closed"],
        ["Output can be confidently wrong", "n/a", "partial", "n/a", "open", "partial"],
        ["Notes accumulate unorganised", "open", "open", "open", "open", "closed"],
      ],
    },
    tnum: "3.3",
    caption: "Each failure, scored against each class of tool",
  },
  {
    p: "Three rows are worth reading carefully rather than counting.",
  },
  {
    p: "\"Board is erased\" is only partial for a scanning application, because a scanner preserves " +
      "the board a student chose to photograph, and the whole difficulty is that choosing costs " +
      "attention during the lecture. A tool that preserves what you remembered to preserve has not " +
      "closed a failure about the cost of remembering.",
  },
  {
    p: "\"Output can be confidently wrong\" is only partial for this project, and that is the " +
      "honest score. The grounding guard rejects text with no lexical support in the evidence; it " +
      "does not detect a claim that is fluently wrong while using the right vocabulary, and its " +
      "threshold was set from two negative examples. The review flags and the retained evidence " +
      "give a student the means to check, which is more than any tool in the other four columns " +
      "offers, and is not the same as the failure being closed.",
  },
  {
    p: "\"Naive automation is unaffordable\" is marked n/a for the recorder and the scanner because " +
      "neither automates anything, which is precisely why neither has the problem and neither " +
      "solves the ones that require automation.",
  },

  { h2: "3.9 What this project takes from each" },
  {
    table: {
      head: ["Class", "What was adopted", "What was rejected, and why"],
      widths: [2200, 3400, 3806],
      rows: [
        ["Audio recorders", "Capture must never stop, and must degrade to retaining raw audio rather than failing.", "Retaining only raw audio. It is durable and unusable."],
        ["Transcription services", "Chunked upload, and treating the transcript as a first-class stored artefact.", "Treating the transcript as the output. A transcript is evidence, not a note."],
        ["Scanning applications", "Perspective and contrast handling before upload; the value of a good crop.", "Manual capture as the only route. It is retained as an override rather than as the mechanism."],
        ["AI note assistants", "Structured Markdown as the output format; a note policy that varies the level of detail.", "Post-hoc generation, and discarding the evidence. Both are the reason a wrong note is undetectable."],
        ["Institutional capture", "The idea that both channels should be recorded together.", "Video as the artefact. A student who must watch a lecture again has not been helped."],
      ],
    },
    tnum: "3.4",
    caption: "What was taken from each class of existing tool, and what was deliberately not",
  },
  {
    p: "The pattern across that table is that the useful contribution of each existing class is " +
      "usually its capture mechanism, and the part that does not transfer is usually what it does " +
      "with what it captured. That is a reasonable summary of where this project sits: the capture " +
      "ideas are largely borrowed, and the handling of the captured material is where the work is.",
  },
];

const IMPL_EXTRA = [
  { h2: "8.9 Build configuration" },
  {
    table: {
      head: ["Setting", "Value", "Reason"],
      widths: [2600, 2400, 4406],
      rows: [
        ["compileSdk / targetSdk", "36", "Current platform; targets the behaviour changes the application was tested against"],
        ["minSdk", "26", "Android 8.0. Covers the devices students actually carry without inheriting the storage and permission model of earlier releases"],
        ["Kotlin", "2.2", "Language version in use across the project"],
        ["Java target", "17", "Required by the Compose compiler in use"],
        ["Compose BOM", "Managed via version catalogue", "One place where every Compose artefact version is decided"],
        ["Room", "2.7, KSP", "KSP rather than kapt: annotation processing is faster and the generated code is easier to read"],
        ["exportSchema", "true", "Schema JSON is written to the repository, so a migration test can be written against the previous version"],
        ["fallbackToDestructiveMigration", "disabled", "A missing migration must fail the build rather than delete a student's notes"],
        ["Release signing", "Debug keystore (temporary)", "A demonstration arrangement. Listed as known defect 3 in Table 15.3"],
        ["Diagnostic log", "Debug builds only", "Compiled out of release; removal checklist tracked separately"],
      ],
    },
    tnum: "8.3",
    caption: "Build configuration, and the reason for each choice",
  },
  {
    p: "The version catalogue is worth one sentence of justification. Dependency versions are " +
      "declared once in gradle/libs.versions.toml rather than in each module's build file, which " +
      "in a single-module project buys nothing today and makes the dependency inventory in the " +
      "next section a file rather than a search.",
  },

  { h2: "8.10 Dependency inventory" },
  {
    table: {
      head: ["Dependency", "Purpose", "Could it be removed?"],
      widths: [2800, 3400, 3206],
      rows: [
        ["androidx.compose (BOM)", "All UI", "No. The UI layer is Compose."],
        ["androidx.navigation:navigation-compose", "The navigation graph", "Possible but not worthwhile; sixteen destinations need a graph."],
        ["androidx.room", "Persistence and migrations", "No. Hand-written SQLite would remove compile-time query checking, which is the property this project relies on most."],
        ["androidx.datastore:datastore-preferences", "Settings", "Yes, at the cost of writing an asynchronous preference store by hand."],
        ["androidx.camera (CameraX)", "Board capture and zoom", "No. Camera2 directly would reintroduce the lifecycle defects CameraX removes."],
        ["com.google.mlkit:text-recognition", "On-device text presence check", "Yes. The check is an optimisation; removing it costs vision calls on frames with no text."],
        ["kotlinx.coroutines", "Three concurrent pipelines", "No."],
        ["JUnit4, Compose UI Test, Espresso", "Tests", "No."],
      ],
    },
    tnum: "8.4",
    caption: "Every third-party dependency in the application",
  },
  {
    p: "Eight dependencies, of which six are first-party Android libraries. The proxy, by " +
      "contrast, has none at all. That asymmetry is deliberate and is argued in Chapter 10: the " +
      "application runs on a device where the platform libraries are already present and " +
      "maintained, and the proxy holds a credential in an environment where every additional " +
      "package is an additional way to lose it.",
  },

  { h2: "8.11 Performance considerations" },
  {
    table: {
      head: ["Concern", "Treatment", "Measured?"],
      widths: [2600, 4400, 2406],
      rows: [
        ["Frame analysis on the main thread", "Runs on a background executor; the main thread never touches a frame", "Observed: no dropped frames in the preview during trials"],
        ["Full-frame image processing", "Analysis runs on a 32 x 32 downsample, not the full frame", "By construction: 1,024 comparisons per analysis"],
        ["Audio buffer copies", "The overlap carry is a single in-place copy per chunk, not a reallocation", "By construction"],
        ["Database writes during capture", "Evidence rows are written once and never updated; appends are one transaction", "By construction"],
        ["Recomposition on the live screen", "State is exposed as a single immutable snapshot per frame of UI, so a timer tick does not recompose the preview", "Observed"],
        ["Peak memory", "One held frame plus one incoming frame; crops are written to disk immediately", "Not measured"],
        ["Battery and thermal over 50 minutes", "No specific treatment beyond the above", "Not measured"],
      ],
    },
    tnum: "8.5",
    caption: "Performance treatments, and which of them have been measured",
  },
  {
    note: "Two rows of that table say 'not measured', and they are the two that matter most to a " +
      "student running the application through a full timetable. Both are listed in Chapter 16 as " +
      "planned work. No claim about battery or thermal behaviour is made anywhere in this report.",
  },

  { h2: "8.12 Code metrics" },
  {
    table: {
      head: ["Metric", "Value", "Note"],
      widths: [3400, 2000, 4006],
      rows: [
        ["Application source", "13,337 lines", "74 Kotlin files"],
        ["Test source", "2,574 lines", "32 files"],
        ["Test-to-source ratio", "~1:5", "Concentrated on the domain layer rather than spread evenly"],
        ["Backend source and tests", "2,939 lines", "Two files, no dependencies"],
        ["Largest single file", "~490 lines", "A Compose screen; no algorithm file exceeds 300"],
        ["Domain layer", "~1,100 lines", "The six algorithms. Roughly 8 per cent of the application, and the target of half its tests"],
        ["Temporary code", "337 lines", "The diagnostic component, scheduled for removal"],
        ["Lint", "Clean", "No suppressions in the domain layer"],
      ],
    },
    tnum: "8.6",
    caption: "Code metrics",
  },
  {
    p: "The row worth noticing is the domain layer: roughly eight per cent of the application " +
      "carries the project's own logic and attracts about half of the tests. That distribution is " +
      "the practical form of the layering rule in Chapter 6 — the part that is hard to get right " +
      "was deliberately made small, dependency-free and testable without a device.",
  },
];

const BACKEND_EXTRA = [
  { h2: "10.6 The life of a request" },
  {
    p: "One composition request traced through the proxy, in the order the checks actually run. " +
      "The ordering is deliberate: every check that can reject cheaply runs before any check that " +
      "costs money.",
  },
  {
    code: "1.  Method and path matched                     -> 404 if unknown\n" +
      "2.  Authorization header present                 -> 401\n" +
      "3.  Bearer token compared in constant time       -> 401\n" +
      "4.  Body size under the ceiling                  -> 413\n" +
      "5.  Body parsed as JSON                          -> 400\n" +
      "6.  Request validated against the route schema   -> 400\n" +
      "7.  Rate window checked and incremented          -> 429\n" +
      "8.  Daily budget checked and incremented         -> 503\n" +
      "    ------------------------------------------------  everything above is free\n" +
      "9.  Provider called with a strict json_schema\n" +
      "10. Response validated against that schema       -> 502\n" +
      "11. Grounding ratio computed against the evidence -> delta discarded if below the floor\n" +
      "12. Response returned",
    caption: "The order of checks on a composition request",
  },
  {
    p: "The line at step 8 is the important one. Everything above it costs the proxy's own CPU and " +
      "nothing else, which means an unauthenticated caller, a malformed client or a burst cannot " +
      "produce a bill. Everything below it has already been paid for by the time it can fail, " +
      "which is why steps 10 and 11 return a specific status rather than a generic failure: the " +
      "client needs to distinguish 'retry this' from 'this will never work'.",
  },
  {
    p: "Steps 2 to 4 also run before the body is fully read. A request with no valid token is " +
      "rejected without the proxy buffering however many megabytes of audio it claimed to be " +
      "sending.",
  },

  { h2: "10.7 Observability" },
  {
    table: {
      head: ["Signal", "Where", "What it answers"],
      widths: [2600, 2600, 4206],
      rows: [
        ["/health models block", "Proxy, unauthenticated", "Is the deployed code the code I think it is?"],
        ["/health budget block", "Proxy", "How much of today's budget is left?"],
        ["Structured request log", "Platform logs", "Which route, which status, how long — never the body"],
        ["Rejection counters", "In memory, exposed on /health", "Are requests failing at auth, at the rate limit, or at the provider?"],
        ["Grounding rejections", "Logged with the ratio, never the text", "Is the guard firing, and how close were the rejections?"],
        ["On-device diagnostic log", "The phone, temporarily", "What did the capture pipeline actually decide, and on what numbers?"],
      ],
    },
    tnum: "10.3",
    caption: "Observability signals",
  },
  {
    p: "No request or response body is ever logged on the proxy. That constraint is the reason the " +
      "grounding rejection log records the ratio and not the delta: a log line containing the " +
      "generated text would contain the lecture, in an environment the student does not control.",
  },
  {
    p: "The on-device log is the last row, and its presence in this table is temporary. It exists " +
      "because the questions in that row could not be answered any other way during field trials 2 " +
      "and 3, and it is scheduled for removal once the placement and duplicate-rejection " +
      "behaviours in Table 15.1 have been confirmed.",
  },

  { h2: "10.8 Operational failure modes" },
  {
    table: {
      head: ["Symptom", "Likely cause", "Action"],
      widths: [2800, 3000, 3606],
      rows: [
        ["First request of the day takes ~13 s", "Free-tier cold start", "None. Documented as accepted risk 6 in Table 15.3."],
        ["Every request returns 401", "Client token rotated on the server but not in the build", "Rebuild the application, or revert the environment variable."],
        ["Every request returns 503", "Daily budget exhausted", "Wait for the reset, or raise the budget variable and redeploy."],
        ["Composition returns 502 repeatedly", "Provider returning prose where the schema requires structure", "Check /health model routing; a provider-side model change is the usual cause."],
        ["A fix appears to have no effect", "The commit was not pushed, or the deploy did not complete", "Read the models block on /health, or the Connection screen on the phone. This has happened."],
        ["Notes stop growing mid-lecture but capture continues", "The session is DEGRADED", "Expected behaviour. Evidence is being retained; recover the audio afterwards."],
      ],
    },
    tnum: "10.4",
    caption: "Operational symptoms and their usual causes",
  },
];

const RESULTS_EXTRA = [
  { h2: "14.5 Measurements in detail" },
  {
    p: "The figures quoted in Tables 14.1 to 14.3, with how each was obtained. Anything that was " +
      "derived rather than observed is marked as derived, and anything that has not been measured " +
      "does not appear.",
  },
  {
    table: {
      head: ["Measurement", "Value", "How it was obtained"],
      widths: [3200, 2000, 4206],
      rows: [
        ["Candidate frames per lecture-hour", "~450", "Observed: sampling interval of 8 s over a 50-minute session"],
        ["Frames accepted per lecture-hour", "~40", "Observed across trials, extrapolated from shorter instrumented runs"],
        ["Field trial 4 acceptance", "3 kept, 9 skipped in 92 s", "Directly observed on the device; Figure 9.7"],
        ["Settle scores on a static board", "0.09 to 0.17", "Observed: 40 consecutive samples in the field log"],
        ["Corrected gate on that data", "0.096", "Derived: 0.45 x the 0.214 hold score recorded in the same log"],
        ["Composition request size, bounded", "~14,000 chars", "Observed from request logging"],
        ["Composition request size, unbounded", "~24,000 chars at minute 40", "Observed before the context caps were added"],
        ["Cost reduction from bounding", "~2/3 of a 50-minute lecture", "Observed: measured before and after"],
        ["Per-call ratio at minute 50", "~17x", "Derived from the two request sizes and their growth curves"],
        ["Verbosity reduction attempt", "~4 per cent", "Observed: the failed first attempt described in Chapter 13.9"],
        ["Honest grounding ratios", "0.34 to 0.71", "Observed across four trials"],
        ["Fabricated passage ratios", "0.08 and 0.11", "Observed: two passages, found on review"],
        ["Pace tracker drift, first version", "8.0 s to 2.0 s in 4 min", "Observed in the field log"],
        ["Words trimmed per lecture as overlap", "~600", "Derived: ~4 words at each of ~149 seams"],
      ],
    },
    tnum: "14.4",
    caption: "Every quantity quoted in this report, and its origin",
  },
  {
    p: "Two entries deserve a caveat that the table cannot carry. The grounding ratios rest on " +
      "four sessions and two negative examples, which is enough to place a threshold and not " +
      "enough to characterise a distribution. And the ~40 accepted frames per lecture-hour is an " +
      "extrapolation from shorter instrumented runs, because no full fifty-minute board session " +
      "has been run with the corrected settle test in place. Both are stated here rather than " +
      "presented as firmer than they are.",
  },

  { h2: "14.6 What changed between the first build and this one" },
  {
    table: {
      head: ["Property", "First working build", "Now", "Cause"],
      widths: [2600, 2200, 2200, 2406],
      rows: [
        ["Board frames captured in the field", "0", "3 in 92 s", "Relative settle test"],
        ["Words lost at chunk seams", "~1 per 20 s", "0", "Two-second overlap"],
        ["Markdown in the reader", "Shown as source", "Rendered", "243-line parser, 292-line renderer"],
        ["Export", "Did nothing", "ZIP with assets", "FileProvider"],
        ["Composition request at minute 40", "~24,000 chars", "~14,000 chars", "Context bounds"],
        ["Cost of a 50-minute lecture", "Baseline", "~1/3 of baseline", "Context bounds, empty-delta acceptance"],
        ["Sampling parameters mid-session", "Fixed at start", "Adjustable, and adaptive", "Tuning panel, pace tracker"],
        ["Fabricated content reaching the note", "Possible", "Rejected below a measured floor", "Grounding guard"],
        ["Notes organisable", "Flat list", "Nested folders, search", "Field trial 1 feedback"],
        ["Deployment verifiable from the phone", "No", "Yes", "/health models block"],
      ],
    },
    tnum: "14.5",
    caption: "What changed, and what caused each change",
  },
  {
    p: "Every row in that table originates in a field trial rather than in a planned iteration. " +
      "That is the honest summary of how this system reached its current state: the design was " +
      "reasonable, the implementation followed it, and almost every improvement came from carrying " +
      "the result into a room where a lecture was happening and watching what it did.",
  },
];

module.exports = { INTRO_EXTRA, LIT_EXTRA, IMPL_EXTRA, BACKEND_EXTRA, RESULTS_EXTRA };

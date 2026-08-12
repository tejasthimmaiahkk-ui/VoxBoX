/**
 * Chapters 12 to 18: testing, challenges, results, limitations, future work, conclusion,
 * references.
 */

const CH_TESTING = [
  { h1: "12. Testing and Validation" },
  {
    p: "180 automated tests run on every build: 134 Android unit tests, 43 backend contract tests " +
      "and 3 instrumentation tests on a physical device. This chapter describes the strategy, and " +
      "then spends more space on what the strategy did not catch than on what it did, because that " +
      "is where the useful information is.",
  },

  { h2: "12.1 Strategy" },
  { fig: "docs/diagrams/fig-testing.png", num: "12.1", caption: "Test distribution, what the suite missed and what was added because of it", opts: { maxH: 780 } },
  {
    p: "Testing is organised by what each level can honestly establish. Unit tests prove logic; " +
      "instrumentation tests prove that the application composes and navigates on a real device; " +
      "contract tests prove that the server behaves correctly against a scripted provider; field " +
      "trials expose everything the other three cannot.",
  },
  {
    p: "The shape of the suite is deliberate. The algorithms carry the project's risk, so they " +
      "carry the tests: the six algorithms of Chapter 7 account for roughly half of all unit " +
      "tests. They are pure functions over plain data — a consequence of the layering rule in " +
      "Chapter 6 — so they need no emulator, no device and no network, and the whole unit suite " +
      "runs in under a minute.",
  },
  {
    p: "The backend is tested through its contract rather than its internals. Forty-three tests " +
      "exercise authentication, the constant-time comparison, the rate window, the daily budget, " +
      "schema rejection, the grounding ratio, empty-delta acceptance and every error mapping in " +
      "Table F.3. They use Node's own test runner, which is consistent with the service having no " +
      "dependencies at all.",
  },
  {
    p: "The user interface is verified on a real device rather than mocked, and only three tests " +
      "do it. Each of the three exists because of a specific field failure, described in section " +
      "12.3.",
  },

  { h2: "12.2 What the suite is good at" },
  {
    table: {
      head: ["Concern", "Coverage", "Confidence"],
      widths: [3200, 3000, 3206],
      rows: [
        ["The six algorithms", "~70 unit tests", "High. Every constant has a test at its boundary."],
        ["Markdown parsing and rendering", "15 tests", "High. Includes malformed input taken from a real note."],
        ["Room schema and migrations", "Migration tests, fallback disabled", "High. A missing migration fails the build."],
        ["Network error handling", "12 tests", "High. Every code in Table F.3 maps to a verified behaviour."],
        ["Proxy contract", "43 tests", "High. Auth, limits and grounding are all exercised."],
        ["Export correctness", "5 tests", "Medium. Verified structurally, never against a populated note in the field."],
        ["Navigation reachability", "3 device tests", "Medium. Covers the graph, not every state within a screen."],
      ],
    },
    tnum: "12.1",
    caption: "Coverage by concern, with an honest confidence assessment",
  },

  { h2: "12.3 What the suite did not catch" },
  {
    p: "Four defects reached a real lecture with the entire suite green. They are worth listing " +
      "individually, because they are four different failure modes and only one of them is a " +
      "coverage gap.",
  },
  {
    table: {
      head: ["Defect", "Why the suite could not see it", "Class of failure"],
      widths: [2600, 4400, 2406],
      rows: [
        ["The settle gate never opened, so no board frame was ever captured", "Tests fed synthetic luminance arrays containing no hand tremor. Real frames from a handheld phone score an order of magnitude higher at rest.", "Unrealistic test input"],
        ["Rename and delete did nothing", "The dialogs compiled and their state changed correctly. The render block sat outside the composable, so nothing drew. No unit test can observe 'was drawn'.", "Untestable without a device"],
        ["Six settings pages crashed on open", "A negative Modifier.padding compiles and throws at runtime. No unit test instantiates a screen.", "Untestable without a device"],
        ["An empty delta was rejected on both sides", "The prompt asked for it, the client rejected it, the server rejected it. Each component was individually correct against its own assumption.", "Contract mismatch between components"],
      ],
    },
    tnum: "12.2",
    caption: "Defects that reached a lecture with a green suite",
  },
  {
    p: "The first is the one that changed how tests are written in this project. A test that feeds " +
      "an input the system will never receive proves that the code handles an input the system " +
      "will never receive. FrameChangeDetectorTest now replays luminance scores taken directly " +
      "from the 218-line field log, tremor included, so the tremor is part of the specification " +
      "rather than an unmodelled fact about reality.",
  },
  {
    p: "The fourth is the one most likely to recur, because it is not a coverage problem at all. " +
      "Three components each behaved correctly according to its own understanding of the contract, " +
      "and the contract had never been written down in a place all three referred to. Appendix F " +
      "now is that place.",
  },

  { h2: "12.4 What was added in response" },
  {
    n: [
      "Field-derived fixtures. Algorithm tests replay measurements from real sessions rather than constructed arrays.",
      "A device smoke suite. Three instrumentation tests walk the navigation graph, open dialogs and reach the proxy from a release build, so a screen that cannot be reached or drawn fails the build.",
      "Property-style seam tests. TranscriptOverlapTest generates a seam at every word boundary of a fixed paragraph rather than testing one hand-picked seam.",
      "A written contract. Appendix F specifies every route, every bound and every error code in one place, so that a client assumption and a server assumption can be compared.",
      "An honest status register. docs/PROJECT_STATUS.md lists features that compile, pass tests and have never been observed working in the field. It currently has nine entries and is reproduced as Table 15.1.",
    ],
  },

  { h2: "12.5 Manual and field validation" },
  {
    table: {
      head: ["Validation", "Method", "Result"],
      widths: [2800, 3600, 3006],
      rows: [
        ["All sixteen destinations reachable", "Device walkthrough", "Pass"],
        ["Accent switching persists across navigation", "Device, DataStore", "Pass"],
        ["Live storage breakdown", "Device", "Pass"],
        ["Live /health model routing on the Connection screen", "Device against the deployed proxy", "Pass"],
        ["Real capture with transcription, timestamps and speaker labels", "Field trial", "Pass"],
        ["Markdown renders rather than showing source", "Field trial", "Pass, after trial 1"],
        ["Board session with manual capture and mid-session tuning", "Field trial 4", "Pass"],
        ["Adaptive tuning visibly changing interval and threshold", "Field trial 4", "Pass"],
        ["Board frames accepted at all", "Field trial 4", "Pass: 3 kept, 9 skipped in 92 s"],
        ["Release build reaching the hosted proxy over HTTPS", "Device", "Pass"],
        ["Diagram placement beside the relevant text", "Not yet observed", "Unproven — see Chapter 15"],
        ["Duplicate rejection by fingerprint", "Not yet observed", "Unproven — see Chapter 15"],
      ],
    },
    tnum: "12.3",
    caption: "Manual and field validation, including what has not been demonstrated",
  },
];

const CH_CHALLENGES = [
  { h1: "13. Challenges Encountered and Solutions" },
  {
    p: "Twelve problems, each stated as it was encountered rather than as it appears in hindsight. " +
      "This is the longest chapter in the report and that is intentional: the most transferable " +
      "output of this project is the set of specific ways in which a reasonable design turned out " +
      "not to survive contact with a lecture theatre.",
  },

  { h2: "13.1 Words disappeared at regular intervals" },
  {
    p: "Symptom. The transcript from the first field trial was missing words, and the gaps were " +
      "evenly spaced through the recording.",
  },
  {
    p: "Diagnosis. The regularity was the clue: chunks were twenty seconds long and the gaps were " +
      "twenty seconds apart. Chunks abutted exactly, so a word straddling a boundary was seen only " +
      "as two fragments and each was guessed independently. \"Product\" became \"produced\" at one " +
      "seam.",
  },
  {
    p: "Resolution. A two-second overlap, so that every word is fully contained in at least one " +
      "chunk, followed by word-level trimming of the resulting repeat with a three-word minimum. " +
      "The three-word minimum matters: two words fire on ordinary speech and remove genuine " +
      "content, such as a lecturer restating a phrase for emphasis.",
  },

  { h2: "13.2 The note displayed its own source code" },
  {
    p: "Symptom. Notes showed literal ## and $$ characters instead of headings and formulas.",
  },
  {
    p: "Diagnosis. There was no Markdown renderer. The design had assumed one and none had been " +
      "written; the reader was displaying the source string as text.",
  },
  {
    p: "Resolution. A 243-line parser producing a sealed block hierarchy, and a 292-line Compose " +
      "renderer. Fifteen test cases, including malformed input taken from a real note. Tables were " +
      "added later, after a separate report described notes as bullet points and nothing else — " +
      "feedback that turned out to have two causes: a renderer that could not draw a table, and a " +
      "prompt that never asked for one. Both were changed.",
  },

  { h2: "13.3 Three defects, one unpushed commit" },
  {
    p: "Symptom. Three separately reported problems, investigated separately over two days: an " +
      "\"AI refinement isn't available\" error that would not clear, a fix that appeared to have no " +
      "effect, and behaviour that did not match the code being read.",
  },
  {
    p: "Diagnosis. The local branch was six commits ahead of the remote. The deployed proxy was " +
      "running older code. Every hypothesis about the application was wrong, because the " +
      "application was not the problem.",
  },
  {
    p: "Resolution. Pushed. Then, so that it could not recur silently, /health was extended to " +
      "report which model each route is actually routed to, and the Connection screen was made to " +
      "display it. The question \"is my fix live?\" is now answerable from the phone.",
  },
  {
    note: "Standing lesson 1: a commit is not a release. Confirm that the deployed system reports what " +
      "you expect before concluding that a server-side fix did not work.",
  },

  { h2: "13.4 Not one board frame, ever" },
  {
    p: "Symptom. A full lecture in board mode produced a note containing speech and no images at " +
      "all. The camera preview was live, the counter showed frames being considered, no error was " +
      "reported, and every unit test passed.",
  },
  {
    p: "Diagnosis. None was available. Two rounds of code reading and one round of additional unit " +
      "tests produced nothing, because the code was correct in every way the tests could express " +
      "and the failure was in a constant.",
  },
  {
    p: "Resolution — of the diagnosis problem, not of the defect. A temporary on-device diagnostic " +
      "log: a 500-entry ring buffer holding a timestamped record of every decision the capture " +
      "pipeline makes, exportable through the share sheet. It is constrained so that it cannot " +
      "itself become a privacy problem: speech is stored as a length and a sixty-character prefix, " +
      "no audio or images are written, there is no network code in the component, it is absent " +
      "from release builds, and its removal checklist is tracked in a file.",
  },
  {
    note: "Standing lesson 2: instrument the running system. The defect was invisible to every test " +
      "and to code review, and obvious within fifteen seconds of reading a log.",
  },

  { h2: "13.5 The settle gate was an order of magnitude wrong" },
  {
    p: "Symptom. With the log in place, the third trial returned 218 lines. Frames were being held " +
      "and then rejected at the settle test, every time, forty times in a row.",
  },
  {
    p: "Diagnosis. The settle test compared against an absolute constant of 0.035. The log showed " +
      "a static board on a handheld phone scoring between 0.09 and 0.17 from hand tremor alone. " +
      "The gate was set below the noise floor of the device it ran on and could never open. Every " +
      "unit test passed because synthetic frames have no tremor in them.",
  },
  {
    p: "Resolution. The settle test now compares against a fraction — 0.45 — of the score that " +
      "triggered the hold. This is scale-free: it asks whether the scene has settled relative to " +
      "how much it moved, which requires no tuning per room, per phone or per lighting condition. " +
      "Field trial 4 accepted three frames and skipped nine in ninety-two seconds: the first " +
      "accepted captures in the project's history.",
  },
  {
    note: "Standing lesson 3: benchmark on realistic inputs. A test that feeds an input the system " +
      "will never receive proves something about an input the system will never receive.",
  },

  { h2: "13.6 A fix that broke a test that was right" },
  {
    p: "Symptom. An intermediate fix for the settle gate added a cap — capture anyway after N " +
      "frames — so that a never-settling scene would eventually yield something. It broke a test " +
      "named personMovingThroughFrameIsNeverAccepted.",
  },
  {
    p: "Diagnosis. The test was correct and the fix was wrong. A scene that never settles and a " +
      "person walking across the board are the same signal. No amount of waiting distinguishes " +
      "them; a cap simply guarantees that the person is eventually photographed and the call spent.",
  },
  {
    p: "Resolution. The cap was removed. The underlying need — the student can see that the board " +
      "is worth capturing and the filter disagrees — was met instead with a manual capture button, " +
      "which is the only component in the system that actually knows.",
  },

  { h2: "13.7 The adaptive controller drifted" },
  {
    p: "Symptom. During a device run the sampling interval fell from 8.0 s to the 2.0 s clamp over " +
      "four minutes of ordinary speech, while the threshold fell from 0.080 to 0.041. The two " +
      "numbers were asserting opposite things about the same lecturer.",
  },
  {
    p: "Diagnosis. Each suggestion divided the current value rather than the session baseline, so " +
      "adjustments compounded. A proportional controller had become an integrating one by " +
      "accident, and the error accumulated instead of being corrected.",
  },
  {
    p: "Resolution. suggest() takes the session baseline as an argument and multiplies that. The " +
      "loop now has no memory of its own output: two identical measurements produce identical " +
      "settings, and the system returns to baseline the moment the pace does. Covered by " +
      "SpeechPaceTrackerTest.repeatedSuggestionsDoNotCompound.",
  },

  { h2: "13.8 The model invented a section" },
  {
    p: "Symptom. A note contained a section headed \"Applications in Industry\" with four confident " +
      "paragraphs. The lecturer never said the phrase. The transcript for that window contains a " +
      "two-minute silence while a diagram was drawn.",
  },
  {
    p: "Diagnosis. The prompt explicitly instructed the model not to introduce material absent from " +
      "the evidence. The instruction was followed on every other call in that session and ignored " +
      "on that one.",
  },
  {
    p: "Resolution. The grounding guard, on the server, where it cannot be bypassed: the delta is " +
      "scored against the evidence it claims to summarise and discarded below a ratio of 0.25 with " +
      "a 120-character minimum. Measured honest deltas score between 0.34 and 0.71; the two " +
      "fabricated passages found on review scored 0.08 and 0.11.",
  },
  {
    note: "Standing lesson 4: a prompt instruction is a request; a validator is a guarantee. The same " +
      "reasoning is applied in five places, listed in Table 11.2.",
  },

  { h2: "13.9 Cost was in the request, not the response" },
  {
    p: "Symptom. A fifty-minute lecture cost roughly three times what was sustainable for a " +
      "student using the system daily.",
  },
  {
    p: "Diagnosis — first attempt, wrong. The obvious target was verbosity: the prompt was " +
      "shortened, the note policy was defaulted to concise, and the model was asked for fewer " +
      "words. The bill moved by about four per cent.",
  },
  {
    p: "Diagnosis — second attempt, correct. Logging request sizes showed that a composition call " +
      "at minute forty was sending roughly 24,000 characters of context and receiving about 600 " +
      "characters back. Trimming the 600 could not matter.",
  },
  {
    p: "Resolution. Bounded context: 4,000 characters of syllabus, 4,000 of note outline, 6,000 of " +
      "recent Markdown. The request is now flat at about 14,000 characters regardless of lecture " +
      "length, and the measured cost of a fifty-minute lecture fell by roughly two thirds.",
  },
  {
    note: "Standing lesson 5: measure the request before blaming the response.",
  },

  { h2: "13.10 A model chosen on a toy benchmark timed out on a real note" },
  {
    p: "Symptom. A model selected during development for its low latency began timing out at " +
      "ninety seconds once notes had grown to a realistic size.",
  },
  {
    p: "Diagnosis. It had been benchmarked on a 0.7-second toy request. Latency for that class of " +
      "model does not scale linearly with context, and the benchmark bore no relationship to the " +
      "workload.",
  },
  {
    p: "Resolution. Re-benchmarked on a grown note with realistic context and chose accordingly. " +
      "This is the same lesson as 13.5 arriving from a completely different direction, which is " +
      "why it is recorded separately rather than folded into it.",
  },

  { h2: "13.11 A button that compiled, passed tests and did nothing" },
  {
    p: "Symptom. Rename and delete had been implemented, committed, and reported as working. On " +
      "the device, pressing either button had no effect whatsoever.",
  },
  {
    p: "Diagnosis. The dialogs' state was being set correctly. The render block had ended up " +
      "outside the composable during an automated edit, so nothing drew. It compiled cleanly and " +
      "109 tests passed.",
  },
  {
    p: "Resolution. The render block was moved inside the composable and — more importantly — the " +
      "device instrumentation suite was created, because no unit test can observe whether " +
      "something was drawn. The earlier claim that rename and delete worked was corrected " +
      "explicitly in the following commit message rather than quietly amended.",
  },
  {
    note: "Standing lesson 6: a compiled control is not a reachable one.",
  },

  { h2: "13.12 A runtime crash the compiler accepts" },
  {
    p: "Symptom. Six settings sub-pages crashed immediately on open.",
  },
  {
    p: "Diagnosis. A negative value passed to Modifier.padding. This compiles without warning and " +
      "throws at runtime with \"Padding must be non-negative\". The intended effect — pulling a " +
      "control slightly outside its container — requires Modifier.offset instead.",
  },
  {
    p: "Resolution. Replaced with Modifier.offset. The same mistake was made twice in one " +
      "development session, in two different components, which is a strong argument that it needs " +
      "a lint rule rather than more care. Adding that rule is an open item in Chapter 16.",
  },

  { h2: "13.13 Device policy blocked the device tests" },
  {
    p: "Symptom. Instrumentation tests could not be installed: INSTALL_FAILED_USER_RESTRICTED.",
  },
  {
    p: "Diagnosis. The manufacturer's Android distribution blocks USB installation by default and " +
      "requires an additional developer setting to be enabled before instrumentation packages will " +
      "install.",
  },
  {
    p: "Resolution. Enabled the setting on the test device. Recorded here because it is not a code " +
      "problem, cost real time, and will be met by anyone reproducing this work on a similar device.",
  },

  { h2: "13.14 Summary of standing lessons" },
  {
    table: {
      head: ["#", "Lesson", "Cost of learning it"],
      widths: [600, 4400, 4406],
      rows: [
        ["1", "A commit is not a release.", "Two days investigating three defects with one cause."],
        ["2", "Instrument the running system.", "One entire field trial producing no usable output."],
        ["3", "Benchmark on realistic inputs.", "Three field trials with zero board captures."],
        ["4", "A prompt instruction is a request; a validator is a guarantee.", "A fabricated section in a student-facing note."],
        ["5", "Measure the request before blaming the response.", "A cost-reduction effort that achieved four per cent."],
        ["6", "A compiled control is not a reachable one.", "A feature reported as working that had never worked."],
      ],
    },
    tnum: "13.1",
    caption: "Standing lessons, and what each cost to learn",
  },
];

const CH_RESULTS = [
  { h1: "14. Results" },
  {
    p: "This chapter reports what was measured. It is deliberately short on claims, because the " +
      "single most important property of the system — accuracy — has not been measured and is " +
      "therefore not claimed anywhere in this report.",
  },

  { h2: "14.1 Build and test results" },
  {
    table: {
      head: ["Measure", "Result"],
      widths: [5000, 4406],
      rows: [
        ["Android unit tests", "134 passing"],
        ["Backend contract tests", "43 passing"],
        ["Device instrumentation tests", "3 passing on a physical phone"],
        ["Lint", "Clean"],
        ["Release build", "Builds, installs, reaches the deployed proxy over HTTPS"],
        ["Database migrations", "4, all tested against a populated database at the previous version"],
        ["Deployed backend", "Live; verified against /health"],
        ["Application source", "13,337 lines of Kotlin across 74 files"],
        ["Test source", "2,574 lines across 32 files"],
      ],
    },
    tnum: "14.1",
    caption: "Build and test results",
  },

  { h2: "14.2 Cost" },
  { fig: "docs/diagrams/fig-cost.png", num: "14.1", caption: "Where a lecture's cost goes, and the controls that bound it", opts: { maxH: 780 } },
  {
    table: {
      head: ["Control", "Effect", "Basis"],
      widths: [3000, 3200, 3206],
      rows: [
        ["On-device frame filter", "~450 candidate frames per hour become ~40 uploads", "Measured across four trials"],
        ["Bounded delta context", "Request flat at ~14,000 characters regardless of lecture length", "Measured"],
        ["Bounded context vs resending the note", "~17x cheaper per call at minute 50", "Derived from measured request sizes"],
        ["Overall reduction from context bounding", "~2/3 of the cost of a 50-minute lecture", "Measured before and after"],
        ["Duplicate rejection by fingerprint", "Avoids repeat extraction of an unchanged board", "Implemented; not yet observed firing in the field"],
        ["Per-minute rate limit", "Bounds a runaway client", "Backend tests"],
        ["Daily budget", "Bounds a leaked client token", "Backend tests"],
        ["Empty-delta acceptance", "Removes a three-call retry storm on every silent chunk", "Measured after the fix"],
      ],
    },
    tnum: "14.2",
    caption: "Cost controls and their measured effect",
  },
  {
    p: "The largest single lever is the on-device filter, and it costs nothing to run. That is the " +
      "clearest result in this project: the most effective way found to reduce the cost of an AI " +
      "system was to write ninety lines of ordinary image processing that decides not to call it.",
  },

  { h2: "14.3 Field trial results" },
  { fig: "docs/diagrams/fig-timeline.png", num: "14.2", caption: "Timeline, with the four field trials and what each changed", opts: { maxH: 720 } },
  {
    table: {
      head: ["Trial", "Board frames accepted", "Principal outcome"],
      widths: [1400, 2400, 5606],
      rows: [
        ["1", "n/a (audio mode)", "Markdown renderer, chunk overlap, export, rename and delete"],
        ["2", "0", "The temporary diagnostic log, because no diagnosis was otherwise available"],
        ["3", "0", "Settle-gate and compounding defects both identified from the 218-line log"],
        ["4", "3 kept, 9 skipped in 92 s", "First accepted captures in the project's history"],
      ],
    },
    tnum: "14.3",
    caption: "Field trials",
  },
  {
    p: "The trend in that table is the honest result of this project. Three trials produced " +
      "nothing and the fourth produced three frames, and the difference between them was not more " +
      "code but better information: a log from the running system, showing what the running system " +
      "was actually doing.",
  },

  { h2: "14.4 What is deliberately not claimed" },
  {
    b: [
      "No transcription accuracy figure. No labelled corpus exists and no scorer has been written.",
      "No board extraction accuracy figure, for the same reason.",
      "No claim that generated notes are correct. The system's position is that generated text is a claim about evidence the system still holds, and that the student can inspect both.",
      "No claim that the grounding guard catches all fabrication. It rejects the two instances that were measured; its threshold was set from four trials' worth of data, which is a small sample.",
      "No battery or thermal measurement across a full fifty-minute board session.",
    ],
  },
  {
    p: "Every one of those is a measurement that could be made and has not been. They are listed " +
      "in Chapter 16 as planned work in the order they would be attempted, and the labelled corpus " +
      "comes first because nothing else in that list can be interpreted without it.",
  },
];

const CH_LIMITATIONS = [
  { h1: "15. Limitations and Known Issues" },
  {
    p: "This chapter is reproduced from the project's own status register, which is maintained " +
      "during development rather than written for this report. It is the most useful chapter in " +
      "the document for anyone continuing the work.",
  },

  { h2: "15.1 Implemented, tested, never observed working in the field" },
  {
    p: "The following compile and have unit tests, and no field session has confirmed the " +
      "behaviour end to end. This is the list most likely to contain a hidden defect, and it is " +
      "presented first for that reason.",
  },
  {
    table: {
      head: ["Feature", "Why it is unconfirmed"],
      widths: [3400, 6006],
      rows: [
        ["Diagram placement beside its related text", "No accepted board frame reached extraction until field trial 4, so the inline placement path has never actually run against real content."],
        ["Duplicate rejection by perceptual fingerprint", "Same reason."],
        ["Transcript overlap repair on real speech", "Unit-tested against synthetic and generated seams only; never checked against a full lecture transcript."],
        ["The grounding guard firing on a live model", "Tested against a scripted provider. It has never rejected a delta from a real model in a real session."],
        ["Verification findings appearing in the reader", "The pass runs and returns findings — this is visible in the field log — but the findings have not been observed rendered as review flags."],
        ["Review flag accept and annotate actions", "No field session has produced a flag to press."],
        ["Export with assets on a populated note", "Wired, compiles and unit-tested; never exercised against a note containing board crops."],
        ["Rename, delete and move on real data", "The dialogs are confirmed to open on a device; the operations have not been confirmed against a populated library."],
        ["Recovery from a retained WAV", "The retention path has fired in the field. The recovery button has never been pressed."],
      ],
    },
    tnum: "15.1",
    caption: "Nine features that are implemented, tested and unproven",
  },

  { h2: "15.2 Not implemented" },
  {
    table: {
      head: ["Item", "Note"],
      widths: [3000, 6406],
      rows: [
        ["Accuracy measurement", "No labelled corpus, no scorer. No accuracy figure is claimed anywhere in this report."],
        ["Diagram type or structure recognition", "The system crops regions a vision model reports as diagrams. It does not understand arrows, charts or entity-relationship structure. This is the most frequently misunderstood limitation of the system."],
        ["On-device diagram pre-filter", "Nothing rejects a crop of a wall or a face before the vision call is spent."],
        ["Consent-aware speaker identity", "Requires enrolment and stored voiceprints; excluded on privacy grounds in Chapter 5."],
        ["Full-note consolidation", "Updates are append-only by design. A pass that reorganises a finished note does not exist."],
        ["Composition batching", "The largest remaining cost lever. Untouched because it crosses the retained-audio recovery path and was judged too risky to add late in the term."],
        ["PDF and DOCX syllabus import", "Markdown and plain text only."],
        ["Offline queueing", "Loss of connectivity is handled by retention and warnings, not by deferred upload."],
      ],
    },
    tnum: "15.2",
    caption: "Not implemented",
  },

  { h2: "15.3 Known defects and accepted risks" },
  {
    table: {
      head: ["#", "Issue", "Severity", "Note"],
      widths: [500, 3200, 2000, 3706],
      rows: [
        ["1", "The temporary diagnostic log is still present", "Must fix before distribution", "Removal checklist in docs/TEMPORARY_DEBUG_LOG.md"],
        ["2", "The client token is recoverable from the APK", "Accepted, bounded", "Bounded by the rate limit and the daily budget; rotate after any public demonstration"],
        ["3", "The release build is signed with the debug keystore", "Must fix before distribution", "A demonstration arrangement only"],
        ["4", "Invalid timestamps such as 00:80 observed in a note", "Open", "The client's formatter provably cannot emit this, so the model invented it. The prompt now forbids it and the client rejects it; unverified in the field."],
        ["5", "Transcription returns one large segment per chunk", "Open", "Every field chunk returned a single segment of 250 to 400 characters, so timestamps are coarse and a speaker change within a chunk is invisible."],
        ["6", "Free-tier cold start", "Accepted", "The first request after idle can take about 13 seconds."],
        ["7", "No lint rule against negative Compose padding", "Open", "The same runtime-crashing mistake was made twice in one development session."],
      ],
    },
    tnum: "15.3",
    caption: "Known defects and accepted risks",
  },
  {
    p: "Item 5 deserves emphasis because it undercuts a feature that appears to work. Speaker " +
      "labelling and timestamping are both implemented correctly and both operate at chunk " +
      "granularity, because the provider returns one segment per chunk. A student reading the note " +
      "sees timestamps every twenty seconds rather than at each sentence, and a question asked " +
      "from the back of the room during a chunk is attributed to whoever dominated that chunk. " +
      "This is a limitation of the input rather than of the algorithm, but it is the student who " +
      "experiences it, so it is recorded as a defect and not as an external constraint.",
  },
];

const CH_FUTURE = [
  { h1: "16. Future Implementation" },
  {
    p: "This chapter is written to be updated. Items appear in the order they would be attempted, " +
      "and each states why it is not done rather than only what it would be — the reason is " +
      "usually more informative than the description.",
  },

  { h2: "16.1 Immediate: close out the current state" },
  {
    table: {
      head: ["#", "Item", "Why it is not done"],
      widths: [500, 3400, 5506],
      rows: [
        ["1", "Confirm diagram placement and duplicate rejection in a field session", "Field trial 4 produced the first accepted frames. The next session with speech in the room unblocks most of Table 15.1."],
        ["2", "Rotate the client token", "Scheduled after the current demonstration period."],
        ["3", "Remove the temporary diagnostic log", "It is still earning its place. Removal follows the confirmation in item 1."],
        ["4", "Replace the debug signing configuration", "Required before any distribution; not required for internal assessment."],
        ["5", "Add a lint rule for negative Compose padding", "Cheap, and not yet done. The same defect occurred twice."],
      ],
    },
    tnum: "16.1",
    caption: "Immediate work",
  },

  { h2: "16.2 Next: measurement" },
  {
    p: "Nothing else in this list can be interpreted without this, which is why it comes first " +
      "among the substantial items.",
  },
  {
    n: [
      "Build a labelled corpus: five to ten recorded lectures with human-verified transcripts and human-verified board content.",
      "Write a scorer: word error rate for transcription, and a content-recall measure for board extraction.",
      "Establish a baseline, then re-measure after every subsequent change. Until this exists, every improvement claim in this project is an opinion rather than a result.",
      "Measure battery and thermal behaviour across a full fifty-minute board session, which has also never been done.",
      "Measure the grounding guard's false-positive rate: how often it discards a delta that was in fact faithful. The current threshold was set from two negative examples, which is not enough to characterise the other side of the boundary.",
    ],
  },

  { h2: "16.3 Then: the limitations that matter most to a student" },
  {
    table: {
      head: ["Item", "What it would take", "Why it matters"],
      widths: [2600, 3600, 3206],
      rows: [
        ["Sub-chunk timestamps and speaker changes", "Either a provider that returns real segmentation, or on-device voice-activity detection to cut chunks at speech boundaries rather than at fixed intervals.", "Fixes known defect 5, which currently degrades two features that are otherwise correct."],
        ["An on-device diagram pre-filter", "ML Kit already runs locally for text presence. Extending it to reject a crop with no board-like content would stop a wall or a face costing a vision call.", "Cost, and the quality of what reaches the note."],
        ["Diagram structure recognition", "A genuinely open problem: recognising that a shape is an entity, an arrow a relationship, a chart a plot.", "The capability most often assumed to be present that is not."],
        ["Composition batching", "Compose from several chunks at once rather than one at a time.", "The largest remaining cost lever. Crosses the retained-audio recovery path, so it needs care."],
        ["Full-note consolidation", "A pass that reorganises a finished note into a clean structure, run once, with the append-only original retained alongside it.", "Append-only composition produces a chronological note; a student revising wants a topical one."],
        ["A second reader for review flags", "A view that lists every flag in a note in one place rather than in reading order.", "A student who wants to check the note before an examination should not have to scroll it."],
      ],
    },
    tnum: "16.2",
    caption: "Substantial future work",
  },

  { h2: "16.4 Later: scope the project deliberately excluded" },
  {
    b: [
      "PDF and DOCX syllabus import.",
      "Offline queueing with deferred upload, replacing the current retention-and-warn behaviour.",
      "Multi-device synchronisation, which requires an account system and a server-side note store.",
      "Consent-aware speaker enrolment, which would let a student label their own lecturer's voice with explicit opt-in.",
      "An iOS client, which means rewriting the capture pipeline against a different camera and audio stack.",
      "Collaborative notes, which adds a permissions model on top of synchronisation.",
    ],
  },
  {
    p: "Each of these was excluded in Chapter 5 with a stated reason, and each reason still holds. " +
      "They are listed here so that the exclusions are recorded as deferrals rather than as " +
      "omissions.",
  },

  { h2: "16.5 How this chapter should be maintained" },
  {
    p: "The status register at docs/PROJECT_STATUS.md is the source for Chapters 15 and 16, and it " +
      "is updated during development rather than at report time. The most valuable section of it " +
      "is the one reproduced as Table 15.1 — features that compile, pass tests and have never been " +
      "observed working. Re-reading that table is the cheapest bug-finding exercise available in " +
      "this project, and it has found three defects so far.",
  },
];

const CH_CONCLUSION = [
  { h1: "17. Conclusion" },
  {
    p: "This project set out to convert a live lecture into a structured, revisable note while the " +
      "lecture is still happening, capturing both channels — speech and board — and keeping the " +
      "result affordable and honest. The system built does that: 13,337 lines of Kotlin across 74 " +
      "files, a dependency-free proxy holding the credential the application must not, ten " +
      "database tables, sixteen screens, 180 automated tests, and four field trials in real " +
      "lectures.",
  },
  {
    p: "Six algorithms in it are its own work, and none of them changed when the AI provider was " +
      "migrated during development. That is the practical test of whether the contribution is real " +
      "rather than a wrapper around somebody else's model: the models were replaced and the " +
      "algorithms were not.",
  },

  { h2: "17.1 What the project demonstrates" },
  {
    n: [
      "That the cost of an AI system is dominated by what is sent to it, and that the most " +
        "effective optimisation available here was ninety lines of ordinary image processing that " +
        "decides not to call it. Roughly 450 candidate frames per lecture-hour become roughly 40.",
      "That a bounded, append-only protocol keeps cost flat as the artefact grows, and " +
        "simultaneously removes the model's ability to damage what it did not write. One mechanism, " +
        "two unrelated problems solved.",
      "That an instruction to a model is a request and a check on its output is a guarantee. This " +
        "project learned that from a fabricated section in a student-facing note and applies the " +
        "conclusion in five places.",
      "That a system which generates text should keep the evidence it generated from, record " +
        "which evidence produced which sentence, and show the reader both. Every architectural " +
        "decision in Chapter 6 follows from that one position.",
    ],
  },

  { h2: "17.2 What it does not demonstrate" },
  {
    p: "No accuracy figure is claimed anywhere in this report, because none has been measured. " +
      "Nine implemented features have never been observed working in the field and are listed " +
      "individually in Table 15.1. The system does not interpret diagrams, and says so repeatedly, " +
      "because it is the capability most often assumed to be present.",
  },
  {
    p: "That list is not an apology. A project of this size that claimed everything worked would " +
      "be making exactly the error its own grounding guard exists to prevent, and holding the " +
      "report to the standard the software is held to seemed the minimum consistency.",
  },

  { h2: "17.3 The most useful output" },
  {
    p: "The most transferable thing this project produced is not the application. It is the six " +
      "standing lessons in Table 13.1, each learned by getting something wrong in front of a " +
      "class, and each costing considerably more to learn than it costs to read.",
  },
  {
    p: "Of those, the one that recurred most often is the third: benchmark on realistic inputs. A " +
      "settle threshold set an order of magnitude below the noise floor of a handheld phone. A " +
      "model chosen on a 0.7-second toy request that timed out at ninety seconds on a real note. A " +
      "test suite that fed synthetic frames and therefore proved something true about frames the " +
      "system will never receive. Three different failures, one cause, and in every case the code " +
      "was correct and the assumption about the world was not.",
  },
  {
    p: "A lecture is delivered once. A system built to capture it has to work the first time, in a " +
      "real room, on a phone held in a hand — and the distance between a design that should work " +
      "and a design that does is measured entirely in trips to that room.",
  },
];

const CH_REFERENCES = [
  { h1: "18. References" },
  {
    p: "References are grouped by the part of the system each informed, and annotated with what " +
      "was taken from it. Where a technique is standard and no single source is being credited, " +
      "that is stated rather than a citation being manufactured.",
  },

  { h2: "18.1 Platform and libraries" },
  {
    table: {
      head: ["Reference", "What was taken from it"],
      widths: [3400, 6006],
      rows: [
        ["Android Developers — AudioRecord and low-level audio capture. developer.android.com", "Buffer-level capture at 16 kHz mono PCM16, which is what makes a two-second chunk overlap constructible at all. Higher-level recorders do not expose the buffer."],
        ["Android Developers — CameraX and LifecycleCameraController. developer.android.com", "Lifecycle-bound camera control, the image-analysis executor pattern, and ZoomState with setZoomRatio, which the zoom slider binds to."],
        ["Android Developers — Room persistence library and migrations. developer.android.com", "Compile-time query verification, the migration framework, and the decision to disable destructive fallback so that a missing migration fails the build rather than the user."],
        ["Android Developers — Jetpack DataStore. developer.android.com", "Asynchronous, transactional settings storage, so that a settings write cannot block a capture thread."],
        ["Android Developers — Jetpack Compose and Material 3. developer.android.com", "Declarative UI, and the state model that makes the live session screen tractable."],
        ["Android Developers — FileProvider and content sharing. developer.android.com", "Delivering an exported note to another application, which is what fixed the export that silently did nothing in field trial 1."],
        ["Google ML Kit — Text Recognition. developers.google.com/ml-kit", "On-device text presence detection, used to reject a frame with no legible text before a network call is made."],
        ["Kotlin documentation — coroutines and structured concurrency. kotlinlang.org", "The dispatcher and scope model behind the three concurrent pipelines described in Chapter 8."],
        ["Node.js 22 documentation — node:test and the native fetch API. nodejs.org", "The test runner and HTTP client used by the proxy, which is what allows it to have no runtime dependencies at all."],
      ],
    },
    tnum: "18.1",
    caption: "Platform and library references",
  },

  { h2: "18.2 Techniques" },
  {
    table: {
      head: ["Technique", "Status, and what was taken"],
      widths: [2600, 6806],
      rows: [
        ["Optimistic concurrency control", "Standard in database systems and in distributed version control. Applied here to a document rather than a row. This project's addition is binding the delta to a content hash as well as to a version number, because a student edit changes the content without changing the version."],
        ["Frame differencing on a downsampled grid", "Standard in video compression and surveillance. The downsample-compare-threshold structure is conventional; the settle test that follows it is specific to this application and is described in Chapter 7."],
        ["Perceptual hashing for near-duplicate detection", "Standard in image retrieval. Relied on here for the property that the hash is stable under handheld tremor and unstable under new writing appearing on the board."],
        ["Proportional feedback control", "Elementary control theory. Recorded here because the first implementation accidentally produced an integrating controller, and the measured drift is given in Table 7.2."],
        ["Automatic faithfulness checking of generated summaries", "An active area in the summarisation literature. This project implements a deliberately simple lexical form — content-word overlap ratio with a floor — and sets its threshold from its own field measurements rather than from any published value."],
        ["Constant-time secret comparison", "Standard practice against timing side channels. Applied to the bearer token comparison in the proxy."],
        ["Ring-buffer diagnostic logging", "Standard practice in embedded and mobile systems, where a log must be bounded because storage is. Used for the temporary diagnostic component described in Chapter 8."],
      ],
    },
    tnum: "18.2",
    caption: "Techniques, with an explicit statement of what is standard and what is this project's own",
  },

  { h2: "18.3 Specifications and formats" },
  {
    table: {
      head: ["Reference", "Use"],
      widths: [3400, 6006],
      rows: [
        ["CommonMark specification. spec.commonmark.org", "The subset of Markdown the parser implements: headings, lists, quotes, fenced code, tables and thematic breaks."],
        ["OpenAI-compatible chat completions interface", "The request and response shape the proxy speaks to the provider, including the strict json_schema response format and provider parameter requirements."],
        ["RFC 7235 — HTTP/1.1 authentication", "The bearer token scheme used on every pipeline route."],
        ["RFC 8446 — TLS 1.3", "The transport for both network hops in Figure 6.9."],
        ["FIPS 180-4 — SHA-256", "The content hash that binds a delta to the note it was computed against, and the syllabus deduplication hash."],
        ["ISO/IEC 19505 — UML 2", "The notation used for the use-case, class, sequence, activity, state-machine, component and deployment diagrams in this report."],
      ],
    },
    tnum: "18.3",
    caption: "Specifications and formats",
  },

  { h2: "18.4 This project's own documents" },
  {
    table: {
      head: ["Document", "Contents"],
      widths: [3400, 6006],
      rows: [
        ["docs/PROJECT_STATUS.md", "The maintained status register. Source for Chapters 15 and 16, including the nine unproven features of Table 15.1."],
        ["docs/SUMMARY.md", "One-page summary of the system and its honest limitations."],
        ["docs/TEMPORARY_DEBUG_LOG.md", "The removal checklist for the temporary diagnostic component."],
        ["server/OPERATIONS.md", "Token and key rotation, error codes, deployment confirmation and rollback."],
        ["Field diagnostic log, 218 entries, field trial 3", "The log from which the settle-gate and compounding defects were both diagnosed."],
        ["tools/diagrams/", "The generator for all twenty-one diagrams in this report. Every figure is produced from the system it describes rather than drawn by hand."],
      ],
    },
    tnum: "18.4",
    caption: "Project documents referenced in this report",
  },
];

module.exports = { CH_TESTING, CH_CHALLENGES, CH_RESULTS, CH_LIMITATIONS, CH_FUTURE, CH_CONCLUSION, CH_REFERENCES };

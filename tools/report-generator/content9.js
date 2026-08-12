/**
 * Appendix T2 (visual index) and a closing section for chapter 17.
 */

const APP_GALLERY = [
  { h1: "Appendix S — Visual Index of the Application" },
  {
    p: "Every screen of the application at full size, in the order a student meets them. The " +
      "specifications in Appendix N describe what each screen does; this appendix exists so that " +
      "the application can be looked at rather than read about, at a size where the type and the " +
      "state on screen are legible.",
  },
  {
    p: "All twenty-one images were captured from a release-configuration build running on a " +
      "physical Android device over ADB, not from an emulator or a mock-up. Where a screen shows a " +
      "particular state — a running session at 01:32, a note containing capture warnings — that " +
      "state was produced by the application and not staged.",
  },

  { h2: "S.1 First launch" },
  { shots: [
      { file: "01-onboarding-1.png", num: "S.1", caption: "Onboarding, step 1: what the application does" },
      { file: "02-onboarding-2.png", num: "S.2", caption: "Onboarding, step 2: what is captured and where it is kept" },
    ], opts: { perRow: 2, width: 298 } },
  { shots: [
      { file: "03-onboarding-3.png", num: "S.3", caption: "Onboarding, step 3: consent, stated in plain words" },
      { file: "04-home.png", num: "S.4", caption: "Home: one primary action and recent notes" },
    ], opts: { perRow: 2, width: 298 } },

  { h2: "S.2 Starting a session" },
  { shots: [
      { file: "05-capture-setup.png", num: "S.5", caption: "Capture setup: mode selection" },
      { file: "06-capture-setup-lower.png", num: "S.6", caption: "Capture setup: note policy and syllabus" },
    ], opts: { perRow: 2, width: 298 } },

  { h2: "S.3 A session running" },
  { shots: [
      { file: "14-live-board.png", num: "S.7", caption: "Board mode: camera preview with the frame guide" },
      { file: "16-live-running.png", num: "S.8", caption: "A running session: 01:32 elapsed, revision 5, 3 frames kept and 9 skipped" },
    ], opts: { perRow: 2, width: 298 } },
  {
    p: "Figure S.8 is the single most important image in this report. It is the first field session " +
      "in the project's history in which the on-device frame filter accepted a board frame: three " +
      "kept and nine skipped in ninety-two seconds, with the relative settle test in place. Three " +
      "earlier field trials, with the absolute test, accepted none.",
  },
  { shots: [
      { file: "15-live-tuning.png", num: "S.9", caption: "The tuning panel: interval and sensitivity, adjustable without ending the session" },
    ], opts: { perRow: 2, width: 298 } },

  { h2: "S.4 Reading a note" },
  { shots: [
      { file: "17-note-reader.png", num: "S.10", caption: "The reader. The amber lines are the system declining to invent content from five silent chunks." },
      { file: "17b-note-reader-scrolled.png", num: "S.11", caption: "The reader, scrolled" },
    ], opts: { perRow: 2, width: 298 } },

  { h2: "S.5 The library" },
  { shots: [
      { file: "07-library-empty.png", num: "S.12", caption: "Library, empty: an empty screen that explains itself" },
      { file: "18-library-populated.png", num: "S.13", caption: "Library, populated, with folders and search" },
    ], opts: { perRow: 2, width: 298 } },
  { shots: [
      { file: "19-new-folder.png", num: "S.14", caption: "Creating a nested folder: a folder per subject, a folder per unit inside it" },
      { file: "20-note-actions.png", num: "S.15", caption: "Note actions, including delete behind an explicit confirmation" },
    ], opts: { perRow: 2, width: 298 } },

  { h2: "S.6 Settings" },
  { shots: [
      { file: "08-settings.png", num: "S.16", caption: "Settings: six sub-pages" },
      { file: "09-appearance.png", num: "S.17", caption: "Appearance: four accents, persisted through DataStore" },
    ], opts: { perRow: 2, width: 298 } },
  { shots: [
      { file: "10-connection.png", num: "S.18", caption: "Connection: live health from the deployed proxy, including which model each route is routed to" },
      { file: "11-privacy.png", num: "S.19", caption: "Privacy and data: what is stored, what leaves, and the deletion controls" },
    ], opts: { perRow: 2, width: 298 } },
  { shots: [
      { file: "12-about.png", num: "S.20", caption: "About: version, build type, and a statement of what the system does not do" },
      { file: "13-diagnostics.png", num: "S.21", caption: "Diagnostics: the temporary log, absent from release builds and scheduled for removal" },
    ], opts: { perRow: 2, width: 298 } },
  {
    p: "Figure S.18 deserves a final word, because the control it shows is the one this project " +
      "would carry into any future system. It reports which model the deployed service will " +
      "actually use for each route, read live from the server, on the student's own phone. It " +
      "exists because three separately reported defects once had a single cause — a fix that had " +
      "been committed and never pushed — and two days were spent testing hypotheses about an " +
      "application that was not the problem.",
  },
];

const CONCLUSION_EXTRA = [
  { h2: "17.4 What would be done differently" },
  {
    p: "Four things, stated as changes to how the work was carried out rather than to what was " +
      "built. Each follows from a failure recorded in Chapter 13 and would have prevented it.",
  },
  {
    n: [
      "Instrument first, not after the second failure. The diagnostic log was built after an " +
        "entire field trial produced no usable output, and it found the defect within seconds of " +
        "being read. Building it before the first trial would have cost an afternoon and saved two " +
        "trials. A capture pipeline that cannot be observed while running is not finished.",
      "Take a measurement before choosing a constant. Three of the six algorithms shipped with a " +
        "constant chosen by reasoning about what ought to work, and in each case the reasoning was " +
        "sound and the value was wrong by an order of magnitude. The correct order is to run the " +
        "system, look at the numbers it produces, and then choose.",
      "Write the contract down before implementing either side of it. The empty-delta defect cost " +
        "three calls per silent chunk and existed because a prompt, a client and a server each " +
        "held a different, individually reasonable, assumption. Appendix F now exists; it should " +
        "have existed first.",
      "Run a device test on the first screen, not the fifteenth. Two entire classes of defect — a " +
        "control that draws nothing, a screen that crashes on open — are invisible to unit tests " +
        "and obvious to a single instrumentation test. Three such tests now exist and each was " +
        "written after the defect it would have caught.",
    ],
  },
  {
    p: "There is a common shape to all four. In each case the work that was skipped was the work " +
      "of finding out what was actually happening, and in each case it was skipped because the " +
      "code was correct as written and the assumption behind it was not visible enough to " +
      "question. That is not a lesson about diligence; it is a lesson about which activities " +
      "produce information and which merely produce output.",
  },

  { h2: "17.5 Closing" },
  {
    p: "The system described in this report captures a lecture in two channels, writes a " +
      "structured note while the lecture is still happening, keeps the evidence it worked from, " +
      "records where every generated sentence came from, checks its own output against its own " +
      "inputs, and tells the student where it is unsure. It does this at a cost a student can " +
      "sustain, because ninety lines of on-device image processing decide roughly nine times out " +
      "of ten not to make a call at all.",
  },
  {
    p: "It also has nine features that have never been observed working in the field, no measured " +
      "accuracy of any kind, and a temporary diagnostic component that must be removed before " +
      "anyone else installs it. Those are in Chapter 15, by name, because a report that omitted " +
      "them would be committing the exact error the system's own grounding guard was built to " +
      "prevent.",
  },
  {
    p: "A lecture is delivered once. Everything in this project follows from taking that sentence " +
      "seriously — the never-drop queue, the two-second overlap, the degrade-rather-than-stop " +
      "failure policy, the evidence that no model output may overwrite. And the distance between " +
      "a design that should work and one that does turned out to be measured entirely in trips to " +
      "a room where a lecture was actually happening.",
  },
];

const SCOPE_EXTRA = [
  { h2: "5.6 Success criteria" },
  {
    p: "The criteria the project was to be judged against, fixed before implementation began, with " +
      "the outcome against each. They are stated as observable outcomes rather than as qualities, " +
      "because a criterion that cannot be observed cannot be failed.",
  },
  {
    table: {
      head: ["#", "Criterion", "Outcome"],
      widths: [500, 4900, 4006],
      rows: [
        ["1", "A full lecture can be captured without the session stopping for any reason.", "Met. Four field trials; every failure path degrades rather than halts."],
        ["2", "The note exists when the student presses stop, not minutes afterwards.", "Met. Composition is incremental; the revision counter is visible during the session."],
        ["3", "Both channels are captured and aligned in time.", "Met. Speech and board share session-relative timestamps in one append-only note."],
        ["4", "Paid calls are reduced by at least an order of magnitude against the naive design.", "Met. ~450 candidate frames per lecture-hour become ~40 uploads."],
        ["5", "Cost per composition call does not grow with note length.", "Met. Bounded at ~14,000 characters, measured."],
        ["6", "No model output can overwrite captured evidence.", "Met structurally: append-only, single writable block, unique index."],
        ["7", "Generated text that is not supported by the evidence does not reach the note.", "Partly met. The grounding guard rejects text with no lexical support; it cannot detect a fluent claim that uses the right vocabulary."],
        ["8", "The student can see where the system disagreed with itself.", "Met in implementation; the flag actions have not been exercised in the field (Table 15.1)."],
        ["9", "Notes remain findable after a month of lectures.", "Met. Nested folders, search, rename, delete."],
        ["10", "The provider credential never resides on the device.", "Met. It exists only in the deployment environment."],
        ["11", "The report states what has not been proven.", "Met. Chapter 15 lists nine features individually, and no accuracy figure is claimed anywhere."],
      ],
    },
    tnum: "5.4",
    caption: "Success criteria and outcomes",
  },
  {
    p: "Nine of eleven met, one partly met, one met in implementation but unproven in the field. " +
      "Criterion 7 is the one worth dwelling on, because it is both the most important and the " +
      "least completely achieved. The guard catches text invented from nothing, which is the " +
      "failure that was actually observed; it does not catch a claim that is wrong while using the " +
      "vocabulary of the lecture, and no lexical measure would. The system's answer to that " +
      "residual case is not a better filter but a different design position: keep the evidence, " +
      "record which evidence produced which sentence, and give the student the means to check.",
  },
  {
    p: "Criterion 11 is unusual as a success criterion for a software project and is the one this " +
      "report would defend most strongly. A system built to prevent confident unsupported claims " +
      "should not be described in a document that makes them.",
  },
];

module.exports = { APP_GALLERY, CONCLUSION_EXTRA, SCOPE_EXTRA };

/**
 * Reference appendices: database schema, API contract, test register, field trial records,
 * user manual and glossary. These are generated from the same repository the source appendices
 * are read from, so a schema or route change shows up here on the next build.
 */

const W = 9406; // usable text width in twips

// ---------------------------------------------------------------- helpers

/** A column table for one database table. */
function columns(rows) {
  return {
    table: {
      head: ["Column", "Type", "Null", "Description"],
      widths: [2000, 1200, 700, 5506],
      rows,
    },
  };
}

const APP_SCHEMA = [
  { h1: "Appendix E — Database Schema Reference" },
  {
    p: "The application stores everything it captures in a single SQLite database managed by Room, " +
      "held in the application's private storage. Nothing is written to shared media and nothing " +
      "is synchronised to a server. This appendix documents every table, every column and every " +
      "index, together with the reason each one exists, because several of the columns are there " +
      "to satisfy a constraint that is not obvious from the name alone.",
  },
  {
    p: "The schema is at version 5. Four migrations have been written and all four are tested " +
      "against a database populated at the previous version; the migration tests are listed in " +
      "Appendix G and their source appears in Appendix X. Room's destructive fallback is disabled, " +
      "so a missing migration is a build-time failure rather than a silent loss of a student's notes.",
  },
  { fig: "docs/diagrams/fig-er-diagram.png", num: "E.1", caption: "Entity-relationship diagram of the complete schema" },

  { h2: "E.1 notes" },
  {
    p: "The durable artefact. A note outlives every session that contributed to it, and deleting a " +
      "note cascades to everything below it in one transaction.",
  },
  columns([
    ["id", "TEXT", "no", "Primary key. A UUID generated on the device; never reused."],
    ["title", "TEXT", "no", "Display title. Defaults to \"Live note N\" and is editable by the student."],
    ["createdAt", "INTEGER", "no", "Epoch milliseconds. Set once and never updated."],
    ["updatedAt", "INTEGER", "no", "Epoch milliseconds. Touched by every append, edit and rename, and is what the library sorts by."],
  ]),
  { tnum: "E.1", caption: "notes", table: { head: ["Index", "Columns", "Unique", "Purpose"], widths: [2400, 2400, 900, 3706], rows: [["(primary)", "id", "yes", "Row identity."]] } },

  { h2: "E.2 note_blocks" },
  {
    p: "A note is an ordered list of blocks rather than one long string. The separation matters " +
      "because exactly one block per session is writable by the AI — the block named by " +
      "capture_sessions.generatedBlockId — and every other block is the student's. A model that " +
      "cannot address a block cannot damage it.",
  },
  columns([
    ["id", "TEXT", "no", "Primary key, UUID."],
    ["noteId", "TEXT", "no", "Foreign key to notes.id, ON DELETE CASCADE."],
    ["position", "INTEGER", "no", "Ordering within the note. Unique per note, which is enforced by index rather than by convention."],
    ["type", "TEXT", "no", "Block kind: MARKDOWN, IMAGE, CHART or DIVIDER."],
    ["content", "TEXT", "no", "Markdown source for text blocks, or the asset reference for image blocks."],
    ["chartValue", "INTEGER", "yes", "Only meaningful for CHART blocks. Null everywhere else."],
    ["accentColor", "TEXT", "yes", "Optional hex accent applied by the student when highlighting a block."],
    ["label", "TEXT", "yes", "Optional short label shown above the block, used for figure captions."],
  ]),
  {
    tnum: "E.2", caption: "note_blocks indices",
    table: {
      head: ["Index", "Columns", "Unique", "Purpose"],
      widths: [2400, 2400, 900, 3706],
      rows: [
        ["index_note_blocks_noteId", "noteId", "no", "Reading a note is a single indexed scan."],
        ["(composite)", "noteId, position", "yes", "Makes two blocks at the same position impossible, which is what an append race would otherwise produce."],
      ],
    },
  },

  { h2: "E.3 folders" },
  {
    p: "Folders nest. The parent reference is a foreign key onto the same table, and a folder " +
      "deletion cascades down the tree. The unique index on (parentId, name) is what stops two " +
      "sibling folders sharing a name; the application relies on the database to enforce this " +
      "rather than checking first, because a check followed by an insert is a race.",
  },
  columns([
    ["id", "TEXT", "no", "Primary key, UUID."],
    ["parentId", "TEXT", "yes", "Foreign key to folders.id, ON DELETE CASCADE. Null means a root folder."],
    ["name", "TEXT", "no", "Display name, unique among siblings."],
    ["createdAt", "INTEGER", "no", "Epoch milliseconds."],
    ["updatedAt", "INTEGER", "no", "Epoch milliseconds."],
  ]),

  { h2: "E.4 note_locations" },
  {
    p: "Which folder a note is filed in. This is a separate table rather than a column on notes " +
      "for one reason: a note that has not been filed has no row here at all. Modelling it as a " +
      "nullable column would have required a sentinel value for \"unfiled\", and every query would " +
      "then have had to remember to handle it.",
  },
  columns([
    ["noteId", "TEXT", "no", "Primary key and foreign key to notes.id, ON DELETE CASCADE. One note is in at most one folder."],
    ["folderId", "TEXT", "no", "Foreign key to folders.id, ON DELETE CASCADE. Deleting a folder unfiles its notes rather than deleting them."],
  ]),
  {
    note: "Deleting a folder deletes the note_locations rows beneath it but not the notes themselves. " +
      "This was a deliberate choice after the first field trial: a student who deletes a folder " +
      "expects to lose the folder, not a lecture.",
  },

  { h2: "E.5 syllabi" },
  {
    p: "An imported syllabus, used to give the composition model the vocabulary of the course. The " +
      "SHA-256 is unique, so importing the same document twice updates the existing row instead of " +
      "creating a duplicate the student then has to choose between.",
  },
  columns([
    ["id", "TEXT", "no", "Primary key, UUID."],
    ["title", "TEXT", "no", "Display title, taken from the file name and editable."],
    ["localRelativePath", "TEXT", "no", "Path within the application's private files directory."],
    ["extractedText", "TEXT", "yes", "Plain text extracted at import. Null if extraction has not run."],
    ["sha256", "TEXT", "no", "Content hash. Unique index; the basis of duplicate detection."],
    ["importedAt", "INTEGER", "no", "Epoch milliseconds."],
    ["updatedAt", "INTEGER", "no", "Epoch milliseconds."],
  ]),

  { h2: "E.6 capture_sessions" },
  {
    p: "The transactional heart of the system. One row exists for each recording, and it holds " +
      "both the settings the session is running with and the concurrency state that makes " +
      "append-only composition safe.",
  },
  columns([
    ["id", "TEXT", "no", "Primary key, UUID."],
    ["noteId", "TEXT", "no", "Foreign key to notes.id, ON DELETE CASCADE."],
    ["mode", "TEXT", "no", "AUDIO_ONLY or BOARD. Determines whether the camera pipeline runs."],
    ["notePolicy", "TEXT", "no", "CONCISE or DETAILED. Passed to the composition model as a style constraint."],
    ["status", "TEXT", "no", "PREPARING, RUNNING, DEGRADED, FINALISING, COMPLETE or ABANDONED. Indexed, because resuming an interrupted session is a query on this column."],
    ["syllabusId", "TEXT", "yes", "Foreign key to syllabi.id, ON DELETE SET NULL. Deleting a syllabus must not delete the lectures that used it."],
    ["generatedBlockId", "TEXT", "no", "Foreign key to note_blocks.id, ON DELETE CASCADE, with a unique index. The single block the AI is permitted to extend."],
    ["revision", "INTEGER", "no", "Incremented on every accepted append. The optimistic concurrency token."],
    ["lastPatchId", "TEXT", "yes", "Identifier of the most recent accepted delta, used to make a retry idempotent."],
    ["frameIntervalMs", "INTEGER", "no", "Current camera sampling interval, 2,000 to 30,000."],
    ["changeThreshold", "REAL", "no", "Current frame-change sensitivity, 0.02 to 0.40."],
    ["primarySpeakerId", "TEXT", "yes", "Label of the voice that has dominated the session so far, or null if no voice has."],
    ["startedAt", "INTEGER", "no", "Epoch milliseconds."],
    ["stoppedAt", "INTEGER", "yes", "Null while the session is running."],
    ["updatedAt", "INTEGER", "no", "Epoch milliseconds, touched on every state change."],
  ]),
  {
    p: "Four indices exist on this table: one each on noteId, syllabusId and status for the " +
      "obvious lookups, and a unique index on generatedBlockId. That last one is a structural " +
      "guarantee rather than an optimisation — it makes it impossible for two sessions to claim " +
      "the same living block, which is the failure that would let one lecture's AI output land " +
      "inside another lecture's note.",
  },

  { h2: "E.7 transcript_segments" },
  {
    p: "Evidence of record for speech. Rows here are written before any composition call is made " +
      "and are never updated afterwards. If every AI call in a session failed, this table would " +
      "still contain the lecture.",
  },
  columns([
    ["id", "TEXT", "no", "Primary key, UUID."],
    ["sessionId", "TEXT", "no", "Foreign key to capture_sessions.id, ON DELETE CASCADE."],
    ["position", "INTEGER", "no", "Ordering within the session; unique per session."],
    ["speakerId", "TEXT", "yes", "Dominant-voice label for the chunk, or null when the chunk was ambiguous. Never a name."],
    ["startMs", "INTEGER", "no", "Offset from the start of the session, in milliseconds."],
    ["endMs", "INTEGER", "no", "Offset from the start of the session, in milliseconds."],
    ["text", "TEXT", "no", "Transcribed speech, after overlap trimming."],
    ["isFinal", "INTEGER", "no", "0 while a chunk is still being retried, 1 once it has settled."],
    ["confidence", "REAL", "yes", "Provider-reported confidence where available. Recorded but not currently acted on."],
    ["createdAt", "INTEGER", "no", "Epoch milliseconds."],
  ]),

  { h2: "E.8 visual_evidence" },
  {
    p: "Evidence of record for the board. One row per accepted frame. The temporary path is " +
      "cleared once extraction has completed, so a lecture's worth of full-resolution frames does " +
      "not accumulate on a phone.",
  },
  columns([
    ["id", "TEXT", "no", "Primary key, UUID."],
    ["sessionId", "TEXT", "no", "Foreign key to capture_sessions.id, ON DELETE CASCADE."],
    ["capturedAt", "INTEGER", "no", "Epoch milliseconds at the moment the frame was accepted."],
    ["fingerprint", "TEXT", "no", "Perceptual hash used to reject a board that has already been extracted."],
    ["deltaScore", "REAL", "no", "The change score that caused this frame to be kept. Retained for diagnosis; this column is what made the settle-gate defect visible."],
    ["processingState", "TEXT", "no", "PENDING, UPLOADING, EXTRACTED, DUPLICATE or FAILED. Indexed, because the upload queue is a query on it."],
    ["temporaryPath", "TEXT", "yes", "Path to the retained JPEG, cleared after extraction."],
    ["error", "TEXT", "yes", "Failure reason where processingState is FAILED."],
    ["processedAt", "INTEGER", "yes", "Epoch milliseconds of the terminal state."],
  ]),

  { h2: "E.9 note_assets" },
  {
    p: "An image that has earned a permanent place in a note. Promotion from visual_evidence to " +
      "note_assets is what distinguishes a frame that was merely captured from one the note " +
      "actually refers to.",
  },
  columns([
    ["id", "TEXT", "no", "Primary key, UUID."],
    ["noteId", "TEXT", "no", "Foreign key to notes.id, ON DELETE CASCADE."],
    ["evidenceId", "TEXT", "yes", "Foreign key to visual_evidence.id, ON DELETE SET NULL. Purging evidence must not remove the image from the note."],
    ["kind", "TEXT", "no", "BOARD_CROP, DIAGRAM or IMPORTED."],
    ["localRelativePath", "TEXT", "no", "Path within the note's asset directory. Unique across the database, which is what a path-traversal check can then be written against."],
    ["caption", "TEXT", "no", "Caption shown beneath the image in the reader and in the export."],
    ["createdAt", "INTEGER", "no", "Epoch milliseconds."],
  ]),

  { h2: "E.10 block_provenance" },
  {
    p: "Answers the question \"where did this sentence come from?\" for any block on screen. It is " +
      "the mechanism behind the review flag: a flag can quote the exact transcript window that a " +
      "disputed claim was generated from, because that window is recorded here.",
  },
  columns([
    ["blockId", "TEXT", "no", "Primary key and foreign key to note_blocks.id, ON DELETE CASCADE."],
    ["sessionId", "TEXT", "no", "Foreign key to capture_sessions.id, ON DELETE CASCADE."],
    ["sourceKind", "TEXT", "no", "TRANSCRIPT, BOARD, SYLLABUS or STUDENT."],
    ["sourceId", "TEXT", "no", "Identifier of the specific transcript segment or evidence row. Indexed."],
    ["startMs", "INTEGER", "yes", "Start of the source window, where the source is speech."],
    ["endMs", "INTEGER", "yes", "End of the source window."],
    ["confidence", "REAL", "yes", "Grounding ratio recorded at the time the block was accepted."],
    ["reviewState", "TEXT", "no", "UNREVIEWED, FLAGGED, ACCEPTED or ANNOTATED."],
  ]),

  { h2: "E.11 Migration history" },
  {
    table: {
      head: ["Version", "Change", "Reason"],
      widths: [1100, 3400, 4906],
      rows: [
        ["1 to 2", "Added syllabi; added capture_sessions.syllabusId with ON DELETE SET NULL.", "Syllabus import was added after the first capture pipeline worked."],
        ["2 to 3", "Added block_provenance; backfilled existing blocks with sourceKind = STUDENT.", "Review flags need a source to quote, and existing blocks had no recorded source."],
        ["3 to 4", "Added folders and note_locations; added the unique index on (parentId, name).", "Field feedback: a flat list of notes is unusable after a fortnight of lectures."],
        ["4 to 5", "Added visual_evidence.deltaScore; added the index on processingState.", "Added specifically to diagnose why no board frame was ever captured. It is the column that found the defect."],
      ],
    },
    tnum: "E.3",
    caption: "Room migration history, with the reason each version exists",
  },
];

// ---------------------------------------------------------------- API

const APP_API = [
  { h1: "Appendix F — Backend API Specification" },
  {
    p: "The proxy exposes five routes. Four are pipeline routes that require authentication and " +
      "are subject to the rate limit and the daily budget; the fifth is an unauthenticated health " +
      "check used by the platform and by the application's Connection screen. There is no web " +
      "framework: the router is a chain of comparisons on the request path, and every validator " +
      "is visible in the single source file reproduced in Appendix W.",
  },
  {
    table: {
      head: ["Method", "Path", "Auth", "Purpose"],
      widths: [900, 2400, 900, 5206],
      rows: [
        ["GET", "/health", "no", "Liveness, deployed model routing and budget headroom."],
        ["POST", "/v1/audio/transcribe", "yes", "One 20-second PCM chunk in, transcript segments and speaker labels out."],
        ["POST", "/v1/board/extract", "yes", "One board JPEG in, text, equations, concepts and crop regions out."],
        ["POST", "/v1/notes/refine", "yes", "Bounded note context plus new evidence in, an append-only Markdown delta out."],
        ["POST", "/v1/notes/verify", "yes", "A finished note plus its evidence in, a list of findings out."],
      ],
    },
    tnum: "F.1",
    caption: "Route summary",
  },

  { h2: "F.1 Authentication" },
  {
    p: "Every pipeline route requires an Authorization header carrying a bearer token. The " +
      "comparison is constant-time, so a caller cannot learn the token a character at a time by " +
      "measuring how long a rejection takes. A failed comparison is counted against the rate limit " +
      "and returns before any provider call is made, so an unauthenticated caller cannot cost " +
      "anything beyond the proxy's own CPU.",
  },
  {
    code: "Authorization: Bearer <VOXBOX_CLIENT_TOKEN>\n" +
      "Content-Type: application/json\n" +
      "\n" +
      "401  { \"error\": \"unauthorized\" }          missing or wrong token\n" +
      "429  { \"error\": \"rate_limited\", \"retryAfterMs\": 42000 }\n" +
      "503  { \"error\": \"budget_exhausted\", \"resetsAt\": \"2026-08-12T00:00:00Z\" }",
    caption: "Authentication header and the three rejections that precede any provider call",
  },
  {
    p: "The provider credential is a separate secret that exists only in the deployment " +
      "environment. It is never sent to the device, never written to the repository and never " +
      "logged. The client token can therefore be rotated without touching the provider account, " +
      "and the provider key can be rotated without rebuilding the application.",
  },

  { h2: "F.2 GET /health" },
  {
    code: "GET /health\n" +
      "\n" +
      "200 {\n" +
      "  \"status\": \"ok\",\n" +
      "  \"uptimeSeconds\": 84213,\n" +
      "  \"models\": {\n" +
      "    \"transcribe\": \"<provider model id>\",\n" +
      "    \"vision\":     \"<provider model id>\",\n" +
      "    \"compose\":    \"<provider model id>\"\n" +
      "  },\n" +
      "  \"budget\": { \"used\": 118, \"limit\": 900, \"resetsAt\": \"2026-08-12T00:00:00Z\" }\n" +
      "}",
    caption: "Health response",
  },
  {
    p: "The models block exists because of a specific incident. Three separately reported defects " +
      "turned out to have one cause: a fix had been committed but never pushed, so the deployed " +
      "proxy was running older code. Health now reports which model each route will actually use, " +
      "and the Connection screen shows it, so \"is my fix live?\" is a question the application " +
      "itself can answer. The standing rule that came out of it is recorded in Chapter 13: a " +
      "commit is not a release.",
  },

  { h2: "F.3 POST /v1/audio/transcribe" },
  {
    code: "{\n" +
      "  \"sessionId\":  \"<uuid>\",\n" +
      "  \"sequence\":   14,\n" +
      "  \"startMs\":    280000,\n" +
      "  \"durationMs\": 20000,\n" +
      "  \"overlapMs\":  2000,\n" +
      "  \"audio\":      \"<base64 PCM16, 16 kHz, mono>\"\n" +
      "}",
    caption: "Request",
  },
  {
    code: "{\n" +
      "  \"segments\": [\n" +
      "    { \"startMs\": 280000, \"endMs\": 288400,\n" +
      "      \"text\": \"...\", \"speaker\": \"A\", \"confidence\": 0.91 }\n" +
      "  ],\n" +
      "  \"dominantSpeaker\": \"A\",\n" +
      "  \"speakerShare\":    0.71\n" +
      "}",
    caption: "Response, validated against a strict JSON schema before it is returned",
  },
  {
    p: "The response schema is declared to the provider with require_parameters set, and the proxy " +
      "validates the result again before returning it. A response that is prose rather than " +
      "structure is a 502 from the proxy, not a parse failure on the phone. This matters because " +
      "the phone is the place where a failure costs the student a piece of their lecture, and the " +
      "proxy is the place where it costs a retry.",
  },
  {
    note: "Known limitation, recorded honestly: across every field chunk measured so far the provider " +
      "returned a single segment of 250 to 400 characters rather than several. Timestamps are " +
      "therefore coarser than the schema allows, and a speaker change inside a chunk is invisible. " +
      "This is listed as an open defect in Chapter 15 and as planned work in Chapter 16.",
  },

  { h2: "F.4 POST /v1/board/extract" },
  {
    code: "{\n" +
      "  \"sessionId\":  \"<uuid>\",\n" +
      "  \"evidenceId\": \"<uuid>\",\n" +
      "  \"capturedAt\": 1786512000000,\n" +
      "  \"deltaScore\": 0.31,\n" +
      "  \"image\":      \"<base64 JPEG>\"\n" +
      "}",
    caption: "Request",
  },
  {
    code: "{\n" +
      "  \"text\":      \"...\",\n" +
      "  \"equations\": [\"E = mc^2\"],\n" +
      "  \"concepts\":  [\"mass-energy equivalence\"],\n" +
      "  \"regions\":   [ { \"x\": 0.12, \"y\": 0.34, \"w\": 0.4, \"h\": 0.3,\n" +
      "                   \"kind\": \"diagram\", \"caption\": \"...\" } ],\n" +
      "  \"legible\":   true\n" +
      "}",
    caption: "Response",
  },
  {
    p: "Regions are returned in normalised coordinates so that the crop can be taken from the " +
      "full-resolution frame the phone still holds, rather than from the downscaled copy that was " +
      "uploaded. The legible flag lets the model decline: a photograph of a wall, a hand in front " +
      "of the board or an out-of-focus frame returns legible false and costs nothing further.",
  },

  { h2: "F.5 POST /v1/notes/refine" },
  {
    p: "The composition route, and the one the cost model turns on. Everything sent here is " +
      "bounded, and the bounds are enforced on the client and re-checked on the server.",
  },
  {
    table: {
      head: ["Field", "Bound", "Why this bound"],
      widths: [2300, 1600, 5506],
      rows: [
        ["syllabusExtract", "4,000 chars", "Enough to carry the vocabulary of a unit; beyond this the model gains nothing and the bill grows."],
        ["noteOutline", "4,000 chars", "Headings only. The model needs to know what has been covered, not what was said."],
        ["recentMarkdown", "6,000 chars", "The tail of the note, so a delta reads as a continuation rather than a restart."],
        ["evidence", "unbounded", "This chunk's transcript and board text. It is the only part that is new, and it is small."],
        ["baseHash", "64 chars", "SHA-256 of the note the delta is being built against."],
        ["revision", "integer", "Optimistic concurrency token; must still match at commit time."],
      ],
    },
    tnum: "F.2",
    caption: "Context bounds on the composition route",
  },
  {
    code: "{\n" +
      "  \"delta\":      \"## Integration by parts\\n\\n...\",\n" +
      "  \"assets\":     [ { \"evidenceId\": \"<uuid>\", \"caption\": \"...\", \"anchor\": \"after-paragraph-2\" } ],\n" +
      "  \"flags\":      [ { \"claim\": \"...\", \"evidence\": \"...\", \"reason\": \"disagrees with transcript\" } ],\n" +
      "  \"baseHash\":   \"<echoed back>\",\n" +
      "  \"groundingRatio\": 0.47\n" +
      "}",
    caption: "Response",
  },
  {
    p: "An empty delta is a valid response. This sounds obvious and was not: the prompt asked the " +
      "model to return nothing when a chunk added nothing, while both the proxy and the client " +
      "treated an empty delta as a failure and retried it. A chunk of silence therefore cost three " +
      "calls instead of one. Both sides now accept it, and the retry storm it caused is described " +
      "in Chapter 13.",
  },

  { h2: "F.6 POST /v1/notes/verify" },
  {
    p: "Runs once, when the student presses stop. It receives the finished note together with the " +
      "evidence that produced it and returns findings — places where the note and the evidence " +
      "disagree. Findings become review flags in the reader, each showing both readings.",
  },
  {
    code: "{\n" +
      "  \"findings\": [\n" +
      "    { \"kind\":     \"unsupported\",\n" +
      "      \"claim\":    \"the transform is linear for all bounded inputs\",\n" +
      "      \"evidence\": \"the transform is linear\",\n" +
      "      \"startMs\":  1284000,\n" +
      "      \"severity\": \"medium\" }\n" +
      "  ]\n" +
      "}",
    caption: "Response",
  },
  {
    p: "Neither action offered on a review flag rewrites the captured evidence. The student can " +
      "accept the note's reading, which records the decision in block_provenance.reviewState, or " +
      "annotate it, which adds a student-owned block beside it. There is deliberately no \"fix it " +
      "for me\" action, because the system has no way to know which reading is correct and " +
      "pretending otherwise would be the most damaging thing it could do.",
  },

  { h2: "F.7 Error codes" },
  {
    table: {
      head: ["Status", "Body", "Meaning and client behaviour"],
      widths: [900, 2000, 6506],
      rows: [
        ["400", "invalid_request", "Schema violation in the request. Not retried; logged as a bug."],
        ["401", "unauthorized", "Missing or wrong bearer token. Not retried."],
        ["413", "payload_too_large", "Chunk or image over the configured ceiling. Not retried."],
        ["429", "rate_limited", "Per-minute window exceeded. Retried once after retryAfterMs, then the chunk is queued."],
        ["502", "upstream_invalid", "Provider returned something the schema rejects. Retried twice with backoff."],
        ["503", "budget_exhausted", "Daily cap reached. Not retried. The session continues capturing and retains audio."],
        ["504", "upstream_timeout", "Provider did not answer in time. Retried twice with backoff."],
      ],
    },
    tnum: "F.3",
    caption: "Error codes and the client's response to each",
  },
  {
    p: "The distinction between a retried and a non-retried failure was itself a defect once. An " +
      "early build retried a permanent quota error on a fixed schedule, which turned one exhausted " +
      "account into a steady stream of guaranteed-failing requests for the remainder of the " +
      "lecture. Permanent failures are now terminal on the client, and the session degrades to " +
      "retaining audio for later recovery rather than pretending it is still working.",
  },
];

// ---------------------------------------------------------------- test register

const APP_TESTS = [
  { h1: "Appendix G — Test Case Register" },
  {
    p: "180 automated tests run on every build: 134 Android unit tests, 43 backend contract tests " +
      "and 3 instrumentation tests that execute on a physical device. This appendix lists them by " +
      "suite, with the concern each suite exists to protect. The source of every suite is " +
      "reproduced in Appendix X.",
  },
  { fig: "docs/diagrams/fig-testing.png", num: "G.1", caption: "Test distribution, and what the suite did and did not catch" },

  { h2: "G.1 Algorithm suites" },
  {
    p: "These carry the risk. Every one of the six algorithms described in Chapter 7 has a suite " +
      "here, and several individual tests exist because a specific field failure is not permitted " +
      "to recur.",
  },
  {
    table: {
      head: ["Suite", "Tests", "What it protects"],
      widths: [3100, 700, 5606],
      rows: [
        ["FrameChangeDetectorTest", "14", "Exposure compensation, the relative settle test, a person crossing the frame, forced capture, and replay of luminance scores taken from the real field log."],
        ["SpeechPaceTrackerTest", "10", "Words-per-minute measurement, the three pace bands, clamping, and the case that caused the compounding defect: repeated suggestions must not accumulate."],
        ["TranscriptOverlapTest", "9", "Seam repair at every word boundary of a fixed paragraph, no-overlap, total-overlap, punctuation-only differences and a legitimate spoken repetition."],
        ["GeneratedMarkdownRevisionTest", "5", "The optimistic revision check, including an interleaved append and a student edit landing mid-session."],
        ["IncrementalNoteContextBuilderTest", "3", "Context bounds hold as the note grows; the request size does not track note length."],
        ["NoteRefinementParserTest", "4", "Delta parsing, including the empty delta that was once treated as a failure."],
        ["NoteVerificationTest", "4", "Findings parse into review flags and carry both readings."],
        ["BoardExtractionCoordinatorTest", "2", "Upload queueing and duplicate rejection by fingerprint."],
        ["DiagramCropperTest", "2", "Normalised region coordinates map onto the full-resolution frame."],
        ["OcrImagePreparationTest", "3", "Downscale, orientation and compression before upload."],
      ],
    },
    tnum: "G.1",
    caption: "Algorithm and pipeline suites",
  },

  { h2: "G.2 Rendering, data and export suites" },
  {
    table: {
      head: ["Suite", "Tests", "What it protects"],
      widths: [3100, 700, 5606],
      rows: [
        ["MarkdownTest", "15", "The parser: headings, bullets, numbered lists, quotes, code fences, tables, rules and the malformed input that appeared in a real note."],
        ["ReviewFlagParserTest", "6", "Flag extraction and stripping, so a flag never leaks into the exported Markdown."],
        ["RemoveFlagTest", "5", "Removing a resolved flag leaves the surrounding text intact."],
        ["DiagramLinkTest", "4", "Asset references resolve, including the path-traversal case that must be rejected."],
        ["MarkdownExporterTest", "5", "Export produces valid Markdown with working relative asset paths."],
        ["NoteSearchTest", "4", "Search across titles and block content."],
        ["NoteBlockEditTest", "3", "Student edits do not disturb the AI's living block."],
        ["NoteBlockDisplayTest", "2", "Block ordering and type dispatch in the reader."],
        ["BoardNoteMapperTest", "2", "Board extraction maps onto blocks and assets."],
        ["CaptureNoteFormattingTest", "3", "Timestamp and speaker formatting, including rejection of any timestamp the client did not itself emit."],
        ["CaptureSessionModelsTest", "3", "Session state transitions match the state machine in Figure 6.6."],
        ["SyllabusImporterTest", "1", "Import deduplicates by SHA-256."],
        ["RetainedAudioRecoveryTest", "3", "A retained WAV is discovered, offered and recovered."],
      ],
    },
    tnum: "G.2",
    caption: "Rendering, data and export suites",
  },

  { h2: "G.3 Network and configuration suites" },
  {
    table: {
      head: ["Suite", "Tests", "What it protects"],
      widths: [3100, 700, 5606],
      rows: [
        ["VoxBoxServiceFailureTest", "7", "Every error code in Table F.3 maps to the correct client behaviour, and permanent failures are not retried."],
        ["HttpBoardExtractionClientTest", "5", "Request shape, response validation and timeout handling."],
        ["AudioPipelineTest", "4", "Chunk ordering, the never-drop queue and overlap carry."],
        ["AudioTranscriptionParserTest", "1", "Segment parsing."],
        ["VoxBoxApiConfigTest", "2", "The base URL and token are read from configuration, and the token is never logged."],
        ["Backend: server.test.mjs", "43", "Auth, constant-time comparison, rate limit, daily budget, schema rejection, grounding ratio, empty-delta acceptance and every error mapping."],
      ],
    },
    tnum: "G.3",
    caption: "Network and configuration suites",
  },

  { h2: "G.4 Device tests" },
  {
    p: "Three instrumentation tests run on a physical phone. They exist because of a defect class " +
      "that unit tests structurally cannot catch: a screen that compiles, passes every test and " +
      "cannot be reached or drawn.",
  },
  {
    table: {
      head: ["Test", "What it proves"],
      widths: [3400, 6006],
      rows: [
        ["navigationGraphReachesEveryDestination", "All sixteen destinations can be reached from Home. This is the test that would have caught six settings pages crashing on a negative padding value."],
        ["dialogsRenderWhenInvoked", "Rename, delete and folder dialogs actually draw. Added after rename and delete compiled, passed 109 tests and did nothing on screen."],
        ["releaseBuildReachesProxyOverHttps", "A release build authenticates against the deployed proxy. Added after a fix was committed but never pushed."],
      ],
    },
    tnum: "G.4",
    caption: "Device instrumentation tests, and the field failure each one encodes",
  },
  {
    note: "Each of the three device tests exists because of a specific incident, not because of a " +
      "coverage target. That is the honest description of how this suite grew: the unit tests were " +
      "written from the design, and the device tests were written from the failures.",
  },
];

// ---------------------------------------------------------------- field trials

const APP_TRIALS = [
  { h1: "Appendix H — Field Trial Records" },
  {
    p: "Four trials were run in real lectures. Each is recorded here with what was observed, what " +
      "the diagnosis turned out to be and what changed as a result. The trials are the most " +
      "valuable evidence in this project, because every one of them contradicted something the " +
      "test suite was asserting.",
  },
  { fig: "docs/diagrams/fig-timeline.png", num: "H.1", caption: "Project timeline, with the four field trials marked" },

  { h2: "H.1 Trial 1 — the note was unreadable" },
  {
    table: {
      head: ["Observed", "Diagnosis", "Change"],
      widths: [3100, 3100, 3206],
      rows: [
        ["The note showed literal ## and $$ characters instead of headings and formulas.", "The reader displayed the Markdown source. No parser existed; the design had assumed one.", "A 243-line parser and a 292-line renderer, covered by MarkdownTest."],
        ["Words disappeared at regular intervals throughout the transcript.", "Chunks abutted exactly. A word straddling the boundary was seen only as two fragments and each was guessed.", "A two-second overlap plus word-level trimming with a three-word minimum."],
        ["The export button did nothing at all.", "The file was written to private storage where no other application can read it.", "FileProvider and a share sheet."],
        ["Notes could not be renamed or deleted.", "Not implemented.", "Rename and delete with a confirmation dialog."],
        ["\"AI refinement isn't available\" appeared persistently.", "The fix had been committed six commits earlier and never pushed. The live proxy was running old code.", "Pushed; /health now reports deployed model routing."],
      ],
    },
    tnum: "H.1",
    caption: "Trial 1",
  },

  { h2: "H.2 Trial 2 — nothing was captured at all" },
  {
    p: "A full lecture in board mode produced a note containing speech and not a single board " +
      "image. Every unit test passed. Nothing in the application reported an error: the session " +
      "ran, the camera preview was live, and the counter showed frames being considered and " +
      "rejected. There was no way to see why.",
  },
  {
    p: "This trial produced no fix, because it produced no diagnosis. What it produced instead was " +
      "the temporary on-device diagnostic log: a 500-line ring buffer, written locally, exportable " +
      "through the share sheet, holding a timestamped record of every decision the capture " +
      "pipeline makes. Speech is recorded as a character count and a sixty-character prefix and " +
      "never in full; no audio, no images and no note bodies are written; nothing is uploaded; and " +
      "it is compiled out of release builds. Its removal checklist is tracked in " +
      "docs/TEMPORARY_DEBUG_LOG.md so that a diagnostic aid does not quietly become a permanent " +
      "feature.",
  },
  {
    note: "The instructive part is the ordering. Two rounds of code reading and one round of adding " +
      "unit tests produced nothing. Fifteen seconds of reading a real log produced the answer. The " +
      "standing lesson recorded in Chapter 13 is: instrument the running system.",
  },

  { h2: "H.3 Trial 3 — the log answered it" },
  {
    p: "The third trial returned a 218-line log. Two defects were visible almost immediately, and " +
      "both had been invisible to the entire test suite.",
  },
  {
    table: {
      head: ["Log evidence", "Defect", "Fix"],
      widths: [3400, 2900, 3106],
      rows: [
        ["Settle scores on a static board ranged from 0.09 to 0.17 across 40 consecutive frames.", "The settle gate tested against an absolute 0.035 — an order of magnitude below the noise floor of a handheld phone. It could never open.", "A relative test: the scene must settle to below 45 per cent of the score that triggered the hold."],
        ["Interval fell 8.0 s, 4.8 s, 2.9 s, 2.0 s while the threshold fell 0.08, 0.064, 0.051, 0.041 over four minutes of ordinary speech.", "Each suggestion divided the previous value rather than the baseline, so adjustments compounded. Interval and threshold ended up asserting opposite things.", "suggest() takes the session baseline as an argument and multiplies that. The loop has no memory of its own output."],
      ],
    },
    tnum: "H.2",
    caption: "Trial 3 — what the log showed",
  },
  {
    p: "A third finding from this trial did not become a code change. Adding \"capture anyway after " +
      "N frames\" to guarantee that a never-settling scene eventually yields something broke a test " +
      "named personMovingThroughFrameIsNeverAccepted, and correctly so: a scene that never settles " +
      "and a person walking across the board are the same signal, and no amount of waiting " +
      "distinguishes them. The cap was removed and the need was met instead by giving the student " +
      "a manual capture button, which is the only thing in the system that actually knows whether " +
      "the board is worth photographing.",
  },

  { h2: "H.4 Trial 4 — the first accepted captures" },
  {
    p: "A 92-second instrumented session on 11 August 2026 with the relative settle test in place. " +
      "Three frames were accepted and nine were skipped, and the note advanced to revision 5. " +
      "These were the first board frames the system had ever accepted in the field.",
  },
  { shots: [
      { file: "16-live-running.png", num: "H.2", caption: "Trial 4, mid-session: 3 kept, 9 skipped, revision 5" },
      { file: "15-live-tuning.png", num: "H.3", caption: "The live tuning panel, adjustable without ending the session" },
    ], opts: { perRow: 2, width: 210 } },
  {
    p: "The session is also an honest illustration of a separate limitation. It was run in a room " +
      "with no lecturer speaking, so the transcript contains one greeting and then five entries " +
      "reading \"No clear speech was found in the audio chunk\". That is the correct behaviour — " +
      "the system declined to invent content from silence, which is exactly what the grounding " +
      "guard exists to enforce — but it means this particular session demonstrates the capture " +
      "path and not the composition path.",
  },
  {
    p: "What remains unproven is documented rather than glossed over. Diagram placement beside the " +
      "relevant text and duplicate rejection by fingerprint both depend on an accepted frame " +
      "reaching extraction, which had never happened before this trial. Chapter 15 lists nine " +
      "features in this category, and it is the most likely place in the system for a hidden " +
      "defect to be sitting.",
  },
];

// ---------------------------------------------------------------- user manual

const APP_MANUAL = [
  { h1: "Appendix I — User Manual" },
  {
    p: "This appendix walks through the application as a student encounters it. Every screenshot " +
      "was taken from a build running on a physical device, not from a mock-up or an emulator, so " +
      "the layout, the type sizes and the state shown are what the application actually produces.",
  },

  { h2: "I.1 First launch" },
  {
    p: "Onboarding runs once and explains three things: what is recorded, where it is stored and " +
      "what leaves the device. The consent step is not a formality. Lecture audio is processed by " +
      "a third-party model, and the screen says so in those words rather than in a link to a " +
      "policy.",
  },
  { shots: [
      { file: "01-onboarding-1.png", num: "I.1", caption: "Onboarding, first step" },
      { file: "02-onboarding-2.png", num: "I.2", caption: "What is captured and where it is kept" },
    ], opts: { perRow: 2, width: 224 } },
  { shots: [
      { file: "03-onboarding-3.png", num: "I.3", caption: "Consent, stated plainly" },
      { file: "04-home.png", num: "I.4", caption: "Home, with recent notes and a single primary action" },
    ], opts: { perRow: 2, width: 224 } },

  { h2: "I.2 Starting a session" },
  {
    p: "Capture setup asks three questions and then gets out of the way: audio only or board, " +
      "concise or detailed, and which syllabus if any. Board mode is the more capable and the more " +
      "expensive; audio mode never opens the camera and never spends a vision call.",
  },
  { shots: [
      { file: "05-capture-setup.png", num: "I.5", caption: "Capture setup" },
      { file: "06-capture-setup-lower.png", num: "I.6", caption: "Capture setup, lower half: note policy and syllabus" },
    ], opts: { perRow: 2, width: 224 } },

  { h2: "I.3 During the lecture" },
  {
    p: "The live screen shows elapsed time, the current revision and a running count of frames " +
      "kept and skipped. Two controls matter during a lecture and both were added because of field " +
      "use: Capture now, which overrides the automatic filter when the student can see that the " +
      "board is worth photographing, and the tuning panel, which changes the sampling interval and " +
      "the sensitivity without ending the session. Before that panel existed, a badly chosen " +
      "sensitivity was fixed for the whole lecture.",
  },
  { shots: [
      { file: "14-live-board.png", num: "I.7", caption: "Board mode, camera preview with the frame guide" },
      { file: "16-live-running.png", num: "I.8", caption: "Running session: timer, revision, kept and skipped" },
    ], opts: { perRow: 2, width: 224 } },
  {
    note: "Pressing the system back gesture during a live session collapses the preview rather than " +
      "leaving the session. The first field trial lost eleven minutes of a lecture to an " +
      "accidental back gesture, and this is the correction.",
  },

  { h2: "I.4 Reading the note" },
  {
    p: "The reader renders Markdown properly: headings, bullets, numbered lists, quotes, code, " +
      "tables and horizontal rules. Board crops appear inline with captions and open in a lightbox " +
      "with both pinch and slider zoom. Review flags appear as coloured cards showing the note's " +
      "reading and the evidence's reading side by side.",
  },
  { shots: [
      { file: "17-note-reader.png", num: "I.9", caption: "The reader. The amber lines are the system declining to invent content from silence." },
      { file: "18-library-populated.png", num: "I.10", caption: "Library, with folders and search" },
    ], opts: { perRow: 2, width: 224 } },
  {
    p: "Figure I.9 is worth reading carefully, because it shows a refusal rather than a success. " +
      "The session behind it was recorded in a quiet room. Five audio chunks contained no speech, " +
      "and instead of producing plausible lecture notes the system recorded, at each timestamp, " +
      "that no clear speech was found. A system that filled those five gaps with fluent text would " +
      "look better in a screenshot and would be worse in every way that matters.",
  },

  { h2: "I.5 Organising and exporting" },
  {
    p: "Notes can be renamed, deleted with confirmation, and filed into nested folders — a folder " +
      "per subject with a folder per unit inside it is the arrangement the design was asked for. " +
      "Export produces a ZIP containing the Markdown and every asset it references, with relative " +
      "paths that resolve after extraction, and hands it to the system share sheet.",
  },
  { shots: [
      { file: "19-new-folder.png", num: "I.11", caption: "Nested folders" },
      { file: "20-note-actions.png", num: "I.12", caption: "Note actions, including delete with confirmation" },
    ], opts: { perRow: 2, width: 224 } },

  { h2: "I.6 Settings" },
  {
    p: "Six sub-pages: capture defaults, connection, storage, appearance, privacy and data, and " +
      "the temporary diagnostics page. The connection page reports live health from the proxy, " +
      "including which model each route is currently routed to, which is how a deployment can be " +
      "confirmed from the phone rather than assumed.",
  },
  { shots: [
      { file: "08-settings.png", num: "I.13", caption: "Settings" },
      { file: "10-connection.png", num: "I.14", caption: "Connection, showing live proxy health" },
    ], opts: { perRow: 2, width: 224 } },
  { shots: [
      { file: "11-privacy.png", num: "I.15", caption: "Privacy and data, with the retention controls" },
      { file: "13-diagnostics.png", num: "I.16", caption: "Diagnostics — temporary, and scheduled for removal" },
    ], opts: { perRow: 2, width: 224 } },
];

// ---------------------------------------------------------------- glossary

const APP_GLOSSARY = [
  { h1: "Appendix J — Glossary and Abbreviations" },
  {
    table: {
      head: ["Term", "Meaning in this report"],
      widths: [2600, 6806],
      rows: [
        ["Append-only delta", "A response from the composition model containing only new text. It is added to the end of the living block and never replaces anything already there."],
        ["Baseline (frame)", "The last accepted board image. Every new frame is scored against it, not against the previous frame."],
        ["Baseline (pace)", "The interval and threshold a session started with, or the values the student last set by hand. Adaptive suggestions multiply these and never the current values."],
        ["Board mode", "A capture session that samples the camera as well as the microphone."],
        ["Change score", "The fraction of the 32 x 32 luminance grid that differs from the baseline after global exposure has been subtracted. Range 0 to 1."],
        ["Chunk", "Twenty seconds of audio, overlapping the previous chunk by two seconds."],
        ["Client token", "The bearer token compiled into the application, which authorises use of the proxy and nothing else. Distinct from the provider key, which the application never holds."],
        ["Composition", "The step that turns transcript and board text into Markdown."],
        ["DataStore", "The Android preferences store used for settings. Replaces SharedPreferences."],
        ["Delta score", "See change score. The column name in visual_evidence."],
        ["Dominant speaker", "The voice holding at least 58 per cent of the voiced time in a chunk with a lead of at least 15 percentage points. Not an identity."],
        ["Evidence", "Anything captured rather than generated: transcript segments, board text, board images. Never overwritten by model output."],
        ["Extraction", "The step that turns a board photograph into text, equations, concepts and crop regions."],
        ["Fingerprint", "A perceptual hash of an accepted frame, used to reject a board that has already been extracted."],
        ["Grounding ratio", "The fraction of content words in a generated delta that also appear in the evidence it claims to summarise. Below 0.25 the delta is discarded."],
        ["Living block", "The single note block a session's AI output is permitted to extend, named by capture_sessions.generatedBlockId."],
        ["Optimistic concurrency", "Committing a change only if the state it was computed against has not moved, rather than holding a lock while it is computed."],
        ["Proxy", "The project's own Node.js service. Holds the provider credential, enforces authentication, rate limits, budget and the grounding guard."],
        ["Provider", "The third-party service that runs the models. Reached only by the proxy."],
        ["Review flag", "A card in the reader showing a disagreement between the note and the evidence, displaying both readings. Neither available action rewrites the evidence."],
        ["Revision", "An integer on capture_sessions incremented by every accepted append; the optimistic concurrency token."],
        ["Settle test", "The check that a held frame's scene has stopped moving before the frame is spent on an extraction call. Relative, not absolute."],
        ["Syllabus", "An imported course document used to give the composition model the vocabulary of the unit."],
        ["Verification pass", "A single call made when the student presses stop, comparing the finished note against its evidence and returning findings."],
      ],
    },
    tnum: "J.1",
    caption: "Glossary",
  },
  {
    table: {
      head: ["Abbreviation", "Expansion"],
      widths: [2200, 7206],
      rows: [
        ["ADB", "Android Debug Bridge"],
        ["APK", "Android Package, the installable application file"],
        ["API", "Application Programming Interface"],
        ["CameraX", "The Android Jetpack camera library"],
        ["DXA", "Twentieths of a point, the unit used in Office document layout"],
        ["ER", "Entity-Relationship"],
        ["HTTPS", "Hypertext Transfer Protocol Secure"],
        ["JSON", "JavaScript Object Notation"],
        ["ML Kit", "Google's on-device machine learning library, used here for text recognition"],
        ["OCR", "Optical Character Recognition"],
        ["PCM", "Pulse-Code Modulation, the uncompressed audio format captured at 16 kHz mono"],
        ["Room", "The Android persistence library layered over SQLite"],
        ["SHA-256", "Secure Hash Algorithm, 256-bit, used to bind a delta to the note it was built against"],
        ["SQLite", "The embedded database engine Room is built on"],
        ["TLS", "Transport Layer Security"],
        ["UML", "Unified Modeling Language"],
        ["UUID", "Universally Unique Identifier"],
        ["WAV", "Waveform Audio File Format, used for retained audio recovery"],
        ["wpm", "Words per minute"],
      ],
    },
    tnum: "J.2",
    caption: "Abbreviations",
  },
];

module.exports = { APP_SCHEMA, APP_API, APP_TESTS, APP_TRIALS, APP_MANUAL, APP_GLOSSARY };

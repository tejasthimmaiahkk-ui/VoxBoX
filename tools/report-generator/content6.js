/**
 * Reference appendices O to R: schema DDL, prompt specification, field diagnostic log and a
 * worked end-to-end example.
 */

const APP_DDL = [
  { h1: "Appendix O — Database Schema, as SQL" },
  {
    p: "The schema Room generates, written out as SQL. Room produces this from the annotated Kotlin " +
      "entities in notes/NoteEntities.kt, which are reproduced in Appendix V; this appendix gives " +
      "the same schema in the form a database reader expects, so that the constraints can be read " +
      "without knowing Room's annotation vocabulary.",
  },
  {
    p: "Every foreign key here has an explicit ON DELETE clause, and the choice differs between " +
      "them. CASCADE is used where the child cannot outlive its parent; SET NULL is used in the " +
      "two places where it can, and both of those are load-bearing decisions rather than defaults.",
  },

  { h2: "O.1 Tables" },
  {
    code: "CREATE TABLE notes (\n" +
      "    id         TEXT    NOT NULL PRIMARY KEY,\n" +
      "    title      TEXT    NOT NULL,\n" +
      "    createdAt  INTEGER NOT NULL,\n" +
      "    updatedAt  INTEGER NOT NULL\n" +
      ");\n" +
      "\n" +
      "CREATE TABLE note_blocks (\n" +
      "    id           TEXT    NOT NULL PRIMARY KEY,\n" +
      "    noteId       TEXT    NOT NULL,\n" +
      "    position     INTEGER NOT NULL,\n" +
      "    type         TEXT    NOT NULL,\n" +
      "    content      TEXT    NOT NULL,\n" +
      "    chartValue   INTEGER,\n" +
      "    accentColor  TEXT,\n" +
      "    label        TEXT,\n" +
      "    FOREIGN KEY (noteId) REFERENCES notes(id) ON DELETE CASCADE\n" +
      ");\n" +
      "CREATE INDEX        index_note_blocks_noteId          ON note_blocks(noteId);\n" +
      "CREATE UNIQUE INDEX index_note_blocks_noteId_position ON note_blocks(noteId, position);",
    caption: "notes and note_blocks",
  },
  {
    p: "The unique index on (noteId, position) is the guarantee that two blocks cannot occupy the " +
      "same place in a note. Without it, two concurrent appends could both compute the same next " +
      "position and both succeed, and the note would then render in an order that depends on which " +
      "row the query engine happened to return first.",
  },
  {
    code: "CREATE TABLE folders (\n" +
      "    id         TEXT    NOT NULL PRIMARY KEY,\n" +
      "    parentId   TEXT,\n" +
      "    name       TEXT    NOT NULL,\n" +
      "    createdAt  INTEGER NOT NULL,\n" +
      "    updatedAt  INTEGER NOT NULL,\n" +
      "    FOREIGN KEY (parentId) REFERENCES folders(id) ON DELETE CASCADE\n" +
      ");\n" +
      "CREATE INDEX        index_folders_parentId      ON folders(parentId);\n" +
      "CREATE UNIQUE INDEX index_folders_parentId_name ON folders(parentId, name);\n" +
      "\n" +
      "CREATE TABLE note_locations (\n" +
      "    noteId    TEXT NOT NULL PRIMARY KEY,\n" +
      "    folderId  TEXT NOT NULL,\n" +
      "    FOREIGN KEY (noteId)   REFERENCES notes(id)   ON DELETE CASCADE,\n" +
      "    FOREIGN KEY (folderId) REFERENCES folders(id) ON DELETE CASCADE\n" +
      ");\n" +
      "CREATE INDEX index_note_locations_folderId ON note_locations(folderId);",
    caption: "folders and note_locations",
  },
  {
    p: "note_locations is where the folder cascade stops. Deleting a folder deletes the rows that " +
      "record which notes were filed in it, and the notes themselves are untouched — they simply " +
      "become unfiled. A student who deletes a folder loses the folder, not a lecture, and that " +
      "behaviour is a property of this table's existence rather than of any code path.",
  },
  {
    code: "CREATE TABLE syllabi (\n" +
      "    id                 TEXT    NOT NULL PRIMARY KEY,\n" +
      "    title              TEXT    NOT NULL,\n" +
      "    localRelativePath  TEXT    NOT NULL,\n" +
      "    extractedText      TEXT,\n" +
      "    sha256             TEXT    NOT NULL,\n" +
      "    importedAt         INTEGER NOT NULL,\n" +
      "    updatedAt          INTEGER NOT NULL\n" +
      ");\n" +
      "CREATE UNIQUE INDEX index_syllabi_sha256 ON syllabi(sha256);",
    caption: "syllabi",
  },
  {
    code: "CREATE TABLE capture_sessions (\n" +
      "    id                TEXT    NOT NULL PRIMARY KEY,\n" +
      "    noteId            TEXT    NOT NULL,\n" +
      "    mode              TEXT    NOT NULL,\n" +
      "    notePolicy        TEXT    NOT NULL,\n" +
      "    status            TEXT    NOT NULL,\n" +
      "    syllabusId        TEXT,\n" +
      "    generatedBlockId  TEXT    NOT NULL,\n" +
      "    revision          INTEGER NOT NULL,\n" +
      "    lastPatchId       TEXT,\n" +
      "    frameIntervalMs   INTEGER NOT NULL,\n" +
      "    changeThreshold   REAL    NOT NULL,\n" +
      "    primarySpeakerId  TEXT,\n" +
      "    startedAt         INTEGER NOT NULL,\n" +
      "    stoppedAt         INTEGER,\n" +
      "    updatedAt         INTEGER NOT NULL,\n" +
      "    FOREIGN KEY (noteId)           REFERENCES notes(id)       ON DELETE CASCADE,\n" +
      "    FOREIGN KEY (syllabusId)       REFERENCES syllabi(id)     ON DELETE SET NULL,\n" +
      "    FOREIGN KEY (generatedBlockId) REFERENCES note_blocks(id) ON DELETE CASCADE\n" +
      ");\n" +
      "CREATE INDEX        index_capture_sessions_noteId           ON capture_sessions(noteId);\n" +
      "CREATE INDEX        index_capture_sessions_syllabusId       ON capture_sessions(syllabusId);\n" +
      "CREATE INDEX        index_capture_sessions_status           ON capture_sessions(status);\n" +
      "CREATE UNIQUE INDEX index_capture_sessions_generatedBlockId ON capture_sessions(generatedBlockId);",
    caption: "capture_sessions",
  },
  {
    p: "Two lines in this definition carry more weight than the rest. The SET NULL on syllabusId " +
      "means that deleting a syllabus does not delete the lectures that were recorded against it; " +
      "a CASCADE there would let a student destroy a term of notes by tidying up a document. The " +
      "unique index on generatedBlockId makes it structurally impossible for two sessions to claim " +
      "the same AI-writable block, which is the failure that would let one lecture's generated " +
      "text land inside another lecture's note.",
  },
  {
    code: "CREATE TABLE transcript_segments (\n" +
      "    id          TEXT    NOT NULL PRIMARY KEY,\n" +
      "    sessionId   TEXT    NOT NULL,\n" +
      "    position    INTEGER NOT NULL,\n" +
      "    speakerId   TEXT,\n" +
      "    startMs     INTEGER NOT NULL,\n" +
      "    endMs       INTEGER NOT NULL,\n" +
      "    text        TEXT    NOT NULL,\n" +
      "    isFinal     INTEGER NOT NULL,\n" +
      "    confidence  REAL,\n" +
      "    createdAt   INTEGER NOT NULL,\n" +
      "    FOREIGN KEY (sessionId) REFERENCES capture_sessions(id) ON DELETE CASCADE\n" +
      ");\n" +
      "CREATE INDEX        index_transcript_segments_sessionId          ON transcript_segments(sessionId);\n" +
      "CREATE UNIQUE INDEX index_transcript_segments_sessionId_position ON transcript_segments(sessionId, position);\n" +
      "\n" +
      "CREATE TABLE visual_evidence (\n" +
      "    id               TEXT    NOT NULL PRIMARY KEY,\n" +
      "    sessionId        TEXT    NOT NULL,\n" +
      "    capturedAt       INTEGER NOT NULL,\n" +
      "    fingerprint      TEXT    NOT NULL,\n" +
      "    deltaScore       REAL    NOT NULL,\n" +
      "    processingState  TEXT    NOT NULL,\n" +
      "    temporaryPath    TEXT,\n" +
      "    error            TEXT,\n" +
      "    processedAt      INTEGER,\n" +
      "    FOREIGN KEY (sessionId) REFERENCES capture_sessions(id) ON DELETE CASCADE\n" +
      ");\n" +
      "CREATE INDEX index_visual_evidence_sessionId       ON visual_evidence(sessionId);\n" +
      "CREATE INDEX index_visual_evidence_processingState ON visual_evidence(processingState);",
    caption: "The two evidence tables",
  },
  {
    p: "visual_evidence.deltaScore is the column that found the settle-gate defect. It records the " +
      "change score that caused a frame to be kept, and it was added in migration 4 to 5 " +
      "specifically so that a session's decisions could be examined afterwards rather than only " +
      "watched at the time.",
  },
  {
    code: "CREATE TABLE note_assets (\n" +
      "    id                 TEXT    NOT NULL PRIMARY KEY,\n" +
      "    noteId             TEXT    NOT NULL,\n" +
      "    evidenceId         TEXT,\n" +
      "    kind               TEXT    NOT NULL,\n" +
      "    localRelativePath  TEXT    NOT NULL,\n" +
      "    caption            TEXT    NOT NULL,\n" +
      "    createdAt          INTEGER NOT NULL,\n" +
      "    FOREIGN KEY (noteId)     REFERENCES notes(id)           ON DELETE CASCADE,\n" +
      "    FOREIGN KEY (evidenceId) REFERENCES visual_evidence(id) ON DELETE SET NULL\n" +
      ");\n" +
      "CREATE INDEX        index_note_assets_noteId            ON note_assets(noteId);\n" +
      "CREATE INDEX        index_note_assets_evidenceId        ON note_assets(evidenceId);\n" +
      "CREATE UNIQUE INDEX index_note_assets_localRelativePath ON note_assets(localRelativePath);\n" +
      "\n" +
      "CREATE TABLE block_provenance (\n" +
      "    blockId      TEXT NOT NULL PRIMARY KEY,\n" +
      "    sessionId    TEXT NOT NULL,\n" +
      "    sourceKind   TEXT NOT NULL,\n" +
      "    sourceId     TEXT NOT NULL,\n" +
      "    startMs      INTEGER,\n" +
      "    endMs        INTEGER,\n" +
      "    confidence   REAL,\n" +
      "    reviewState  TEXT NOT NULL,\n" +
      "    FOREIGN KEY (blockId)   REFERENCES note_blocks(id)      ON DELETE CASCADE,\n" +
      "    FOREIGN KEY (sessionId) REFERENCES capture_sessions(id) ON DELETE CASCADE\n" +
      ");\n" +
      "CREATE INDEX index_block_provenance_sessionId ON block_provenance(sessionId);\n" +
      "CREATE INDEX index_block_provenance_sourceId  ON block_provenance(sourceId);",
    caption: "note_assets and block_provenance",
  },
  {
    p: "The second SET NULL in the schema is note_assets.evidenceId. Purging captured evidence to " +
      "reclaim storage must not remove an image the note actually refers to; the asset survives and " +
      "simply loses its link back to the frame it came from. The unique index on " +
      "localRelativePath is what the path-traversal check in reader/DiagramBlock.kt is written " +
      "against: an asset reference is resolved by looking it up here, so a reference that is not a " +
      "row cannot resolve at all.",
  },

  { h2: "O.2 Constraint summary" },
  {
    table: {
      head: ["Constraint", "Type", "What it makes impossible"],
      widths: [3400, 1800, 4206],
      rows: [
        ["note_blocks(noteId, position)", "Unique index", "Two blocks in the same place in a note"],
        ["folders(parentId, name)", "Unique index", "Two sibling folders with the same name"],
        ["syllabi(sha256)", "Unique index", "The same document imported twice"],
        ["capture_sessions(generatedBlockId)", "Unique index", "Two sessions writing to the same AI-owned block"],
        ["transcript_segments(sessionId, position)", "Unique index", "Two segments at the same offset in a session"],
        ["note_assets(localRelativePath)", "Unique index", "Two assets claiming the same file, and an unresolvable reference"],
        ["capture_sessions.syllabusId", "FK SET NULL", "Deleting a syllabus destroying a term of lectures"],
        ["note_assets.evidenceId", "FK SET NULL", "Purging evidence removing an image from a note"],
        ["Every other FK", "FK CASCADE", "Orphan rows surviving a delete"],
      ],
    },
    tnum: "O.1",
    caption: "Every constraint in the schema, and the failure each prevents",
  },
];

const APP_PROMPTS = [
  { h1: "Appendix P — Prompt Specification" },
  {
    p: "The instructions sent to each model, with the reasoning for the parts that are there " +
      "because of a specific failure. These are recorded in the report for the same reason the " +
      "algorithms are: they are part of the system's behaviour, they were revised in response to " +
      "observed defects, and a reader cannot judge the system's output without seeing what it was " +
      "asked for.",
  },
  {
    note: "A theme runs through this appendix and should be read alongside it. Every constraint below " +
      "that actually matters is enforced somewhere other than the prompt. The prompt asks; the " +
      "schema, the validator or the grounding guard decides. Where a constraint exists only as an " +
      "instruction, that is noted, and it should be read as a weakness rather than as a design.",
  },

  { h2: "P.1 Transcription" },
  {
    code: "Transcribe the supplied audio exactly as spoken.\n" +
      "\n" +
      "  - Return segments with start and end offsets in milliseconds, relative to the\n" +
      "    start of this chunk.\n" +
      "  - Label distinct voices A, B, C ... within this chunk only. Do not attempt to\n" +
      "    match a voice to any earlier chunk.\n" +
      "  - Do not summarise, correct grammar, remove filler words or reorder anything.\n" +
      "  - If a passage is inaudible, mark it inaudible rather than guessing at it.\n" +
      "  - If the chunk contains no clear speech, return an empty segment list.",
    caption: "System instruction for /v1/audio/transcribe",
  },
  {
    table: {
      head: ["Instruction", "Why", "Enforced elsewhere?"],
      widths: [3200, 4200, 2006],
      rows: [
        ["Offsets relative to this chunk", "The client owns absolute time and adds the chunk's start offset itself. A model that returned absolute time would have to be told the offset, and could then get it wrong.", "Yes — the client rejects any timestamp it did not compute"],
        ["Voice labels within this chunk only", "Cross-chunk voice matching is speaker identification, which is out of scope on privacy grounds.", "Yes — labels are stored per segment and never joined"],
        ["Do not correct or summarise", "This is evidence. It is stored before any refinement and never updated.", "Partly — nothing checks that the transcript is verbatim"],
        ["Mark inaudible rather than guess", "A guess is indistinguishable from a transcription on the page.", "No — instruction only"],
        ["Empty list when there is no speech", "Silence must be representable. Figure N.10 is the correct behaviour.", "Yes — the client and proxy both accept an empty result"],
      ],
    },
    tnum: "P.1",
    caption: "Transcription instructions",
  },

  { h2: "P.2 Board extraction" },
  {
    code: "Read the supplied photograph of a lecture board.\n" +
      "\n" +
      "  - Return the visible text verbatim, preserving line structure.\n" +
      "  - Return equations separately, in LaTeX.\n" +
      "  - Return the concepts named on the board, as short noun phrases.\n" +
      "  - Return regions that contain a diagram, as normalised coordinates in [0,1],\n" +
      "    each with a short caption describing what it shows.\n" +
      "  - Set legible to false if the board is out of focus, obscured, or not a board.\n" +
      "  - Do not explain, expand, complete or correct anything on the board. If a\n" +
      "    derivation is unfinished, return it unfinished.",
    caption: "System instruction for /v1/board/extract",
  },
  {
    p: "The last instruction is the important one and it is the one with no enforcement behind it. " +
      "A partially written derivation is exactly the case where a model is most inclined to be " +
      "helpful, and a completed derivation attributed to the lecturer is a fabrication of the most " +
      "damaging kind — plausible, well-formed and wrong in an examination. The grounding guard " +
      "provides indirect protection because the completed portion has no support in the transcript, " +
      "but nothing checks the extraction itself against the image.",
  },
  {
    p: "Normalised coordinates are requested rather than pixels so that the crop can be taken from " +
      "the full-resolution frame the phone still holds rather than from the downscaled copy that " +
      "was uploaded. The legible flag is the model's route to declining: a photograph of a wall, a " +
      "hand in front of the board or an out-of-focus frame returns false and costs nothing further.",
  },

  { h2: "P.3 Note composition" },
  {
    code: "You are extending a lecture note that is being written during the lecture.\n" +
      "\n" +
      "CONTEXT (what you can see)\n" +
      "  - A syllabus extract, if one was imported.\n" +
      "  - An outline of the note so far: headings only.\n" +
      "  - The tail of the note as it currently stands.\n" +
      "  - New evidence: this chunk's transcript, and any board text extracted since\n" +
      "    the last update.\n" +
      "\n" +
      "TASK\n" +
      "  Return ONLY the new Markdown to append. Never repeat, rewrite or reorder text\n" +
      "  that is already in the note.\n" +
      "\n" +
      "CONSTRAINTS\n" +
      "  - Everything you write must be supported by the new evidence. Do not add\n" +
      "    background, applications, examples or context that the evidence does not\n" +
      "    contain, however obvious it seems.\n" +
      "  - If the new evidence adds nothing, return an empty delta. This is a correct\n" +
      "    and expected outcome, not a failure.\n" +
      "  - Do not invent timestamps. Use only the timestamps supplied in the evidence.\n" +
      "  - Where the evidence is ambiguous or where board and speech disagree, emit a\n" +
      "    review flag containing both readings rather than choosing one.\n" +
      "\n" +
      "STYLE\n" +
      "  - Use varied Markdown structure: headings for topics, numbered lists for\n" +
      "    procedures and derivations, tables for comparisons and parameter lists,\n" +
      "    horizontal rules between topics, blockquotes for definitions.\n" +
      "  - Do not write every note as an unbroken sequence of bullet points.\n" +
      "  - Note policy CONCISE: the shortest form that preserves the reasoning.\n" +
      "  - Note policy DETAILED: keep the lecturer's own explanation of why each step\n" +
      "    is taken.",
    caption: "System instruction for /v1/notes/refine",
  },
  {
    p: "Three parts of this prompt exist because of a specific defect, and each is worth reading " +
      "against what actually enforces it.",
  },
  {
    table: {
      head: ["Instruction", "The defect that produced it", "What actually enforces it"],
      widths: [2600, 3600, 3206],
      rows: [
        ["Everything must be supported by the evidence", "A section headed \"Applications in Industry\" with four confident paragraphs, generated during a two-minute silence.", "The grounding guard, on the proxy. The instruction alone was already present when the defect occurred."],
        ["An empty delta is correct", "A silent chunk cost three calls, because the prompt asked for an empty delta while the client and the server both treated one as a failure.", "Explicit acceptance on both sides. The prompt was already right; the code was not."],
        ["Do not invent timestamps", "A timestamp of 00:80 appeared in a real note. The client's formatter provably cannot emit an invalid minute value, so the model produced it.", "The client rejects any timestamp it did not itself emit."],
        ["Use varied structure", "Field feedback that notes were bullet points and nothing else.", "Nothing. This one is instruction only — but the renderer can now draw what it asks for, which it previously could not."],
      ],
    },
    tnum: "P.2",
    caption: "Composition instructions, the defect behind each, and where enforcement actually lives",
  },
  {
    p: "The fourth row is the honest one. The style instruction has no validator behind it, and it " +
      "would be difficult to write one that did not itself become a source of bad structure. It is " +
      "included here as an instruction and reported as such rather than presented as a guarantee.",
  },

  { h2: "P.4 Verification" },
  {
    code: "Compare the finished note against the evidence that produced it.\n" +
      "\n" +
      "  Report findings only. Do not rewrite the note.\n" +
      "\n" +
      "  For each finding return:\n" +
      "    kind      unsupported | contradicted | incomplete\n" +
      "    claim     the sentence in the note\n" +
      "    evidence  the passage of transcript or board text it should rest on\n" +
      "    startMs   where in the session the evidence sits\n" +
      "    severity  low | medium | high\n" +
      "\n" +
      "  Do not report stylistic differences, and do not report a finding for text the\n" +
      "  student wrote themselves.",
    caption: "System instruction for /v1/notes/verify",
  },
  {
    p: "The instruction not to rewrite is backed by the response schema, which has no field a " +
      "rewritten note could be returned in. That is the pattern the whole system tries to follow: " +
      "where an instruction matters, arrange for the alternative to be unrepresentable rather than " +
      "merely discouraged.",
  },
  {
    p: "The exclusion of student-written text depends on block_provenance.sourceKind, which " +
      "records STUDENT for blocks the student authored. Without that column the verification pass " +
      "would report the student's own corrections as unsupported claims, which is both useless and " +
      "insulting.",
  },
];

const APP_LOG = [
  { h1: "Appendix Q — Field Diagnostic Log" },
  {
    p: "An annotated extract from the 218-line log returned by field trial 3. This is the document " +
      "from which two defects were diagnosed within seconds after two rounds of code reading and a " +
      "round of additional unit tests had produced nothing. It is reproduced here because it is " +
      "the single most useful artefact this project generated, and because the shape of it — what " +
      "is logged and what is deliberately not — is itself a design decision.",
  },
  {
    p: "Speech is recorded as a character count and a sixty-character prefix, never in full. No " +
      "audio, no images and no note bodies appear. Nothing is uploaded. Timestamps are relative to " +
      "the start of the session.",
  },

  { h2: "Q.1 Session start" },
  {
    code: "00:00.118  SESSION  start mode=BOARD policy=DETAILED syllabus=none\n" +
      "00:00.204  SESSION  note=8f21c4 block=1a09be revision=0\n" +
      "00:00.311  AUDIO    recorder started 16000Hz mono pcm16 chunk=20000ms overlap=2000ms\n" +
      "00:00.402  BOARD    sampler started interval=8000ms threshold=0.080\n" +
      "00:00.688  BOARD    baseline captured score=0.000",
    caption: "Lines 1 to 5",
  },
  {
    p: "Nothing surprising here, and that is the point: the session configured itself exactly as " +
      "asked. Every hypothesis that the failure was a configuration problem is eliminated in five " +
      "lines.",
  },

  { h2: "Q.2 The pattern that repeats for the whole lecture" },
  {
    code: "00:08.691  BOARD    frame score=0.214 threshold=0.080  -> HOLD\n" +
      "00:16.702  BOARD    settling=0.131 gate=0.035             -> UNSETTLED, replace held\n" +
      "00:24.694  BOARD    settling=0.117 gate=0.035             -> UNSETTLED, replace held\n" +
      "00:32.706  BOARD    settling=0.148 gate=0.035             -> UNSETTLED, replace held\n" +
      "00:40.699  BOARD    settling=0.092 gate=0.035             -> UNSETTLED, replace held\n" +
      "00:48.711  BOARD    settling=0.163 gate=0.035             -> UNSETTLED, replace held\n" +
      "00:56.703  BOARD    settling=0.104 gate=0.035             -> UNSETTLED, replace held",
    caption: "Lines 12 to 18, and the same pattern for a further 33 samples",
  },
  {
    p: "This is the defect, in plain sight. The settling score never falls below 0.092 and the gate " +
      "is 0.035. Forty consecutive samples, in a room where the board was static and the phone was " +
      "resting in a hand, and not one of them cleared a threshold set at less than half the " +
      "minimum observed value.",
  },
  {
    p: "The diagnosis follows immediately once the numbers are visible: the gate is not slightly " +
      "too tight, it is below the noise floor of the device it runs on. No amount of adjustment to " +
      "the constant would have been safe, because the correct constant would differ per phone, per " +
      "grip and per room. That is what made the fix a relative test rather than a larger number.",
  },
  {
    table: {
      head: ["Statistic over the 40 settle samples", "Value"],
      widths: [5406, 4000],
      rows: [
        ["Minimum settling score", "0.092"],
        ["Maximum settling score", "0.171"],
        ["Median settling score", "0.128"],
        ["Gate in force", "0.035"],
        ["Samples that cleared the gate", "0"],
        ["Gate under the corrected rule (0.45 x hold score of 0.214)", "0.096"],
        ["Samples that would clear the corrected gate", "6"],
      ],
    },
    tnum: "Q.1",
    caption: "The forty settle samples from field trial 3, and what the corrected rule does to them",
  },
  {
    p: "The last two rows are the fix, evaluated against the data that revealed the problem. Six " +
      "of forty samples clear the corrected gate — not most of them, which would mean the filter " +
      "had stopped filtering, and not none, which was the defect.",
  },

  { h2: "Q.3 The second defect, visible in the same log" },
  {
    code: "00:20.140  PACE     chunk words=54 duration=20000ms -> 162 wpm\n" +
      "00:20.142  PACE     suggest interval 8000 -> 4800  threshold 0.080 -> 0.064\n" +
      "01:40.180  PACE     chunk words=51 duration=20000ms -> 153 wpm\n" +
      "01:40.183  PACE     suggest interval 4800 -> 2880  threshold 0.064 -> 0.051\n" +
      "02:40.210  PACE     chunk words=49 duration=20000ms -> 147 wpm\n" +
      "02:40.213  PACE     suggest interval 2880 -> 2000  threshold 0.051 -> 0.041\n" +
      "                                       ^^^^ clamped",
    caption: "Lines 31, 32, 74, 75, 118 and 119",
  },
  {
    p: "Three measurements — 162, 153 and 147 words per minute — that are all in the same band and " +
      "differ by less than ten per cent. Three suggestions that between them move the interval by a " +
      "factor of four and drive it into its clamp. The output bears no relationship to the input " +
      "because each suggestion was computed from the previous suggestion rather than from the " +
      "session baseline.",
  },
  {
    p: "Note also what the two lines disagree about. By 02:40 the interval says the lecturer is " +
      "racing and the threshold says the opposite: a lower threshold means keep more frames, which " +
      "is the slow-speech response. Two halves of one controller had drifted in opposite " +
      "directions, which is only possible when neither is anchored.",
  },

  { h2: "Q.4 What the log did not need to contain" },
  {
    p: "Both diagnoses came from roughly fifteen lines out of 218, and neither required a single " +
      "word of transcribed speech. That is the argument for the constraints on the log rather than " +
      "an accident of this particular failure: what is needed to debug a capture pipeline is the " +
      "pipeline's decisions and the numbers behind them, not its content.",
  },
  {
    table: {
      head: ["Logged", "Not logged"],
      widths: [4703, 4703],
      rows: [
        ["Session configuration at start", "Any audio"],
        ["Frame scores, thresholds and decisions", "Any image, or any image data"],
        ["Settle scores and the gate in force", "Note content"],
        ["Words per minute per chunk", "Transcribed speech beyond a 60-character prefix"],
        ["Suggested and applied parameter changes", "Any identifier that would link a session to a person"],
        ["Chunk lengths, queue depth, retry counts", "The client token, or any part of it"],
        ["HTTP status codes and error kinds", "Request or response bodies"],
      ],
    },
    tnum: "Q.2",
    caption: "The diagnostic log's contents, by design",
  },
];

const APP_WALKED = [
  { h1: "Appendix R — Worked Example: One Lecture, End to End" },
  {
    p: "A single fifty-minute lecture traced through the whole system, with the numbers each stage " +
      "produces. Figures are typical values measured across the four field trials rather than a " +
      "single recorded session, and are given so that the shape of the system's work is legible " +
      "without reading the code.",
  },

  { h2: "R.1 Before the lecture" },
  {
    table: {
      head: ["Step", "What happens", "State written"],
      widths: [2200, 4200, 3006],
      rows: [
        ["Open capture setup", "Student picks board mode and the detailed note policy", "None yet"],
        ["Select a syllabus", "The unit's Markdown file, imported earlier", "syllabi row already exists"],
        ["Press start", "Permissions confirmed; camera and microphone acquired", "notes row; note_blocks row for the living block; capture_sessions row at PREPARING, revision 0"],
        ["Session becomes RUNNING", "Three coroutines start on separate dispatchers", "capture_sessions.status = RUNNING"],
      ],
    },
    tnum: "R.1",
    caption: "Session start",
  },

  { h2: "R.2 During the lecture: the audio pipeline" },
  {
    table: {
      head: ["Quantity", "Value", "Derivation"],
      widths: [3400, 2200, 3806],
      rows: [
        ["Session length", "50 minutes", "Given"],
        ["Chunk length", "20 s", "Design constant"],
        ["Overlap", "2 s", "Design constant"],
        ["Chunks produced", "~150", "3000 s / (20 s - 2 s advance) = 166; fewer in practice because the last chunk is short"],
        ["Audio transcribed", "~55 minutes", "10 per cent more than the lecture, which is the cost of the overlap"],
        ["Transcript segments stored", "~150", "One per chunk, because the provider returns a single segment per chunk (known defect 5)"],
        ["Words trimmed as overlap", "~600", "About 4 words per seam, at 149 seams"],
        ["Words lost", "0", "Every word is fully contained in at least one chunk"],
      ],
    },
    tnum: "R.2",
    caption: "The audio pipeline over one lecture",
  },
  {
    p: "The 10 per cent extra audio is the entire price of the overlap mechanism, and it buys the " +
      "elimination of a defect that removed roughly one word every twenty seconds — around 150 " +
      "words per lecture, concentrated at exactly the boundaries where a sentence is being " +
      "completed.",
  },

  { h2: "R.3 During the lecture: the board pipeline" },
  {
    table: {
      head: ["Stage", "Count", "Cost"],
      widths: [3400, 2200, 3806],
      rows: [
        ["Frames sampled at 8 s intervals", "~375", "Free — on device"],
        ["Frames exceeding the change threshold", "~120", "Free — on device"],
        ["Frames surviving the settle test", "~45", "Free — on device"],
        ["Frames rejected as duplicates by fingerprint", "~5", "Free — on device"],
        ["Frames uploaded for extraction", "~40", "40 vision calls"],
        ["Frames the student captured manually", "2 to 5", "Included in the above"],
        ["Regions promoted to note assets", "~15", "Free — a crop from a frame already held"],
      ],
    },
    tnum: "R.3",
    caption: "The board pipeline over one lecture",
  },
  {
    p: "Three hundred and thirty-five of the 375 sampled frames are discarded without a network " +
      "call. That single line is the difference between a system a student can afford to run daily " +
      "and one they cannot, and it is achieved by ninety lines of ordinary image processing that " +
      "no model is involved in.",
  },

  { h2: "R.4 During the lecture: composition" },
  {
    table: {
      head: ["Quantity", "Value", "Note"],
      widths: [3400, 2200, 3806],
      rows: [
        ["Composition calls", "~150", "One per audio chunk"],
        ["Calls returning an empty delta", "~25", "Silence, questions from the room, or a chunk that adds nothing. Accepted, not retried."],
        ["Calls discarded by the grounding guard", "0 to 2", "Measured honest deltas score 0.34 to 0.71"],
        ["Calls discarded by the revision check", "~4", "A board extraction landed first; the request is rebuilt"],
        ["Final revision", "~120", "One per accepted append"],
        ["Request size, first call", "~2,500 chars", "Little note to send yet"],
        ["Request size, last call", "~14,000 chars", "At the context cap, and flat from about minute 15"],
        ["Request size, last call without bounding", "~24,000 chars", "What it would have been"],
      ],
    },
    tnum: "R.4",
    caption: "Composition over one lecture",
  },

  { h2: "R.5 At the end" },
  {
    n: [
      "The student presses stop. The session moves to FINALISING and the camera is released.",
      "The audio queue is flushed: any chunk still awaiting transcription is completed before the session closes.",
      "One verification call compares the finished note against its evidence and returns findings.",
      "Findings become review flags in the note, each showing the note's reading and the evidence's reading.",
      "The session moves to COMPLETE, stoppedAt is written, and the reader opens on the finished note.",
    ],
  },
  {
    table: {
      head: ["Final artefact", "Typical size"],
      widths: [4703, 4703],
      rows: [
        ["Note, as Markdown", "8,000 to 15,000 characters"],
        ["Note blocks", "10 to 40"],
        ["Board images kept in the note", "~15"],
        ["Transcript segments retained as evidence", "~150"],
        ["Visual evidence rows retained", "~40"],
        ["Review flags", "0 to 5"],
        ["Full-resolution frames remaining on the device", "0 — deleted after extraction"],
        ["Total AI calls for the lecture", "~190"],
      ],
    },
    tnum: "R.5",
    caption: "What one lecture leaves behind",
  },

  { h2: "R.6 What the student has that they did not have before" },
  {
    p: "A single artefact, in one place, that contains the structured explanation and the board " +
      "content aligned in time, that can be searched, that can be filed into the unit it belongs " +
      "to, and that can be exported and read anywhere. Behind it, the evidence it was built from " +
      "is still on the device: the transcript exactly as captured and the board text exactly as " +
      "read, neither of which any model output has been allowed to modify.",
  },
  {
    p: "And where the system was unsure, it says so — as a review flag showing both readings, or " +
      "as a line recording that no clear speech was found. That is the whole of the design " +
      "argument in one sentence: a note a student can trust is one that tells them where not to " +
      "trust it.",
  },
];

module.exports = { APP_DDL, APP_PROMPTS, APP_LOG, APP_WALKED };

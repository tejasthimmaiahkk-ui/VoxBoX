/**
 * Reference appendices K to N: traceability matrix, design decision record, annotated
 * algorithm walkthrough and the screen specification.
 */

// ---------------------------------------------------------------- traceability

function trace(rows) {
  return {
    table: {
      head: ["Req.", "Design", "Implemented in", "Verified by"],
      widths: [800, 2400, 3200, 3006],
      rows,
    },
  };
}

const APP_TRACE = [
  { h1: "Appendix K — Requirements Traceability Matrix" },
  {
    p: "Every requirement in Chapter 4, traced forward to the design section that specifies it, " +
      "the source file that implements it, and the test that verifies it. A requirement with a " +
      "blank verification column would be an aspiration rather than a requirement; there are none " +
      "in this table, and where a requirement is verified only by field observation that is stated " +
      "explicitly rather than left implied.",
  },
  {
    p: "File paths are relative to VoxBox/app/src/main/java/me/thimmaiah/voxbox for the " +
      "application, and to the repository root otherwise. All of them are reproduced in full in " +
      "Appendices V to Y.",
  },

  { h2: "K.1 Capture requirements" },
  trace([
    ["FR-01", "6.4 live session", "audio/PcmAudioChunkRecorder.kt", "AudioPipelineTest; field trials 1-4"],
    ["FR-02", "7.3 overlap repair", "audio/PcmAudioChunkRecorder.kt (PcmChunkAccumulator)", "AudioPipelineTest"],
    ["FR-03", "8.2 audio capture", "audio/AudioChunkQueue.kt", "AudioPipelineTest (ordering, never-drop)"],
    ["FR-04", "7.3 overlap repair", "audio/TranscriptOverlap.kt", "TranscriptOverlapTest (9 cases)"],
    ["FR-05", "6.5 frame decision", "board/BoardSampler.kt", "Device walkthrough; field trial 4"],
    ["FR-06", "7.1 frame-change detection", "board/FrameChangeDetector.kt", "FrameChangeDetectorTest"],
    ["FR-07", "7.1.3 the settle test", "board/FrameChangeDetector.kt (settleRatio)", "FrameChangeDetectorTest.personMovingThroughFrameIsNeverAccepted"],
    ["FR-08", "7.1.1 the computation", "board/FrameChangeDetector.kt (compareLuma)", "FrameChangeDetectorTest (exposure cases)"],
    ["FR-09", "7.1.4 the rejected fix", "board/FrameChangeDetector.kt (forceNextCapture)", "Device test; field trial 4, Figure 9.7"],
    ["FR-10", "9.4 live session screen", "ui/live/LiveSessionScreen.kt (tuning panel)", "Device test; field trial 4, Figure 9.8"],
    ["FR-11", "7.2 adaptive sampling", "board/SpeechPaceTracker.kt", "SpeechPaceTrackerTest (10 cases)"],
    ["FR-12", "7.1.5 fingerprinting", "board/BoardExtractionCoordinator.kt", "BoardExtractionCoordinatorTest"],
    ["FR-13", "7.5 dominant speaker", "audio/SpeakerAggregator.kt", "AudioTranscriptionParserTest; field observation"],
    ["FR-14", "6.6 DEGRADED state", "audio/RetainedAudioStore.kt", "RetainedAudioRecoveryTest"],
    ["FR-15", "6.6 DEGRADED state", "net/VoxBoxService.kt", "VoxBoxServiceFailureTest (7 cases)"],
  ]),

  { h2: "K.2 Composition requirements" },
  trace([
    ["FR-16", "6.4 live session", "capture/CaptureSessionController.kt", "Revision counter observable in Figure 9.7"],
    ["FR-17", "7.4.1 the protocol", "notes/IncrementalNoteContextBuilder.kt", "IncrementalNoteContextBuilderTest"],
    ["FR-18", "7.4 append-only delta", "notes/GeneratedMarkdownRevision.kt", "GeneratedMarkdownRevisionTest"],
    ["FR-19", "7.4.2 hash binding", "notes/GeneratedMarkdownRevision.kt (contentHash)", "GeneratedMarkdownRevisionTest"],
    ["FR-20", "7.4.2 optimistic check", "notes/NoteRepository.kt (appendBlock)", "GeneratedMarkdownRevisionTest (interleaved append)"],
    ["FR-21", "7.4.3 the empty delta", "notes/NoteRefinementParser.kt; server/server.mjs", "NoteRefinementParserTest; backend tests"],
    ["FR-22", "7.6 grounding guard", "server/server.mjs (groundingRatio)", "Backend tests"],
    ["FR-23", "6.8 data model", "notes/NoteEntities.kt (unique index)", "Schema constraint; migration tests"],
    ["FR-24", "6.8 block_provenance", "notes/NoteEntities.kt; notes/NoteRepository.kt", "NoteVerificationTest"],
    ["FR-25", "6.4 FINALISING state", "notes/NoteVerification.kt", "NoteVerificationTest; field log"],
    ["FR-26", "11.6 validation", "reader/ReviewFlagParser.kt; reader/ReviewFlagCard.kt", "ReviewFlagParserTest (6 cases)"],
    ["FR-27", "6.7 invariants", "reader/ReviewFlagCard.kt (two actions only)", "Design constraint; RemoveFlagTest"],
    ["FR-28", "11.6 validation", "capture/CaptureNoteFormatting.kt", "CaptureNoteFormattingTest"],
    ["FR-29", "6.3 data flow", "notes/BoardNoteMapper.kt", "BoardNoteMapperTest; unproven in the field (Table 15.1)"],
    ["FR-30", "8.6 Markdown", "reader/Markdown.kt; server prompt", "MarkdownTest (15 cases)"],
  ]),

  { h2: "K.3 Reading, organising and export requirements" },
  trace([
    ["FR-31", "8.6 Markdown", "reader/Markdown.kt; reader/MarkdownRender.kt", "MarkdownTest (15 cases)"],
    ["FR-32", "9.5 the reader", "reader/DiagramBlock.kt", "DiagramLinkTest"],
    ["FR-33", "9.5 the reader", "reader/DiagramBlock.kt (lightbox)", "Device test; field trial 3 regression"],
    ["FR-34", "11.6 validation", "reader/DiagramBlock.kt (resolveAsset)", "DiagramLinkTest (traversal case)"],
    ["FR-35", "6.8 living block", "ui/reader/NoteEditor.kt", "NoteBlockEditTest"],
    ["FR-36", "9.6 organising", "ui/library/LibraryScreen.kt", "Device test"],
    ["FR-37", "9.6 organising", "ui/library/LibraryScreen.kt (confirm dialog)", "Device test"],
    ["FR-38", "6.8 folders", "notes/NoteEntities.kt; ui/library/FolderBrowser.kt", "Schema; device test"],
    ["FR-39", "9.6 organising", "notes/NoteSearch.kt", "NoteSearchTest"],
    ["FR-40", "9.6 export", "export/MarkdownExporter.kt", "MarkdownExporterTest"],
    ["FR-41", "9.6 export", "ui/VbShare.kt (FileProvider)", "Device test; field trial 1 regression"],
    ["FR-42", "9.6 export", "export/MarkdownExporter.kt", "MarkdownExporterTest"],
  ]),

  { h2: "K.4 Non-functional requirements" },
  trace([
    ["NFR-01", "6.9 deployment", "server/server.mjs (environment only)", "Architecture; secret scan on every commit"],
    ["NFR-02", "6.9 deployment", "AndroidManifest.xml; net/VoxBoxApiConfig.kt", "Device test"],
    ["NFR-03", "10.3 what it enforces", "server/server.mjs (constant-time compare)", "Backend tests"],
    ["NFR-04", "10.3 what it enforces", "server/server.mjs (rate window)", "Backend tests"],
    ["NFR-05", "10.3 what it enforces", "server/server.mjs (daily budget)", "Backend tests"],
    ["NFR-06", "11.3 device data", "storage/AssetStore.kt", "Code review; no MediaStore usage anywhere"],
    ["NFR-07", "11.4 diagnostic log", "debug/VbDebugLog.kt", "Code review; 60-character prefix"],
    ["NFR-08", "11.4 diagnostic log", "app/build.gradle.kts", "Build configuration"],
    ["NFR-09", "7.4 delta protocol", "notes/IncrementalNoteContextBuilder.kt", "Measured; Table 14.2"],
    ["NFR-10", "7.1 frame filter", "board/FrameChangeDetector.kt", "Measured: ~450 to ~40 per hour"],
    ["NFR-11", "6.6 DEGRADED state", "audio/RetainedAudioStore.kt", "VoxBoxServiceFailureTest; field trial"],
    ["NFR-12", "8.7 networking", "net/VoxBoxService.kt", "VoxBoxServiceFailureTest"],
    ["NFR-13", "8.1 codebase", "app/build.gradle.kts (minSdk 26)", "Device test"],
    ["NFR-14", "8.5 concurrency", "capture/CaptureSessionController.kt", "Field trials 1-4"],
    ["NFR-15", "6.8 data model", "notes/VoxBoxDatabase.kt (fallback disabled)", "Migration tests"],
    ["NFR-16", "11.5 consent", "ui/onboarding/OnboardingScreen.kt", "Figure I.3"],
    ["NFR-17", "editorial", "this report", "No accuracy figure appears anywhere in it"],
    ["NFR-18", "editorial", "Chapter 15; docs/PROJECT_STATUS.md", "Table 15.1 lists nine unproven features"],
  ]),

  { h2: "K.5 Coverage summary" },
  {
    table: {
      head: ["Category", "Total", "Verified by test", "Verified by device or field only", "Unproven"],
      widths: [3006, 1400, 1800, 2200, 1000],
      rows: [
        ["Capture (FR-01 to FR-15)", "15", "12", "3", "0"],
        ["Composition (FR-16 to FR-30)", "15", "13", "1", "1"],
        ["Reading and export (FR-31 to FR-42)", "12", "8", "4", "0"],
        ["Non-functional (NFR-01 to NFR-18)", "18", "10", "8", "0"],
        ["Total", "60", "43", "16", "1"],
      ],
    },
    tnum: "K.1",
    caption: "Traceability coverage",
  },
  {
    p: "The single unproven requirement is FR-29, inline placement of board images beside the text " +
      "they relate to. It is implemented and unit-tested, and it has never run against real " +
      "content because no accepted board frame reached extraction until field trial 4. It is the " +
      "first entry in Table 15.1 and the first item of immediate work in Table 16.1.",
  },
];

// ---------------------------------------------------------------- decisions

function adr(n, title, context, decision, consequence, status) {
  return [
    { h2: "L." + n + "  " + title },
    { table: {
        head: ["", ""],
        widths: [1900, 7506],
        rows: [
          ["Status", status],
          ["Context", context],
          ["Decision", decision],
          ["Consequence", consequence],
        ],
      } },
  ];
}

const APP_DECISIONS = [
  { h1: "Appendix L — Design Decision Record" },
  {
    p: "Twenty decisions that shaped this system, each recorded with the situation that forced it, " +
      "what was decided, and what the decision cost as well as what it bought. Several were " +
      "reversed once; those are marked, because a reversed decision is more informative than one " +
      "that was right first time.",
  },

  ...adr(1, "A hosted proxy exists at all",
    "The AI provider credential cannot be compiled into an APK, because an APK is decompilable by anyone who installs it.",
    "Introduce a hosted proxy that holds the credential. The application authenticates to the proxy with a separate, lower-value client token.",
    "An entire component, its deployment, its operations runbook and its failure modes are added to the project. In exchange, the provider account is not recoverable from a distributed application, and there is somewhere to put rate limiting, budget accounting, schema enforcement and the grounding guard.",
    "Accepted; unchanged"),

  ...adr(2, "The proxy has no runtime dependencies",
    "A proxy holding a credential is the component where a compromised transitive dependency does the most damage.",
    "Write the service against the Node.js standard library only. No web framework, no body parser, no validation library.",
    "About eighty lines of routing and parsing are written by hand. In exchange the entire security surface can be audited by reading one file, and there is no supply chain.",
    "Accepted; unchanged"),

  ...adr(3, "Evidence and generated text are stored separately",
    "A language model given thin evidence produces confident, fluent, unsupported text. A student cannot distinguish it from grounded text on the page.",
    "Write the raw transcript and board text to local storage before any model is consulted, and never allow model output to modify them. Record, for every generated block, which evidence produced it.",
    "Three additional tables and a provenance write on every append. In exchange the system can check its own output against its own inputs, which is what the verification pass and the review flags both depend on.",
    "Accepted; the organising principle of the project"),

  ...adr(4, "Composition is append-only, not rewriting",
    "Re-sending the note so the model can rewrite it grows quadratically in cost with lecture length, and grants the model the ability to delete text the student wrote.",
    "The model receives bounded context and returns only new text, which is appended to a single designated block.",
    "The note is chronological rather than topical, and a consolidation pass to reorganise it does not exist. In exchange cost is flat, and the model structurally cannot damage what it did not write.",
    "Accepted; the consolidation pass is future work"),

  ...adr(5, "Concurrency is optimistic, not locked",
    "The audio and board pipelines both append to the same note, run at different speeds, and neither can be made to wait without dropping evidence.",
    "Check that the revision has not moved before committing; discard and rebuild if it has.",
    "A losing append is thrown away and recomputed. That is cheap only because the context is bounded — the two decisions depend on each other.",
    "Accepted; unchanged"),

  ...adr(6, "The delta is bound to a content hash, not only a revision number",
    "A student can edit a sentence while the session is running. That changes the content without necessarily changing anything a counter would notice.",
    "Send a SHA-256 of the note the delta was computed against, and compare it at commit time.",
    "One hash computation per append. In exchange the concurrency check is honest about the case that actually occurs.",
    "Accepted; unchanged"),

  ...adr(7, "The frame filter runs on the device",
    "Sending every camera frame for analysis is what makes the naive design unaffordable.",
    "Downsample to a 32 x 32 luminance grid and decide locally whether a frame is worth a paid call.",
    "Ninety lines of image processing, and a filter that can be wrong. In exchange roughly 450 candidate frames an hour become roughly 40 uploads, which is the single largest cost saving in the system.",
    "Accepted; unchanged"),

  ...adr(8, "The settle test is relative, not absolute",
    "The first version compared the settling score against a constant of 0.035. A static board on a handheld phone scores 0.09 to 0.17 from hand tremor alone, so the gate could never open. Three field trials produced zero captures.",
    "Compare against 0.45 of the score that triggered the hold.",
    "The test is now scale-free and needs no tuning per room, phone or lighting condition. The cost was three wasted field trials and the construction of a diagnostic log to find it.",
    "Reversed once; the current form is the second attempt"),

  ...adr(9, "There is no 'capture anyway after N frames' fallback",
    "A never-settling scene yields nothing. An obvious fix is to capture anyway after a while.",
    "Rejected. A scene that never settles and a person walking across the board are the same signal, and a cap simply guarantees that the person is eventually photographed.",
    "A genuinely ambiguous scene still yields nothing automatically. The need is met by a manual capture button instead, which is the only component that actually knows.",
    "Proposed and rejected; the test that caught it was correct"),

  ...adr(10, "Adaptive suggestions anchor to the session baseline",
    "The first version divided the current value by the pace factor, so adjustments compounded. Over four minutes the interval fell to its clamp while the threshold fell in the opposite direction.",
    "suggest() takes the session baseline as an argument and multiplies that. The loop keeps no memory of its own output.",
    "Two identical measurements now produce identical settings, and the system returns to baseline when the pace does. A manual slider move resets the baseline.",
    "Reversed once; the current form is the second attempt"),

  ...adr(11, "Chunks overlap by two seconds",
    "Chunks that abut exactly cut words in half at every seam, and the model guesses at each fragment.",
    "Overlap by two seconds so every word is fully contained in at least one chunk, then trim the repeat.",
    "Ten per cent more audio is transcribed. In exchange no word is only ever seen as a fragment.",
    "Accepted; unchanged"),

  ...adr(12, "Overlap trimming is word-level with a three-word minimum",
    "Two transcriptions of the same audio differ in punctuation, casing and fillers, so a character-level comparison finds no common prefix.",
    "Compare normalised words and require a run of at least three before declaring an overlap.",
    "One and two-word runs occur constantly in speech, and a lower minimum removes genuine content such as a lecturer restating a phrase. Three is the smallest run that does not fire on natural repetition.",
    "Accepted; unchanged"),

  ...adr(13, "Speaker labels are per chunk, and may be refused",
    "Diarisation within a chunk is unreliable, and a confident wrong attribution is worse than none.",
    "Assign a label only when one voice holds at least 58 per cent of voiced time with a 15-point margin; otherwise mark the chunk AMBIGUOUS.",
    "Many chunks carry no speaker label. In exchange no label in the system is ever a guess.",
    "Accepted; degraded in practice by known defect 5"),

  ...adr(14, "No speaker identification by name",
    "Attaching a name to a voice requires enrolment and stored voiceprints.",
    "Excluded on privacy grounds. The system reports that the voice changed and never who it is.",
    "A student cannot filter a note by speaker. In exchange the application stores nothing that could identify a person by their voice.",
    "Accepted; listed as possible future work only with explicit opt-in"),

  ...adr(15, "The grounding guard runs on the server",
    "A check that runs on the client can be bypassed by an altered client, and a check expressed as a prompt instruction can be ignored by the model.",
    "Score the delta against its evidence on the proxy, and discard below the floor before the response is returned.",
    "The proxy must receive the evidence as well as the delta, which slightly increases request size. In exchange the check is in a place neither a prompt nor a modified client can reach.",
    "Accepted; unchanged"),

  ...adr(16, "The grounding floor is 0.25, not higher",
    "A faithful summary legitimately introduces words the lecturer never said: connectives, headings, 'therefore'.",
    "Set the floor at 0.25 with a 120-character minimum, from measured data: honest deltas scored 0.34 to 0.71; two fabricated passages scored 0.08 and 0.11.",
    "A sufficiently well-disguised fabrication would pass. The threshold was set from four trials' worth of data, which is a small sample, and its false-positive rate has not been measured.",
    "Accepted; measurement is future work"),

  ...adr(17, "note_locations is a table, not a nullable column",
    "A note that has not been filed needs to be representable.",
    "Model filing as a separate table, so an unfiled note has no row rather than a sentinel value.",
    "One extra join when listing a folder. In exchange no query needs to remember to handle an 'unfiled' sentinel, and the query that forgets cannot lose a note from the library.",
    "Accepted; unchanged"),

  ...adr(18, "Deleting a folder does not delete its notes",
    "Cascade semantics are a choice, and the obvious one is wrong here.",
    "Deleting a folder deletes its note_locations rows and leaves the notes themselves, which become unfiled.",
    "A student who deletes a folder loses the folder, not a lecture. This was decided after the first field trial, when a student's expectation was clear and did not match the schema's default reading.",
    "Accepted; unchanged"),

  ...adr(19, "A temporary diagnostic log was added, with a removal checklist",
    "The second field trial produced a complete failure with no diagnosis available. Two rounds of code reading and additional unit tests produced nothing.",
    "Add a 500-entry on-device ring buffer with a share-sheet export, constrained so it cannot become a privacy problem, and write its removal checklist at the same time as the feature.",
    "It found the settle-gate defect within fifteen seconds of being read. It is also a component that must be removed, and it is listed as an open defect until it is.",
    "Accepted as temporary; removal is item 3 of Table 16.1"),

  ...adr(20, "The report states what has not been proven",
    "A project report that claims everything works is making the same error the grounding guard exists to prevent.",
    "NFR-17 and NFR-18 constrain this report: no unmeasured accuracy figure may be published, and features that have never been observed working must be listed individually.",
    "Chapter 15 is longer and less flattering than it would otherwise be. In exchange every claim in this document can be traced to a measurement or an observation.",
    "Accepted; the reason Table 15.1 exists"),
];

// ---------------------------------------------------------------- walkthrough

const APP_WALKTHROUGH = [
  { h1: "Appendix M — Annotated Algorithm Walkthrough" },
  {
    p: "The six algorithms of Chapter 7, presented as annotated source with the reasoning attached " +
      "to the lines it applies to. The complete files are in Appendix V; this appendix isolates " +
      "the parts that carry the decisions and explains why each is written the way it is.",
  },

  { h2: "M.1 FrameChangeDetector — the comparison" },
  {
    code: "private fun compareLuma(\n" +
      "    baseline: IntArray,\n" +
      "    sample: IntArray,\n" +
      "    pixelThreshold: Int,\n" +
      "): LumaComparison {\n" +
      "    var sumDelta = 0L\n" +
      "    for (i in baseline.indices) sumDelta += (sample[i] - baseline[i])\n" +
      "    val meanDelta = sumDelta.toDouble() / baseline.size\n" +
      "\n" +
      "    var changed = 0\n" +
      "    for (i in baseline.indices) {\n" +
      "        val delta = (sample[i] - baseline[i]) - meanDelta\n" +
      "        if (kotlin.math.abs(delta) > pixelThreshold) changed++\n" +
      "    }\n" +
      "    return LumaComparison(\n" +
      "        score = changed.toDouble() / baseline.size,\n" +
      "        meanDelta = meanDelta,\n" +
      "    )\n" +
      "}",
    caption: "board/FrameChangeDetector.kt — the exposure-compensated comparison",
  },
  {
    p: "The first loop computes the mean signed difference across all 1,024 cells. That single " +
      "number is the global exposure shift: a cloud passing the window, an automatic exposure " +
      "adjustment, a light being switched on. Subtracting it in the second loop is what stops a " +
      "uniform brightness change scoring as a total change and triggering a capture of a board " +
      "nobody has written on.",
  },
  {
    p: "The score is returned as a fraction rather than a count so that the threshold is " +
      "independent of the grid size. If the grid were ever changed from 32 x 32 to 48 x 48, no " +
      "constant elsewhere in the system would need to move.",
  },

  { h2: "M.2 FrameChangeDetector — the settle test" },
  {
    code: "// A frame is held, not captured, when it first exceeds the threshold. The next\n" +
      "// frame decides whether the scene has settled.\n" +
      "//\n" +
      "// This was originally an absolute test against 0.035. A field log showed a static\n" +
      "// board on a handheld phone scoring 0.09 to 0.17 from tremor alone, so the gate\n" +
      "// could never open and three trials produced zero captures. The test is now\n" +
      "// relative to the score that caused the hold, which makes it scale-free.\n" +
      "private val settleRatio: Double = 0.45\n" +
      "\n" +
      "val settling = compareLuma(awaiting.grid, sample, centeredPixelThreshold)\n" +
      "if (settling.score >= settleRatio * metrics.score) {\n" +
      "    // Still moving. Replace the held frame rather than capturing this one:\n" +
      "    // a person crossing the board is exactly this signal, repeated.\n" +
      "    awaiting = Held(sample, metrics)\n" +
      "    return Decision.Skip(reason = SKIP_UNSETTLED)\n" +
      "}\n" +
      "return Decision.Capture(score = metrics.score)",
    caption: "board/FrameChangeDetector.kt — the settle test, with the defect it replaced recorded in the comment",
  },
  {
    p: "The comment is left in the source deliberately. A future reader looking at settleRatio = " +
      "0.45 has no way to know that the obvious alternative was tried, shipped and failed in three " +
      "field trials, and the most likely change anyone would make to this file is to replace it " +
      "with an absolute constant again.",
  },

  { h2: "M.3 SpeechPaceTracker — the anchored suggestion" },
  {
    code: "/**\n" +
      " * @param baseIntervalMs the interval the SESSION started with, or the value the\n" +
      " *        student last set by hand -- never the current interval.\n" +
      " * @param baseThreshold  likewise.\n" +
      " *\n" +
      " * Passing the current values here reintroduces the compounding defect: each\n" +
      " * suggestion would then be applied to the previous suggestion's output, which\n" +
      " * turns a proportional controller into an integrating one. Measured drift with\n" +
      " * that mistake: 8.0s -> 4.8s -> 2.9s -> 2.0s over four minutes of ordinary speech.\n" +
      " */\n" +
      "fun suggest(baseIntervalMs: Long, baseThreshold: Double): PaceSuggestion? {\n" +
      "    val pace = window.median() ?: return null\n" +
      "    val (iFactor, tFactor) = when {\n" +
      "        pace < SLOW_WPM -> 1.60 to 1.25\n" +
      "        pace > FAST_WPM -> 0.60 to 0.80\n" +
      "        else -> return null\n" +
      "    }\n" +
      "    return PaceSuggestion(\n" +
      "        intervalMs = (baseIntervalMs * iFactor).toLong()\n" +
      "            .coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS),\n" +
      "        threshold = (baseThreshold * tFactor)\n" +
      "            .coerceIn(MIN_THRESHOLD, MAX_THRESHOLD),\n" +
      "    )\n" +
      "}",
    caption: "board/SpeechPaceTracker.kt — the parameter names are the fix",
  },
  {
    p: "The correction to the compounding defect is entirely in the interface. The body would be " +
      "identical if the parameters were the current values; what makes it correct is that the " +
      "caller cannot pass them without ignoring the parameter names and the documentation. " +
      "Returning null in the middle band matters too: no suggestion is different from a suggestion " +
      "of 1.0, because it leaves a student's manual setting untouched.",
  },

  { h2: "M.4 TranscriptOverlap — word-level trimming" },
  {
    code: "private const val MIN_WORDS = 3\n" +
      "\n" +
      "fun trimRepeatedPrefix(\n" +
      "    previous: String,\n" +
      "    incoming: String,\n" +
      "    minimumWords: Int = MIN_WORDS,\n" +
      "): String {\n" +
      "    val tail = normalise(previous).takeLast(MAX_LOOKBACK_WORDS)\n" +
      "    val head = normalise(incoming)\n" +
      "\n" +
      "    // Longest run of normalised words that ends `previous` and begins `incoming`.\n" +
      "    var best = 0\n" +
      "    for (n in minOf(tail.size, head.size) downTo minimumWords) {\n" +
      "        if (tail.takeLast(n) == head.take(n)) { best = n; break }\n" +
      "    }\n" +
      "    if (best == 0) return incoming\n" +
      "    return dropFirstWords(incoming, best)\n" +
      "}\n" +
      "\n" +
      "// Normalisation is what makes this work at all: the two transcriptions of the\n" +
      "// overlapping audio differ in punctuation, casing and filler words even when the\n" +
      "// words agree, so a character-level comparison finds no common prefix.\n" +
      "private fun normalise(s: String): List<String> =\n" +
      "    s.lowercase().split(WHITESPACE).map { it.trim(PUNCTUATION) }.filter { it.isNotEmpty() }",
    caption: "audio/TranscriptOverlap.kt",
  },
  {
    p: "The search runs from the longest possible run downwards and stops at the first match, so " +
      "the longest overlap wins. Running upwards would find the shortest, which for a lecturer who " +
      "says \"and so we have, and so we have the result\" would trim three words and leave a " +
      "duplicate of the rest.",
  },
  {
    p: "The trimmed text is dropped from the incoming chunk rather than from the previous one. " +
      "That ordering matters because the previous chunk has already been written to " +
      "transcript_segments and, under the evidence-preservation rule, evidence is never updated " +
      "after it is written.",
  },

  { h2: "M.5 The append, as one transaction" },
  {
    code: "@Transaction\n" +
      "suspend fun appendGenerated(\n" +
      "    sessionId: String,\n" +
      "    delta: String,\n" +
      "    expectedRevision: Long,\n" +
      "    expectedHash: String,\n" +
      "): AppendResult {\n" +
      "    val session = sessionDao.byId(sessionId) ?: return AppendResult.Gone\n" +
      "    if (session.revision != expectedRevision) return AppendResult.Stale\n" +
      "\n" +
      "    val block = blockDao.byId(session.generatedBlockId) ?: return AppendResult.Gone\n" +
      "    if (sha256(block.content) != expectedHash) return AppendResult.Stale\n" +
      "\n" +
      "    blockDao.update(block.copy(content = block.content + delta))\n" +
      "    sessionDao.update(session.copy(revision = session.revision + 1))\n" +
      "    return AppendResult.Applied(session.revision + 1)\n" +
      "}",
    caption: "notes/NoteRepository.kt — the revision and hash check, and the append, in one transaction",
  },
  {
    p: "Both checks are inside the transaction. Reading the revision outside it and then " +
      "committing would leave a window in which a second append could pass its own check against " +
      "state that is about to change — which is the exact race the mechanism exists to prevent, " +
      "reintroduced one line higher up.",
  },
  {
    p: "Two distinct failure results are returned rather than a boolean. Gone means the session or " +
      "block no longer exists, which happens when a note is deleted mid-session and is terminal. " +
      "Stale means another append landed first, which is expected and is retried after rebuilding " +
      "the request. Collapsing them into a single false would make the retry logic either too " +
      "aggressive or too timid depending on which case it assumed.",
  },

  { h2: "M.6 The grounding guard, on the server" },
  {
    code: "const MIN_GROUNDING_RATIO = 0.25;\n" +
      "const GROUNDING_MIN_CHARS = 120;\n" +
      "\n" +
      "export function groundingRatio(delta, request) {\n" +
      "  const deltaWords = contentWords(delta);\n" +
      "  if (deltaWords.size === 0) return 1;          // an empty delta is trivially grounded\n" +
      "  const evidence = contentWords(\n" +
      "    [request.evidence, request.recentMarkdown, request.syllabusExtract].join(\" \")\n" +
      "  );\n" +
      "  let hit = 0;\n" +
      "  for (const w of deltaWords) if (evidence.has(w)) hit++;\n" +
      "  return hit / deltaWords.size;\n" +
      "}\n" +
      "\n" +
      "// A short delta can clear the ratio by coincidence -- four content words, two of\n" +
      "// which appear anywhere in the evidence, is 0.5. The length floor closes that.\n" +
      "const grounded =\n" +
      "  delta.length < GROUNDING_MIN_CHARS ||\n" +
      "  groundingRatio(delta, request) >= MIN_GROUNDING_RATIO;",
    caption: "server/server.mjs — the guard",
  },
  {
    p: "An empty delta returns a ratio of 1 rather than dividing by zero. That is not a defensive " +
      "special case: an empty delta is the correct response to a chunk that adds nothing, and " +
      "treating it as ungrounded is precisely the defect described in Chapter 13.9, where a silent " +
      "chunk cost three calls instead of one.",
  },
  {
    p: "The evidence set includes the recent Markdown and the syllabus extract as well as the new " +
      "transcript. A delta that continues a sentence begun in the previous chunk shares its " +
      "vocabulary with the note rather than with the newest evidence, and scoring only against the " +
      "newest evidence would reject exactly the continuations the append-only design is built to " +
      "produce.",
  },
];

// ---------------------------------------------------------------- screens

function screenSpec(rows) {
  return {
    table: {
      head: ["Aspect", "Specification"],
      widths: [2000, 7406],
      rows,
    },
  };
}

const APP_SCREENS = [
  { h1: "Appendix N — Screen Specification" },
  {
    p: "Each of the application's principal destinations, with the screenshot taken from a build " +
      "running on a physical device and a specification of what the screen holds, what it does and " +
      "what state it can be in. Sixteen destinations exist; the eleven that carry the application's " +
      "function are specified here, and the remaining five are settings sub-pages of the same shape " +
      "as those shown.",
  },

  { h2: "N.1 Onboarding" },
  { shots: [
      { file: "01-onboarding-1.png", num: "N.1", caption: "Onboarding, step 1" },
      { file: "02-onboarding-2.png", num: "N.2", caption: "Onboarding, step 2" },
      { file: "03-onboarding-3.png", num: "N.3", caption: "Onboarding, step 3: consent" },
    ], opts: { perRow: 3, width: 176 } },
  screenSpec([
    ["Purpose", "Explain what is recorded, where it is stored and what leaves the device, and obtain consent before the first session."],
    ["Shown", "Once, on first launch. Never again unless application data is cleared."],
    ["Controls", "Next, Back, and a final Accept that is the consent action."],
    ["States", "Three steps; no error state. The flow cannot be skipped."],
    ["Data written", "A single DataStore flag recording that onboarding completed."],
    ["Design note", "The consent step states in plain words that lecture audio is processed by a third-party model. It is deliberately not a link to a policy document."],
  ]),

  { h2: "N.2 Home" },
  { shots: [
      { file: "04-home.png", num: "N.4", caption: "Home" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Purpose", "One primary action and recent work."],
    ["Controls", "Start a session (primary); recent note list; navigation to Library and Settings."],
    ["States", "Empty (no notes yet), populated, and resumable (an interrupted session exists)."],
    ["Data read", "notes ordered by updatedAt; capture_sessions where status is RUNNING or DEGRADED."],
    ["Design note", "The resumable state exists because a process killed mid-lecture leaves a session that can be continued. Without a route back to it, the evidence already captured would be unreachable."],
  ]),

  { h2: "N.3 Capture setup" },
  { shots: [
      { file: "05-capture-setup.png", num: "N.5", caption: "Capture setup, upper half" },
      { file: "06-capture-setup-lower.png", num: "N.6", caption: "Capture setup, lower half" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Purpose", "Three decisions before a session starts: mode, note policy, syllabus."],
    ["Controls", "Mode (audio only / board), policy (concise / detailed), syllabus picker, Start."],
    ["States", "Default, syllabus selected, permission not yet granted."],
    ["Data written", "capture_sessions row at status PREPARING, plus the note and its living block."],
    ["Design note", "Board mode is the more capable and the more expensive, and the difference is stated on this screen rather than buried in settings. Audio mode never opens the camera and never spends a vision call."],
  ]),

  { h2: "N.4 Live session" },
  { shots: [
      { file: "14-live-board.png", num: "N.7", caption: "Board mode, camera preview and frame guide" },
      { file: "16-live-running.png", num: "N.8", caption: "Running: 01:32, revision 5, 3 kept and 9 skipped" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Purpose", "The only screen visible while the lecture is happening, and therefore the only one whose design has a cost measured in missed explanation."],
    ["Displays", "Elapsed time, current revision, frames kept and skipped, camera preview with frame guide, zoom level, queued work indicator."],
    ["Controls", "Capture now, Hide preview, the tuning panel (auto toggle, interval slider, threshold slider), Stop and finish."],
    ["States", "PREPARING, RUNNING, DEGRADED (warning banner, audio retained), FINALISING (verification pass running)."],
    ["Back behaviour", "Collapses the preview. It does not leave the session. Field trial 1 lost eleven minutes of a lecture to an accidental back gesture."],
    ["Design note", "Both Capture now and the tuning panel were added after field use and neither was in the original design. The panel displays what the adaptive algorithm has done — 'Every 8 s', '8 % change to keep a frame' — so the student can see the system adjusting rather than wonder whether it is."],
  ]),

  { h2: "N.5 Live tuning panel" },
  { shots: [
      { file: "15-live-tuning.png", num: "N.9", caption: "The tuning panel, adjustable mid-session" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Purpose", "Change the sampling interval and the change threshold without ending the session."],
    ["Controls", "Auto toggle (adaptive sampling on/off), interval slider (2 to 30 s), threshold slider (0.02 to 0.40)."],
    ["Side effect", "Moving either slider by hand resets the adaptive baseline to the new value, so the student's judgement becomes the new anchor rather than something the loop drifts away from."],
    ["Data written", "capture_sessions.frameIntervalMs and changeThreshold, persisted immediately."],
    ["Design note", "Before this panel existed, a badly chosen sensitivity was locked in for fifty minutes. It was reported after field trial 3 in exactly those terms."],
  ]),

  { h2: "N.6 Note reader" },
  { shots: [
      { file: "17-note-reader.png", num: "N.10", caption: "The reader" },
      { file: "17b-note-reader-scrolled.png", num: "N.11", caption: "The reader, scrolled" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Purpose", "Read the finished note."],
    ["Renders", "Headings, paragraphs, bullets, numbered lists, quotes, fenced code, tables, thematic breaks, inline board crops with captions, and review flags."],
    ["Controls", "Outline, edit, search within note, share. Tapping an image opens the lightbox."],
    ["States", "Empty, populated, containing review flags, containing capture warnings."],
    ["Design note", "Figure N.10 shows a refusal rather than a success. Five audio chunks contained no speech and the system recorded that fact at each timestamp instead of producing plausible lecture notes. A system that filled those gaps with fluent text would screenshot better and be worse in every way that matters."],
  ]),

  { h2: "N.7 Library" },
  { shots: [
      { file: "07-library-empty.png", num: "N.12", caption: "Library, empty" },
      { file: "18-library-populated.png", num: "N.13", caption: "Library, populated" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Purpose", "Find a note among sixty."],
    ["Controls", "Search, folder breadcrumb, note actions (rename, delete, move), new folder."],
    ["States", "Empty, populated, inside a folder, search results."],
    ["Data read", "notes joined to note_locations; folders by parentId."],
    ["Design note", "Requested from field trial 1 in the student's own terms: a folder per subject, with a folder per unit inside it. A flat reverse-chronological list is unusable after a fortnight."],
  ]),

  { h2: "N.8 Folder creation and note actions" },
  { shots: [
      { file: "19-new-folder.png", num: "N.14", caption: "Creating a nested folder" },
      { file: "20-note-actions.png", num: "N.15", caption: "Note actions" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Purpose", "Organise the library."],
    ["Controls", "Create folder (name, parent), rename note, move note, delete note behind an explicit confirmation."],
    ["Constraint", "A folder name must be unique among its siblings. This is enforced by a unique index on (parentId, name) rather than by checking first, because a check followed by an insert is a race."],
    ["Design note", "These dialogs are the ones that compiled, passed 109 tests and drew nothing, because the render block had ended up outside the composable. They are the reason the device instrumentation suite exists."],
  ]),

  { h2: "N.9 Settings" },
  { shots: [
      { file: "08-settings.png", num: "N.16", caption: "Settings" },
      { file: "09-appearance.png", num: "N.17", caption: "Appearance" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Purpose", "Six sub-pages: capture defaults, connection, storage, appearance, privacy and data, diagnostics."],
    ["Design note", "All six crashed on open at one point, because of a negative value passed to Modifier.padding — which compiles without warning and throws at runtime. No unit test instantiates a screen, so nothing caught it. The same mistake was made twice in one session in two different components."],
  ]),

  { h2: "N.10 Connection" },
  { shots: [
      { file: "10-connection.png", num: "N.18", caption: "Connection, showing live proxy health" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Purpose", "Answer the question 'is the deployed service the one I think it is?' from the phone."],
    ["Displays", "Live /health response: status, uptime, the model routed to each of the three pipeline routes, and budget headroom."],
    ["States", "Reachable, unreachable, cold start in progress (about 13 seconds on the free tier)."],
    ["Design note", "The model routing display exists because three separately reported defects turned out to have one cause: a fix had been committed and never pushed, so the deployed proxy was running older code while the repository looked correct."],
  ]),

  { h2: "N.11 Privacy, and diagnostics" },
  { shots: [
      { file: "11-privacy.png", num: "N.19", caption: "Privacy and data" },
      { file: "13-diagnostics.png", num: "N.20", caption: "Diagnostics — temporary" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Privacy page", "States what is stored on the device, what leaves it, and offers deletion of retained audio and of captured evidence."],
    ["Diagnostics page", "Displays the temporary 500-entry ring buffer and exports it through the share sheet. Absent from release builds."],
    ["Constraint on the log", "Speech is stored as a character count and a sixty-character prefix and never in full. No audio, images or note bodies are written. There is no network code in the component."],
    ["Status", "The diagnostics page is scheduled for removal; its checklist is docs/TEMPORARY_DEBUG_LOG.md, and it is listed as known defect 1 in Table 15.3 until it is gone."],
  ]),

  { h2: "N.12 About" },
  { shots: [
      { file: "12-about.png", num: "N.21", caption: "About" },
    ], opts: { perRow: 2, width: 224 } },
  screenSpec([
    ["Purpose", "Version, build type, and an honest statement of what the system does and does not do."],
    ["Design note", "The About page states that the system does not interpret diagrams. It is the capability most often assumed to be present, and stating it in the application rather than only in this report seemed the correct place for it."],
  ]),
];

module.exports = { APP_TRACE, APP_DECISIONS, APP_WALKTHROUGH, APP_SCREENS };

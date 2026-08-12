/**
 * Chapters 6 to 11: design, algorithms, implementation, user interface, backend, security.
 */

const CH_DESIGN = [
  { h1: "6. System Design and Architecture" },
  {
    p: "This chapter describes the structure of the system: how it is layered, what the components " +
      "are, how data moves between them, where each piece is deployed and how the persistent state " +
      "is modelled. Every diagram in this chapter was generated from the system it describes, so a " +
      "structural change is reflected the next time this report is built.",
  },

  { h2: "6.1 Architectural overview" },
  {
    p: "The system is four layers on the phone, one hosted process and one third party. The layer " +
      "rule is strict and is worth stating because it is what keeps the algorithms testable: a " +
      "layer may call the layer directly beneath it and may not reach past it. In particular, no " +
      "screen touches the database and no algorithm touches the network.",
  },
  { fig: "docs/diagrams/fig-architecture.png", num: "6.1", caption: "Layered architecture: four layers on the device, one hosted proxy, one third-party provider", opts: { maxH: 840 } },
  {
    table: {
      head: ["Layer", "Responsibility", "What it must not do"],
      widths: [1900, 3800, 3706],
      rows: [
        ["Presentation", "Render state; collect input. Sixteen Compose destinations.", "Hold business state, call the network, or touch Room."],
        ["Coordination", "Own the running session; sequence the pipelines; project state for the UI.", "Contain algorithmic logic, which would make it untestable without a device."],
        ["Domain", "The six algorithms. Pure functions over plain data.", "Know about Android, Room, the network or coroutine scope."],
        ["Data", "Room, DataStore and the asset store. Transactional appends.", "Interpret content; it stores what it is given."],
        ["Proxy", "Hold the credential; authenticate; limit; validate; ground.", "Store any student data. It is stateless apart from rate and budget counters."],
        ["Provider", "Run the models.", "Receive any student identifier. The proxy has none to send."],
      ],
    },
    tnum: "6.1",
    caption: "Layer responsibilities, and the constraint on each",
  },
  {
    p: "The domain layer is the part of this system that is genuinely this project's own work, and " +
      "the rule that it may not know about Android is what makes that work verifiable. All six " +
      "algorithms are pure functions over plain data structures: FrameChangeDetector takes two " +
      "luminance arrays and returns a decision, SpeechPaceTracker takes text and a duration and " +
      "returns a suggestion. None requires an emulator, a device or a network to test, which is " +
      "why 134 unit tests can cover them in under a minute.",
  },

  { h2: "6.2 Components" },
  { fig: "docs/diagrams/fig-component.png", num: "6.2", caption: "UML component diagram, with provided and required interfaces" },
  {
    p: "Seven components, of which one is temporary. The debug component is drawn in red and has " +
      "no inbound dependency from any other component: nothing in the system requires it, it is " +
      "compiled out of release builds, and its removal checklist is tracked in a file specifically " +
      "so that a diagnostic aid added under pressure does not quietly become a permanent feature. " +
      "The circumstances that produced it are described in Chapter 13.",
  },

  { h2: "6.3 Data flow" },
  {
    p: "Figure 6.3 decomposes the system into seven processes and four data stores. Reading it " +
      "left to right: capture produces evidence, evidence is understood, understanding is " +
      "composed into a delta, the delta is committed if the note has not moved, and at the end of " +
      "the session the whole note is verified against the evidence that produced it.",
  },
  { fig: "docs/diagrams/fig-dfd-l1.png", num: "6.3", caption: "Level 1 data-flow diagram: seven processes and four data stores" },
  {
    p: "Two features of this diagram are the point of the design. First, D2 and D3 — the " +
      "transcript segments and the visual evidence — are written by processes 2.0 and 3.0 and are " +
      "read but never written by anything downstream. They are evidence of record. Second, process " +
      "6.0 reads them again at the end of the session and compares them against D4, the note. The " +
      "system checks its own output against its own inputs, which it can only do because it kept " +
      "the inputs.",
  },

  { h2: "6.4 The live session, end to end" },
  {
    p: "Figure 6.4 traces one cycle of a live board session. The cycle repeats roughly every eight " +
      "seconds during ordinary speech, more often when the lecturer is speaking quickly, less " +
      "often during a pause.",
  },
  { fig: "docs/diagrams/fig-sequence-live.png", num: "6.4", caption: "UML sequence diagram: one cycle of a live board session", opts: { maxH: 800 } },
  {
    p: "Three things in this sequence carry most of the system's design weight. The settle test at " +
      "the top decides whether a network call happens at all. The context bounds in the middle " +
      "decide what the call costs. The alt block near the bottom decides whether the result is " +
      "allowed to land: if another append arrived while this one was being composed, the revision " +
      "has moved and this delta is discarded and rebuilt rather than merged.",
  },
  {
    p: "The loop annotation in the middle is doing a lot of quiet work. Audio and board are " +
      "independent pipelines running concurrently; the audio loop turns every twenty seconds " +
      "regardless of what the camera is doing, and the board loop turns whenever a frame survives " +
      "the filter. They meet only at the append, and the append is the only place either can " +
      "conflict with the other.",
  },

  { h2: "6.5 The board frame decision" },
  {
    p: "Figure 6.5 is the on-device filter in full. It is the single most important piece of " +
      "engineering in the system by cost, because everything to the right of it is a paid call and " +
      "everything to the left of it is free.",
  },
  { fig: "docs/diagrams/fig-activity-frame.png", num: "6.5", caption: "UML activity diagram: the board frame decision", opts: { maxH: 800 } },
  {
    p: "The two decision diamonds ask different questions and both are necessary. The first asks " +
      "whether anything changed; the second asks whether the change has finished. The distinction " +
      "matters because a lecturer writing a new line and a person walking across the board produce " +
      "an identical answer to the first question and opposite answers to the second. The details, " +
      "including a defect in the second test that made the entire filter inoperative for three " +
      "field trials, are in Chapter 7.",
  },

  { h2: "6.6 Session state" },
  { fig: "docs/diagrams/fig-state-session.png", num: "6.6", caption: "UML state machine: capture session lifecycle" },
  {
    p: "The status column of capture_sessions takes exactly these six values, and every transition " +
      "is written to the database before it is shown on screen. The ordering matters: a state that " +
      "is displayed but not persisted is a state the system loses if the process is killed, and a " +
      "process being killed mid-lecture is not a hypothetical on a phone that is also running a " +
      "camera, a microphone and a network client for fifty minutes.",
  },
  {
    p: "DEGRADED is the state that most repays attention. It exists because the failure mode this " +
      "system must never have is stopping. When the network fails or the daily budget is " +
      "exhausted, the session does not end: it retains raw audio to local storage, warns the " +
      "student on screen, and keeps recording. The AI work is lost; the lecture is not.",
  },

  { h2: "6.7 Note lifecycle" },
  { fig: "docs/diagrams/fig-note-lifecycle.png", num: "6.7", caption: "Note lifecycle, with the invariants held at every stage" },
  {
    p: "The five invariants listed beneath Figure 6.7 are the contract the rest of the system is " +
      "written against. They are stated as invariants rather than as behaviours because each one " +
      "is enforced structurally — by a unique index, by a foreign key, by a hash comparison — " +
      "rather than by a convention that a future change might quietly break.",
  },

  { h2: "6.8 Data model" },
  {
    p: "Ten tables in a single SQLite database held in application-private storage. The full " +
      "column-by-column reference, with the reason each column exists, is Appendix E; this section " +
      "gives the shape.",
  },
  { fig: "docs/diagrams/fig-er-diagram.png", num: "6.8", caption: "Entity-relationship diagram of the on-device database" },
  {
    table: {
      head: ["Table", "Rows per lecture", "Role"],
      widths: [2200, 1800, 5406],
      rows: [
        ["notes", "1", "The durable artefact"],
        ["note_blocks", "10 to 40", "Ordered content; exactly one is AI-writable per session"],
        ["folders", "0 to a few", "Nested organisation, unique name among siblings"],
        ["note_locations", "0 or 1", "Which folder a note is filed in; absent means unfiled"],
        ["syllabi", "0 or 1 referenced", "Course vocabulary for the composition model"],
        ["capture_sessions", "1", "Settings and concurrency state; the transactional heart"],
        ["transcript_segments", "~150", "Evidence of record for speech; never updated"],
        ["visual_evidence", "~40", "Evidence of record for the board; never updated"],
        ["note_assets", "~40", "Images promoted into the note itself"],
        ["block_provenance", "10 to 40", "Which evidence produced which block"],
      ],
    },
    tnum: "6.2",
    caption: "Tables and their role, with typical row counts for one fifty-minute lecture",
  },
  {
    p: "Two modelling decisions in this schema are worth defending explicitly, because in both " +
      "cases the simpler alternative is the one most designs would take.",
  },
  {
    n: [
      "note_locations is a table rather than a nullable column on notes. A note that has not been " +
        "filed has no row here at all. As a nullable column it would have required a sentinel value " +
        "for 'unfiled' that every query would then have had to remember to handle, and the query " +
        "that forgets is the one that loses a student's note from the library.",
      "capture_sessions.generatedBlockId carries a unique index. This is a structural guarantee " +
        "rather than a performance one: it makes it impossible for two sessions to claim the same " +
        "living block, which is the failure that would let one lecture's generated text land inside " +
        "another lecture's note. Enforcing it in the schema means no future code change can " +
        "reintroduce it.",
    ],
  },

  { h2: "6.9 Deployment" },
  { fig: "docs/diagrams/fig-deployment.png", num: "6.9", caption: "UML deployment diagram: three nodes, two network hops" },
  {
    p: "Three nodes. The credential placement shown at the bottom of Figure 6.9 is the single " +
      "constraint from which the entire proxy exists: the provider key must not be in the APK, an " +
      "APK is decompilable, therefore the key must live somewhere the APK does not, therefore " +
      "there is a server. Everything else the proxy does — rate limiting, budget, schema " +
      "enforcement, the grounding guard — was added afterwards because there was already a place " +
      "to put it.",
  },
];

const CH_ALGORITHMS = [
  { h1: "7. Algorithms Implemented in this Project" },
  {
    p: "Six algorithms in this system are its own work rather than a library call. They are " +
      "presented here with the reasoning behind each, the measurements that set their constants, " +
      "and — where it applies — the defect the first version contained and how it was found. That " +
      "last part is included deliberately. Four of the six had a first version that was wrong in a " +
      "way no test caught, and the manner of each failure is more instructive than the final code.",
  },
  {
    p: "None of these six changed when the AI provider was migrated during development. That is " +
      "the practical test of whether they are the project's own work: the models were replaced and " +
      "the algorithms were not.",
  },

  { h2: "7.1 Algorithm 1: on-device frame-change detection" },
  {
    p: "The problem. A camera pointed at a board produces a frame whenever it is asked to. Almost " +
      "all of them are worthless: the board has not changed, or a person is standing in front of " +
      "it, or the phone moved. Sending all of them for analysis is what makes the naive design " +
      "unaffordable. The filter must decide, on the device and for free, which frames are worth a " +
      "paid call.",
  },
  { fig: "docs/diagrams/fig-algo-framechange.png", num: "7.1", caption: "Frame-change detection: the computation, the defect it replaced and why a ratio is the right shape", opts: { maxH: 780 } },
  { h3: "7.1.1 The computation" },
  {
    code: "downsample frame to a 32 x 32 luminance grid          -> current[1024]\n" +
      "\n" +
      "mean_delta = mean( current[i] - baseline[i] )        // global exposure shift\n" +
      "changed[i] = 1 if |(current[i]-baseline[i]) - mean_delta| > pixelThreshold\n" +
      "score      = mean( changed[i] )                      // 0.0 .. 1.0\n" +
      "\n" +
      "if score < changeThreshold:            discard, no call is spent\n" +
      "else:                                  hold the frame, await settle\n" +
      "\n" +
      "on the next frame:\n" +
      "  settling = compareLuma(held, next).score\n" +
      "  if settling >= settleRatio * score:  scene still moving, replace held frame\n" +
      "  else:                                accept; fingerprint; upload",
    caption: "Algorithm 1, in full. settleRatio = 0.45.",
  },
  {
    p: "Subtracting the mean delta before thresholding is what makes the filter survive a cloud " +
      "passing the window. Without it, a uniform brightness change across the whole board scores " +
      "as a total change and triggers a capture of a board that has not been written on. With it, " +
      "a uniform change subtracts to zero and only genuinely differing cells survive.",
  },
  { h3: "7.1.2 The defect, and how it was found" },
  {
    p: "The settle test originally compared the settling score against an absolute constant of " +
      "0.035. The reasoning was straightforward and wrong: a settled scene should show almost no " +
      "change, so a small constant should suffice.",
  },
  {
    p: "Three field trials produced zero accepted board frames. Every unit test passed throughout. " +
      "The tests fed synthetic frames — arrays constructed in the test — which contain no hand " +
      "tremor, no rolling shutter and no sensor noise, and therefore settle to a score of almost " +
      "exactly zero. On a phone held in a hand, the diagnostic log showed a static board scoring " +
      "between 0.09 and 0.17 across forty consecutive frames. The gate was set an order of " +
      "magnitude below the noise floor of the device it ran on, so it could never open.",
  },
  {
    note: "This is the single most instructive defect in the project. It was invisible to the entire " +
      "test suite, invisible to code review, and obvious within fifteen seconds of reading a log " +
      "from the running system. The standing lesson recorded in Chapter 13 is: instrument the " +
      "running system, and benchmark on realistic inputs rather than constructed ones.",
  },
  { h3: "7.1.3 Why the corrected test is relative" },
  {
    p: "The fix replaces the constant with a fraction of the score that triggered the hold. This " +
      "is not merely a larger threshold; it is a differently shaped question. An absolute test " +
      "asks \"is the scene still?\", which depends on the device, the grip, the lighting and the " +
      "room. A relative test asks \"has the scene settled relative to how much it moved?\", which " +
      "depends on none of those.",
  },
  {
    p: "It also captures the distinction the application actually needs. A lecturer writing a line " +
      "and a person crossing the board produce the same large initial score. Writing settles; a " +
      "person keeps moving. Comparing against a fraction of the initial score separates them " +
      "without needing to know anything about either.",
  },
  { h3: "7.1.4 A rejected fix" },
  {
    p: "An intermediate attempt added a cap: capture anyway after N frames, so a never-settling " +
      "scene eventually yields something. It broke a test named " +
      "personMovingThroughFrameIsNeverAccepted, and the test was right. A scene that never settles " +
      "and a person walking across the board are the same signal; no amount of waiting " +
      "distinguishes them, and a cap simply guarantees that the person is eventually photographed. " +
      "The cap was removed. The need it was trying to meet — the student can see the board is " +
      "worth capturing and the filter disagrees — was met instead with a manual capture button, " +
      "which is the only component in the system that actually knows.",
  },
  { h3: "7.1.5 Result" },
  {
    table: {
      head: ["Measure", "Value", "Source"],
      widths: [3400, 2200, 3806],
      rows: [
        ["Candidate frames per lecture-hour", "~450", "Sampling interval of 8 s"],
        ["Frames accepted per lecture-hour", "~40", "Measured across trials"],
        ["Reduction in paid vision calls", "~90 per cent", "Derived"],
        ["Field trial 4, 92-second session", "3 kept, 9 skipped", "Figure H.2"],
        ["Grid size", "32 x 32 = 1,024 cells", "Design"],
        ["Test cases", "14", "FrameChangeDetectorTest"],
      ],
    },
    tnum: "7.1",
    caption: "Algorithm 1 in numbers",
  },

  { h2: "7.2 Algorithm 2: adaptive sampling from measured speech pace" },
  {
    p: "The problem. The sampling interval and the change threshold are two numbers that " +
      "materially affect both cost and completeness, and there is no correct value for either. A " +
      "lecturer dictating slowly needs sampling every twenty seconds; the same lecturer racing " +
      "through a derivation needs sampling every three. Asking the student to guess before the " +
      "lecture starts, and then locking the guess in for fifty minutes, is what the first version " +
      "did, and it was reported as a problem after the third trial in exactly those terms.",
  },
  { fig: "docs/diagrams/fig-algo-adaptive.png", num: "7.2", caption: "Adaptive sampling: the control loop, the mapping, and the compounding defect", opts: { maxH: 780 } },
  { h3: "7.2.1 The loop" },
  {
    code: "observe(text, durationMs) -> wordsPerMinute\n" +
      "    push into a rolling window of the last 5 chunks\n" +
      "\n" +
      "suggest(baseIntervalMs, baseThreshold) -> PaceSuggestion?\n" +
      "    pace = median(window)\n" +
      "    if pace <  90:  interval = base * 1.60 ; threshold = base * 1.25\n" +
      "    if pace > 150:  interval = base * 0.60 ; threshold = base * 0.80\n" +
      "    else:           no suggestion\n" +
      "    clamp interval to 2_000 .. 30_000 ms\n" +
      "    clamp threshold to 0.02 .. 0.40",
    caption: "Algorithm 2. The arguments are the session baseline, not the current values.",
  },
  { h3: "7.2.2 The defect: an accidental integrator" },
  {
    p: "The first implementation divided the current interval by the pace factor. Each suggestion " +
      "therefore compounded on the previous one, and a device run produced the following within " +
      "four minutes of ordinary speech:",
  },
  {
    table: {
      head: ["Elapsed", "Interval", "Threshold", "What each is asserting"],
      widths: [1600, 2200, 2200, 3406],
      rows: [
        ["0:00", "8.0 s", "0.080", "baseline"],
        ["1:20", "4.8 s", "0.064", "sampling faster, and more sensitive"],
        ["2:40", "2.9 s", "0.051", "still accelerating"],
        ["4:00", "2.0 s (clamped)", "0.041", "interval says 'racing'; threshold says the opposite"],
      ],
    },
    tnum: "7.2",
    caption: "Measured compounding drift in the first implementation",
  },
  {
    p: "Neither number reflected the measurement any longer. The loop had become an integrating " +
      "controller by accident: every output was fed back as the next input's baseline, so the " +
      "error accumulated instead of being corrected. The fix is one line of interface design — " +
      "suggest() takes the session baseline as an argument — and it removes the loop's memory of " +
      "its own output entirely. Two identical measurements now produce identical settings, and the " +
      "system returns to baseline the moment the pace does.",
  },
  {
    p: "A manual move of either slider resets the baseline to the new value, so the student's " +
      "judgement is treated as the new anchor rather than as something the loop will drift away " +
      "from over the next four minutes.",
  },

  { h2: "7.3 Algorithm 3: transcript overlap repair" },
  {
    p: "The problem. Audio is transcribed in chunks. If chunks abut exactly, a word straddling the " +
      "boundary is seen only as two fragments, and the model guesses at each. In the first field " +
      "trial \"product\" became \"produced\" at a chunk seam, and words disappeared at regular " +
      "intervals throughout the transcript — regular because the seams were regular.",
  },
  { fig: "docs/diagrams/fig-algo-overlap.png", num: "7.3", caption: "Overlap repair and per-chunk dominant speaker", opts: { maxH: 780 } },
  {
    p: "The solution has two halves. Chunks overlap by two seconds, so every word is fully " +
      "contained in at least one chunk. That guarantees nothing is lost and guarantees something " +
      "is duplicated, so the duplicate must then be removed.",
  },
  { h3: "7.3.1 Why the trimming is word-level" },
  {
    p: "The two transcriptions of the overlapping audio are rarely identical. Punctuation, casing " +
      "and filler words differ between calls even when the words agree, so a character-level " +
      "comparison finds no common prefix and the repeat survives into the note. trimRepeatedPrefix " +
      "compares normalised words — lower-cased, stripped of punctuation — and requires a run of at " +
      "least three before it will declare an overlap.",
  },
  {
    p: "Three is not arbitrary. One or two matching words occur constantly in ordinary speech, and " +
      "a two-word minimum removes genuine content: a lecturer who says \"so, so, so\" or restates " +
      "a phrase for emphasis loses it. Three is the smallest run that does not fire on natural " +
      "repetition, and the suite tests exactly that case.",
  },

  { h2: "7.4 Algorithm 4: append-only delta with optimistic revision control" },
  {
    p: "The problem has two halves and one mechanism solves both. The first half is cost: a note " +
      "that is re-sent in full on every update grows in cost quadratically with lecture length. " +
      "The second half is safety: a model that can rewrite the note can delete something the " +
      "student wrote, and no amount of instruction reliably prevents it.",
  },
  { fig: "docs/diagrams/fig-algo-delta.png", num: "7.4", caption: "Append-only delta protocol: what is sent, the six steps of an append, and the measured cost", opts: { maxH: 800 } },
  { h3: "7.4.1 The protocol" },
  {
    code: "read   revision r, contentHash h\n" +
      "build  request = { syllabus<=4k, outline<=4k, recent<=6k, newEvidence, h, r }\n" +
      "call   proxy; response is schema-validated or it is a 502\n" +
      "ground groundingRatio(delta, evidence) >= 0.25  and  |delta| >= 120 chars\n" +
      "check  revision in the database is still r\n" +
      "commit append delta to the living block; revision := r + 1   (one transaction)",
    caption: "Algorithm 4",
  },
  {
    p: "The model receives an outline of what has been covered and the tail of the note, never the " +
      "whole note. It returns only new text. It can address exactly one block — the one named by " +
      "capture_sessions.generatedBlockId — and the schema's unique index on that column means no " +
      "second session can claim it.",
  },
  { h3: "7.4.2 Why optimistic rather than locking" },
  {
    p: "Two things append to the same note concurrently: the audio pipeline finishing a chunk, and " +
      "the board pipeline finishing an extraction. They are independent, they take different " +
      "amounts of time, and neither can be made to wait without dropping evidence. A lock " +
      "serialises them and stalls the camera behind a slow transcription call. An optimistic check " +
      "costs one integer read and loses nothing when it fails, because rebuilding the request is " +
      "cheap — the context is bounded regardless.",
  },
  {
    p: "The SHA-256 is what makes the check honest. A revision number alone is satisfied by an " +
      "edit that changes text without changing the count, and in this system that is a routine " +
      "event: the student can correct a sentence while the session is running. Binding the delta " +
      "to a hash of the content it was computed against closes the gap.",
  },
  { h3: "7.4.3 The empty delta" },
  {
    p: "A chunk of silence, or of a lecturer answering an unrelated question, adds nothing. The " +
      "prompt correctly asked the model to return an empty delta in that case. Both the proxy and " +
      "the client then treated an empty delta as a failure and retried it, so a silent chunk cost " +
      "three calls instead of one. Both sides now accept it. This is a small bug with a large " +
      "lesson: the instruction and the validator were written at different times against different " +
      "assumptions, and nothing checked that they agreed.",
  },

  { h2: "7.5 Algorithm 5: per-chunk dominant speaker" },
  {
    p: "The provider returns diarisation labels within a chunk. Rather than trust them " +
      "individually, a chunk is assigned a single label only when one voice holds at least 58 per " +
      "cent of the voiced time with a lead of at least 15 percentage points over the next. " +
      "Otherwise the chunk is marked AMBIGUOUS and no speaker is claimed.",
  },
  {
    p: "This is deliberately not speaker identification. No voiceprint is enrolled, nothing is " +
      "stored that could identify a person, and no name is ever attached. The label answers one " +
      "question — did the voice change? — which is what a student needs in order to read a note " +
      "and distinguish the lecturer's explanation from a question asked from the back of the room. " +
      "Attaching identities would require enrolment and stored voiceprints, and is excluded on " +
      "privacy grounds in Chapter 5.",
  },
  {
    p: "Being willing to answer \"I do not know\" is the design point. A confident wrong " +
      "attribution is worse than no attribution, because a student reading the note has no way to " +
      "detect it.",
  },

  { h2: "7.6 Algorithm 6: the grounding guard" },
  {
    p: "The only algorithm in this list that runs on the server, and the one with the clearest " +
      "origin. During the third field trial the note contained a section headed \"Applications in " +
      "Industry\" with four confident paragraphs, at a point where the transcript contains a " +
      "two-minute silence while a diagram was drawn. The prompt in force at the time explicitly " +
      "forbade adding material absent from the evidence.",
  },
  { fig: "docs/diagrams/fig-algo-grounding.png", num: "7.5", caption: "The grounding guard, and the incident that produced it", opts: { maxH: 780 } },
  {
    code: "content(x)   = words of x, lower-cased, stop words removed, lightly stemmed\n" +
      "ratio        = |content(delta) INTERSECT content(evidence)| / |content(delta)|\n" +
      "\n" +
      "accept if    ratio >= 0.25   AND   |delta| >= 120 characters\n" +
      "else         discard the delta; keep the evidence; the next chunk retries",
    caption: "Algorithm 6",
  },
  {
    table: {
      head: ["Sample", "Grounding ratio", "Outcome"],
      widths: [4200, 2200, 3006],
      rows: [
        ["Honest deltas across four field trials", "0.34 to 0.71", "Accepted"],
        ["The fabricated \"Applications in Industry\" section", "0.08", "Would be rejected"],
        ["A second fabricated passage found on review", "0.11", "Would be rejected"],
        ["Threshold in force", "0.25", "Separates the two with room on both sides"],
      ],
    },
    tnum: "7.3",
    caption: "Measured grounding ratios, and where the threshold sits",
  },
  {
    p: "The floor is not higher because a faithful summary legitimately introduces words the " +
      "lecturer never said: connectives, section headings, the word \"therefore\". A ratio of 0.9 " +
      "would reject good summaries. The 120-character minimum exists so that a one-line delta " +
      "cannot pass on coincidence, which a short delta can when it contains four words and two of " +
      "them appear in the transcript.",
  },
  {
    note: "The general principle, which appears four times in this system: a prompt instruction is a " +
      "request, and a validator is a guarantee. Chapter 11 lists the other three places the same " +
      "reasoning is applied.",
  },

  { h2: "7.7 Summary" },
  {
    table: {
      head: ["Algorithm", "Runs on", "Constant", "Set from"],
      widths: [3000, 1400, 2200, 2806],
      rows: [
        ["1. Frame-change detection", "Device", "settleRatio = 0.45", "Field log; noise floor of a handheld phone"],
        ["2. Adaptive sampling", "Device", "90 and 150 wpm", "Observed speech pace across four trials"],
        ["3. Transcript overlap repair", "Device", "3-word minimum", "Smallest run that spares genuine repetition"],
        ["4. Append-only delta", "Both", "4k / 4k / 6k chars", "Measured request size against cost"],
        ["5. Dominant speaker", "Device", "58 % share, 15 pt margin", "Observed diarisation confidence"],
        ["6. Grounding guard", "Server", "ratio 0.25, 120 chars", "Measured honest vs fabricated deltas"],
      ],
    },
    tnum: "7.4",
    caption: "The six algorithms, and the origin of every constant in them",
  },
  {
    p: "Every constant in that table came from a measurement rather than from a preference. That " +
      "is stated because the first version of three of these six used a constant chosen by " +
      "reasoning about what ought to work, and in each case the reasoning was sound and the " +
      "constant was wrong by an order of magnitude.",
  },
];

const CH_IMPLEMENTATION = [
  { h1: "8. Implementation" },
  {
    p: "This chapter records how the design in Chapter 6 became working software, and the platform " +
      "realities that shaped it. It is organised by subsystem, and each section states what was " +
      "difficult rather than restating what the code does — the code itself is reproduced in full " +
      "in Appendix V.",
  },

  { h2: "8.1 Composition of the codebase" },
  {
    table: {
      head: ["Area", "Files", "Approx. lines", "Notes"],
      widths: [2800, 1000, 1600, 4006],
      rows: [
        ["UI and navigation", "31", "5,400", "Sixteen destinations, theme layer, design primitives"],
        ["Capture (audio, camera)", "12", "2,100", "AudioRecord, CameraX, the frame filter, pace tracking"],
        ["Notes and persistence", "14", "2,700", "Room entities, DAOs, repository, five migrations"],
        ["AI client and delta assembly", "9", "1,800", "HTTP client, request building, response parsing"],
        ["Markdown and rendering", "5", "1,000", "Parser (243 lines), renderer (292 lines), flag parsing"],
        ["Debug (temporary)", "3", "337", "Ring buffer, screen, export"],
        ["Tests", "32", "2,574", "134 unit, 3 instrumentation"],
        ["Backend", "2", "2,939", "Service and contract tests, no dependencies"],
      ],
    },
    tnum: "8.1",
    caption: "Where the code is",
  },

  { h2: "8.2 Audio capture" },
  {
    p: "Audio is captured with AudioRecord at 16 kHz, mono, PCM16, and not with a higher-level " +
      "recorder. The reason is chunking: the pipeline needs direct access to the sample buffer in " +
      "order to cut a chunk at a chosen offset and carry the last two seconds forward into the " +
      "next one. MediaRecorder produces an encoded file and does not expose the buffer, which " +
      "makes a two-second overlap impossible to construct without decoding what was just encoded.",
  },
  {
    code: "class PcmChunkAccumulator(\n" +
      "    private val chunkMs: Long = 20_000,\n" +
      "    private val overlapMs: Long = 2_000,\n" +
      ") {\n" +
      "    fun takeChunk(): Chunk? {\n" +
      "        if (buffered < chunkSamples) return null\n" +
      "        val chunk = buffer.copyOf(chunkSamples)\n" +
      "        // carry the tail forward so the next chunk begins 2 s earlier\n" +
      "        buffer.copyInto(buffer, 0, chunkSamples - overlapSamples, buffered)\n" +
      "        buffered = overlapSamples\n" +
      "        return Chunk(chunk, startMs, chunkMs, overlapMs)\n" +
      "    }\n" +
      "}",
    caption: "The overlap carry, which is the whole of the fix for words lost at chunk seams",
  },
  {
    p: "Chunks enter an ordered queue that is never allowed to drop. Transcription is slower than " +
      "capture on a poor connection, and the queue is what stops that turning into lost lecture. " +
      "If the queue grows beyond a ceiling the session enters DEGRADED and retains raw audio to " +
      "disk instead, which is recoverable afterwards; what it never does is discard the oldest " +
      "chunk to make room.",
  },

  { h2: "8.3 Camera and the frame filter" },
  {
    p: "CameraX with LifecycleCameraController removes an entire class of camera lifecycle defects " +
      "by binding the camera to the composable's lifecycle rather than to manually managed state. " +
      "Frame analysis runs on a background executor at the configured interval and never on the " +
      "main thread.",
  },
  {
    p: "The 32 x 32 downsample is done once per analysed frame and produces a 1,024-byte array. " +
      "The cost of comparing two such arrays is negligible, which is what makes it affordable to " +
      "run this analysis every few seconds for fifty minutes on a battery. Analysing full frames " +
      "would be both slower and no more accurate for this purpose: the question is whether a " +
      "region of the board has changed, and 32 x 32 resolves a region.",
  },
  {
    p: "The zoom slider is bound to CameraX's ZoomState and calls setZoomRatio. This was reported " +
      "as broken after the third field trial — pinch worked and the slider did not — and the cause " +
      "was that the slider was updating its own local state without ever reaching the controller. " +
      "The thumb moved and nothing happened, which is the most confusing possible failure because " +
      "the control appears to be working.",
  },

  { h2: "8.4 Persistence" },
  {
    p: "Room with compile-time query verification, ten entities, and destructive fallback " +
      "explicitly disabled. Disabling the fallback means a missing migration fails the build " +
      "rather than silently dropping a student's notes on upgrade, which is the correct trade for " +
      "an application whose entire purpose is not losing things.",
  },
  {
    p: "Appends are transactional. Committing a delta writes the block content and increments the " +
      "session revision in one transaction, so there is no window in which the note has advanced " +
      "and the revision has not — a window in which a second concurrent append would pass its " +
      "revision check incorrectly.",
  },

  { h2: "8.5 Concurrency" },
  {
    p: "Three pipelines run concurrently for the duration of a session: audio capture and " +
      "transcription, camera sampling and extraction, and note composition. They are Kotlin " +
      "coroutines on distinct dispatchers, and they share exactly one mutable resource — the note " +
      "— through exactly one mechanism, the revision-checked append described in Chapter 7.",
  },
  {
    p: "That single point of contact is the reason the concurrency in this system is tractable. " +
      "There is no shared mutable state between the pipelines, no lock held across a network call, " +
      "and no ordering requirement between the audio and board loops. Each writes evidence " +
      "independently and each attempts an append; the loser of a race rebuilds and retries.",
  },

  { h2: "8.6 Markdown parsing and rendering" },
  {
    p: "The reader originally displayed Markdown source. The first field trial reported the note " +
      "showing literal ## and $$ characters, which is a defect of the most embarrassing kind: the " +
      "design had assumed a renderer and none had been written.",
  },
  {
    p: "The parser is 243 lines and produces a sealed hierarchy of block types — heading, " +
      "paragraph, bullet, numbered, quote, code, table, divider — and the renderer is 292 lines of " +
      "Compose that maps each to a composable. Tables were added after the third trial, when the " +
      "feedback was that notes used bullet points and nothing else. That feedback was correct and " +
      "had two causes: the renderer could not draw a table, and the prompt never asked for one. " +
      "Both were changed.",
  },

  { h2: "8.7 Networking" },
  {
    p: "One client, one base URL, one bearer token read from configuration and never logged. Retry " +
      "behaviour is deliberate and differentiated: transient failures (502, 504) are retried twice " +
      "with backoff, and permanent failures (400, 401, 413, 503-budget) are terminal.",
  },
  {
    p: "That distinction was itself a defect once. An early build retried a permanent quota error " +
      "on a fixed schedule, turning one exhausted account into a steady stream of " +
      "guaranteed-failing requests for the rest of the lecture. The current behaviour is that a " +
      "permanent failure moves the session to DEGRADED, warns the student, and retains audio.",
  },

  { h2: "8.8 The temporary diagnostic log" },
  {
    p: "VbDebugLog is 83 lines: a 500-entry ring buffer with a timestamp, a tag and a short " +
      "message, plus a settings screen that displays it and exports it through the share sheet. It " +
      "exists because the second field trial produced a complete failure with no diagnosis " +
      "available, and it produced the diagnosis for the third trial within seconds.",
  },
  {
    table: {
      head: ["Rule", "How it is enforced"],
      widths: [3600, 5806],
      rows: [
        ["Speech is never stored in full", "Recorded as a character count plus a 60-character prefix"],
        ["No audio, images or note bodies", "The API accepts only a tag and a short string"],
        ["Nothing is uploaded", "There is no network code in the component"],
        ["Absent from release builds", "Build configuration; the screen is not in the release navigation graph"],
        ["It will actually be removed", "Removal checklist tracked in docs/TEMPORARY_DEBUG_LOG.md"],
      ],
    },
    tnum: "8.2",
    caption: "Constraints on the temporary diagnostic log",
  },
  {
    note: "The last row is the one that matters. A diagnostic aid added under pressure during a " +
      "failing trial is exactly the kind of thing that becomes permanent by inattention. Writing " +
      "the removal checklist at the same time as the feature was the mechanism chosen to prevent " +
      "that, and it is listed as an open item in Chapter 15 until it is done.",
  },
];

const CH_UI = [
  { h1: "9. User Interface Design" },
  {
    p: "Sixteen destinations. This chapter describes the navigation structure, the two rules that " +
      "govern it, and the interface changes that came directly out of using the application in a " +
      "real lecture rather than out of design intent. Every screenshot was taken from a build " +
      "running on a physical device.",
  },

  { h2: "9.1 Navigation structure" },
  { fig: "docs/diagrams/fig-navigation.png", num: "9.1", caption: "Screen map and navigation graph" },
  {
    p: "The two rules at the foot of Figure 9.1 both came from field use and both correct a " +
      "specific loss.",
  },
  {
    n: [
      "Pressing back during a live session collapses the camera preview rather than leaving the " +
        "session. The first field trial lost eleven minutes of a lecture to an accidental back " +
        "gesture, and no amount of confirmation dialog is an acceptable answer during a lecture — " +
        "the correct answer is that the gesture does something harmless.",
      "Finishing a session clears the capture destinations from the back stack and lands on the " +
        "note reader. A finished session is not a place a student can return to, so it is not left " +
        "on the stack for them to navigate back into.",
    ],
  },

  { h2: "9.2 Onboarding and consent" },
  {
    p: "Onboarding runs once and covers three things: what is recorded, where it is stored, and " +
      "what leaves the device. The consent step states in plain words that lecture audio is " +
      "processed by a third-party model. It is not a link to a policy, because a link to a policy " +
      "is a way of satisfying a requirement without informing anybody.",
  },
  { shots: [
      { file: "01-onboarding-1.png", num: "9.2", caption: "Onboarding, first step" },
      { file: "03-onboarding-3.png", num: "9.3", caption: "Consent, stated in plain words" },
    ], opts: { perRow: 2, width: 224 } },

  { h2: "9.3 Home and capture setup" },
  {
    p: "Home carries one primary action and a list of recent notes. Capture setup asks three " +
      "questions — mode, note policy, syllabus — and then gets out of the way. Board mode is the " +
      "more capable and the more expensive; audio mode never opens the camera and never spends a " +
      "vision call, and the difference is stated on the screen rather than buried in settings.",
  },
  { shots: [
      { file: "04-home.png", num: "9.4", caption: "Home" },
      { file: "05-capture-setup.png", num: "9.5", caption: "Capture setup" },
    ], opts: { perRow: 2, width: 224 } },

  { h2: "9.4 The live session screen" },
  {
    p: "This is the screen that matters, because it is the only one the student sees while the " +
      "lecture is happening and therefore the only one whose design has a cost measured in missed " +
      "explanation. It shows elapsed time, the current revision, a running count of frames kept " +
      "and skipped, and the camera preview with a frame guide.",
  },
  { shots: [
      { file: "14-live-board.png", num: "9.6", caption: "Board mode, with the frame guide" },
      { file: "16-live-running.png", num: "9.7", caption: "A running session: 01:32, revision 5, 3 kept and 9 skipped" },
    ], opts: { perRow: 2, width: 224 } },
  {
    p: "Two controls on this screen were added because of field use and neither was in the " +
      "original design.",
  },
  {
    p: "Capture now overrides the automatic filter. The filter is right about most frames and " +
      "wrong about some, and the student can see which is which. Giving them a button is both " +
      "cheaper and more reliable than any amount of additional tuning of the filter, and it is " +
      "what replaced the rejected \"capture anyway after N frames\" fix described in Chapter 7.",
  },
  {
    p: "The tuning panel changes the sampling interval and the sensitivity without ending the " +
      "session. Before it existed, a badly chosen sensitivity was locked in for fifty minutes, " +
      "which was reported after the third trial in exactly those terms. The panel also shows what " +
      "the adaptive algorithm has done — \"Every 8 s\", \"8 % change to keep a frame\" — so the " +
      "student can see the system adjusting rather than wondering whether it is.",
  },
  { shots: [
      { file: "15-live-tuning.png", num: "9.8", caption: "The tuning panel, adjustable mid-session" },
    ], opts: { perRow: 2, width: 224 } },

  { h2: "9.5 The reader" },
  {
    p: "The reader renders Markdown properly: headings, bullets, numbered lists, quotes, code, " +
      "tables and horizontal rules. Board crops appear inline with captions and open in a lightbox " +
      "with pinch and slider zoom. Review flags render as cards showing the note's reading and the " +
      "evidence's reading side by side, with two actions, neither of which rewrites the evidence.",
  },
  { shots: [
      { file: "17-note-reader.png", num: "9.9", caption: "The reader" },
      { file: "18-library-populated.png", num: "9.10", caption: "Library, with folders and search" },
    ], opts: { perRow: 2, width: 224 } },
  {
    p: "Figure 9.9 shows a refusal rather than a success, and is included for that reason. The " +
      "session behind it was recorded in a quiet room; five audio chunks contained no speech. " +
      "Instead of producing plausible lecture notes, the system recorded at each timestamp that no " +
      "clear speech was found. A system that filled those five gaps with fluent text would look " +
      "better in a screenshot and would be worse in every way that matters to a student revising " +
      "from it.",
  },

  { h2: "9.6 Organising and settings" },
  {
    p: "Nested folders — a folder per subject with a folder per unit inside it — plus search, " +
      "rename, and delete behind an explicit confirmation. Settings has six sub-pages, of which " +
      "Connection is the one that earns its place: it reports live health from the proxy including " +
      "which model each route is currently routed to, so that a deployment can be confirmed from " +
      "the phone rather than assumed.",
  },
  { shots: [
      { file: "19-new-folder.png", num: "9.11", caption: "Creating a nested folder" },
      { file: "20-note-actions.png", num: "9.12", caption: "Note actions, including delete behind a confirmation" },
    ], opts: { perRow: 2, width: 224 } },
  { shots: [
      { file: "08-settings.png", num: "9.13", caption: "Settings" },
      { file: "10-connection.png", num: "9.14", caption: "Connection, showing live proxy health and model routing" },
    ], opts: { perRow: 2, width: 224 } },

  { h2: "9.7 Two interface defects worth recording" },
  {
    p: "Both compiled, both passed the test suite, and both were completely broken on the device.",
  },
  {
    p: "The first: six settings sub-pages crashed on open. The cause was a negative value passed " +
      "to Modifier.padding, which compiles without complaint and throws at runtime. No unit test " +
      "instantiates those screens, so nothing caught it. The same mistake was made twice in one " +
      "development session, in two different components, which is the strongest possible argument " +
      "that it needs a lint rule rather than more care. It is listed as an open item in Chapter 15.",
  },
  {
    p: "The second: rename and delete did nothing. The state was set correctly and the dialog was " +
      "never drawn, because the render block had ended up outside the composable during an " +
      "automated edit. It compiled, 109 tests passed, and pressing the button had no effect " +
      "whatsoever. This is the defect that produced the device instrumentation suite: a compiled " +
      "control is not a reachable one, and the only thing that can tell the difference is a test " +
      "that runs on a device and presses it.",
  },
];

const CH_BACKEND = [
  { h1: "10. Backend Service and Deployment" },

  { h2: "10.1 Why a backend exists at all" },
  {
    p: "The application would be simpler without one. It exists because of a single constraint: " +
      "the AI provider credential must not be in the APK. An APK can be decompiled by anyone who " +
      "installs it, so a credential compiled into it is a credential published. Therefore the " +
      "credential must live somewhere the APK does not, and therefore there is a server.",
  },
  {
    p: "Everything else the proxy does was added afterwards, because there was already a place to " +
      "put it. That ordering is worth stating: rate limiting, budget accounting, schema " +
      "enforcement and the grounding guard are all valuable, and none of them would have justified " +
      "a server on its own.",
  },

  { h2: "10.2 What it is" },
  {
    table: {
      head: ["Property", "Value", "Reason"],
      widths: [2400, 2400, 4606],
      rows: [
        ["Runtime", "Node.js 22", "Available on the target platform; native fetch and native test runner"],
        ["Dependencies", "None", "The entire security surface is readable in one file. No transitive supply chain."],
        ["Web framework", "None", "The router is a chain of path comparisons. Nothing is hidden in middleware."],
        ["Source", "One file, 2,939 lines including tests", "Auditable by reading it"],
        ["Container", "node:22-alpine, non-root", "Minimal base; no shell tooling in the image"],
        ["Hosting", "Render web service", "Free tier; a cold start after idle costs about 13 seconds"],
        ["Secrets", "Environment variables only", "Never in the repository, never in the image, never logged"],
      ],
    },
    tnum: "10.1",
    caption: "The proxy",
  },
  {
    p: "The decision to use no dependencies at all is unusual and was deliberate. A proxy that " +
      "holds a credential is exactly the component where a transitive dependency with a " +
      "compromised maintainer does the most damage, and the amount of code saved by a framework " +
      "here is a router and a body parser — perhaps eighty lines. Eighty lines is a price worth " +
      "paying for a security surface that can be audited by reading a single file.",
  },

  { h2: "10.3 What it enforces" },
  {
    n: [
      "Authentication. Every pipeline route requires a bearer token, compared in constant time so " +
        "that a caller cannot learn it a character at a time by measuring rejection latency. A " +
        "failure returns before any provider call is made.",
      "Rate limiting. A per-minute window, counted per token. Bursts are refused at the proxy, " +
        "which means a client defect cannot become a bill.",
      "Daily budget. A hard ceiling on requests per day. This is the control that bounds the " +
        "damage if the client token is extracted from the APK, which it can be.",
      "Schema enforcement. Every provider response is requested with a strict json_schema and " +
        "provider parameter requirements set, and is validated again on return. Prose where " +
        "structure was required is a 502 from the proxy, not a parse failure on the phone.",
      "Grounding. Composition responses are scored against the evidence they claim to summarise " +
        "and discarded below the floor. Chapter 7 gives the measurements behind the threshold.",
    ],
  },

  { h2: "10.4 Deployment topology" },
  { fig: "docs/diagrams/fig-deployment.png", num: "10.1", caption: "Deployment: three nodes, two network hops, one credential" },

  { h2: "10.5 Operations" },
  {
    table: {
      head: ["Task", "Procedure"],
      widths: [2800, 6606],
      rows: [
        ["Rotate the client token", "Change VOXBOX_CLIENT_TOKEN in the environment, redeploy, rebuild the APK. Old tokens stop working immediately."],
        ["Rotate the provider key", "Change OPENROUTER_API_KEY in the environment and redeploy. No application change; no rebuild."],
        ["Confirm a deployment", "GET /health and read the models block, or open the Connection screen on the phone."],
        ["Roll back", "Redeploy the previous image. The proxy is stateless apart from in-memory counters."],
        ["Change the daily budget", "One environment variable; takes effect on redeploy."],
      ],
    },
    tnum: "10.2",
    caption: "Operational procedures (full runbook: server/OPERATIONS.md)",
  },
  {
    note: "The models block in /health exists because of a specific incident. Three separately " +
      "reported defects turned out to have one cause: a fix had been committed and never pushed, " +
      "so the live proxy was running older code while the repository looked correct. The " +
      "application can now answer \"is my fix live?\" from the Connection screen. The standing rule " +
      "recorded in Chapter 13 is: a commit is not a release.",
  },
];

const CH_SECURITY = [
  { h1: "11. Security, Privacy and Consent" },
  {
    p: "This chapter states what the system protects, how, and — equally important — what it does " +
      "not protect and has chosen to disclose instead.",
  },

  { h2: "11.1 Trust boundaries" },
  { fig: "docs/diagrams/fig-security.png", num: "11.1", caption: "Trust boundaries, threats considered and the control at each", opts: { maxH: 800 } },

  { h2: "11.2 Credential handling" },
  {
    p: "Two credentials exist and the distinction between them is the centre of this chapter's " +
      "argument.",
  },
  {
    table: {
      head: ["", "Provider key", "Client token"],
      widths: [2200, 3600, 3606],
      rows: [
        ["Lives in", "The deployment environment only", "The APK"],
        ["Grants", "Full use of the AI provider account", "Use of this proxy and nothing else"],
        ["Recoverable by an attacker", "No route exists", "Yes, by decompiling the APK"],
        ["Damage if leaked", "Unbounded provider spend", "Bounded by rate limit and daily budget"],
        ["Rotation", "One environment variable, no rebuild", "One environment variable plus an APK rebuild"],
      ],
    },
    tnum: "11.1",
    caption: "The two credentials",
  },
  {
    p: "The client token being extractable is an accepted risk rather than an oversight, and it is " +
      "worth being explicit about why. A client secret embedded in a distributed application is " +
      "always recoverable; there is no configuration of obfuscation that changes this. The " +
      "engineering response is not to pretend otherwise but to make the recovered secret worth " +
      "little: it authorises use of a rate-limited, budget-capped proxy and grants no access to " +
      "the provider account, no access to any student's data, and no access to anything else. It " +
      "is listed as a known, bounded risk in Chapter 15, and rotating it after any public demo is " +
      "an operational procedure in Chapter 10.",
  },

  { h2: "11.3 Data handling on the device" },
  {
    b: [
      "All captured media is written to application-private storage. Nothing is written to MediaStore or to shared storage, so no other application can read a lecture recording or a board photograph.",
      "Full-resolution frames are deleted once extraction has completed. A lecture's worth of raw camera frames does not accumulate.",
      "Retained audio, written when the network fails, is offered for recovery and is deletable from the storage settings page.",
      "Deleting a note cascades in one transaction to its blocks, sessions, transcript segments, visual evidence, assets and provenance. No orphan row survives a delete.",
    ],
  },

  { h2: "11.4 The diagnostic log" },
  {
    p: "The temporary diagnostic log is the highest-risk component in the system by nature, " +
      "because a log of a capture pipeline is a log of a lecture. It is constrained accordingly: " +
      "transcribed speech is stored as a character count and a sixty-character prefix and never in " +
      "full; no audio, images or note bodies are written; there is no network code in the " +
      "component at all; and it is absent from release builds. Its removal checklist is tracked in " +
      "a file so that it does not survive by inattention.",
  },

  { h2: "11.5 Consent" },
  {
    p: "Consent is obtained during onboarding, before the first recording, and states plainly that " +
      "lecture audio is processed by a third-party model. The system cannot verify institutional " +
      "policy on recording lectures and does not pretend to; it states what it does and leaves the " +
      "judgement where it belongs.",
  },
  {
    p: "The residual risk is disclosed rather than mitigated, and it is a real one. Lecture audio " +
      "leaves the device and is processed by a third party under that party's retention policy. " +
      "The proxy sends no student identifier — it has none to send — so the audio is not linked to " +
      "a person by this system, but the audio itself leaves. That is stated in Figure 11.1 as a " +
      "residual risk and stated again here, because a privacy chapter that lists only the things " +
      "the system does well is not a privacy chapter.",
  },

  { h2: "11.6 Validation as a security control" },
  {
    p: "Four places in this system apply the same reasoning: an instruction to a model changes the " +
      "probability of an output, and only a check on the output changes what can reach the " +
      "student.",
  },
  {
    table: {
      head: ["Check", "What it prevents", "Where it runs"],
      widths: [2800, 4400, 2206],
      rows: [
        ["Strict json_schema on every response", "Prose arriving where structure is required", "Proxy"],
        ["Grounding ratio floor", "Fabricated content reaching the note", "Proxy"],
        ["Rejection of timestamps the client did not emit", "Invented timestamps such as 00:80, observed in a real note", "Client"],
        ["Path-traversal check on asset references", "An asset reference escaping the note's directory", "Client"],
        ["Revision and hash check before append", "A stale delta overwriting newer text", "Client"],
      ],
    },
    tnum: "11.2",
    caption: "Output validation, and the failure each check prevents",
  },
  {
    p: "None of these is sophisticated. Each exists because the alternative — trusting the " +
      "instruction — was tried first and failed in front of a class.",
  },
];

module.exports = { CH_DESIGN, CH_ALGORITHMS, CH_IMPLEMENTATION, CH_UI, CH_BACKEND, CH_SECURITY };

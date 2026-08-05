# VoxBox — Review 1 Handbook

Your reference sheet for the scope review. Read sections 1–3 before you walk in; the rest is
lookup during questions.

**Golden rule for this review:** never claim a number you have not measured. An examiner who catches
one bluff discounts everything else. "Implemented and contract-tested, accuracy not yet measured —
here is my measurement plan" is a *strong* answer at review 1, not a weak one. Scope reviews reward
clear boundaries, not big claims.

**Deck map** — `outputs/VoxBox_Review1_Scope.pptx`, 14 slides:

| Slides | Content | Handbook |
| --- | --- | --- |
| 1–3 | Title · the problem · what VoxBox does | §1 |
| **4** | **Scope — in and out.** The slide the review is marked on | §2 |
| 5–6 | How it works end to end · system architecture | §3 |
| 7–8 | Core frame filter · the other four algorithms | §4 |
| 9 | Alternatives considered and rejected | §5 |
| 10 | Speaker focus and background noise | §6 |
| 11–12 | Validation status · real-lecture trial | §7–8 |
| 13–14 | Roadmap · closing | §9 |

---

## 1. The 30-second pitch

> VoxBox is an Android app that turns a live lecture into a structured Markdown note while the class
> is happening. It listens continuously, and in board mode it also watches the board through the
> camera. It filters out frames that haven't changed on the phone itself, so it only spends AI calls
> on new content. Everything is stored locally, and the AI is never allowed to silently rewrite what
> was actually captured — disagreements are surfaced as review flags for the student to accept.

If you say only one more sentence, make it this:

> The design principle throughout is **evidence preservation**: the raw transcript and board text are
> kept, and any AI suggestion sits next to them rather than replacing them.

---

## 2. Scope — say this crisply

Scope reviews are marked on whether you know your own boundaries. Have this table in your head.

### In scope (built and working)

| Capability | Status |
| --- | --- |
| Continuous foreground voice capture, 20-second chunks | Working, device-verified |
| Camera board capture at an adjustable interval (2–30 s) | Working, device-verified |
| On-device frame-change filtering before any API call | Working, unit-tested |
| Diarized transcript with per-chunk speaker labels | Working, live-verified |
| Incremental Markdown note that grows during the lecture | Working, live-verified |
| Board text, equations, concepts and diagram crops | Working, live-verified |
| Two note styles: Runnable (structured) and Verbatim | Working |
| Detail control: short / balanced / elaborate + custom instruction | Working |
| End-of-session check of formulas, units and concepts | Working, live-verified |
| Local storage, folders, syllabus context, Obsidian export | Working |
| Hosted authenticated HTTPS backend, key never in the APK | Deployed |

### Explicitly out of scope (say this before they ask)

| Not doing | Why |
| --- | --- |
| Background or screen-off recording | Deliberate. Capture is visible and foreground-only, for consent and privacy. |
| Identifying *who* the teacher is across a lecture | Needs voice enrolment and consent. See §6 — this is the question they will ask. |
| Real-time word-by-word live captions | Chunked at 20 s. Different problem, different latency budget. |
| Multi-user accounts, cloud sync, sharing | Local-first by design. Export is the sharing story. |
| Handwriting recognition of arbitrary scrawl | Bounded by what the vision model can read; uncertainty is flagged, not hidden. |
| PDF/DOCX syllabus import | Markdown and text only for now. |

**Framing sentence if pushed:** *"I scoped it to one student, one device, one lecture at a time. That
let me go deep on evidence handling and cost control rather than shallow on collaboration features."*

---

## 3. Architecture in one breath

Two capture lanes run independently and merge into a single note. This is the diagram on the
"How it works, end to end" slide — walk it left to right.

```
  CAPTURE              ON DEVICE                 VIA THE PROXY            THE NOTE
                       (my code)                 (model calls)            (my code)

  Microphone  ──▶  Ordered audio queue   ──▶  Transcription        ──┐
  16 kHz mono      unbounded, never             text + per-chunk      │
  20 s chunks      dropped                      dominant speaker      │
                                                                      ├──▶  Note builder
  Camera      ──▶  32×32 change filter   ──▶  Board extraction     ──┘      append-only delta
  frame every      ~450/hr → ~40 sent;          text, equations,             SHA-256 binding
  2–30 s           rest deleted on phone        concepts, crops              revision check
                                                                                   │
                                                                                   ▼
                                                                          Live Markdown note

  Everything captured is written to Room as-is. No model output overwrites it.
  At session end, a second model re-reads the note against the evidence and
  returns findings, appended as review flags.
```

Deployment view: the phone talks only to **an authenticated HTTPS proxy** (Node, hosted on Render),
which holds the API key and forwards to **OpenRouter** — transcription, vision, note and
verification models, each selected per role and swappable by configuration.

**Why a proxy at all?** So the API key never ships inside the APK. An APK is a distributable
archive — any credential compiled into it can be recovered. The key lives in the server's secret
store, and the app carries only a client token that can be rotated independently.

---

## 4. "What algorithms did *you* implement?"

This is the question that separates a wrapper from a project. **Five things are yours, not the API's.**

### 4.1 Frame-change detection — the core cost-saving algorithm

Runs entirely on the phone, before any network call.

1. Downsample each JPEG to a **32×32 luminance grid**.
2. **Compensate for global exposure change** — subtract the mean brightness shift, so the lights
   dimming or auto-exposure adjusting does not read as "the board changed".
3. Combine two signals: **centred pixel difference** and **fraction of pixels that changed**.
4. Compare against the last *successfully committed* board state, not merely the last frame seen.
5. Score ≥ threshold → send to the vision model. Below → **delete the frame immediately**, no API call.

**Two-phase commit is the subtle part.** A new baseline is only adopted after the note update and the
diagram crops have been saved. If extraction fails, the baseline stays where it was, so the same
board change is detected again on the next frame instead of being lost forever.

*Why it matters:* at an 8-second interval a one-hour lecture is ~450 frames. A lecturer writes maybe
30–50 distinct board states. Filtering locally is roughly an order-of-magnitude reduction in vision
calls, and it is the single biggest reason the running cost is small.

### 4.2 Per-chunk dominant-speaker heuristic

Given the diarized segments of **one** audio chunk:

- sum unique voiced duration per speaker label;
- mark a dominant label **only** if the leader has **≥ 58 %** of that chunk's voiced time **and** at
  least a **15 percentage-point margin** over the runner-up;
- otherwise report `AMBIGUOUS` or `UNAVAILABLE` rather than guessing;
- a manual override applies to that chunk only and expires at the next one.

The thresholds are tunable constants, and the honest position on what this does and does not mean is
in §6.

### 4.3 Append-only delta protocol with content-hash binding

Naively, each update would resend the whole growing note — cost grows quadratically over a lecture.
Instead:

- the app sends a **bounded context** (title, outline, recent Markdown) plus a **SHA-256 of the full
  current note**;
- the model returns only the **new text to append**;
- the server binds the response to that hash and to an expected revision number;
- Room applies it under **optimistic concurrency**: it commits only if the revision still matches,
  rejects blank erasure, and treats an identical replay as idempotent.

*Effect:* cost per update stays roughly flat instead of growing with note length, and two concurrent
updates cannot silently clobber each other.

### 4.4 Independent queues with different failure policies

Audio and frames are **not** treated the same:

- **Audio** → unbounded, strictly ordered queue. Speech is irreplaceable, so nothing is ever dropped.
- **Frames** → one-slot, drop-oldest queue. If a newer frame arrives while one is processing, the
  older one is discarded — the board has already moved on.

Getting this backwards would either lose speech or stall the pipeline behind stale images.

### 4.5 Syllabus relevance selection

A syllabus can be far larger than the context budget. The app scores bounded syllabus chunks by token
overlap against the *current* evidence (stop-worded, length-filtered) and forwards only the most
relevant excerpts — at most 6 from the app, capped again at 8 / 12,000 characters by the server.

Crucially the syllabus is labelled **context, never proof**. It cannot be used to claim something was
taught.

---

## 5. "Why this approach and not the alternatives?"

| Alternative | Why not |
| --- | --- |
| **Record and transcribe afterwards** (Otter, Google Recorder) | Audio only. Misses the board entirely, which is where the formulas and diagrams are. No structure during the lecture. |
| **Send every camera frame to the AI** | ~450 frames/hour instead of ~40. Roughly 10× the cost and latency for no extra information. |
| **On-device OCR only** (ML Kit) | Reads printed text acceptably but is weak on handwriting, gives no concept structure and no diagram regions. *Kept as an offline fallback when the network is down.* |
| **Whisper alone for audio** | No speaker labels. Speaker focus would be impossible. |
| **Put the API key in the app** | Extractable from the APK in a minute. Also would require porting all prompt and schema logic into Kotlin. |
| **Resend the whole note each update** | Cost grows with note length; a long lecture becomes expensive and slow. |
| **Let the AI correct the teacher directly** | Destroys evidence. If the AI is wrong, the student never knows what was actually said. |

### Model choices — and a real finding worth telling them

Models are selected per role and are swappable by configuration. Selection was by **measurement
against this project's own contracts**, not by price or popularity. One example is worth telling,
because it shows engineering judgement:

> My first choice for note generation handled a small test request in 0.7 seconds. When I measured it
> against a realistic note built up over a lecture, it timed out at 90 seconds — twice. Audio chunks
> arrive every 20 seconds, so that isn't a slow feature, it's a broken one. I switched to a model that
> does the same job in about 1.5 seconds and still preserves captured claims correctly.

*That is a strong review answer:* it shows you measure at realistic scale and treat latency as a
correctness property.

---

## 6. THE question: "Does it focus on the key person, or will noise be added?"

They will ask this. Answer it precisely — an over-claim here is very easy to catch.

**The honest answer, in order:**

1. **Yes, there is speaker focus — but it is per-chunk, not per-person.** Within each 20-second chunk,
   the system identifies the dominant speaker by voiced duration and prioritises that speaker's
   content in Runnable mode.

2. **It does *not* identify the teacher across the lecture.** Speaker labels like "A" and "B" come
   back independently for each chunk. "A" in chunk 5 is *not guaranteed* to be the same person as
   "A" in chunk 6. So the system deliberately never accumulates a persistent identity, because that
   claim would not be supported by the data it receives.

3. **When it isn't confident, it says so.** If no speaker crosses 58 % with a 15-point margin, it
   reports AMBIGUOUS rather than picking one. The student can override for the current chunk.

4. **Noise handling has three layers:**
   - Hardware: `VOICE_RECOGNITION` audio source with platform noise suppression and automatic gain
     control enabled when the device offers them.
   - Model: the transcription model handles moderate background noise; silent chunks return no
     segments and are discarded rather than hallucinated into text.
   - Content: in Runnable mode, unrelated audience chatter is de-prioritised — but questions and
     corrections from other speakers are kept when they clarify the lesson, because dropping them
     would lose real teaching content.

5. **What would be needed for true teacher identity:** voice embeddings with an enrolment step,
   cross-chunk clustering, and — importantly — **consent**, since that is biometric data. That is
   named as future work, deliberately not claimed now.

**Do not say:** "it learns the teacher's voice." It does not. Saying so and then being asked "how?"
is the fastest way to lose marks.

---

## 7. Validation status (by level of evidence)

Keep these straight. If asked "have you tested it?", answer with the level.

| Level | What it means | Examples |
| --- | --- | --- |
| **Automated** | Repeatable tests pass | 88 Android unit tests / 28 suites; 30 backend tests; 0 lint errors |
| **Device-verified** | Ran on the real phone (Android 16, Redmi) | Voice session 11.1 s; camera + audio session 32.3 s; both automated and repeatable |
| **Live-provider verified** | Real AI models, through the deployed proxy | All four endpoints; two-speaker clip correctly split A/B; equation read exactly; diagram crop matched a known rectangle closely |
| **Real-world trial** | An actual lecture video | One 2.5-minute algebra segment, 3 board states. Produced a full structured note — and exposed 4 defects, all since fixed |
| **Not yet measured** | Be explicit | Word error rate, diarization accuracy, OCR accuracy, diagram-crop IoU on a real corpus, battery, long-session endurance |

**If asked for accuracy percentages:** *"I haven't published one, because I don't yet have a fixed
labelled corpus and a scorer. Quoting a number from a handful of runs would be misleading. The
measurement plan is in my test plan document, and it's the next phase of work."*

That answer is stronger than a made-up figure.

### Cost — a real measured number you can quote

Roughly **$0.18 per lecture-hour** across transcription, board vision and note generation. The
backend also enforces a hard daily request budget and a per-caller rate limit, so cost cannot run
away.

---

## 8. The real-lecture trial — use it, don't hide it

You tested on a YouTube algebra lecture (2.5 minutes, 3 board states). It produced a complete
structured note **and** revealed four defects, all now fixed:

| Defect found | Fix |
| --- | --- |
| Note generation timed out at session scale | Measured candidates properly; switched to a model ~60× faster on the same task |
| Some formulas rendered as raw text in Obsidian | Server now normalises LaTeX delimiters to the forms Obsidian actually renders |
| Far too much content for a small concept | Added short / balanced / elaborate control plus a custom instruction box |
| A worked example appeared that was on neither board nor transcript | Prompt now forbids examples not present in the evidence |

**Say this out loud.** "I tested on real material, it broke in four places, here is each fix" is
exactly what a reviewer wants to hear at review 1. It demonstrates a working test-and-fix loop, which
is worth more than a demo that happens to go well.

---

## 9. Future plans (have 3 ready, ranked)

1. **Measured evaluation.** Build fixed corpora — noisy classroom audio, board/diagram images,
   labelled notes — plus a scorer, then publish word error rate, diarization error, OCR accuracy and
   crop IoU. *This is the honest next step and should be your first answer.*
2. **Consent-aware persistent speaker identity.** Voice embeddings with enrolment and cross-chunk
   clustering, so "the teacher" can be tracked across a whole lecture rather than per chunk.
3. **Full-note consolidation.** Today updates are append-only. A deliberate end-of-lecture pass that
   reorganises and de-duplicates the whole note would fix the repetition still visible in long
   sessions.

Further out, if they want more: nested folder browsing, PDF/DOCX syllabus import, whole-vault export,
long-session battery and storage profiling, and per-user sign-in so the backend token becomes
revocable per device.

---

## 10. Likely questions, with answers

**"Is this just a wrapper around ChatGPT?"**
No. The AI does transcription, reading the board, and writing prose. What I built is everything
around it: the on-device change filter that decides what is even worth sending, the chunking and
recovery pipeline, the append-only delta protocol with hash binding and optimistic concurrency, the
speaker heuristic, the evidence-preservation rules, and the local database and export. If you swapped
the AI provider tomorrow — which I have already done once — that work all stays.

**"What happens if the internet drops mid-lecture?"**
Nothing is lost. Every audio chunk is written to private storage *before* processing and is deleted
only after both its transcript and the note update have committed. If a chunk can't be transcribed,
the WAV stays on the phone, the note records a visible warning, and the setup screen lists the
retained file with Recover and Delete buttons. Board capture additionally falls back to on-device OCR.

**"What if the AI gets something wrong?"**
That's the central design question. The rule is: **the AI never silently rewrites captured evidence.**
If it disagrees with what was said or written, it must return a separate correction record with the
original claim, the suggestion and the reason — shown to the student as a suggestion to accept, not an
applied edit. At the end of the session a second, *different* model checks the formulas, units and
concepts and adds its findings the same way.

**"Why a different model for the checking step?"**
Because a model reviewing its own output is biased toward approving it. In my first real session the
checker was the same model that wrote the notes and reported no problems on a note that did contain
errors. Using a different model removes that self-consistency bias.

**"How do you know the board frame filter actually works?"**
It's unit-tested on synthetic frames: identical frames rejected, localised writing accepted, and — the
important one — a *global brightness change* is rejected, which is what the exposure compensation is
for. What I haven't done yet is measure precision and recall on a real labelled corpus.

**"Is the data private?"**
Notes, audio, images and syllabi are stored in app-private storage; Android backup is disabled for
them. Frames that don't change never leave the phone. What is sent for processing goes over HTTPS to
my own authenticated server, which forwards it in memory without writing it to disk. I should be clear
that the AI provider does process what is sent — I don't claim zero retention on their side. And
before recording a class, obtaining consent is the user's responsibility.

**"Why Android and not a web app?"**
It needs sustained microphone and camera access, foreground-service-style lifecycle handling, and
local-first storage. That's native territory. It's also where the student already is during a lecture.

**"How much does it cost to run?"**
About $0.18 per lecture-hour measured, on a free hosting tier. There's a hard daily request cap and a
per-caller rate limit in the backend so it can't run away.

**"What was the hardest part?"**
Honestly, the failure paths rather than the happy path. Making sure a network failure, a provider
outage or a model returning malformed output never loses captured speech — that's where most of the
design went: recovery files, two-phase baseline commits, optimistic revision control, typed error
classification.

---

## 11. Demo script (~3 minutes)

Have the app **already installed and warmed up** — hit the health URL a minute before, or the free
tier will take up to a minute to wake and it will look like a hang.

1. **Live tab.** Point out the three numbered steps: mode, destination, style. Show the new detail
   control — *short and precise* vs *elaborate*.
2. **Pick Live board.** Show the camera interval and change-sensitivity sliders. Say the line: *"this
   is where the cost saving happens — similar frames never leave the phone."*
3. **Start.** Talk over a slide or a board for ~40 seconds. Point at the live counters: accepted vs
   skipped frames — **skipped is the number to draw attention to**.
4. **Stop.** Show the structured note appearing, the review flags, and the end-of-session check.
5. **Notes tab.** Open the saved note, show the export.

**If the demo fails:** don't panic or fake it. Say *"the hosted tier is cold, here's the note from my
recorded run"* and show the exported Markdown. Reviewers mind a bluff far more than a cold start.

---

## 12. Numbers worth memorising

| Number | What it is |
| --- | --- |
| 20 s | Audio chunk length |
| 2–30 s | Camera interval range (8 s default) |
| 32×32 | Luminance grid for change detection |
| 58 % / 15 pts | Dominant-speaker thresholds |
| ~$0.18 | Measured cost per lecture-hour |
| 88 / 30 | Android unit tests / backend tests, all passing |
| 4 | Defects found by the real-lecture trial, all fixed |

---

## 13. Three things not to say

1. ❌ *"It learns the teacher's voice."* → It's per-chunk dominance, not identity. §6.
2. ❌ *"It's 95 % accurate."* → No corpus, no scorer, no published number yet. §7.
3. ❌ *"The backend is secure."* → The client token is compiled into the APK and is extractable. Say
   the key is protected, the endpoint requires a token, and a hard spend cap is the real backstop.

If you're unsure of a number under pressure, say **"I'd have to check my test plan for the exact
figure"**. That is a perfectly good answer and costs you nothing.

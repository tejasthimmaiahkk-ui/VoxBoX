# VoxBox — Project Status

**Project title:** Structured Voice to Notes using AI
**Application name:** VoxBox
**Last updated:** 2026-08-12
**Purpose of this file:** a single honest record of what exists, what does not, and what is
believed to work but has never been proven. Keep it current — it is the input to the report's
future-work chapter, and re-reading it is the cheapest bug-finding exercise available.

---

## 1. Current state at a glance

| Measure | Value |
| --- | --- |
| Application source | 13,337 lines, 74 Kotlin files |
| Test source | 2,574 lines, 32 files |
| Backend source and tests | 2,939 lines |
| Android unit tests | 134 passing |
| Backend tests | 43 passing |
| Instrumentation tests | 3 passing |
| Lint | clean |
| Deployed backend | live, verified against `/health` |
| Field trials | 4 |
| Report | 384 pages — 200 narrative + 184 source appendices |
| Report diagrams | 21, all generated from the system they describe (`tools/diagrams/`) |
| Report screenshots | 21, captured from the physical device |

---

## 2. Done and verified on a device

These have been exercised on the physical phone, not merely compiled.

- Onboarding, all four destinations, all six settings sub-pages.
- Accent switching, persisted through DataStore across navigation.
- Live storage breakdown and live `/health` model routing on the Connection screen.
- Real capture session: speech transcribed with timestamp and speaker label.
- Markdown rendering — the same note that previously showed raw `##` now renders.
- Live board session with manual capture, tuning panel and a correctly positioned zoom thumb.
- Adaptive tuning visibly changing interval and threshold from measured speech pace.
- Folder creation dialog.
- Release build reaching the hosted proxy over HTTPS.
- **Field trial 4, 12 Aug 2026 — the settle-gate fix confirmed.** A 92-second instrumented board
  session accepted 3 frames and skipped 9, reaching revision 5. These are the first board frames
  the system has ever accepted in the field; the three earlier trials accepted none. Captured as
  `docs/screenshots/16-live-running.png` and reproduced as Figure S.8 in the report.

## 3. Done, tested, but never seen working in the field

Compiles and has unit tests, but no field session has confirmed the behaviour end to end.
**This is the list most likely to contain a hidden bug.**

| Item | Why it is unconfirmed |
| --- | --- |
| Diagram placement beside its text | Field trial 4 produced the first accepted frames, but the room was silent so no composition ran against them. The inline placement path still has not executed on real content. |
| Diagram duplicate rejection by fingerprint | Same reason |
| Transcript overlap de-duplication on real speech | Unit-tested against synthetic seams only; never checked against a lecture transcript |
| Grounding guard on a real session | Tested against a scripted provider; never triggered by a live model |
| End-of-session verification findings appearing in the reader | The pass runs and returns findings (seen in the field log), but the findings have not been observed rendered in the note |
| Review-flag accept and annotate actions | No field session has produced a flag to press |
| Export share sheet on a real note with assets | Wired and compiles; not exercised with a populated note |
| Rename, delete, move-to-folder | Dialogs confirmed to open; the operations themselves not confirmed against real data |
| Audio recovery from a retained WAV | The retention path has fired in the field; the recovery button has not been pressed |

## 4. Not implemented

| Item | Notes |
| --- | --- |
| Accuracy measurement | No labelled corpus, no scorer. No accuracy figure is claimed anywhere. |
| Diagram type or structure understanding | No arrow, chart or ER recognition. The system crops regions a model identifies and does not interpret them. |
| On-device diagram pre-filter | Nothing rejects a crop of a wall or a face before the call is spent. |
| Consent-aware speaker identity | Requires enrolment and stored voiceprints; deliberately out of current scope. |
| Full-note consolidation pass | Updates are append-only by design; a consolidation step does not exist. |
| Note refinement batching | Largest remaining cost lever. Untouched because it crosses the retained-audio recovery path. |
| PDF / DOCX syllabus import | Markdown and plain text only. |
| Offline queueing | A total loss of connectivity is handled by retention and warnings, not by deferred upload. |

## 5. Known defects and risks

| # | Issue | Severity | Notes |
| --- | --- | --- | --- |
| 1 | Temporary debug log still present | Must-fix before distribution | Removal checklist in `docs/TEMPORARY_DEBUG_LOG.md` |
| 2 | Client token is in the APK | Accepted, bounded | Recoverable by anyone holding the APK; bounded by rate limit and daily budget. Rotate after any demo. |
| 3 | Release build is signed with the debug keystore | Must-fix before distribution | Demo-only arrangement |
| 4 | `00:80`-style invalid timestamps observed in a note | Open | Our formatter provably cannot emit this, so the model is inventing timestamps. Prompt now forbids it; unverified. |
| 5 | Transcription returns one large segment per chunk | Open | Every field chunk returned `segments=1` of 250–400 characters, so timestamps are coarse and speaker changes within a chunk are invisible |
| 6 | Free-tier cold start | Accepted | First request after idle can take ~13 s |
| 7 | No lint rule against negative Compose padding | Open | The same runtime-crashing mistake was made twice in one session |

## 6. Standing lessons

Recorded because each was learned by getting it wrong, and each is cheap to repeat.

1. **A commit is not a release.** Three reported defects had one cause: the fix was never pushed.
   Always confirm `/health` reports the expected models before calling a server fix done.
2. **A compiled control is not a reachable one.** Rename and delete compiled, passed tests, and
   did nothing, because the dialogs were rendered outside the composable.
3. **Benchmark on realistic inputs.** A model chosen on a 0.7 s toy request timed out at 90 s on a
   grown note.
4. **Measure request size before blaming output.** The cost was input, not verbosity.
5. **A prompt instruction is a request; a validator is a guarantee.** The model invented a section
   despite being told not to.
6. **Instrument the running system.** The settle-gate defect was invisible to every test and
   obvious within seconds of reading a timestamped log.

## 7. Next actions, in order

1. ~~Run one instrumented field session and confirm the settle-gate fix lands captures.~~
   **Done, 12 Aug 2026: 3 kept, 9 skipped in 92 s.** The room was silent, so this proved the
   capture path and not the composition path.
2. Run the same session **with speech in the room**, so an accepted frame actually reaches
   extraction and composition. This is what unblocks the rest of section 3.
3. Verify diagram placement and duplicate rejection from that session.
4. Rotate the client token.
5. Build the labelled corpus and scorer (section 4, item 1).
6. Remove the temporary debug log once field diagnosis is complete.
7. Replace the debug signing configuration before any distribution.
8. Update chapter 16 of the report (Future Implementation) as items above are completed —
   the chapter is written to be revised, and `tools/report-generator/content3.js` holds it.

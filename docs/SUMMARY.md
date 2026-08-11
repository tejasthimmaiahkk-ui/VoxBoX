# VoxBox — One-Page Summary

**Structured Voice to Notes using AI** · application name **VoxBox**

## What it is

An Android application that turns a live lecture into a structured Markdown note while the class
is happening. It listens continuously and, in board mode, watches the board through the camera.
The note is written incrementally, so when the session ends the note is already finished.

## The one idea that shapes everything

**Evidence preservation.** The raw transcript and board text are written first and kept exactly as
captured. Nothing the AI produces overwrites them. Where the model disagrees with the evidence, the
disagreement is shown to the student as a review flag with both readings, and neither available
action rewrites the captured line.

## How it works

```
Microphone --> ordered audio queue  --> transcription  --+
20 s chunks     never drops             + speaker        |
2 s overlap                                              +--> append-only note builder
                                                         |    SHA-256 bound, revision checked
Camera     --> 32x32 change filter  --> board extraction-+
every 2-30 s    ~450/hr becomes ~40     text, equations,
adaptive        rest deleted on phone   concepts, crops
```

The phone talks only to an authenticated HTTPS proxy, which holds the AI provider credential so it
never ships inside the APK.

## What is this project's own work

Six algorithms, none of which changed when the AI provider was migrated during development:

1. **On-device frame-change detection** — 32×32 luminance grid, exposure compensated, with a
   *relative* settle test so a person crossing the board never costs a call.
2. **Adaptive sampling** — words per minute measured from the transcript drives interval and
   sensitivity together, anchored to the session baseline so adjustments cannot compound.
3. **Per-chunk dominant speaker** — ≥58% of voiced time with a ≥15-point margin, otherwise
   AMBIGUOUS. No identity claim, no voiceprint.
4. **Append-only delta protocol** — bounded context plus a content hash; the model returns only new
   text; committed only if the revision still matches. Cost stays flat as the note grows.
5. **Transcript overlap de-duplication** — chunks overlap by two seconds so no word is only ever
   seen as a fragment; the repeat is trimmed on a word basis with a three-word minimum.
6. **Grounding guard** — generated text is scored against the evidence it claims to summarise and
   discarded if it does not match, because a prompt instruction is a request, not a guarantee.

## State

| | |
| --- | --- |
| Source | 13,337 lines Kotlin · 74 files |
| Tests | 134 Android · 43 backend · 3 device · lint clean |
| Backend | deployed and live |
| Field trials | 4 |
| Report | 223 pages |

## What is honestly not done

- **Accuracy is unmeasured.** No labelled corpus, no scorer, no percentage claimed anywhere.
- **No diagram interpretation.** The system crops regions a vision model identifies; it does not
  understand arrows, charts or entity-relationship structure.
- **Several features are tested but unproven in the field** — see `PROJECT_STATUS.md` section 3,
  which is the most likely place for a hidden bug.

## Where to look

| File | Contents |
| --- | --- |
| `docs/PROJECT_STATUS.md` | Full status, known defects, standing lessons, next actions |
| `docs/REVIEW_1_HANDBOOK.md` | Review preparation and anticipated questions |
| `docs/TEMPORARY_DEBUG_LOG.md` | Removal checklist for the diagnostic log |
| `server/OPERATIONS.md` | Token and key rotation, error codes, rollback |
| `outputs/VoxBox_Project_Report.docx` | The 223-page project report |

# VoxBox Project Guide

## Approved working title

**VoxBox: A Hybrid Voice-Command and AI-Assisted System for Creating Structured Visual Notes on Android**

Short presentation title: **VoxBox — Voice-to-Structured Visual Notes on Android**

## Project summary

VoxBox is an Android note-taking application that turns speech into editable, structured note blocks. Ordinary dictation creates flat text; VoxBox adds a deterministic voice-command language named **VoxScript** so the user can speak headings, bullets, Roman-number lists, checklists, dividers, highlights, charts and bounded diagrams.

The first working system does not require an AI API key. Android's native speech recognition provides the transcript and VoxScript converts recognized phrases into predictable blocks. A later, optional AI provider will summarize, title, tag, rewrite or reorganize a note according to user instructions. AI output must be previewed and must not silently replace the original note.

## Why this is a strong high-credit project

- **Demonstration depth:** speech, parsing, visual rendering, local persistence, organization and optional AI form one visible end-to-end flow.
- **Research depth:** speech word error rate, command intent/slot accuracy, latency, persistence integrity and later AI faithfulness can be measured.
- **Engineering depth:** Android permission handling, speech lifecycle, Compose UI, Room, block rendering, parser design, testing and privacy are all defensible in a viva.
- **Feasible scope:** the offline deterministic core is independent of network/API availability. Charts are limited to pie, bar and progress; diagrams are limited to flowcharts and relationship maps during the two-month project.
- **Zero-budget baseline:** Android SDK, Jetpack libraries and local development tools are sufficient for the core.

## Marks-first strategy

| Assessment area | Planned evidence |
| --- | --- |
| Demonstration — 60 | Physical-device flow: speak, structure, edit, save, relaunch, search and reopen a visual note |
| Writing — 20 | Clear problem, objectives, method, architecture, algorithm, evaluation and limitations |
| Viva — 20 | Speech lifecycle, parser rules, block model, offline/online behavior, privacy, tests and trade-offs |
| Report — 20 | Continuously maintained report with screenshots, diagrams, results, citations and test evidence |
| Three reviews — 30 | Increasing verified evidence rather than repeating the same proposal |

## Core user story

1. The user opens or creates a note.
2. The user starts a visible, bounded speech session.
3. Android returns partial and final transcript results.
4. VoxScript classifies plain dictation or a command and extracts its values.
5. VoxBox renders an editable typed block.
6. The note is saved locally and can be organized, searched and reopened.
7. When an AI provider is configured and enabled, the user may preview a summary or requested reorganization.

Example command:

> Tejas, pie chart, 25 percent, yellow, label wheat.

Expected block: a pie chart with a yellow 25% wheat section and a white 75% remainder. The value, label and colors remain editable.

## Version 1 scope

### Must have

- Compose note library and block editor.
- Local Room persistence for notes, blocks and user rules.
- Runtime microphone permission flow with denial handling.
- Native `SpeechRecognizer` integration with on-device recognition preferred when available.
- Push-to-talk or bounded speech sessions with partial/final transcript feedback.
- Customizable wake word, command aliases, default colors and formatting presets.
- VoxScript commands for headings, paragraphs, bullet/numbered/Roman lists, checklists, divider, quote, code, highlights and hierarchy controls.
- Native pie, bar and progress chart blocks.
- Bounded flowchart and relationship-map blocks.
- Undo last command, typed correction and plain dictation fallback.
- Search, notebooks/folders and tags.
- Automated tests plus physical-device verification.

### Optional after the offline core is stable

- AI-generated summary, title and tags.
- User-defined AI formatting/reorganization instruction.
- Preview/diff before applying AI output.
- Provider abstraction so the AI service can change without rewriting the note model.

### Explicitly out of scope for the two-month release

- Always-on background microphone.
- Fully arbitrary diagrams or generated artwork.
- Multi-user real-time collaboration.
- Cross-device cloud synchronization.
- Training a new speech foundation model.
- Silent AI rewriting or automatic deletion of the original note.

## Planned architecture

```text
Compose screens
    ↓ immutable UI state / user actions
ViewModels
    ↓
Use cases
    ├── SpeechGateway → Android SpeechRecognizer
    ├── VoxScriptParser → ParsedCommand / PlainDictation
    ├── BlockRenderer → text, list, chart, diagram blocks
    ├── NoteRepository → Room database
    └── NoteEnhancementProvider → optional AI implementation
```

The UI follows unidirectional data flow. Speech, parsing, persistence and AI are isolated behind interfaces so each can be tested or replaced independently.

## Planned data model

- `Note`: identity, title, notebook, timestamps and ordering metadata.
- `NoteBlock`: identity, note, block type, position and typed payload.
- `BlockStyle`: hierarchy, list style, color and emphasis.
- `ChartPayload`: chart type, values, labels and colors.
- `DiagramPayload`: nodes, edges and direction.
- `VoiceRule`: wake word, aliases, defaults and enable/disable state.
- `AiEnhancement`: input version, instruction, preview, provider metadata and acceptance state.

## Evaluation plan

- Speech word error rate on a versioned phrase corpus in quiet and noisy conditions.
- VoxScript intent accuracy and slot accuracy.
- Rejection/ambiguity cases for incomplete or unsafe commands.
- End-to-end speech-result-to-rendered-block latency.
- Note save/reload and app-relaunch integrity.
- Physical-device stability and permission-state behavior.
- Later AI comparison: raw transcript versus VoxScript versus AI-enhanced note, scored for faithfulness, coverage, instruction compliance, hallucination and user acceptance.

No unmeasured accuracy claim is allowed in the PPT, report or viva.

## Two-verification rule

A completed task should normally pass both:

1. **Automated evidence:** build/lint/test, parser corpus, repository tests or instrumentation as appropriate.
2. **Independent runtime evidence:** physical-device flow, persisted-data recovery, visual inspection or a second targeted test.

If a verification cannot run, `PROJECT_LOG.md` must record the exact reason. Do not describe a blocked check as passed.

## Eight-week delivery plan

| Week | Main deliverable | Review/report evidence |
| --- | --- | --- |
| 1 | Scope, Review-1 PPT, guide, log, report structure, clean baseline | Approved problem, objectives, feasibility and sources |
| 2 | Block model, Room foundation, library/editor shell | Data model, architecture and persistence tests |
| 3 | Permission flow and native speech gateway | Speech lifecycle, error states and device evidence |
| 4 | VoxScript parser and structured text/list blocks | Grammar, intent/slot corpus and parser metrics |
| 5 | Charts, flow diagrams, customization and editing | Visual demo, renderer tests and Review-2 evidence |
| 6 | Organization/search; optional AI provider foundation | AI/privacy method and comparison protocol |
| 7 | Integration, physical-device QA, accessibility and performance | Results, screenshots, limitations and Review-3 deck |
| 8 | Bug fixing, final report, PPT, viva and rehearsed fallback demo | Submission package and final verification matrix |

## Required project records

- `PROJECT_GUIDE.md`: current agreed design and implementation map.
- `PROJECT_LOG.md`: append-only record of completed work, changes, verification and blockers.
- `docs/VOXSCRIPT_SPEC.md`: versioned command grammar and examples.
- `docs/TEST_PLAN.md`: planned automated and device tests.
- `docs/REPORT_DRAFT.md`: report written alongside implementation.
- `docs/VIVA_NOTES.md`: defensible design decisions and short answers.
- `outputs/VoxBox_Review_Meeting_1.pptx`: initial review presentation.

## Current implementation baseline — 2026-07-19

Implemented and verified:

- Native Android `SpeechRecognizer` controller with lifecycle cleanup.
- On-device recognizer preference and automatic system-recognizer fallback when the on-device language is unavailable.
- System fallback does not force the offline preference and clearly discloses that recognition may use network service.
- Runtime microphone permission request plus permission-denied and granted UI states.
- Partial/final transcript callbacks, stop/cancel actions and mapped speech errors.
- Compose capture screen with status, transcript, accessible microphone control and dark-theme verification.
- Deterministic parser support for plain dictation, heading, bullet point and pie-chart commands.
- Editable-preview concept demonstrated with the requested 25% yellow `wheat` chart.
- Seven parser unit tests covering positive and invalid cases.

Not implemented yet:

- Persistent notes, block editing, Room, search or organization.
- Remaining VoxScript intents, full chart set and diagrams.
- AI provider, summarization or custom AI instruction.
- Measured speech word error rate or spoken-command accuracy corpus.

## Source-control policy

- The repository root is `D:\College Project` so documentation, the Android application and review artifacts stay synchronized.
- Commit only successful, scoped milestones.
- Never commit API keys, keystores, `local.properties`, build folders or generated APK/AAB files.
- Preserve the original note/transcript and do not design any AI feature that silently overwrites user work.

## Primary technical sources

- [Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer)
- [Android RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent)
- [Request runtime permissions](https://developer.android.com/training/permissions/requesting)
- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)

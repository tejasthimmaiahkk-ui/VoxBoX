# VoxBox Test Plan

Status: Initial plan  
Date: 2026-07-19

## Verification policy

Each runnable milestone uses two relevant forms of evidence: an automated check and an independent device/visual/runtime check. A blocked check is logged as blocked, never passed.

## Automated test layers

### Parser unit tests

- Recognize every supported intent and alias.
- Extract chart values, labels and colors.
- Distinguish commands from plain dictation.
- Reject invalid ranges and missing required slots.
- Return ambiguity rather than guessing.
- Preserve source text for correction and audit.

### Domain tests

- Convert parsed commands into the correct typed block.
- Undo only the most recent reversible operation.
- Keep stable block ordering after insert, move and delete.
- Round-trip chart/diagram payload serialization.

### Repository/database tests

- Create, update, delete and reload a note.
- Preserve block order and payloads across app relaunch.
- Search titles, content and tags.
- Verify migrations when the schema version changes.

### ViewModel/UI tests

- Permission states: not requested, granted, denied and permanently denied.
- Speech states: idle, listening, partial result, processing, success and error.
- Editor updates from dictation and commands.
- AI preview never applies without explicit acceptance.

### Build checks

- Unit tests.
- Android lint.
- Debug APK assembly.
- Instrumentation/Compose tests where applicable.

## Physical-device matrix

| Area | Cases |
| --- | --- |
| Permission | Allow, deny, deny twice/settings recovery |
| Speech lifecycle | Start, stop, cancel, error, rotate/background/foreground where supported |
| Environment | Quiet room and moderate background noise |
| Commands | Heading, list, highlight, chart, diagram, undo and save |
| Persistence | Force-close/relaunch and reopen saved note |
| Visual | Small/large note, dark/light theme, chart labels and diagram readability |
| Demo | Cold-start timed rehearsal and offline/network-failure fallback |

## Evaluation datasets

### Speech phrase corpus

- Versioned text file containing scripted dictation and command phrases.
- Record at least two environmental conditions.
- Keep expected transcript and actual recognizer transcript separate.
- Compute word error rate only after the corpus protocol is fixed.

### Command corpus

- Positive examples for each intent.
- Alias and phrasing variations.
- Missing-slot and out-of-range examples.
- Phrases intentionally treated as plain dictation.
- Confusable commands for error analysis.

## Planned metrics

- Word error rate.
- Intent accuracy.
- Slot precision/recall or exact slot accuracy.
- End-to-end structure accuracy.
- Speech-result-to-block-render latency.
- Persistence/reload pass rate.
- Crash-free scripted demo repetitions.
- Later AI: faithfulness, coverage, instruction compliance, hallucination and acceptance.

## Current verification status

- Review-1 PPT export: passed.
- Review-1 PPT overflow check: passed.
- Review-1 PPT full-size visual inspection: passed after two defects were corrected.
- Android starter build and unit test: passed after pinning SDK-compatible AndroidX versions.
- Physical-device baseline: passed on `2411DRN47I`, Android 16 / API 36.
- Baseline install/launch and `Hello Android!` UI hierarchy: passed; evidence is in `evidence/baseline/`.
- Native speech/VoxScript build: passed.
- Parser unit tests: seven total tests passed in the milestone test task, including six VoxScript cases.
- Permission denied/granted UI: passed on the physical device.
- On-device-language failure to system-recognizer fallback: passed on the physical device.
- Active system-recognizer `Listening…`, stop and cancel UI: passed on the physical device.
- Requested 25% yellow wheat chart preview: passed visually and through UI hierarchy.
- Human-spoken phrase transcription and word error rate: pending; automated ADB control did not supply microphone speech.
- Room persistence, full editor, organization and remaining command tests: pending.

## 2026-07-19/20 persistence-foundation evidence

- Room schema generation, unit tests, debug APK assembly and Android lint passed with `gradlew.bat testDebugUnitTest assembleDebug lintDebug` (53 actionable tasks; 22 executed).
- The unit suite now includes four mapping cases: dictation, heading, pie-chart editable slots and rejection of invalid commands.
- Repeated `gradlew.bat testDebugUnitTest assembleDebug --console=plain` on 2026-07-20: **BUILD SUCCESSFUL**.
- Physical-device persistence/relaunch verification passed on `2411DRN47I`, Android 16 / API 36. A newly created `Voice note 1` accepted the deterministic yellow 25% wheat pie-chart preview; the UI reported `pie chart saved locally.` before force-stop and still reported `1 saved note` / `Voice note 1` after relaunch.
- The device-private `voxbox-notes.db`, WAL and SHM files were present after the flow. Detailed reproduction evidence: `evidence/persistence-milestone/README.md`.
- Note-detail block rendering was not implemented in the persistence-foundation milestone, so that milestone's UI claim was deliberately limited to the stored-note shell and save status.

## 2026-07-21 note-detail reopening evidence

- `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain` passed: unit tests, fresh debug APK assembly and lint all completed successfully.
- Two local unit tests verify that a persisted pie chart reopens with its typed percentage/color/label slots and that an out-of-range malformed chart is not rendered as another block kind.
- On `2411DRN47I` (Android 16 / API 36), `Voice note 5` saved the deterministic yellow 25% wheat chart, survived force-stop/relaunch, and then reopened through the new `Open` action. The detail UI rendered `Wheat`, `25%`, and `yellow • remainder white` from the saved Room row.
- Detailed command/device reproduction: `evidence/note-detail-milestone/README.md`.
- Editing, deletion and reordering of stored blocks remain pending and are not represented as passed cases.

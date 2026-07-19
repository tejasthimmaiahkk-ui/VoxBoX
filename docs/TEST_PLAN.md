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
- Feature tests: pending because feature implementation has not started.

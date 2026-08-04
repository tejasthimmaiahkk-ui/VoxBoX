# VoxScript Specification

Status: Draft 0.6 — optional/legacy deterministic layer
Date: 2026-08-03

## Purpose

VoxScript is VoxBox's deterministic, customizable voice-command language. It converts recognized text into typed note operations without requiring a generative AI service.

The refined VoxBox product no longer expects a teacher or other source speaker to dictate formatting commands during an ordinary lecture. The primary workflow is a foreground continuous Voice or Live board session that turns natural speech and selected frame evidence into revisioned Markdown. VoxScript remains useful for:

- an exact user-authored formatting command;
- a predictable offline/low-cost shortcut;
- accessibility and manual correction;
- deterministic demonstrations and parser research; and
- compatibility with the verified bounded-speech baseline.

It is therefore an optional/legacy input layer, not the main continuous-note algorithm.

## Design principles

- Plain dictation must stay possible.
- Commands must be easy to learn, repeat and unit-test.
- The parser must prefer rejection or clarification over destructive guessing.
- Parsed values remain editable after rendering.
- Wake words and aliases are user-configurable.
- Parser output is independent of Android UI and persistence code.

## Conceptual grammar

```text
utterance      = command | dictation
command        = wake_word action arguments? controls?
wake_word      = configured alias
action         = text_action | list_action | style_action | visual_action | session_action
arguments      = free_text | key_value+
controls       = confirmation | placement | scope
```

Default development wake word: `Vox`. Demonstration alias: `Tejas`.

## Planned parser result

```text
ParseResult
├── PlainDictation(text)
├── Command(intent, slots, confidence, sourceText)
├── Ambiguous(candidates, missingSlots, sourceText)
└── Rejected(reason, sourceText)
```

## Initial intent catalog

| Category | Intent | Example |
| --- | --- | --- |
| Text | `CREATE_HEADING` | “Vox heading Photosynthesis” |
| Text | `CREATE_PARAGRAPH` | “Vox paragraph Plants convert light into energy” |
| List | `START_BULLET_LIST` | “Vox bullet point Light intensity” |
| List | `START_NUMBERED_LIST` | “Vox numbered point Collect the sample” |
| List | `START_ROMAN_LIST` | “Vox Roman point Introduction” |
| List | `CREATE_CHECKLIST_ITEM` | “Vox checklist Submit assignment” |
| Style | `HIGHLIGHT` | “Vox highlight yellow chlorophyll” |
| Structure | `CREATE_DIVIDER` | “Vox divider” |
| Visual | `CREATE_PIE_CHART` | “Tejas pie chart 25 percent yellow label wheat” |
| Visual | `CREATE_BAR_CHART` | “Vox bar chart maths 80 science 65” |
| Visual | `CREATE_PROGRESS` | “Vox progress chart 60 percent label syllabus” |
| Diagram | `CREATE_FLOW` | “Vox flow seed to plant to flower” |
| Diagram | `CREATE_RELATIONSHIP_MAP` | “Vox relationship cell contains nucleus and cytoplasm” |
| Control | `UNDO_LAST` | “Vox undo last command” |
| Control | `END_LIST` | “Vox end list” |
| Control | `NEW_SECTION` | “Vox new section Results” |
| Control | `SAVE_NOTE` | “Vox save note” |

## Implemented subset in Draft 0.2

- Wake words: `Vox`, `Tejas`, `Note`.
- Plain speech without a wake word returns `PlainDictation`.
- `heading` / `title` returns `Heading`.
- `bullet point` / `bullet` returns `BulletPoint`; the longest alias is matched first.
- `pie chart` extracts integer percentage, supported named color and text after `label` or `tag`.
- Pie values outside 0–100 and missing color/label return `InvalidCommand`.
- Unknown wake-word commands return `InvalidCommand` rather than being treated as dictation.

All other catalog entries remain planned and must not be described as implemented.

## Persistence integration in Draft 0.3

The current Android baseline maps accepted parser results to local typed blocks before saving:

- plain dictation → `PARAGRAPH`;
- heading → `HEADING`;
- bullet point → `BULLET_POINT`;
- pie chart → `PIE_CHART`, preserving its percentage, color and label as separate editable fields.

`InvalidCommand` has no persistence mapping, so an incomplete command cannot create a saved block. The initial Room schema assigns each block a stable position within one local note. The library can reopen a saved note and renders the implemented paragraph, heading, bullet-point and pie-chart kinds in ascending stored position. Reopened paragraphs, headings and bullets support non-blank text edits; reopened pie charts support validated whole-number percentage (0-100), named color and non-blank label edits. An update preserves the row identity, block kind and position, and updates the parent note timestamp only when that row exists. Pie-chart reopening and editing use the separately stored percentage, color and label slots; malformed visual rows are disclosed instead of guessed. On-device verification has confirmed typed-slot reopening; the physical edit/relaunch verification remains pending because no device was connected or authorized during the 2026-07-28 recheck.

## Pie-chart slots

Required:

- `value`: integer or decimal from 0 to 100.
- `label`: non-blank user text.

Optional with defaults:

- `color`: named or configured color; default from user preferences.
- `remainingColor`: default white.
- `title`: default inferred from surrounding section or `Chart`.

Example normalized result:

```json
{
  "intent": "CREATE_PIE_CHART",
  "slots": {
    "value": 25,
    "label": "wheat",
    "color": "yellow",
    "remainingColor": "white"
  }
}
```

## Ambiguity rules

- A chart value outside 0–100 is rejected.
- A pie chart without a value or label requests correction/clarification.
- Unknown colors fall back only if the user has enabled a default-color rule; otherwise the command is ambiguous.
- Destructive actions such as deleting a note require explicit confirmation.
- An utterance without the wake word is plain dictation unless the user has enabled command-only mode.
- `Undo last command` affects only the last reversible edit in the active note.

## Customization model

The user may configure:

- wake words;
- action aliases, such as `topic` → `heading`;
- default chart and highlight colors;
- preferred numbered-list style;
- whether the parser asks for confirmation on ambiguous commands;
- plain-dictation versus command-first mode.

## Versioning policy

- Every grammar change increments this document's draft version.
- Every supported intent requires positive, negative and ambiguous test phrases.
- Existing phrases must not change behavior silently; deliberate breaking changes require a migration note in `PROJECT_LOG.md`.

## 2026-07-30 implementation verification note

No VoxScript grammar or parsing behavior changed. The typed editing regression reran successfully with 16 passing unit tests and lint reporting 0 errors; the physical editing verification is still blocked because Android Debug Bridge reported no connected or authorized target. This is a device-evidence blocker, not a grammar validation result.

## Continuous-session integration boundary — 2026-08-03

The new continuous session pipeline has two independent note policies:

- `RUNNABLE`: incremental AI-assisted Markdown derived from transcript/frame evidence; and
- `VERBATIM`: local timestamped evidence formatting without semantic summarization.

Neither policy routes ordinary teacher speech through VoxScript. This prevents phrases such as “new section” or “delete that” in a lecture from being mistaken for destructive note commands. A future integration must require an explicit user command channel/state before applying VoxScript to a continuous session.

Rules for any future integration:

- Keep the original transcript segment and timestamps even when a command is accepted.
- Never parse model-generated Markdown back through VoxScript.
- Do not let syllabus text trigger commands.
- Require confirmation for destructive commands.
- Keep deterministic command edits separate from AI correction suggestions in provenance.
- Apply edits against the current Markdown revision so a stale command cannot overwrite a newer note.

The continuous multimodal MVP did not add new VoxScript intents. The full Android JVM suite now contains 70 passing tests across 23 suites, but this count is an integrated project result rather than a new grammar-accuracy measurement. The previously implemented plain dictation, heading, bullet and pie-chart subset remains the verified parser baseline. Remaining intents in this specification are planned until their own focused tests and UI/device evidence exist.

# VoxScript Specification

Status: Draft 0.4
Date: 2026-07-21

## Purpose

VoxScript is VoxBox's deterministic, customizable voice-command language. It converts recognized text into typed note operations without requiring a generative AI service.

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

`InvalidCommand` has no persistence mapping, so an incomplete command cannot create a saved block. The initial Room schema assigns each block a stable position within one local note. The library can reopen a saved note and renders the implemented paragraph, heading, bullet-point and pie-chart kinds read-only in ascending stored position. Pie-chart reopening uses the separately stored percentage, color and label slots; malformed visual rows are disclosed instead of guessed. On-device verification has confirmed a saved pie-chart block survives relaunch and reopens with those slots rendered. Full typed editing remains planned.

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

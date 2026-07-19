# VoxBox Viva Notes

## One-sentence answer

VoxBox is an Android app that converts speech into editable structured visual note blocks through a deterministic command language, with optional AI summarization and organization.

## What is the novelty?

The core novelty is the combination of ordinary dictation with a customizable, testable voice-command language that creates structured text, editable charts and bounded diagrams directly during capture. AI is separated as an optional semantic enhancement rather than being required for every action.

## Why not use only AI?

Deterministic commands are predictable, fast to test, usable without a project API key and suitable for exact operations such as “create a 25% yellow pie slice.” AI is useful for semantic summarization and reorganization but can be unavailable, costly or unfaithful. VoxBox uses each approach for the task it handles best.

## Why not continuous speech recognition?

Android documents that `SpeechRecognizer` is not intended for continuous recognition because of battery and bandwidth implications. VoxBox therefore uses visible push-to-talk or bounded sessions and destroys recognizer resources correctly.

## Is the native recognizer always offline?

No. VoxBox can prefer on-device recognition when available and can request offline behavior, but Android notes that a recognizer implementation may ignore the offline preference. The application must disclose the active/fallback behavior rather than claim universal offline speech recognition.

## Why a block model?

A flat transcript mixes content and presentation. Typed blocks let the user edit a chart value, heading level, list style or diagram relationship independently. They also simplify persistence, rendering, testing and later AI previews.

## Why Room?

Room provides a structured local database, compile-time query checking, migration support and a clean repository boundary. It is appropriate for notes, ordered blocks, notebooks, tags and configurable voice rules.

## How is ambiguity handled?

The parser returns an explicit ambiguous or rejected result when required values are missing or invalid. It does not guess destructive operations. The user can correct the transcript, supply the missing value or fall back to typed editing.

## How will accuracy be evaluated?

- Word error rate for speech recognition.
- Intent and slot accuracy for VoxScript.
- End-to-end structure accuracy and latency.
- Persistence/reload integrity and physical-device reliability.
- Later AI faithfulness, coverage, instruction compliance, hallucination and acceptance.

## What prevents AI from overwriting notes?

AI enhancement is opt-in, the original note/transcript is preserved, output is shown as a preview or diff, and applying it requires explicit user acceptance.

## Why is the project feasible in two months?

The demonstration does not depend on AI. The scope is bounded to a single-user local Android app, a finite command catalog, three chart types and two diagram families. Work is staged so speech, parser, persistence and rendering can be verified independently before integration.

## Current honest status

- Concept, scope, architecture, grammar draft, evaluation plan and Review-1 PPT are complete.
- Native permission handling, bounded speech capture, partial/final callback state and recognizer cleanup are implemented.
- Android initially reports an on-device recognizer on `2411DRN47I`, but the selected language is unavailable; VoxBox now falls back to the system recognizer and discloses possible network use.
- Plain dictation, heading, bullet and pie-chart parsing are implemented and unit-tested.
- The 25% yellow wheat chart preview is verified on the physical device.
- Room persistence, the full editor/organization layer, remaining commands and AI are not implemented.
- Human-spoken transcription metrics are not available yet; the device pass verified activation/state handling, not word error rate.
- No accuracy result is available yet.

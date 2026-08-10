# Temporary debug log — REMOVE BEFORE RELEASE

**Added:** 2026-08-10
**Reason:** field failures during real lectures could not be diagnosed from screenshots.
**Owner:** Tejas

This file exists so the feature below is not forgotten. It is a diagnostic aid, not a product
feature, and it should not survive into a build anyone else installs.

---

## What was added

| File | What it does |
| --- | --- |
| `VoxBox/app/src/main/java/me/thimmaiah/voxbox/debug/VbDebugLog.kt` | In-memory ring buffer (2,000 entries) written to `filesDir/voxbox-debug.log` on demand |
| `settings/DebugLogScreen.kt` | Settings sub-page: view entry count, share the log, clear it, switch it off |
| Call sites tagged `VbDebugLog.log(...)` | Instrumentation across capture, transcription, the frame filter and note refinement |

Find every trace of it with:

```bash
grep -rn "VbDebugLog\|DebugLogScreen\|TEMPORARY_DEBUG_LOG" VoxBox/app/src docs/
```

## Why it is a privacy question, not just dead code

A lecture is other people talking, and some of them have not agreed to be recorded. The log is
therefore written to be safe to share with a stranger:

- **Transcribed speech is never stored in full.** `logText` records a character count and at most
  a 60-character prefix, enough to tell *which* utterance a bug involved without reproducing it.
- **No audio, no images, no note bodies, no note titles.**
- **Nothing is uploaded.** The file sits in app-private storage until the student taps share.

If you extend the logging, keep those rules. A log that quietly accumulates a full transcript is a
recording of a lecture by another name, and it would sit outside every consent statement the app
makes on the About screen.

## Removal checklist

Before any build that leaves your own device:

1. Delete `debug/VbDebugLog.kt` and `settings/DebugLogScreen.kt`.
2. Delete the `Diagnostics` group and its route from `SettingsScreen.kt`, `VoxBoxNav.kt` and
   `VoxBoxApp.kt`.
3. Remove every `VbDebugLog.` call site — the grep above finds them all.
4. Delete `docs/TEMPORARY_DEBUG_LOG.md` (this file).
5. Rebuild and confirm the grep returns nothing.

## If it ships by accident

It fails safe rather than silently: the log is off-by-default in release builds
(`VbDebugLog.enabled` is set from `BuildConfig.DEBUG` at startup), holds at most 2,000 entries,
and never leaves the device on its own. That is a mitigation, not a reason to keep it.

# Live-note delta integration handoff

The proxy and Android HTTP/model layer now support bounded Markdown deltas without breaking the current full-note flow. `CaptureSessionViewModel` still requests the legacy full response by default; this file records the remaining wiring so it can be enabled deliberately after the capture-pipeline changes settle.

## Current compatibility behavior

- Omitting `responseMode` preserves the existing request and complete `markdown` response.
- `HttpNoteRefinementClient` parses both legacy full responses and delta responses.
- `NoteRefinement.materializeMarkdown(existingMarkdown)` returns the full response as-is or appends a validated `markdownDelta`.
- The HTTP client rejects a delta whose echoed SHA-256 does not match its request context.
- The server rejects replaying the same `sessionId` and `requestId` with different evidence or context (`409 idempotency_conflict`).

## Remaining ViewModel wiring

For each runnable-note update:

1. Read the canonical complete Markdown at `baseRevision` and compute its lowercase SHA-256 over UTF-8 bytes.
2. Build a bounded `IncrementalNoteContext`: title (maximum 240 characters), an outline (maximum 12,000), recent Markdown (maximum 24,000), and the hash.
3. Send `responseMode = DELTA`, that `noteContext`, and `existingMarkdown = ""`. Continue sending only the new transcript/board evidence.
4. Before persistence, confirm the local revision is still `baseRevision` and the complete note still hashes to `baseContentSha256`.
5. Apply `result.materializeMarkdown(currentMarkdown)` and persist it with the normal revision transaction. If either revision or hash changed, discard the stale response and retry with a new `requestId`.

Keep full mode as a fallback for migration/recovery and for any operation that intentionally needs a complete rewrite rather than an append-only lecture delta.

## Syllabus context

New clients should send at most eight `SyllabusContextExcerpt` values, each with at most 2,000 text characters and at most 12,000 characters in total. The legacy raw `syllabusContext` remains accepted; the server relevance-selects and forwards at most 12,000 characters. Explicit excerpts take precedence.

## Endpoint configuration

Debug uses `http://127.0.0.1:8787` from `BuildConfig.VOXBOX_API_BASE_URL` and still requires:

```powershell
adb reverse tcp:8787 tcp:8787
```

Release builds require an explicit HTTPS base URL:

```powershell
cd "D:\College Project\VoxBox"
.\gradlew.bat assembleRelease -PVOXBOX_API_BASE_URL=https://api.example.com
```

The `VOXBOX_API_BASE_URL` environment variable is an equivalent input. The release validation task fails for missing or insecure configuration.

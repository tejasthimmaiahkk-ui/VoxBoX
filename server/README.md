# VoxBox live-notes proxy

This dependency-free Node.js 20+ service is the server boundary for VoxBox board extraction, diarized audio transcription, and incremental Markdown note refinement. It keeps the AI provider key out of the Android APK.

> **Credential warning:** never put a real provider key in this directory, source control, Gradle, Android resources, or the APK. It belongs in the server process environment or the host's secret store only. The automated tests use fake transports and make no billable requests.

The proxy is intentionally small and ephemeral. It does not create server-side note sessions or persist notes, audio, images, or syllabus content. `sessionId` values correlate client requests. The only retained application data is a bounded in-memory note-refinement replay cache: up to 128 responses for approximately 10 minutes, keyed by `sessionId` and `requestId`.

The complete machine-readable contract is in [`openapi.yaml`](./openapi.yaml).

## Run in deterministic mock mode

```powershell
cd "D:\College Project\server"
$env:MOCK_AI="1"
npm start
```

`MOCK_AI=1` applies to all three pipelines and makes no provider calls. The older `MOCK_VISION=1` variable remains a compatibility alias and currently also enables mock mode for every pipeline.

- Board mock output does not inspect the image.
- Audio mock output does not transcribe the audio; it returns one fixed speaker-labelled segment after validating the media.
- Note mock output deterministically formats the supplied evidence and is not an AI accuracy result.
- Every mock response includes `"source": "mock"`.

## Run with OpenRouter

Set the provider key and the client token only in the process environment:

```powershell
$env:OPENROUTER_API_KEY="<server-side-key>"
$env:VOXBOX_CLIENT_TOKEN="<long-random-token>"
Remove-Item Env:MOCK_AI -ErrorAction SilentlyContinue
Remove-Item Env:MOCK_VISION -ErrorAction SilentlyContinue
npm start
```

`.env.example` is a template; this server does not automatically load it. If mock mode is disabled and no key is configured, provider-backed requests return HTTP `503` with error code `provider_not_configured`.

The implemented model routing is fixed in `server.mjs`:

| Pipeline | Model | Provider API |
| --- | --- | --- |
| Board/projector frame | `google/gemini-2.5-flash-lite` | Chat Completions, image_url, strict JSON Schema |
| Markdown note refinement | `openai/gpt-oss-120b` | Chat Completions, strict JSON Schema |
| Audio transcription | `google/gemini-3.1-flash-lite` | Chat Completions, `input_audio`, strict diarized JSON Schema |

## Security boundary

The server binds to `127.0.0.1:8787` by default. For a USB-connected Android debug device:

```powershell
adb reverse tcp:8787 tcp:8787
```

This cleartext loopback route is for development only. A hosted deployment must terminate TLS at the platform, set `VOXBOX_CLIENT_TOKEN`, and keep the provider key in the host's secret store. The proxy now authenticates callers and bounds its own spend, but it still relies on the host for TLS.

The Android debug build uses `http://127.0.0.1:8787` through `BuildConfig.VOXBOX_API_BASE_URL`, so the `adb reverse` workflow above remains unchanged. Release builds have no localhost fallback and fail validation unless an HTTPS base URL is supplied:

```powershell
cd "D:\College Project\VoxBox"
.\gradlew.bat assembleRelease -PVOXBOX_API_BASE_URL=https://api.example.com
```

The `VOXBOX_API_BASE_URL` environment variable is also accepted. Release validation rejects a missing value, cleartext HTTP, credentials, query strings, and fragments; the clients repeat the HTTPS/configuration check before opening a connection.

Requests and responses use `Cache-Control: no-store`. Submitted media and text are decoded or forwarded in memory and are not written to disk by this proxy. Live provider mode still sends the selected data to OpenRouter, which forwards it to the upstream that serves the chosen model; provider-side processing is governed by the configured account and those providers' policies. This proxy makes no zero-retention guarantee about them.

## Common HTTP contract

- `GET /health` is the only non-POST route.
- All pipeline requests are JSON objects with `Content-Type: application/json`.
- The complete JSON body limit is 14 MiB.
- Unknown top-level request fields are rejected.
- Media is raw Base64 without a `data:` URL prefix; decoded size and file signatures are checked before any provider call.
- Error responses use `{ "error": { "code": "string", "message": "string", "retryable": boolean } }`.
- Expected error statuses include `400`, `409`, `413`, `415`, `429`, `502`, and `503`; unknown routes return `404`.

### Provider failure classification

An upstream failure is no longer collapsed into a single `502`. The proxy reads a bounded provider
error body plus the response headers and reports:

| Upstream | `code` | Proxy status | `retryable` |
| --- | --- | --- | --- |
| `429` with `insufficient_quota` | `<kind>_quota_exhausted` | `429` | `false` |
| `429` otherwise | `<kind>_rate_limited` | `429` | `true` |
| `401` or `403` | `<kind>_auth_error` | `502` | `false` |
| other `4xx` | `<kind>_request_rejected` | `502` | `false` |
| `5xx` or unreadable | `<kind>_provider_error` | `502` | `true` |

`<kind>` is `transcription`, `vision`, or `note`. Classified failures also carry a `provider` object
with the upstream `status`, `type`, `code`, truncated `message`, `requestId` (`x-request-id`),
`retryAfterSeconds` (from `Retry-After` or the rate-limit reset headers) and the remaining
request/token counters.

The proxy logs only that classification. It never logs or forwards request bodies, media, or the
configured API key, and it truncates the provider body to 8 KiB before parsing.

## `GET /health`

Response:

```json
{
  "status": "ok",
  "mode": "mock",
  "models": {
    "vision": "google/gemini-2.5-flash-lite",
    "notes": "openai/gpt-oss-120b",
    "transcription": "google/gemini-3.1-flash-lite"
  },
  "retention": "in-memory-forwarding-only"
}
```

`mode` is `mock` when either mock environment variable is `1`; otherwise it is `live`. Health does not reveal whether a key is configured.

## `POST /v1/board/extract`

Request:

```json
{
  "imageBase64": "<raw Base64>",
  "mimeType": "image/jpeg"
}
```

Accepted MIME types are `image/jpeg`, `image/png`, and `image/webp`. The decoded image limit is 8 MiB. The bytes must match the declared JPEG, PNG, or WebP signature.

Response:

```json
{
  "title": "Calculus",
  "summary": "Derivative rules shown on the board.",
  "visibleText": ["d/dx x^2 = 2x"],
  "concepts": ["derivatives", "power rule"],
  "equations": ["\\frac{d}{dx}x^2=2x"],
  "diagramRegions": [
    {
      "left": 0.1,
      "top": 0.2,
      "width": 0.5,
      "height": 0.6,
      "caption": "Tangent graph"
    }
  ],
  "confidence": 0.95,
  "warnings": [],
  "source": "openrouter"
}
```

Diagram coordinates are normalized to `0..1`; width and height must be positive, and each rectangle must remain inside the frame. The response provides crop metadata, not cropped image bytes. The Android client is responsible for locally cropping and saving diagram assets before deleting its temporary source frame. This endpoint receives still frames, not a continuous video stream, and performs no frame-change detection or server-side media retention.

## `POST /v1/audio/transcribe`

Request:

```json
{
  "audioBase64": "<raw Base64>",
  "mimeType": "audio/wav",
  "sessionId": "class-2026-08-03",
  "chunkId": "chunk-0007",
  "offsetMs": 30000,
  "language": "en"
}
```

`sessionId` and `chunkId` are non-blank strings up to 128 characters. `offsetMs` is a non-negative integer and is added to provider-relative segment times. `language` is optional and limited to 16 characters.

Accepted MIME types are `audio/wav`, `audio/x-wav`, `audio/mpeg`, `audio/mp4`, `audio/ogg`, `audio/webm`, and `audio/flac`. The decoded audio limit is 10 MiB and the declared type must match the file signature.

Response:

```json
{
  "sessionId": "class-2026-08-03",
  "chunkId": "chunk-0007",
  "text": "The power rule gives two x.",
  "durationMs": 2500,
  "segments": [
    {
      "id": "chunk-0007:seg_1",
      "speakerId": "A",
      "startMs": 30250,
      "endMs": 32250,
      "text": "The power rule gives two x."
    }
  ],
  "source": "openrouter"
}
```

Speaker labels come from each diarized transcription result. The proxy does not decide which label is the teacher and does not guarantee that a label remains stable across independent chunks; speaker-focus policy belongs in the client/session pipeline.

## `POST /v1/notes/refine`

This stateless endpoint combines incremental transcript and optional board evidence with client-owned note context. It supports the original complete-Markdown response and a bounded append-only Markdown delta for long lectures. The client remains the canonical owner of note history and must apply or reject a response.

Request:

```json
{
  "requestId": "request-12",
  "sessionId": "class-2026-08-03",
  "baseRevision": 7,
  "mode": "video",
  "notePolicy": "runnable",
  "primarySpeakerId": "A",
  "syllabusContext": "Unit 1: limits, continuity, and derivatives",
  "existingMarkdown": "# Calculus\n\n## Limits",
  "transcriptSegments": [
    {
      "id": "chunk-0007:seg_1",
      "speakerId": "A",
      "startMs": 30250,
      "endMs": 32250,
      "text": "The derivative of x squared is two x.",
      "isPrimarySpeaker": true
    }
  ],
  "boardEvidence": {
    "id": "frame-18",
    "capturedAtMs": 32000,
    "summary": "Power rule example",
    "visibleText": ["d/dx x^2 = 2x"],
    "concepts": ["power rule"],
    "equations": ["\\frac{d}{dx}x^2=2x"],
    "diagramCaptions": ["Tangent graph"]
  }
}
```

Rules and limits:

- `requestId` and `sessionId`: non-blank, at most 128 characters.
- `baseRevision`: non-negative integer.
- `mode`: `voice` or `video`.
- `notePolicy`: `runnable` or `verbatim`.
- `primarySpeakerId`: optional, at most 64 characters.
- `syllabusContext` and `existingMarkdown`: optional strings, each at most 120,000 characters.
- `responseMode`: optional `full` or `delta`; omitted means `full` for backward compatibility.
- `syllabusExcerpts`: optional relevance-selected context with at most 8 entries. Each has `id`, `heading` (at most 240 characters), and non-blank `text` (at most 2,000 characters); total excerpt text is at most 12,000 characters.
- `transcriptSegments`: required array with at most 80 entries. Each entry needs an ID, non-negative start/end times, non-blank text, and `endMs >= startMs`; `speakerId` and `isPrimarySpeaker` are optional.
- `boardEvidence`: optional or `null`. When present it needs `id`, `capturedAtMs`, and arrays for `visibleText`, `concepts`, and `equations`; `summary` and `diagramCaptions` are optional.
- At least one transcript segment or one board-evidence object is required.
- Board-evidence lists accept at most 100 visible-text lines, 40 concepts, 40 equations, and 20 diagram captions. Individual list strings are truncated to 4,000 characters by the proxy.

When legacy `syllabusContext` is longer than 12,000 characters, the proxy scores bounded syllabus chunks against the new transcript/board evidence and forwards at most 12,000 relevant characters to the provider. Explicit `syllabusExcerpts` take precedence. This bounds provider use without breaking existing clients.

`runnable` asks for concise structured study notes that merge repetition and omit clear tangents/stumbles. `verbatim` asks for a faithful chronological readable transcript note. In both modes, the provider is instructed to preserve captured claims and expose likely conflicts in `corrections` instead of silently rewriting teacher, OCR, or transcription evidence. Syllabus text is context only and is not treated as proof that something was taught.

Response:

```json
{
  "requestId": "request-12",
  "sessionId": "class-2026-08-03",
  "baseRevision": 7,
  "nextRevision": 8,
  "title": "Calculus: Power Rule",
  "markdown": "# Calculus: Power Rule\n\n- **Power rule:** $d(x^2)/dx = 2x$",
  "corrections": [
    {
      "captured": "Captured claim",
      "suggested": "Suggested clarification",
      "reason": "Why the evidence may conflict",
      "severity": "warning",
      "evidenceIds": ["chunk-0007:seg_1", "frame-18"]
    }
  ],
  "consumedEvidenceIds": ["chunk-0007:seg_1", "frame-18"],
  "warnings": [],
  "source": "openrouter"
}
```

The server validates that response identity matches the request and that `nextRevision` equals `baseRevision + 1`. A successful response is cached in memory by `sessionId:requestId`; replaying that pair within approximately 10 minutes returns the cached result without another provider request. Therefore the client must not reuse a `requestId` for different evidence.

### Bounded delta mode

For long-running sessions, set `responseMode` to `delta`, send an empty `existingMarkdown`, and include only bounded context:

```json
{
  "responseMode": "delta",
  "existingMarkdown": "",
  "noteContext": {
    "title": "Calculus",
    "outlineMarkdown": "# Calculus\n\n- Limits\n- Derivatives",
    "recentMarkdown": "## Power rule\n\n- Previous recent point.",
    "contentSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
  }
}
```

`title` is limited to 240 characters, `outlineMarkdown` to 12,000, and `recentMarkdown` to 24,000. `contentSha256` must be the lowercase SHA-256 of the complete canonical note. A delta response is:

```json
{
  "requestId": "request-13",
  "sessionId": "class-2026-08-03",
  "baseRevision": 8,
  "nextRevision": 9,
  "title": "Calculus: Chain Rule",
  "updateMode": "delta",
  "baseContentSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "markdownDelta": "## Chain rule\n\n- Differentiate the outer function first.",
  "corrections": [],
  "consumedEvidenceIds": ["chunk-0008:seg_1"],
  "warnings": [],
  "source": "openrouter"
}
```

The client must apply a delta only when the revision and echoed content hash still match local state. The delta provider budget is 2,000 output tokens; full-note compatibility mode remains 5,000. Provider corrections and consumed IDs are rejected if they cite evidence IDs not present in the request.

Replay safety is content-aware: the cache fingerprints the normalized request. Reusing the same `sessionId:requestId` with changed evidence or context returns HTTP `409` with `idempotency_conflict` instead of replaying stale notes.

## Test

```powershell
npm test
```

The 22 tests cover mock behavior across all pipelines, request validation before provider calls, image signatures and crop bounds, diarized transcription with chunk offsets, strict note response identity/revisions, bounded delta context, relevance-selected syllabus forwarding, evidence provenance, content-aware idempotency, provider-failure classification, client-token authentication, the rate limit and daily budget, and fenced or prefixed model output. All provider transports are mocked, so the suite makes no real or billable requests.


## Provider routing

All three pipelines call OpenRouter's OpenAI-compatible `POST /api/v1/chat/completions` with a
`strict` `json_schema` response format. OpenRouter is not used through its dedicated
`/api/v1/audio/transcriptions` endpoint, because that endpoint returns no speaker labels and this
project's speaker-focus feature depends on per-segment speakers.

Diarization therefore comes from an audio-capable model constrained by the diarized transcript
schema. That is **speaker segmentation inferred from the audio, not acoustic diarization with voice
embeddings**. It matches how this project already treats labels: local to one chunk and never an
identity claim. Accuracy on real classroom audio is unmeasured.

Every request sets `provider: { require_parameters: true }`. OpenRouter load-balances a single model
id across several upstream providers, and without this a request can land on one that ignores
`response_format` and aborts mid-generation. That was observed live as `finish_reason: "error"` with
a half-written JSON object, and the pin removed it.

Abnormal completions are reported distinctly rather than as malformed JSON:

| Condition | `code` | Proxy status | `retryable` |
| --- | --- | --- | --- |
| `finish_reason: length` or native `MAX_TOKENS` | `upstream_output_truncated` | `502` | `true` |
| any other non-`stop` finish reason | `upstream_generation_failed` | `502` | `true` |
| unparseable body after fence/preamble normalization | `invalid_upstream_response` | `502` | `true` |

Model output is normalized before parsing: a Markdown code fence or a short preamble around the JSON
object is stripped, because models that honour `strict` almost always still occasionally add one.

## Environment variables

| Variable | Purpose |
| --- | --- |
| `OPENROUTER_API_KEY` | Provider credential. Required in live mode. Never ships in the APK. |
| `VOXBOX_CLIENT_TOKEN` | Shared bearer token the app presents. Required in live mode. |
| `VOXBOX_RATE_LIMIT_MAX` / `VOXBOX_RATE_LIMIT_WINDOW_MS` | Per-caller fixed window. Defaults 60 per 60 s. |
| `VOXBOX_DAILY_REQUEST_BUDGET` | Hard ceiling on billable calls per UTC day. Default 1500. |
| `VOXBOX_TRANSCRIPTION_MODEL` / `VOXBOX_VISION_MODEL` / `VOXBOX_NOTE_MODEL` | Model overrides. |
| `MOCK_AI` | Deterministic responses, no provider call, no budget consumption. |
| `HOST` / `PORT` | Bind address. Use `0.0.0.0` and the platform port when hosted. |

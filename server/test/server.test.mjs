import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import {
  createVoxBoxServer,
  NOTE_MODEL,
  TRANSCRIPTION_MODEL,
  VISION_MODEL,
} from "../server.mjs";

const servers = [];
const jpeg = Buffer.from([0xff, 0xd8, 0xff, 0xd9]);

function tinyWav() {
  const bytes = Buffer.alloc(44);
  bytes.write("RIFF", 0, "ascii");
  bytes.writeUInt32LE(36, 4);
  bytes.write("WAVE", 8, "ascii");
  bytes.write("fmt ", 12, "ascii");
  bytes.writeUInt32LE(16, 16);
  bytes.writeUInt16LE(1, 20);
  bytes.writeUInt16LE(1, 22);
  bytes.writeUInt32LE(16_000, 24);
  bytes.writeUInt32LE(32_000, 28);
  bytes.writeUInt16LE(2, 32);
  bytes.writeUInt16LE(16, 34);
  bytes.write("data", 36, "ascii");
  bytes.writeUInt32LE(0, 40);
  return bytes;
}

async function start(options) {
  const server = createVoxBoxServer(options);
  servers.push(server);
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  return `http://127.0.0.1:${server.address().port}`;
}

const CLIENT_TOKEN = "test-client-token";

async function post(base, path, body, { token = CLIENT_TOKEN } = {}) {
  const headers = { "content-type": "application/json" };
  if (token !== null) headers.authorization = `Bearer ${token}`;
  return fetch(`${base}${path}`, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
}

function noteRequest(overrides = {}) {
  return {
    requestId: "request-1",
    sessionId: "session-1",
    baseRevision: 0,
    mode: "voice",
    notePolicy: "runnable",
    primarySpeakerId: "A",
    syllabusContext: "Unit 1: derivatives",
    existingMarkdown: "# Calculus",
    transcriptSegments: [{
      id: "segment-1",
      speakerId: "A",
      startMs: 1_000,
      endMs: 4_000,
      text: "The derivative of x squared is two x.",
      isPrimarySpeaker: true,
    }],
    boardEvidence: null,
    ...overrides,
  };
}

afterEach(async () => {
  await Promise.all(servers.splice(0).map((server) => new Promise((resolve) => server.close(resolve))));
});

test("health and all pipelines work deterministically in mock mode", async () => {
  const base = await start({ env: { MOCK_AI: "1" } });
  const health = await fetch(`${base}/health`).then((response) => response.json());
  assert.deepEqual(health, {
    status: "ok",
    mode: "mock",
    models: { vision: VISION_MODEL, notes: NOTE_MODEL, transcription: TRANSCRIPTION_MODEL },
    retention: "in-memory-forwarding-only",
    budget: { day: new Date().toISOString().slice(0, 10), used: 0, limit: 1_500 },
  });

  const boardResponse = await post(base, "/v1/board/extract", {
    imageBase64: jpeg.toString("base64"),
    mimeType: "image/jpeg",
  });
  assert.equal(boardResponse.status, 200);
  const board = await boardResponse.json();
  assert.deepEqual(Object.keys(board), [
    "title",
    "summary",
    "visibleText",
    "concepts",
    "equations",
    "diagramRegions",
    "confidence",
    "warnings",
    "source",
  ]);
  assert.equal(board.source, "mock");

  const audioResponse = await post(base, "/v1/audio/transcribe", {
    audioBase64: tinyWav().toString("base64"),
    mimeType: "audio/wav",
    sessionId: "session-1",
    chunkId: "chunk-1",
    offsetMs: 20_000,
    language: "en",
  });
  assert.equal(audioResponse.status, 200);
  const audio = await audioResponse.json();
  assert.equal(audio.source, "mock");
  assert.equal(audio.segments[0].speakerId, "A");
  assert.equal(audio.segments[0].startMs, 20_000);

  const noteResponse = await post(base, "/v1/notes/refine", noteRequest());
  assert.equal(noteResponse.status, 200);
  const note = await noteResponse.json();
  assert.equal(note.source, "mock");
  assert.equal(note.baseRevision, 0);
  assert.equal(note.nextRevision, 1);
  assert.match(note.markdown, /derivative of x squared/i);
  assert.deepEqual(note.consumedEvidenceIds, ["segment-1"]);
});

test("invalid media and unknown fields are rejected before any provider call", async () => {
  let calls = 0;
  const base = await start({
    env: { OPENROUTER_API_KEY: "test-only", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => {
      calls += 1;
      throw new Error("must not run");
    },
  });
  const badBase64 = await post(base, "/v1/board/extract", {
    imageBase64: "not base64!",
    mimeType: "image/jpeg",
  });
  assert.equal(badBase64.status, 400);
  const wrongSignature = await post(base, "/v1/board/extract", {
    imageBase64: Buffer.from("frame").toString("base64"),
    mimeType: "image/jpeg",
  });
  assert.equal(wrongSignature.status, 400);
  const unknownField = await post(base, "/v1/notes/refine", { ...noteRequest(), embeddedKey: "no" });
  assert.equal(unknownField.status, 400);
  assert.equal(calls, 0);
});

test("live board mode sends the fixed vision model and validates diagram crops", async () => {
  let request;
  const expected = {
    title: "Calculus",
    summary: "Derivative rules",
    visibleText: ["d/dx x² = 2x"],
    concepts: ["derivatives"],
    equations: ["\\frac{d}{dx}x^2=2x"],
    diagramRegions: [{ left: 0.1, top: 0.2, width: 0.5, height: 0.6, caption: "Tangent graph" }],
    confidence: 0.95,
    warnings: [],
  };
  const base = await start({
    env: { OPENROUTER_API_KEY: "test-only-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async (url, options) => {
      request = { url, options };
      return new Response(JSON.stringify({ choices: [{ message: { content: JSON.stringify(expected) } }] }), {
        status: 200,
        headers: { "content-type": "application/json" },
      });
    },
  });
  const response = await post(base, "/v1/board/extract", {
    imageBase64: jpeg.toString("base64"),
    mimeType: "image/jpeg",
  });
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { ...expected, source: "openrouter" });
  const body = JSON.parse(request.options.body);
  assert.equal(request.url, "https://openrouter.ai/api/v1/chat/completions");
  assert.equal(body.model, VISION_MODEL);
  assert.equal(body.response_format.json_schema.strict, true);
  assert.equal(body.response_format.json_schema.name, "board_extraction");
  assert.equal(body.max_tokens, 4_000);
  assert.match(body.messages[0].content[1].image_url.url, /^data:image\/jpeg;base64,/);
  assert.equal(request.options.headers.Authorization, "Bearer test-only-secret");
});

test("out-of-bounds diagram regions from the provider are rejected", async () => {
  const invalid = {
    title: "Bad crop",
    summary: "",
    visibleText: [],
    concepts: [],
    equations: [],
    diagramRegions: [{ left: 0.8, top: 0.1, width: 0.4, height: 0.5, caption: "Outside" }],
    confidence: 0.4,
    warnings: [],
  };
  const base = await start({
    env: { OPENROUTER_API_KEY: "test", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => new Response(JSON.stringify({ choices: [{ message: { content: JSON.stringify(invalid) } }] }), { status: 200 }),
  });
  const response = await post(base, "/v1/board/extract", {
    imageBase64: jpeg.toString("base64"),
    mimeType: "image/jpeg",
  });
  assert.equal(response.status, 502);
  assert.equal((await response.json()).error.code, "invalid_upstream_response");
});

test("live transcription requests a diarized schema and applies the chunk offset", async () => {
  let request;
  const base = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async (url, options) => {
      request = { url, options };
      return new Response(JSON.stringify({
        choices: [{
          message: {
            content: JSON.stringify({
              text: "Power rule.",
              durationMs: 2_500,
              segments: [{ speaker: "A", startMs: 250, endMs: 2_250, text: " Power rule. " }],
            }),
          },
        }],
      }), { status: 200, headers: { "content-type": "application/json" } });
    },
  });
  const response = await post(base, "/v1/audio/transcribe", {
    audioBase64: tinyWav().toString("base64"),
    mimeType: "audio/wav",
    sessionId: "session-2",
    chunkId: "chunk-7",
    offsetMs: 30_000,
    language: "en",
  });
  assert.equal(response.status, 200);
  const result = await response.json();
  // Model offsets are clip-relative; the proxy rebases them onto the session timeline.
  assert.equal(result.segments[0].startMs, 30_250);
  assert.equal(result.segments[0].endMs, 32_250);
  assert.equal(result.segments[0].speakerId, "A");
  assert.equal(result.source, "openrouter");
  assert.equal(request.url, "https://openrouter.ai/api/v1/chat/completions");

  const sent = JSON.parse(request.options.body);
  assert.equal(sent.model, TRANSCRIPTION_MODEL);
  assert.equal(sent.response_format.json_schema.name, "diarized_transcript");
  assert.equal(sent.response_format.json_schema.strict, true);
  const parts = sent.messages[0].content;
  assert.equal(parts.at(-1).type, "input_audio");
  assert.equal(parts.at(-1).input_audio.format, "wav");
  assert.ok(parts[0].text.includes("must not be treated as identities"));
  assert.equal(request.options.headers.Authorization, "Bearer provider-secret");
});

test("live note refinement enforces request identity and caches idempotent replays", async () => {
  let calls = 0;
  let upstreamBody;
  const requestBody = noteRequest();
  const expected = {
    requestId: requestBody.requestId,
    sessionId: requestBody.sessionId,
    baseRevision: 0,
    nextRevision: 1,
    title: "Derivatives",
    markdown: "# Derivatives\n\n- **Power rule:** $d(x^2)/dx=2x$",
    corrections: [],
    consumedEvidenceIds: ["segment-1"],
    warnings: [],
  };
  const base = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async (_url, options) => {
      calls += 1;
      upstreamBody = JSON.parse(options.body);
      return new Response(JSON.stringify({ choices: [{ message: { content: JSON.stringify(expected) } }] }), { status: 200 });
    },
  });
  const first = await post(base, "/v1/notes/refine", requestBody);
  const second = await post(base, "/v1/notes/refine", requestBody);
  assert.equal(first.status, 200);
  assert.equal(second.status, 200);
  assert.deepEqual(await first.json(), { ...expected, source: "openrouter" });
  assert.deepEqual(await second.json(), { ...expected, source: "openrouter" });
  assert.equal(calls, 1);
  assert.equal(upstreamBody.model, NOTE_MODEL);
  assert.equal(upstreamBody.model, NOTE_MODEL);
  assert.equal(upstreamBody.response_format.json_schema.strict, true);
  assert.match(upstreamBody.messages[0].content, /Never silently correct/);
});

test("note refinement rejects empty evidence and invalid revision relationships", async () => {
  const base = await start({ env: { MOCK_AI: "1" } });
  const empty = await post(base, "/v1/notes/refine", noteRequest({ transcriptSegments: [] }));
  assert.equal(empty.status, 400);

  const live = await start({
    env: { OPENROUTER_API_KEY: "test", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => new Response(JSON.stringify({ choices: [{ message: { content: JSON.stringify({
        requestId: "request-1",
        sessionId: "session-1",
        baseRevision: 0,
        nextRevision: 9,
        title: "Wrong revision",
        markdown: "# Wrong",
        corrections: [],
        consumedEvidenceIds: ["segment-1"],
        warnings: [],
      }) } }] }), { status: 200 }),
  });
  const invalidRevision = await post(live, "/v1/notes/refine", noteRequest());
  assert.equal(invalidRevision.status, 502);
  assert.equal((await invalidRevision.json()).error.code, "invalid_upstream_response");
});

test("delta refinement uses bounded note context and selects relevant syllabus text", async () => {
  let calls = 0;
  let upstreamBody;
  const contentSha256 = "a".repeat(64);
  const longSyllabus = [
    ...Array.from({ length: 20 }, (_, index) => `Unit ${index}: unrelated administrative material. `.repeat(40)),
    "Power rule and derivatives: the derivative of x squared is two x. Tangents represent instantaneous slope.",
  ].join("\n\n");
  const requestBody = noteRequest({
    requestId: "delta-1",
    baseRevision: 41,
    existingMarkdown: "",
    responseMode: "delta",
    noteContext: {
      title: "Calculus",
      outlineMarkdown: "# Calculus\n## Derivatives",
      recentMarkdown: "- Limits lead into derivatives.",
      contentSha256,
    },
    syllabusContext: longSyllabus,
  });
  const expected = {
    requestId: requestBody.requestId,
    sessionId: requestBody.sessionId,
    baseRevision: 41,
    nextRevision: 42,
    title: "Calculus",
    updateMode: "delta",
    baseContentSha256: contentSha256,
    markdownDelta: "## Power rule\n- $d(x^2)/dx=2x$",
    corrections: [],
    consumedEvidenceIds: ["segment-1"],
    warnings: [],
  };
  const base = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async (_url, options) => {
      calls += 1;
      upstreamBody = JSON.parse(options.body);
      return new Response(JSON.stringify({ choices: [{ message: { content: JSON.stringify(expected) } }] }), { status: 200 });
    },
  });

  const first = await post(base, "/v1/notes/refine", requestBody);
  const replay = await post(base, "/v1/notes/refine", requestBody);
  assert.equal(first.status, 200);
  assert.equal(replay.status, 200);
  assert.deepEqual(await first.json(), { ...expected, source: "openrouter" });
  assert.deepEqual(await replay.json(), { ...expected, source: "openrouter" });
  assert.equal(calls, 1);
  assert.equal(upstreamBody.max_tokens, 3_000);
  assert.equal(upstreamBody.response_format.json_schema.name, "note_delta");
  const providerRequest = JSON.parse(upstreamBody.messages[1].content);
  assert.equal(providerRequest.existingMarkdown, "");
  assert.ok(providerRequest.syllabusContext.length <= 12_000);
  assert.match(providerRequest.syllabusContext, /derivative of x squared/i);
  assert.equal(providerRequest.noteContext.contentSha256, contentSha256);
});

test("note idempotency rejects changed evidence instead of replaying a stale response", async () => {
  let calls = 0;
  const requestBody = noteRequest({ requestId: "stable-request" });
  const expected = {
    requestId: requestBody.requestId,
    sessionId: requestBody.sessionId,
    baseRevision: 0,
    nextRevision: 1,
    title: "Calculus",
    markdown: "# Calculus\n\n- Power rule",
    corrections: [],
    consumedEvidenceIds: ["segment-1"],
    warnings: [],
  };
  const base = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => {
      calls += 1;
      return new Response(JSON.stringify({ choices: [{ message: { content: JSON.stringify(expected) } }] }), { status: 200 });
    },
  });
  assert.equal((await post(base, "/v1/notes/refine", requestBody)).status, 200);
  const conflict = await post(base, "/v1/notes/refine", {
    ...requestBody,
    transcriptSegments: [{ ...requestBody.transcriptSegments[0], text: "Different evidence." }],
  });
  assert.equal(conflict.status, 409);
  assert.equal((await conflict.json()).error.code, "idempotency_conflict");
  assert.equal(calls, 1);
});

test("note provider cannot cite evidence outside the request", async () => {
  const requestBody = noteRequest({ requestId: "bad-evidence" });
  const base = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => new Response(JSON.stringify({ choices: [{ message: { content: JSON.stringify({
        requestId: requestBody.requestId,
        sessionId: requestBody.sessionId,
        baseRevision: 0,
        nextRevision: 1,
        title: "Calculus",
        markdown: "# Calculus\n\n- Unsupported claim",
        corrections: [{
          captured: "claim",
          suggested: "change",
          reason: "unsupported",
          severity: "warning",
          evidenceIds: ["invented-frame"],
        }],
        consumedEvidenceIds: ["segment-1"],
        warnings: [],
      }) } }] }), { status: 200 }),
  });
  const response = await post(base, "/v1/notes/refine", requestBody);
  assert.equal(response.status, 502);
  assert.equal((await response.json()).error.code, "invalid_upstream_response");
});

test("delta requests require bounded context and bounded syllabus excerpts", async () => {
  const base = await start({ env: { MOCK_AI: "1" } });
  const missingContext = await post(base, "/v1/notes/refine", noteRequest({
    responseMode: "delta",
    existingMarkdown: "",
  }));
  assert.equal(missingContext.status, 400);

  const incompleteContext = await post(base, "/v1/notes/refine", noteRequest({
    responseMode: "delta",
    existingMarkdown: "",
    noteContext: {
      outlineMarkdown: "# Calculus",
      recentMarkdown: "",
      contentSha256: "a".repeat(64),
    },
  }));
  assert.equal(incompleteContext.status, 400);

  const tooManyExcerpts = await post(base, "/v1/notes/refine", noteRequest({
    syllabusExcerpts: Array.from({ length: 9 }, (_, index) => ({
      id: `excerpt-${index}`,
      heading: "Heading",
      text: "Context",
    })),
  }));
  assert.equal(tooManyExcerpts.status, 400);
});

function audioRequest(overrides = {}) {
  return {
    audioBase64: tinyWav().toString("base64"),
    mimeType: "audio/wav",
    sessionId: "session-quota",
    chunkId: "chunk-1",
    offsetMs: 0,
    ...overrides,
  };
}

function providerError(status, body, headers = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json", ...headers },
  });
}

test("an exhausted provider quota is preserved as 429 and marked non-retryable", async () => {
  const base = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => providerError(
      429,
      { error: { message: "You exceeded your current quota.", type: "insufficient_quota", code: "insufficient_quota" } },
      { "x-request-id": "req_quota_1" },
    ),
  });

  const response = await post(base, "/v1/audio/transcribe", audioRequest());
  assert.equal(response.status, 429);
  const payload = (await response.json()).error;
  assert.equal(payload.code, "transcription_quota_exhausted");
  assert.equal(payload.retryable, false);
  assert.equal(payload.provider.status, 429);
  assert.equal(payload.provider.type, "insufficient_quota");
  assert.equal(payload.provider.requestId, "req_quota_1");
});

test("a transient rate limit stays retryable and reports its retry delay", async () => {
  const base = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => providerError(
      429,
      { error: { message: "Rate limit reached.", type: "rate_limit_error", code: "rate_limit_exceeded" } },
      { "retry-after": "12", "x-ratelimit-remaining-requests": "0" },
    ),
  });

  const response = await post(base, "/v1/audio/transcribe", audioRequest());
  assert.equal(response.status, 429);
  const payload = (await response.json()).error;
  assert.equal(payload.code, "transcription_rate_limited");
  assert.equal(payload.retryable, true);
  assert.equal(payload.provider.retryAfterSeconds, 12);
  assert.equal(payload.provider.remainingRequests, "0");
  assert.match(payload.message, /12 second/);
});

test("a rejected server credential is non-retryable and never echoes the key", async () => {
  const base = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => providerError(401, {
      error: { message: "Incorrect API key provided.", type: "invalid_request_error", code: "invalid_api_key" },
    }),
  });

  const response = await post(base, "/v1/audio/transcribe", audioRequest());
  assert.equal(response.status, 502);
  const body = await response.text();
  assert.equal(JSON.parse(body).error.code, "transcription_auth_error");
  assert.equal(JSON.parse(body).error.retryable, false);
  assert.ok(!body.includes("provider-secret"));
});

test("vision and note provider failures use the same classification", async () => {
  const visionBase = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => providerError(429, {
      error: { message: "quota", type: "insufficient_quota", code: "insufficient_quota" },
    }),
  });
  const vision = await post(visionBase, "/v1/board/extract", {
    imageBase64: jpeg.toString("base64"),
    mimeType: "image/jpeg",
  });
  assert.equal(vision.status, 429);
  assert.equal((await vision.json()).error.code, "vision_quota_exhausted");

  const noteBase = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => providerError(500, { error: { message: "server error", type: "server_error" } }),
  });
  const note = await post(noteBase, "/v1/notes/refine", noteRequest({ requestId: "provider-500" }));
  assert.equal(note.status, 502);
  const notePayload = (await note.json()).error;
  assert.equal(notePayload.code, "note_provider_error");
  assert.equal(notePayload.retryable, true);
});

test("a non-JSON provider error body still classifies without leaking its contents", async () => {
  const base = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => new Response("<html>Gateway timeout</html>", {
      status: 504,
      headers: { "content-type": "text/html" },
    }),
  });

  const response = await post(base, "/v1/audio/transcribe", audioRequest());
  assert.equal(response.status, 502);
  const body = await response.text();
  const payload = JSON.parse(body).error;
  assert.equal(payload.code, "transcription_provider_error");
  assert.equal(payload.retryable, true);
  assert.equal(payload.provider.status, 504);
  assert.ok(!body.includes("<html>"));
});

test("a live server refuses to forward anything without a configured client token", async () => {
  const base = await start({ env: { OPENROUTER_API_KEY: "provider-secret" } });

  const response = await post(base, "/v1/audio/transcribe", audioRequest());
  assert.equal(response.status, 503);
  const payload = (await response.json()).error;
  assert.equal(payload.code, "client_auth_not_configured");
  assert.equal(payload.retryable, false);
});

test("pipeline routes reject a missing or wrong client token", async () => {
  const base = await start({
    env: { MOCK_AI: "1", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
  });

  const missing = await post(base, "/v1/audio/transcribe", audioRequest(), { token: null });
  assert.equal(missing.status, 401);
  assert.equal((await missing.json()).error.code, "unauthorized");

  const wrong = await post(base, "/v1/audio/transcribe", audioRequest(), { token: "not-the-token" });
  assert.equal(wrong.status, 401);
  assert.equal((await wrong.json()).error.retryable, false);

  const correct = await post(base, "/v1/audio/transcribe", audioRequest());
  assert.equal(correct.status, 200);
});

test("health stays reachable without a token so platform checks and pingers work", async () => {
  const base = await start({
    env: { MOCK_AI: "1", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
  });

  const health = await fetch(`${base}/health`).then((response) => response.json());
  assert.equal(health.status, "ok");
  // The health payload must never carry a credential.
  assert.ok(!JSON.stringify(health).includes(CLIENT_TOKEN));
});

test("a burst beyond the rate limit is rejected with a retry delay", async () => {
  const base = await start({
    env: {
      MOCK_AI: "1",
      VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN,
      VOXBOX_RATE_LIMIT_MAX: "3",
      VOXBOX_RATE_LIMIT_WINDOW_MS: "60000",
    },
  });

  for (let index = 0; index < 3; index += 1) {
    assert.equal((await post(base, "/v1/audio/transcribe", audioRequest())).status, 200);
  }
  const limited = await post(base, "/v1/audio/transcribe", audioRequest());
  assert.equal(limited.status, 429);
  const payload = (await limited.json()).error;
  assert.equal(payload.code, "rate_limited");
  assert.equal(payload.retryable, true);
  assert.ok(payload.retryAfterSeconds >= 1);
});

test("the daily budget caps billable calls and never counts mock calls", async () => {
  const live = await start({
    env: {
      OPENROUTER_API_KEY: "provider-secret",
      VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN,
      VOXBOX_DAILY_REQUEST_BUDGET: "2",
    },
    fetchImpl: async () => new Response(JSON.stringify({
      choices: [{
        message: {
          content: JSON.stringify({
            text: "ok",
            durationMs: 1_000,
            segments: [{ speaker: "A", startMs: 0, endMs: 1_000, text: "ok" }],
          }),
        },
      }],
    }), { status: 200, headers: { "content-type": "application/json" } }),
  });

  assert.equal((await post(live, "/v1/audio/transcribe", audioRequest())).status, 200);
  assert.equal((await post(live, "/v1/audio/transcribe", audioRequest())).status, 200);
  const exhausted = await post(live, "/v1/audio/transcribe", audioRequest());
  assert.equal(exhausted.status, 429);
  const payload = (await exhausted.json()).error;
  assert.equal(payload.code, "daily_budget_exhausted");
  assert.equal(payload.retryable, false);

  // Mock mode reaches no provider, so it must not consume the budget.
  const mock = await start({
    env: { MOCK_AI: "1", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN, VOXBOX_DAILY_REQUEST_BUDGET: "1" },
  });
  for (let index = 0; index < 3; index += 1) {
    assert.equal((await post(mock, "/v1/audio/transcribe", audioRequest())).status, 200);
  }
  const health = await fetch(`${mock}/health`).then((response) => response.json());
  assert.equal(health.budget.used, 0);
});

test("model output wrapped in a Markdown fence or preamble is still accepted", async () => {
  const transcript = {
    text: "Power rule.",
    durationMs: 2_000,
    segments: [{ speaker: "A", startMs: 0, endMs: 2_000, text: "Power rule." }],
  };
  // Observed live: a model that returns clean JSON can intermittently fence it.
  for (const wrap of [
    (json) => "```json\n" + json + "\n```",
    (json) => "```\n" + json + "\n```",
    (json) => "Here is the transcript:\n" + json,
  ]) {
    const base = await start({
      env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
      fetchImpl: async () => new Response(JSON.stringify({
        choices: [{ message: { content: wrap(JSON.stringify(transcript)) } }],
      }), { status: 200, headers: { "content-type": "application/json" } }),
    });
    const response = await post(base, "/v1/audio/transcribe", audioRequest());
    assert.equal(response.status, 200);
    assert.equal((await response.json()).segments[0].text, "Power rule.");
  }

  // Genuinely unparseable output must still be reported as a provider failure.
  const broken = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => new Response(JSON.stringify({
      choices: [{ message: { content: "I could not hear any audio." } }],
    }), { status: 200, headers: { "content-type": "application/json" } }),
  });
  const failed = await post(broken, "/v1/audio/transcribe", audioRequest());
  assert.equal(failed.status, 502);
  assert.equal((await failed.json()).error.code, "invalid_upstream_response");
});

function verifyRequest(overrides = {}) {
  return {
    sessionId: "session-verify",
    requestId: "verify-1",
    noteMarkdown: "# Photosynthesis\n\n- Photosynthesis releases CO2 and consumes O2.\n",
    subjectHint: "Biology",
    ...overrides,
  };
}

test("the verification pass returns review findings and never replacement note content", async () => {
  let sent;
  const modelReply = {
    findings: [{
      claim: "Photosynthesis releases CO2 and consumes O2.",
      issue: "The gas direction is inverted.",
      suggestion: "Photosynthesis consumes CO2 and releases O2.",
      kind: "concept",
      severity: "warning",
      confidence: 0.95,
    }],
    checkedFormulas: ["6CO2 + 6H2O -> C6H12O6 + 6O2"],
    checkedConcepts: ["photosynthesis"],
    warnings: [],
  };
  const base = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async (_url, options) => {
      sent = JSON.parse(options.body);
      return new Response(JSON.stringify({
        choices: [{ message: { content: JSON.stringify(modelReply) } }],
      }), { status: 200, headers: { "content-type": "application/json" } });
    },
  });

  const response = await post(base, "/v1/notes/verify", verifyRequest());
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.source, "openrouter");
  assert.equal(body.sessionId, "session-verify");
  assert.equal(body.findings.length, 1);
  assert.equal(body.findings[0].kind, "concept");
  assert.equal(body.findings[0].severity, "warning");
  // The contract must not be able to carry a rewritten note.
  assert.ok(!Object.keys(body).some((key) => /markdown/i.test(key)));
  assert.equal(sent.response_format.json_schema.name, "note_verification");
  assert.ok(sent.messages[0].content.includes("reviewing, not editing"));
});

test("verification requires the client token and is covered by the daily budget", async () => {
  const base = await start({
    env: {
      OPENROUTER_API_KEY: "provider-secret",
      VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN,
      VOXBOX_DAILY_REQUEST_BUDGET: "1",
    },
    fetchImpl: async () => new Response(JSON.stringify({
      choices: [{ message: { content: JSON.stringify({ findings: [], checkedFormulas: [], checkedConcepts: [], warnings: [] }) } }],
    }), { status: 200, headers: { "content-type": "application/json" } }),
  });

  const unauthenticated = await post(base, "/v1/notes/verify", verifyRequest(), { token: null });
  assert.equal(unauthenticated.status, 401);

  assert.equal((await post(base, "/v1/notes/verify", verifyRequest())).status, 200);
  const overBudget = await post(base, "/v1/notes/verify", verifyRequest());
  assert.equal(overBudget.status, 429);
  assert.equal((await overBudget.json()).error.code, "daily_budget_exhausted");
});

test("verification rejects an oversized note and invalid model findings", async () => {
  const mock = await start({ env: { MOCK_AI: "1", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN } });
  const tooLong = await post(mock, "/v1/notes/verify", verifyRequest({ noteMarkdown: "x".repeat(24_001) }));
  assert.equal(tooLong.status, 400);
  const mocked = await post(mock, "/v1/notes/verify", verifyRequest());
  assert.equal(mocked.status, 200);
  assert.equal((await mocked.json()).source, "mock");

  const badSeverity = await start({
    env: { OPENROUTER_API_KEY: "provider-secret", VOXBOX_CLIENT_TOKEN: CLIENT_TOKEN },
    fetchImpl: async () => new Response(JSON.stringify({
      choices: [{ message: { content: JSON.stringify({
        findings: [{ claim: "a", issue: "b", suggestion: "c", kind: "concept", severity: "critical", confidence: 0.5 }],
        checkedFormulas: [], checkedConcepts: [], warnings: [],
      }) } }],
    }), { status: 200, headers: { "content-type": "application/json" } }),
  });
  const rejected = await post(badSeverity, "/v1/notes/verify", verifyRequest());
  assert.equal(rejected.status, 502);
  assert.equal((await rejected.json()).error.code, "invalid_upstream_response");
});

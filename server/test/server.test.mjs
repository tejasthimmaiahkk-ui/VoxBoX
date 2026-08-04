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

async function post(base, path, body) {
  return fetch(`${base}${path}`, {
    method: "POST",
    headers: { "content-type": "application/json" },
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
    env: { OPENAI_API_KEY: "test-only" },
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
    env: { OPENAI_API_KEY: "test-only-secret" },
    fetchImpl: async (url, options) => {
      request = { url, options };
      return new Response(JSON.stringify({ output_text: JSON.stringify(expected) }), {
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
  assert.deepEqual(await response.json(), { ...expected, source: "openai" });
  const body = JSON.parse(request.options.body);
  assert.equal(request.url, "https://api.openai.com/v1/responses");
  assert.equal(body.model, VISION_MODEL);
  assert.equal(body.text.format.strict, true);
  assert.equal(body.max_output_tokens, 2_000);
  assert.match(body.input[0].content[1].image_url, /^data:image\/jpeg;base64,/);
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
    env: { OPENAI_API_KEY: "test" },
    fetchImpl: async () => new Response(JSON.stringify({ output_text: JSON.stringify(invalid) }), { status: 200 }),
  });
  const response = await post(base, "/v1/board/extract", {
    imageBase64: jpeg.toString("base64"),
    mimeType: "image/jpeg",
  });
  assert.equal(response.status, 502);
  assert.equal((await response.json()).error.code, "invalid_upstream_response");
});

test("live transcription uses multipart diarized transcription and applies the chunk offset", async () => {
  let request;
  const base = await start({
    env: { OPENAI_API_KEY: "audio-secret" },
    fetchImpl: async (url, options) => {
      request = { url, options };
      return new Response(JSON.stringify({
        text: "Power rule.",
        duration: 2.5,
        segments: [{ id: "seg_1", speaker: "A", start: 0.25, end: 2.25, text: " Power rule. " }],
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
  assert.equal(result.segments[0].startMs, 30_250);
  assert.equal(result.segments[0].endMs, 32_250);
  assert.equal(result.source, "openai");
  assert.equal(request.url, "https://api.openai.com/v1/audio/transcriptions");
  assert.equal(request.options.body.get("model"), TRANSCRIPTION_MODEL);
  assert.equal(request.options.body.get("response_format"), "diarized_json");
  assert.equal(request.options.body.get("chunking_strategy"), "auto");
  assert.equal(request.options.headers.Authorization, "Bearer audio-secret");
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
    env: { OPENAI_API_KEY: "note-secret" },
    fetchImpl: async (_url, options) => {
      calls += 1;
      upstreamBody = JSON.parse(options.body);
      return new Response(JSON.stringify({ output_text: JSON.stringify(expected) }), { status: 200 });
    },
  });
  const first = await post(base, "/v1/notes/refine", requestBody);
  const second = await post(base, "/v1/notes/refine", requestBody);
  assert.equal(first.status, 200);
  assert.equal(second.status, 200);
  assert.deepEqual(await first.json(), { ...expected, source: "openai" });
  assert.deepEqual(await second.json(), { ...expected, source: "openai" });
  assert.equal(calls, 1);
  assert.equal(upstreamBody.model, NOTE_MODEL);
  assert.equal(upstreamBody.store, false);
  assert.equal(upstreamBody.text.format.strict, true);
  assert.match(upstreamBody.input[0].content[0].text, /Never silently correct/);
});

test("note refinement rejects empty evidence and invalid revision relationships", async () => {
  const base = await start({ env: { MOCK_AI: "1" } });
  const empty = await post(base, "/v1/notes/refine", noteRequest({ transcriptSegments: [] }));
  assert.equal(empty.status, 400);

  const live = await start({
    env: { OPENAI_API_KEY: "test" },
    fetchImpl: async () => new Response(JSON.stringify({
      output_text: JSON.stringify({
        requestId: "request-1",
        sessionId: "session-1",
        baseRevision: 0,
        nextRevision: 9,
        title: "Wrong revision",
        markdown: "# Wrong",
        corrections: [],
        consumedEvidenceIds: ["segment-1"],
        warnings: [],
      }),
    }), { status: 200 }),
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
    env: { OPENAI_API_KEY: "note-secret" },
    fetchImpl: async (_url, options) => {
      calls += 1;
      upstreamBody = JSON.parse(options.body);
      return new Response(JSON.stringify({ output_text: JSON.stringify(expected) }), { status: 200 });
    },
  });

  const first = await post(base, "/v1/notes/refine", requestBody);
  const replay = await post(base, "/v1/notes/refine", requestBody);
  assert.equal(first.status, 200);
  assert.equal(replay.status, 200);
  assert.deepEqual(await first.json(), { ...expected, source: "openai" });
  assert.deepEqual(await replay.json(), { ...expected, source: "openai" });
  assert.equal(calls, 1);
  assert.equal(upstreamBody.max_output_tokens, 2_000);
  assert.equal(upstreamBody.text.format.name, "note_delta");
  const providerRequest = JSON.parse(upstreamBody.input[1].content[0].text);
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
    env: { OPENAI_API_KEY: "note-secret" },
    fetchImpl: async () => {
      calls += 1;
      return new Response(JSON.stringify({ output_text: JSON.stringify(expected) }), { status: 200 });
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
    env: { OPENAI_API_KEY: "note-secret" },
    fetchImpl: async () => new Response(JSON.stringify({
      output_text: JSON.stringify({
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
      }),
    }), { status: 200 }),
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

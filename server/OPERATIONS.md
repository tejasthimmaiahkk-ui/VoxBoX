# VoxBox operations runbook

Day-to-day management of the deployed proxy and the installed app. `DEPLOYMENT.md` covers the
first-time setup; this file covers everything after that.

**Your setup**

| Thing | Value |
| --- | --- |
| Render service | `voxbox-proxy` |
| Proxy URL | `https://voxbox-proxy.onrender.com` |
| Repo | `D:\CollegeProject` (GitHub `tejasthimmaiahkk-ui/VoxBoX`, branch `main`) |
| Android package | `me.thimmaiah.voxbox` |
| Test device | `5dfb3db8` (Redmi 2411DRN47I, Android 16) |

If `adb` is not on your PATH, use the full path:
`C:\Users\tejas\AppData\Local\Android\Sdk\platform-tools\adb.exe`

---

## Jump to what you need

| I need to… | Section |
| --- | --- |
| Wake the server before a demo | [1](#1-wake-the-server-before-a-demo) |
| Check the server is healthy and how much budget is used | [2](#2-check-health-and-budget) |
| Rotate the client token | [3](#3-rotate-the-client-token) |
| Rotate the OpenRouter key | [4](#4-rotate-the-openrouter-key) |
| React to a suspected leak, right now | [5](#5-suspected-leak-emergency-steps) |
| Change the daily budget or rate limit | [6](#6-change-budget-and-rate-limits) |
| Change which AI model is used | [7](#7-change-a-model) |
| Rebuild and reinstall the app | [8](#8-rebuild-and-reinstall-the-app) |
| Work out why the app showed a warning | [9](#9-error-codes-and-what-they-mean) |
| Read server logs | [10](#10-read-the-logs) |
| Roll back a bad deploy | [11](#11-roll-back-a-deploy) |
| Go back to local development | [12](#12-switch-back-to-local-development) |

---

## 1. Wake the server before a demo

The Render free tier sleeps after ~15 minutes idle. The first request then takes 30–60 seconds,
which in a viva looks like the app has frozen.

**Do this 2 minutes before you present:**

```bash
curl -s https://voxbox-proxy.onrender.com/health
```

Repeat if you then sit idle for more than ten minutes. Keep the browser tab
`https://voxbox-proxy.onrender.com/health` open and refresh it once before you start — same effect,
no terminal needed.

`/health` is deliberately unauthenticated so this works from anywhere, and so uptime pingers can
reach it.

---

## 2. Check health and budget

```bash
curl -s https://voxbox-proxy.onrender.com/health
```

Expected:

```json
{"status":"ok","mode":"live","models":{...},"retention":"in-memory-forwarding-only",
 "budget":{"day":"2026-08-04","used":0,"limit":1500}}
```

Read it as:

- `"mode":"live"` — real models are being called. If it says `"mock"`, `MOCK_AI` is still set in
  Render and nothing real is happening. Remove that variable.
- `budget.used` vs `budget.limit` — billable calls so far today, UTC. Resets at 00:00 UTC.
- `budget.used` resets to 0 whenever the service restarts or wakes from sleep, because the counter is
  in memory. Treat it as a floor, not an exact total. **OpenRouter's own dashboard is the
  authoritative spend figure.**

---

## 3. Rotate the client token

Do this after any demo where the token was shared, after this chat, or if anything feels off.
Budget about five minutes, most of it the APK rebuild.

### Step 1 — generate a new token

```bash
openssl rand -base64 32
```

Copy the whole output including any trailing `=`. You will paste it twice.

### Step 2 — update Render

1. Open the `voxbox-proxy` service → **Environment** in the left sidebar.
2. Find `VOXBOX_CLIENT_TOKEN` → **Edit** → paste the new value → **Save changes**.
3. Render redeploys automatically. Wait for the deploy to go green (about a minute).

**From this moment the old APK is dead.** It will get `401` on every call and the app will show an
"AI credential rejected" banner. That is expected and is the point.

### Step 3 — confirm the server took it

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  -H "content-type: application/json" \
  -H "authorization: Bearer OLD-TOKEN" -d '{}' \
  https://voxbox-proxy.onrender.com/v1/notes/refine
```

Must print `401`. If it prints `400`, the old token still works — the deploy has not finished, or the
save did not apply. Wait and retry.

### Step 4 — rebuild the app with the new token

```bash
cd D:\CollegeProject\VoxBox
./gradlew.bat assembleRelease -PVOXBOX_API_BASE_URL=https://voxbox-proxy.onrender.com -PVOXBOX_CLIENT_TOKEN=NEW-TOKEN
```

If the token has characters your shell dislikes, wrap it in single quotes:
`-PVOXBOX_CLIENT_TOKEN='NEW-TOKEN'`

The build fails fast if the token is under 24 characters or contains whitespace.

### Step 5 — reinstall

```bash
adb -s 5dfb3db8 install -r D:\CollegeProject\VoxBox\app\build\outputs\apk\release\app-release.apk
```

Reinstalling over the top keeps your saved notes. See [section 8](#8-rebuild-and-reinstall-the-app)
if the install is rejected.

### Step 6 — verify end to end

Open the app → **Live** → give it a title → **Start live session** → talk for ten seconds → **Stop
and finish note**. A transcript means the new token works.

> There is **no per-device revocation.** The token is shared by every APK built with it, so rotating
> invalidates every copy at once. That is the accepted trade-off of a shared token, and it is why the
> daily budget matters.

---

## 4. Rotate the OpenRouter key

Do this if the key was ever pasted somewhere it shouldn't be, or on a schedule.

1. **OpenRouter dashboard → Keys → Create key.** Copy it once; you cannot read it again later.
2. **Render → `voxbox-proxy` → Environment →** edit `OPENROUTER_API_KEY` → paste → **Save changes**.
3. Wait for the redeploy to go green.
4. Verify a real call still works:

   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" -X POST \
     -H "content-type: application/json" \
     -H "authorization: Bearer YOUR-CLIENT-TOKEN" -d '{}' \
     https://voxbox-proxy.onrender.com/v1/notes/refine
   ```

   `400` means the request reached provider validation, so the server is configured. A `503` with
   `provider_not_configured` means the key is missing or blank.
5. **Delete the old key in OpenRouter.** Do this only after step 4 passes, or you will take the
   service down.

**No app rebuild is needed.** The provider key never enters the APK — that is the whole point of the
proxy.

---

## 5. Suspected leak: emergency steps

If you think the client token has been shared publicly and you want it dead **now**:

1. **Render → Environment →** set `VOXBOX_DAILY_REQUEST_BUDGET` to `0` → **Save changes**.
   Every billable call now returns `429 daily_budget_exhausted`. Spend stops within a minute.
2. Rotate the token properly ([section 3](#3-rotate-the-client-token)).
3. Set the budget back to `1500`.
4. Check the damage in the OpenRouter dashboard, not in `/health` — the in-memory counter resets on
   restart and will understate it.

If the **OpenRouter key** leaked instead, that is more serious: delete it in the OpenRouter dashboard
immediately, then follow [section 4](#4-rotate-the-openrouter-key). Deleting the key stops all spend
regardless of what the proxy does.

---

## 6. Change budget and rate limits

Render → **Environment**. Edit, save, wait for the redeploy.

| Variable | Default | What it does |
| --- | --- | --- |
| `VOXBOX_DAILY_REQUEST_BUDGET` | `1500` | Hard stop on billable calls per UTC day. `0` blocks everything. |
| `VOXBOX_RATE_LIMIT_MAX` | `60` | Requests allowed per caller per window. |
| `VOXBOX_RATE_LIMIT_WINDOW_MS` | `60000` | Window length in milliseconds. |

Sizing guidance: one lecture-hour is roughly 180 audio chunks plus frames and note updates, so
**1500/day comfortably covers several lectures**. Measured cost is about **$0.18 per lecture-hour**,
so 1500 requests is on the order of a dollar or two of worst-case exposure.

Also set a spend limit in the OpenRouter dashboard. The proxy counts *requests*; only OpenRouter
counts *money*.

---

## 7. Change a model

Render → **Environment** → add or edit, then save.

| Variable | Current default |
| --- | --- |
| `VOXBOX_TRANSCRIPTION_MODEL` | `google/gemini-3.1-flash-lite` |
| `VOXBOX_VISION_MODEL` | `google/gemini-2.5-flash-lite` |
| `VOXBOX_NOTE_MODEL` | `openai/gpt-oss-120b` |
| `VOXBOX_VERIFY_MODEL` | falls back to the note model |

No rebuild needed — the app never sees model names. Confirm the change took with
`curl -s https://voxbox-proxy.onrender.com/health`, which echoes the configured models.

**Before switching, know what these defaults were chosen for.** They were measured against this
project's own contracts, not picked by price:

- The transcription model is the one that produced correct speaker labels **and** honest timestamps.
  A cheaper one reported 8 seconds of segments for a 16.7-second clip, which would corrupt every
  stored timestamp.
- The vision model produced a far better diagram crop (IoU 0.59 vs 0.32).
- The note model caught a planted factual error **and preserved the captured claim**. A cheaper one
  caught the same error but silently deleted the original claim, which breaks this project's rule
  that evidence is never rewritten without review.

A replacement must support audio or image input as appropriate **and** `structured_outputs`, or
requests will fail. The reasoning is recorded at the top of `server/server.mjs`.

---

## 8. Rebuild and reinstall the app

```bash
cd D:\CollegeProject\VoxBox
./gradlew.bat assembleRelease -PVOXBOX_API_BASE_URL=https://voxbox-proxy.onrender.com -PVOXBOX_CLIENT_TOKEN=YOUR-TOKEN
adb -s 5dfb3db8 install -r D:\CollegeProject\VoxBox\app\build\outputs\apk\release\app-release.apk
```

**If the install is rejected:**

| Message | Cause | Fix |
| --- | --- | --- |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | A debug build is installed; signatures differ | `adb -s 5dfb3db8 uninstall me.thimmaiah.voxbox` then install again. **This erases saved notes.** |
| `INSTALL_FAILED_USER_RESTRICTED` | HyperOS blocking the install | Enable **Install via USB** in Developer options. Plain `adb install` usually works where Gradle's installer does not. |

Permissions are requested in-app on first session. To pre-grant them for a demo:

```bash
adb -s 5dfb3db8 shell pm grant me.thimmaiah.voxbox android.permission.RECORD_AUDIO
adb -s 5dfb3db8 shell pm grant me.thimmaiah.voxbox android.permission.CAMERA
```

> The release APK is signed with the **debug keystore** so it installs without keystore setup. Fine
> for coursework and demos; **never publish it** — that key is shared and unprotected.

---

## 9. Error codes and what they mean

When something fails, the app writes a warning into the note and shows a banner. Each maps to a
proxy error code.

### Your setup is wrong

| Code | Status | Meaning | Fix |
| --- | --- | --- | --- |
| `unauthorized` | 401 | App's token does not match the server's | Token was rotated on one side only. Rebuild the APK ([3](#3-rotate-the-client-token)). |
| `client_auth_not_configured` | 503 | Server has no `VOXBOX_CLIENT_TOKEN` | Set it in Render Environment. |
| `provider_not_configured` | 503 | Server has no `OPENROUTER_API_KEY` | Set it in Render Environment. |

### You have hit a limit

| Code | Status | Meaning | Fix |
| --- | --- | --- | --- |
| `daily_budget_exhausted` | 429 | Proxy's own daily cap reached | Wait for 00:00 UTC, or raise `VOXBOX_DAILY_REQUEST_BUDGET`. |
| `rate_limited` | 429 | Too many requests too fast from one caller | Wait the reported seconds. Raise `VOXBOX_RATE_LIMIT_MAX` if it is a real workload. |
| `transcription_quota_exhausted` (also `vision_`, `note_`) | 429 | **OpenRouter account** is out of credit | Top up OpenRouter. The app will not retry — retrying cannot help. |
| `transcription_rate_limited` (also `vision_`, `note_`) | 429 | OpenRouter is throttling | Transient. The app retries automatically. |

### Provider or model trouble

| Code | Status | Meaning | Fix |
| --- | --- | --- | --- |
| `transcription_auth_error` (also `vision_`, `note_`) | 502 | OpenRouter rejected the server's key | Key is wrong, deleted or expired. See [4](#4-rotate-the-openrouter-key). |
| `upstream_output_truncated` | 502 | Model hit its output limit mid-answer | Usually a very dense board. Transient; retry. |
| `upstream_generation_failed` | 502 | Upstream aborted mid-generation | Transient. If persistent, the configured model may not support structured output. |
| `invalid_upstream_response` | 502 | Model returned something unusable | Transient. If persistent, check a recently changed model. |
| `<kind>_request_rejected` | 502 | The model refused the request itself | Usually an unsupported model for that input type. Check [section 7](#7-change-a-model). |

### Nothing is broken

| What you see | Meaning |
| --- | --- |
| First call takes 30–60 s | Free tier waking. See [1](#1-wake-the-server-before-a-demo). |
| "No clear speech was found…" | Silence or inaudible audio. Not an error. |
| "unrecovered WAV file(s) remain private" | Audio was kept because its transcript never committed. Recover or delete it from the Live setup screen. |

**Captured audio is never thrown away on failure.** Retained WAVs are listed on the Live setup screen
with a Recover and a Delete button. Recovering re-transcribes and appends a labelled section to the
original note.

---

## 10. Read the logs

Render → `voxbox-proxy` → **Logs** in the left sidebar. **Live tail** follows in real time.

Useful searches:

- `provider failure` — every classified upstream failure, with status, type, code and request id
- `listening` — confirms a successful start after deploy

The proxy logs **only** that classification. It never logs request bodies, audio, images, note
content, the API key or the client token — by design, so the logs stay safe to screenshot for your
report.

---

## 11. Roll back a deploy

Render → `voxbox-proxy` → **Events**. Find the last deploy that says **live**, click **Rollback**.

This reverts the running code only. Environment variables are **not** rolled back, so if you broke it
by editing a variable, fix the variable instead.

---

## 12. Switch back to local development

The hosted proxy and the local workflow coexist; debug builds always use loopback.

```bash
cd D:\CollegeProject\server
MOCK_AI=1 node server.mjs
```

In a second terminal:

```bash
adb -s 5dfb3db8 reverse tcp:8787 tcp:8787
cd D:\CollegeProject\VoxBox
./gradlew.bat assembleDebug
adb -s 5dfb3db8 install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds ignore `VOXBOX_API_BASE_URL` and always target `http://127.0.0.1:8787`. A mock proxy
with no token accepts unauthenticated calls, so no token is needed locally.

You must uninstall the release build first if the signatures clash — see [section 8](#8-rebuild-and-reinstall-the-app).

To run the opt-in device scenarios, see the procedure note in `docs/TEST_PLAN.md`; Gradle's
`connectedDebugAndroidTest` does not work on this handset.

---

## What this setup does and does not protect

**Protected.** The OpenRouter key never leaves the server. Unauthenticated callers get `401` before
any provider call. No caller can exceed the rate limit, and nobody can exceed the daily budget.

**Not protected.** The client token is compiled into the APK, and an APK is a zip file — anyone with
your APK can extract it in about a minute. It deters casual abuse of a URL someone stumbles across;
it is **not** a real secret. The daily budget is what actually bounds your exposure.

Say it that way in your report. Do not describe the backend as secured by the token. Per-user sign-in
with revocable per-device tokens is the real fix and needs an account model this project does not
have.

# Deploying the VoxBox proxy

Goal: ordinary wireless use with **no USB cable, no `adb reverse` and no laptop proxy terminal**,
while the OpenRouter key stays on the server and never enters the APK.

```
Phone (Wi-Fi or mobile data) → HTTPS proxy (holds the key) → OpenRouter → model
```

Everything below is done by you. Claude cannot create accounts, deploy, or handle your key.

---

## 1. What you need before starting

| Item | Where it lives | Notes |
| --- | --- | --- |
| OpenRouter API key | Host's secret store only | Never in Git, Gradle, the APK, or chat |
| Client token | Host's secret store **and** the release build command | Generate a fresh one, see below |
| A host account | Render, Google Cloud Run, or Fly.io | All have a $0 tier adequate for a demo |

Generate the client token:

```bash
openssl rand -base64 32
```

Keep it somewhere you can paste twice: once into the host, once into the release build. It is not a
high-value secret (see the honesty note at the bottom), but it must not be committed.

---

## 2. Deploy

### Option A — Render (simplest)

1. Push this repository to GitHub.
2. In Render, choose **New → Blueprint** and point it at the repo. It reads `render.yaml` from the
   **repository root**.
3. Render prompts for `OPENROUTER_API_KEY` and `VOXBOX_CLIENT_TOKEN` because both are marked
   `sync: false`. Paste them there.
4. Deploy. Your URL will look like `https://voxbox-proxy.onrender.com`.

> **Both `render.yaml` and `Dockerfile` live at the repository root, and that is deliberate.**
> Render resolves the Dockerfile path from the repo root, and — importantly — a service that already
> exists keeps the build settings it was created with. Editing the blueprint afterwards does not
> reliably re-sync them, so a Dockerfile nested in `server/` keeps failing with
> `failed to read dockerfile: open Dockerfile: no such file or directory` even after the blueprint
> looks correct. A root Dockerfile matches Render's defaults, so it works whether or not the
> blueprint is re-read. `.dockerignore` keeps the context to a few kilobytes by excluding the Android
> project, evidence and docs.
>
> If a build still fails with that message, the service is holding a stale path. Fix it in
> **Settings → Build & Deploy**: set *Dockerfile Path* to `./Dockerfile` and *Docker Build Context
> Directory* to `.`, then use **Manual Deploy → Deploy latest commit**.

The free tier sleeps after about 15 minutes idle and takes 30–60 seconds to wake. See step 5.

### Option B — Google Cloud Run

```bash
cd server
gcloud run deploy voxbox-proxy \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --set-secrets OPENROUTER_API_KEY=openrouter-key:latest,VOXBOX_CLIENT_TOKEN=voxbox-token:latest
```

`--allow-unauthenticated` refers to Google's own IAM layer. The proxy still enforces its own bearer
token, so the endpoint is not open. Create the two secrets in Secret Manager first. Cloud Run scales
to zero and has a far gentler cold start than Render's free tier.

### Option C — Fly.io

```bash
cd server
fly launch --no-deploy
fly secrets set OPENROUTER_API_KEY=... VOXBOX_CLIENT_TOKEN=...
fly deploy
```

---

## 3. Verify the deployment before touching the app

```bash
# Should return 200 with mode "live".
curl -s https://YOUR-URL/health

# Should return 401: the endpoint is not an open relay.
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  -H "content-type: application/json" -d '{}' \
  https://YOUR-URL/v1/notes/refine

# Should return 400, not 401: the token was accepted and the empty body was rejected.
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  -H "content-type: application/json" \
  -H "authorization: Bearer YOUR-TOKEN" -d '{}' \
  https://YOUR-URL/v1/notes/refine
```

If the second command returns anything other than `401`, **stop and fix it before continuing** —
anyone who finds the URL could spend your OpenRouter credit.

Check `/health` also reports `"mode":"live"`. If it says `mock`, `MOCK_AI` is still set and no real
model is being called.

---

## 4. Build the release APK against it

```bash
cd VoxBox
./gradlew.bat assembleRelease \
  -PVOXBOX_API_BASE_URL=https://YOUR-URL \
  -PVOXBOX_CLIENT_TOKEN=YOUR-TOKEN
```

Both are required. The build fails if the URL is missing, not HTTPS, or carries credentials, a query
or a fragment, and if the token is missing, shorter than 24 characters, or contains whitespace.

The release build is signed with the local **debug keystore** so it installs without any keystore
setup. That is a coursework and device-testing convenience, not a distribution setup: the debug
keystore is shared and unprotected, so this APK must never be published. A real release needs its own
keystore with the password supplied outside version control.

Uninstall any debug build first — the signatures differ and the install will otherwise be rejected:

```bash
adb uninstall me.thimmaiah.voxbox
```

Install it directly, because this handset blocks Gradle's split-session installer:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

That `adb install` is the **last** time you need the cable. After it, the app talks to the hosted
proxy over Wi-Fi or mobile data.

Debug builds are unaffected and still use `http://127.0.0.1:8787` with `adb reverse`.

---

## 5. Before a demo or viva

On a sleeping free tier the first request after idle takes 30–60 seconds, which looks like a hang
mid-demo. Warm it up:

```bash
curl -s https://YOUR-URL/health
```

Do that a minute before you start, and again if you have been idle for more than ten minutes. On
Cloud Run this matters far less. A free uptime pinger hitting `/health` every 10 minutes also works;
`/health` is unauthenticated precisely so pingers and platform health checks can reach it.

---

## 6. Rotating the client token

1. Generate a new token.
2. Update `VOXBOX_CLIENT_TOKEN` in the host and let it redeploy.
3. Rebuild and reinstall the APK with the new `-PVOXBOX_CLIENT_TOKEN`.

Old APKs stop working at step 2. There is no per-device revocation; the token is shared by every
build that was compiled with it.

---

## 7. Cost control

The proxy enforces its own ceilings, independent of OpenRouter:

| Variable | Default | Effect |
| --- | --- | --- |
| `VOXBOX_DAILY_REQUEST_BUDGET` | 1500 | Hard stop on billable calls per UTC day |
| `VOXBOX_RATE_LIMIT_MAX` | 60 | Requests per caller per window |
| `VOXBOX_RATE_LIMIT_WINDOW_MS` | 60000 | Window length |

`GET /health` reports the day's usage against the budget. Measured cost is roughly **$0.18 per
lecture-hour** across transcription, board vision and note refinement. Also set a spend limit in the
OpenRouter dashboard as a second line of defence — the proxy counts requests, not currency.

Both limiters are in-memory and per instance. They reset on restart and do not coordinate across
replicas, which is fine for one free-tier instance and must be replaced with shared state before
running more than one.

---

## What this does and does not protect

**Protected.** The OpenRouter key never leaves the server. Unauthenticated callers get `401` before
any provider call. A single caller cannot exceed the rate limit, and nobody can exceed the daily
budget.

**Not protected.** The client token is compiled into the APK, and an APK is a zip file — anyone who
has your APK can extract the token in about a minute. It deters casual abuse of a URL someone
stumbles across; it is **not** a real secret. The daily budget is what actually bounds your
exposure, which is why it should be set to something you would be comfortable losing.

Describe it that way in the report. Do not describe the backend as secured by the token.

Per-user sign-in with revocable per-device tokens is the real fix, and it needs an account model
this project does not currently have.

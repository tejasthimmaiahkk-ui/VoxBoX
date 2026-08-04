# VoxBox organized UI and board-capture evidence — 2026-07-30

## Claim boundary

This milestone verifies an organized Notes/Speak/Board UI, real rear-camera preview, user-triggered still capture, editable review, explicit local save and persistence after force-stop/relaunch on Redmi model `2411DRN47I`, Android 16 / API 36.

“Live” means the CameraX preview remains live while the user aims. VoxBox does not continuously capture or upload video. The proxy run used deterministic mock mode, which did not analyze the image. No real OpenAI request was made, so these files are not AI-accuracy evidence. The offline OCR fallback path ran with the proxy stopped, but the actual captured frame was black/dark and contained no legible text; that is routing/state evidence, not OCR-accuracy evidence.

## Canonical screenshots

- `speak-redesign-device.png` — organized **Speak** destination rendered on the physical device.
- `board-live.png` — real visible rear-camera feed before the final ready-state/status-race correction.
- `final-board-live.png` — final patched `Aim at the board or projector, then capture one frame` ready state and live-camera indicator; the photographed scene itself is dark.
- `final-board-review.png` — editable review explicitly labelled `Mock response — image not analyzed`.
- `board-saved.png` — saved-note detail for `Mock board capture`, including the local-save status and ordered board-derived blocks.
- `notes-after-relaunch.png` — Notes library after force-stop/relaunch, still listing `Mock board capture`.
- `offline-review-actual.png` — physical fallback result explicitly labelled `Offline OCR fallback`; the captured scene had no legible text.

Only the files above are canonical milestone screenshots. Intermediate automation captures are intentionally excluded from the evidence set.

## Automated evidence summary

- Android: `testDebugUnitTest`, `assembleDebug`, `lintDebug` and `assembleDebugAndroidTest` passed.
- Results: 36 unit tests, 0 failures/errors; lint 0 errors and 17 version/SDK update advisories; debug and Android-test APKs built.
- Proxy: 3/3 `node --test` cases passed using mock/fake transports with no live or billable request.
- Search: literal, case-insensitive title/block-content/block-label behavior has unit coverage. Physical search filtering input was not completed because the device rejected the synthetic text/key-input method.

## Security notes

- The key exposed in chat must be revoked; reducing device security does not make it safe.
- No pasted credential was stored in Android, this evidence folder or the repository.
- Android backup is disabled.
- The USB demo uses cleartext loopback only through debug configuration and `adb reverse tcp:8787 tcp:8787`. A release build requires an authenticated HTTPS backend.
- The new board note and all of its ordered blocks are inserted atomically in one Room transaction.

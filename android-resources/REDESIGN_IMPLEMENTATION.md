# VoxBox UI redesign — implementation plan

Written for Claude Code working inside `VoxBox/app/src/main/java/me/thimmaiah/voxbox`. Every path is relative to that package unless stated otherwise. The design reference is `VoxBox Redesign.dc.html` in this project; the faithful capture of what ships today is `VoxBox Current UI.dc.html`.

---

## 1. What changes and why

Today `CaptureSessionScreen.kt` carries five numbered setup steps, running-session telemetry, recovery of unrecovered audio, refinement failures and review flags in one scrolling column. `BoardCaptureScreen.kt` duplicates a second camera flow in a separate tab. `VoxBoxScreen.kt` owns the note list and the note detail with a preview-only Markdown renderer.

The redesign makes four moves:

1. **Four destinations instead of three.** `Home / Capture / Library / Settings`. Live and Board merge into one Capture flow with a mode switch, so there is one camera implementation instead of two.
2. **Durable configuration leaves the capture path.** Recovery, connection, appearance, privacy, export defaults and evidence claims become Settings sub-pages. Capture keeps only the four decisions a user makes before each session: mode, destination, note style, and (board only) camera interval/sensitivity behind a collapsed disclosure.
3. **The note detail becomes a reader.** Outline, reading controls, find, inline block editing, evidence expansion, review flags, a diagram lightbox, focus mode, and share.
4. **The board camera gains zoom.** Pinch plus a vertical slider on the preview, with a live magnification read-out.

---

## 2. Colour system

Five colours are mandatory. Status meanings are fixed and never follow the accent; the accent is user-switchable between the four hues.

### 2.1 `ui/theme/Color.kt` — replace the file body

```kotlin
package me.thimmaiah.voxbox.ui.theme

import androidx.compose.ui.graphics.Color

// Accent hues — user-selectable, used for primary actions and selection.
val VbBlue        = Color(0xFF4C7DFF)
val VbBlueDark    = Color(0xFF2A5BE0)   // light-theme variant, contrast-safe on white
val VbGreen       = Color(0xFF22B77E)
val VbGreenDark   = Color(0xFF0E8A57)
val VbOrange      = Color(0xFFF79320)
val VbOrangeDark  = Color(0xFFC96A00)
val VbRed         = Color(0xFFF2555B)
val VbRedDark     = Color(0xFFD8353C)

// On-accent foregrounds. Orange needs a dark foreground to pass contrast.
val VbOnAccentLight = Color(0xFFFFFFFF)
val VbOnAccentDark  = Color(0xFF1B1206)

// Dark ground
val VbBgDark   = Color(0xFF0B0C0F)
val VbSfDark   = Color(0xFF15171C)
val VbSf2Dark  = Color(0xFF1E222A)
val VbFgDark   = Color(0xFFF3F5F8)
val VbFg2Dark  = Color(0xFFA6AEBB)
val VbFg3Dark  = Color(0xFF6C7480)
val VbLineDark = Color(0x1AFFFFFF)      // white @ 10%

// Light ground
val VbBgLight   = Color(0xFFF4F5F3)
val VbSfLight   = Color(0xFFFFFFFF)
val VbSf2Light  = Color(0xFFEAECF0)
val VbFgLight   = Color(0xFF12141A)
val VbFg2Light  = Color(0xFF5A6270)
val VbFg3Light  = Color(0xFF8A929E)
val VbLineLight = Color(0x1C0C0E12)     // near-black @ 11%

// Live session ground — always dark, both themes.
val VbLiveBg     = Color(0xFF07080A)
val VbLiveSf     = Color(0xFF0F1116)
val VbLiveLine   = Color(0x14FFFFFF)
val VbLiveFg     = Color(0xFFF3F5F8)
val VbLiveFg2    = Color(0xFF8E96A3)
val VbLiveFgBody = Color(0xFFDDE2EA)
```

### 2.2 Fixed status semantics

Do not tie these to the accent.

| Role | Token | Meaning in UI |
| --- | --- | --- |
| Saved / verified | `VbGreen` (`VbGreenDark` in light) | "SAVED LOCALLY" pill, device-verified rows, permission ready |
| Needs review | `VbOrange` (`VbOrangeDark` in light) | Review flags, unrecovered audio badge, not-yet-evaluated rows |
| Recording / destructive / failure | `VbRed` (`VbRedDark` in light) | LIVE pill, stop button, delete, refinement failures |
| App accent | user choice of the four | Primary buttons, selected state, focus rings, links |
| Ground | `VbBgDark` / near-black | Page background in dark; the live session ground in both themes |

### 2.3 `ui/theme/Theme.kt` — accent-aware scheme

Add an accent enum and build the `ColorScheme` from it. Keep the existing `VoxBoxTheme` name so no call sites change.

```kotlin
enum class VbAccent { Blue, Green, Orange, Red }

@Immutable
data class VbStatus(
    val saved: Color,
    val review: Color,
    val danger: Color,
    val line: Color,
    val fg2: Color,
    val fg3: Color,
)

val LocalVbStatus = staticCompositionLocalOf { VbStatus(VbGreen, VbOrange, VbRed, VbLineDark, VbFg2Dark, VbFg3Dark) }

private fun accentColor(a: VbAccent, dark: Boolean) = when (a) {
    VbAccent.Blue   -> if (dark) VbBlue else VbBlueDark
    VbAccent.Green  -> if (dark) VbGreen else VbGreenDark
    VbAccent.Orange -> if (dark) VbOrange else VbOrangeDark
    VbAccent.Red    -> if (dark) VbRed else VbRedDark
}

private fun onAccent(a: VbAccent) =
    if (a == VbAccent.Orange) VbOnAccentDark else VbOnAccentLight

@Composable
fun VoxBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: VbAccent = VbAccent.Blue,
    content: @Composable () -> Unit,
) {
    val acc = accentColor(accent, darkTheme)
    val scheme = if (darkTheme) darkColorScheme(
        primary = acc, onPrimary = onAccent(accent),
        primaryContainer = acc.copy(alpha = 0.18f), onPrimaryContainer = acc,
        background = VbBgDark, onBackground = VbFgDark,
        surface = VbSfDark, onSurface = VbFgDark,
        surfaceVariant = VbSf2Dark, onSurfaceVariant = VbFg2Dark,
        outlineVariant = VbLineDark,
        error = VbRed, onError = Color.White,
    ) else lightColorScheme(
        primary = acc, onPrimary = onAccent(accent),
        primaryContainer = acc.copy(alpha = 0.12f), onPrimaryContainer = acc,
        background = VbBgLight, onBackground = VbFgLight,
        surface = VbSfLight, onSurface = VbFgLight,
        surfaceVariant = VbSf2Light, onSurfaceVariant = VbFg2Light,
        outlineVariant = VbLineLight,
        error = VbRedDark, onError = Color.White,
    )
    val status = VbStatus(
        saved  = if (darkTheme) VbGreen else VbGreenDark,
        review = if (darkTheme) VbOrange else VbOrangeDark,
        danger = if (darkTheme) VbRed else VbRedDark,
        line   = if (darkTheme) VbLineDark else VbLineLight,
        fg2    = if (darkTheme) VbFg2Dark else VbFg2Light,
        fg3    = if (darkTheme) VbFg3Dark else VbFg3Light,
    )
    CompositionLocalProvider(LocalVbStatus provides status) {
        MaterialTheme(colorScheme = scheme, typography = VoxBoxTypography, content = content)
    }
}
```

Remove `dynamicColor` if present. Dynamic colour would break the fixed status semantics.

### 2.4 Shape and spacing tokens

Add to `ui/theme/Theme.kt`:

```kotlin
object VbShape {
    val pill  = RoundedCornerShape(percent = 50)
    val card  = RoundedCornerShape(24.dp)
    val cardL = RoundedCornerShape(28.dp)
    val row   = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val media = RoundedCornerShape(18.dp)
}

object VbSpace {
    val screenH = 20.dp   // horizontal screen padding
    val gap     = 12.dp   // between rows in a group
    val section = 20.dp   // between sections
    val cardPad = 16.dp
}
```

Every touch target is at least 44.dp. Bottom-nav items are 60x32.dp pills inside 64.dp rows; icon buttons are 40.dp with a 48.dp minimum hit box via `Modifier.minimumInteractiveComponentSize()`.

---

## 3. Type

`ui/theme/Type.kt`. Two families: a display serif for screen titles, a grotesque for everything else. Add the fonts to `app/src/main/res/font/`.

| Role | Family | Size / line height / weight |
| --- | --- | --- |
| `displayLarge` — screen titles ("Ready to capture") | Caprasimo (or Playfair Display SemiBold if you prefer a Google-hosted fallback) | 34.sp / 37.sp / Normal |
| `displayMedium` — sub-page titles | same serif | 30.sp / 33.sp / Normal |
| `titleLarge` — card titles | Figtree | 19.sp / 24.sp / Bold |
| `titleMedium` — row titles | Figtree | 15.sp / 20.sp / SemiBold |
| `bodyLarge` — reader body | Figtree, size from the reading setting | 16.sp / 26.sp / Normal |
| `bodyMedium` — supporting copy | Figtree | 13.sp / 20.sp / Normal |
| `bodySmall` — captions, meta | Figtree | 12.sp / 17.sp / Normal |
| `labelLarge` — buttons | Figtree | 14.sp / 14.sp / SemiBold |
| `labelSmall` — pills, uppercase eyebrows | Figtree | 11.sp / 11.sp / Bold, `letterSpacing = 0.1.em` |
| Code / formulas / clocks | `FontFamily.Monospace`, tabular figures | 14.sp / 22.sp / SemiBold |

Reading size in the reader multiplies `bodyLarge` only: Small 14.sp/22.sp, Default 16.sp/26.sp, Large 18.sp/31.sp, Huge 20.sp/35.sp.

---

## 4. Navigation

### 4.1 New file: `nav/VoxBoxNav.kt`

```kotlin
sealed interface VbRoute {
    data object Home : VbRoute
    data object Capture : VbRoute
    data object Library : VbRoute
    data object Settings : VbRoute
    data object Live : VbRoute                       // full-screen, no bottom bar
    data class Note(val id: String) : VbRoute        // full-screen reader
    data object SetAppearance : VbRoute
    data object SetAudioRecovery : VbRoute
    data object SetConnection : VbRoute
    data object SetPrivacy : VbRoute
    data object SetExport : VbRoute
    data object SetAbout : VbRoute
    data object Onboarding : VbRoute
}
```

Bottom bar shows only on `Home`, `Capture`, `Library`, `Settings`. `Live` and `Note` are full-screen; `Live` additionally blocks back navigation with a confirm dialog because leaving stops capture.

### 4.2 `MainActivity.kt`

Wrap in `VoxBoxTheme(darkTheme = prefs.theme.isDark, accent = prefs.accent)` read from a `SettingsRepository` (§8). Keep the existing single-activity setup; add `NavHost` with the routes above and `enableEdgeToEdge()`.

### 4.3 Bottom bar composable — `ui/VbBottomBar.kt`

Four items. The selected item animates its pill container in with `animateFloatAsState` on scale (0.82 → 1.04 → 1.0, `spring(dampingRatio = 0.55f, stiffness = 420f)`), and the label crossfades weight Medium → Bold. Icons come from `ui/VoxBoxIcons.kt`; add `VbIcons.Home` and `VbIcons.Sliders` following the existing hand-drawn `Path` style so nothing depends on the extended icon artifact.

---

## 5. Screens

### 5.1 Home — new file `home/HomeScreen.kt`

Order top to bottom:

1. **App row.** Wordmark in `displayMedium` at 22.sp, plus a theme toggle icon button on the right that rotates 35° on press (`animateFloatAsState`) and crossfades the sun/moon path.
2. **Date eyebrow** (`labelSmall`, uppercase, `fg3`) and **`displayLarge` greeting**.
3. **Start card.** `VbShape.cardL`, `surface`, 1.dp `outlineVariant` border, elevation via `Modifier.shadow(18.dp, ambientColor = …)`.
   - Segmented mode switch (Voice / Board) on `surfaceVariant`, pill thumb animated with `animateDpAsState(spring(stiffness = 500f))`.
   - A 92.dp circular primary button with two `vbRing` pulses behind it: `rememberInfiniteTransition` driving scale 1 → 1.85 and alpha 0.55 → 0, 2600 ms, second ring offset 900 ms. Press scale 0.94 via `interactionSource`.
   - Mode blurb in `bodyMedium`, and a text button "Session options" → `Capture`.
4. **Review-flag banner**, only when `flagCount > 0`. Border and tint from `status.review`. Tapping opens the note's reader scrolled to its flags section.
5. **Continue a note** — two most recent notes, `VbShape.card` rows, 42.dp initial circle, `animateFloatAsState` translationY -2.dp on press.

Entry animation: each block `AnimatedVisibility` with `slideInVertically(initialOffsetY = { 18 }) + fadeIn()`, `tween(500, delayMillis = index * 60, easing = CubicBezierEasing(0.2f, 0.85f, 0.25f, 1f))`.

### 5.2 Capture setup — rewrite `session/CaptureSessionScreen.kt`

Delete the numbered step cards. Keep the existing `CaptureSessionViewModel` and `CaptureSessionModels.kt` state; only the composition changes. Sections, in order, each a plain `titleMedium` header with no card chrome:

1. **Mode** — two large tiles (Voice / Live board), 26.dp radius, selected tile gets `primaryContainer` fill, `primary` border 1.5.dp and `primary` content colour. `animateColorAsState(tween(300))` on all three.
2. **Where it goes** — segmented `New note` / `Continue`. New note reveals a pill text field; Continue reveals a radio list of recent notes. Swap with `AnimatedContent` + `fadeIn/fadeOut(tween(300))`.
3. **How it is written** — two radio cards: Runnable notes / Verbatim, copy exactly as in the prototype (the deduplication and flagging promise is a product claim; do not soften it).
4. **Camera options** — board mode only, collapsed `ExpandableCard`. Header shows a live summary: `"Every ${interval}s · ${threshold}% change to keep a frame"`. Expanded body holds the two sliders (interval 2–30 s, sensitivity 2–30 %) plus the retention note. Chevron rotates 180° with `animateFloatAsState(spring())`; body uses `AnimatedVisibility(expandVertically() + fadeIn())`.
5. **Start** — permission line with a `status.saved` dot, then a 58.dp full-width pill primary button.

What moves out of this file, and where it goes:

| Removed from capture | New home |
| --- | --- |
| Unrecovered audio list + recover/delete | `settings/AudioRecoveryScreen.kt` |
| Refinement failure list | `settings/AudioRecoveryScreen.kt` (second section) |
| Review flags | The note reader, inline at the flagged block |
| Folder chips + new-folder field | Library filter row; creation moves to a Library overflow action |
| Syllabus picker + import | `settings/SyllabusScreen.kt` if you keep it; otherwise a Capture overflow action. Not on the main path. |
| Proxy / provider status | `settings/ConnectionScreen.kt` |
| Evidence and claims copy | `settings/AboutScreen.kt` |

### 5.3 Live session — new file `session/LiveSessionScreen.kt`

Always dark (`VbLiveBg`), regardless of theme. Structure:

- **Header row (fixed):** collapse chevron (stops the session after confirming), a LIVE pill whose dot blinks `alpha 1 → 0.2 → 1` over 1400 ms, a monospace tabular clock, and `rev N` on the right.
- **Board mode preview:** square `AspectRatio(1f)`, 28.dp corners, holding the CameraX `PreviewView` (§6). Overlays: a 16.dp inset guide rectangle at white 20 %, a top hint pill, the zoom slider (right edge), a magnification read-out (bottom right), and kept/skipped counters (bottom left). A `vbScan` sweep — a 120.dp gradient band translating -8 % → 108 % over 3400 ms — reads as "watching" without implying continuous upload.
- **Voice mode preview:** seven 8.dp bars, each `scaleY` 0.22 → 1 over 1100 ms with 130 ms stagger, driven by one `rememberInfiniteTransition`. Replace with real amplitude from the recorder if `AudioRecord.read` RMS is already available; the animation is a placeholder for it, not a fake meter to keep.
- **Living note panel (fills remaining height):** "Living note" title, a queue pill (`primary` dot blinking, `"${n} queued"` / `"up to date"`), the accumulating note bullets, a divider, then the reverse-chronological evidence transcript with monospace timestamps and a diarization label. New items enter with `slideInHorizontally(initialOffsetX = { 16 }) + fadeIn()`, `tween(450)`.
- **Stop button (fixed):** full-width 58.dp pill, `status.danger`, "Stop and finish note". On completion navigate to `Note(id)` and show a snackbar: "Saved locally. Transcript evidence and crops kept."

`BoardCaptureScreen.kt` is deleted. Its single-shot capture becomes a long-press on the Home start button, routed through the same `CameraController`.

### 5.4 Library — rewrite the list half of `ui/VoxBoxScreen.kt` into `library/LibraryScreen.kt`

Title, a pill search field, a folder chip row (`All` plus folders, selected chip filled with `primary`), then notes grouped under `This week` / `Earlier` eyebrows. Each row: 44.dp initial circle, title, and a meta line. A flag count renders as a `status.review` pill. Row press lifts -2.dp and borders in `primary`.

### 5.5 Markdown reader — new file `reader/NoteReaderScreen.kt`

This replaces `ui/MarkdownNotePreview.kt`'s preview-only rendering. Keep the existing block model; add the reader chrome around it.

**Top bar** (sticky, `background` at 88 % with `Modifier.blur` or a `hazeChild` if you already depend on Haze): back, note title, then outline / `Aa` / find / share icon buttons. In focus mode the bar and the title block hide with `AnimatedVisibility(fadeIn/fadeOut(tween(300)))` and a small floating exit-focus FAB appears bottom-right.

**Find bar** slides in under the top bar when find is active: query field, `1 of 3` counter, close. Matches render as `background = primary.copy(alpha = 0.26f)` spans on the body text.

**Body blocks** — one composable per block type:

| Block | Rendering |
| --- | --- |
| Heading | `titleLarge`, 6.dp top spacing |
| Bullet | 5.dp `primary` dot, top-aligned to the first text line via `Modifier.padding(top = fontSize * 0.62f)` |
| Paragraph | `bodyLarge` at the reading size, `TextAlign.Start`, no justification |
| Code / formula | `surfaceVariant` fill, 18.dp radius, monospace `primary`, horizontal scroll, never wrapped |
| Diagram | `surface` card, 170.dp image, caption in `bodySmall`; tap opens the lightbox |
| Review flag | `status.review` tinted card: "Captured" vs "Suggested", a confidence figure, the contradiction note, and two actions — `Keep captured` (outline) and `Add annotation` (filled `status.review`). Neither action rewrites the captured line. |

**Evidence toggle.** Any block with backing transcript gets a small outlined `Evidence` chip. Expanded, it shows a 2.dp `primary` left rule with the timestamp, speaker label and the quoted utterance, `AnimatedVisibility(expandVertically() + fadeIn())`.

**Tap to edit.** Tapping a text block opens a bottom sheet with a `TextField` on the raw Markdown, a `MARKDOWN · REV n` badge, Cancel / Save, and the line: "Saving writes a new revision. The transcript evidence behind this line is not changed." Save bumps the revision and re-renders that block only.

**Reading sheet** (`Aa`): four size buttons, a Sans / Serif switch, and a Focus mode toggle. Persist to `SettingsRepository`.

**Outline sheet:** section list with the active section marked by a 3.dp `primary` rule; flag count shown as a `status.review` pill. Tapping scrolls via `LazyListState.animateScrollToItem` — do not use `scrollIntoView`-style hacks.

**Share sheet:** Markdown + diagrams (.zip), Markdown only, Copy as text.

**Lightbox:** full-bleed `rgba(3,4,6,0.94)` scrim, the crop scaled in with `scaleIn(initialScale = 0.82f, spring())`, a caption naming the source frame and timestamp, pinch-to-zoom on the image itself.

### 5.6 Settings — new files under `settings/`

`SettingsScreen.kt` is three grouped cards with uppercase eyebrows:

- **Capture** — `Unrecovered audio` (badge = count, `status.review`), `Connection & models`.
- **App** — `Appearance` (summary `"Dark · Blue accent"`, trailing accent dot), `Export defaults`.
- **Data** — `Privacy & storage` (with the on-device total), `About & evidence`.

Sub-pages, each with a back row and `titleMedium` header:

| File | Contents |
| --- | --- |
| `AppearanceScreen.kt` | Theme segmented (Dark/Light), a four-swatch accent row (blue/green/orange/red) with a check on the active swatch and an `onBackground` ring, reading-size segmented, and a live preview paragraph. Include the note that status colours never change. |
| `AudioRecoveryScreen.kt` | Unrecovered audio files (time, duration, size, why it failed, source note) with Recover / Delete. Recover appends a labelled "Recovered audio" section to the original note and does not re-run refinement — state that in the screen. Second section: refinement failures. |
| `ConnectionScreen.kt` | Proxy reachability with latency, the proxy URL, the "provider key never lives in the app" note, model routing rows (transcription / board extraction / note synthesis), measured cost per lecture-hour, and a Mock mode switch. |
| `PrivacyScreen.kt` | Storage total with a three-segment bar (crops / notes+transcripts / recovery audio) and a legend, `Keep raw frames` switch (default off), `Keep screen awake while capturing` switch, `Clear export cache`, and the foreground-only + backup-disabled note. |
| `ExportScreen.kt` | Radio: Markdown + assets (.zip) / Markdown only; `Include review flags` switch with "exported as a labelled section, never inline"; the 24-hour cache note. |
| `AboutScreen.kt` | Three status rows — green device-verified, accent live-contract, orange not-yet-evaluated — plus the speaker-focus and consent notes. Keep this copy conservative; it is a claims surface. |

Switch rows share one composable: label, supporting line, and a 46x28.dp track whose 22.dp knob animates with `animateDpAsState(spring(dampingRatio = 0.5f, stiffness = 600f))`.

### 5.7 Onboarding — new file `onboarding/OnboardingScreen.kt`

Three panes, `HorizontalPager`, dots, Skip:

1. What VoxBox does, and that capture is foreground-only.
2. Microphone (and camera, if board) permission request with the reason stated before the system dialog.
3. Accent + theme pick, so the first screen already looks like the user's choice.

Then a consent line the user must acknowledge: recording may require permission from their institution. Show once, gated on a `SettingsRepository` flag.

---

## 6. Camera zoom

CameraX exposes zoom through `CameraControl` and `ZoomState`. One controller serves both the live session and the single-shot capture.

New file `camera/VbCameraController.kt`:

```kotlin
class VbCameraController {
    private var camera: Camera? = null

    private val _zoom = MutableStateFlow(1f)          // linear ratio
    val zoom: StateFlow<Float> = _zoom
    private val _range = MutableStateFlow(1f to 8f)
    val range: StateFlow<Pair<Float, Float>> = _range

    fun bind(camera: Camera) {
        this.camera = camera
        camera.cameraInfo.zoomState.value?.let {
            _range.value = it.minZoomRatio to it.maxZoomRatio
            _zoom.value = it.zoomRatio
        }
    }

    /** fraction: 0f..1f from the slider or pinch accumulator. */
    fun setFraction(fraction: Float) {
        val (min, max) = _range.value
        val ratio = min + fraction.coerceIn(0f, 1f) * (max - min)
        camera?.cameraControl?.setZoomRatio(ratio)
        _zoom.value = ratio
    }

    fun nudge(scaleDelta: Float) {
        val (min, max) = _range.value
        val ratio = (_zoom.value * scaleDelta).coerceIn(min, max)
        camera?.cameraControl?.setZoomRatio(ratio)
        _zoom.value = ratio
    }

    fun tapToFocus(x: Float, y: Float, meteringFactory: MeteringPointFactory) {
        val point = meteringFactory.createPoint(x, y)
        camera?.cameraControl?.startFocusAndMetering(
            FocusMeteringAction.Builder(point).build()
        )
    }
}
```

Prefer `setZoomRatio` over `setLinearZoom`: the read-out must show real magnification (`"2.4×"`), and `linearZoom` is perceptually spaced, not a ratio.

**Pinch.** On the preview container:

```kotlin
Modifier.pointerInput(Unit) {
    detectTransformGestures { _, _, zoomChange, _ -> controller.nudge(zoomChange) }
}
```

**Slider.** Vertical, on the preview's right edge, 8.dp track with a 30.dp white thumb, bottom = min zoom. Use `pointerInput` + `detectDragGestures` and convert the y position to a fraction (`1f - y / height`); a `Slider` rotated 270° fights accessibility and hit testing. Track fill uses `primary`. The whole control needs a 44.dp-wide touch area even though the visible track is 8.dp.

**Read-out.** `"%.1f×"` on a translucent pill bottom-right, monospace tabular. Fade it to 40 % alpha 1.2 s after the last zoom change (`LaunchedEffect(zoom) { delay(1200); … }`).

**Bounds.** Clamp to `ZoomState.minZoomRatio`/`maxZoomRatio` — never a hardcoded 8×. On devices with an ultra-wide lens `min` is below 1.0; the slider must accept that. Label such positions `"0.6×"`, not `"1×"`.

**Frame differencing interaction.** Zoom changes the whole frame, so the change-detector will spike and accept a frame on the next tick. That is correct behaviour, but suppress the *first* comparison after a zoom settles (reset the reference frame) so one zoom does not produce several accepted frames. Do this in the existing frame-diff step, and keep the guarantee already in the code: a raw frame is deleted only after its note update and crops commit.

**Accessibility.** Give the slider `Modifier.semantics { progressBarRangeInfo = … ; contentDescription = "Camera zoom" }` and support D-pad/keyboard increments of 0.2×.

---

## 7. Animation

Target: expressive but never blocking. Nothing waits on an animation to become usable, and every one respects reduced-motion.

| # | Where | Motion | Spec |
| --- | --- | --- | --- |
| 1 | Screen enter | Rise + slight scale | `fadeIn() + slideInVertically { 14 } + scaleIn(0.985f)`, 380 ms, `CubicBezier(0.2, 0.85, 0.25, 1)` |
| 2 | Home blocks | Staggered rise | `tween(500, delay = i * 60)`, offset 18.dp |
| 3 | Bottom-nav selection | Pill pop | scale 0.82 → 1.04 → 1.0, `spring(0.55, 420)`; label weight crossfade |
| 4 | Record button | Two pulse rings | scale 1 → 1.85, alpha 0.55 → 0, 2600 ms linear-out, ring 2 offset 900 ms |
| 5 | Segmented switches | Thumb slide + colour | `animateDpAsState(spring(stiffness = 500f))` + `animateColorAsState(tween(280))` |
| 6 | Disclosure | Chevron 180° + expand | `spring()` rotation, `expandVertically() + fadeIn(300 ms)` |
| 7 | LIVE / queue dots | Blink | alpha 1 → 0.2 → 1, 1400 ms / 1000 ms, `ease-in-out` |
| 8 | Voice meter | 7 bars | scaleY 0.22 → 1, 1100 ms, 130 ms stagger |
| 9 | Board scan band | Sweep | translateY -8 % → 108 %, 3400 ms, `CubicBezier(0.5, 0, 0.5, 1)` |
| 10 | Transcript / note arrival | Slide from right | `slideInHorizontally { 16 } + fadeIn()`, 450 ms |
| 11 | Bottom sheets | Rise from edge | translateY 102 % → 0, 340 ms, `CubicBezier(0.2, 0.9, 0.25, 1)`; scrim `fadeIn(250 ms)` |
| 12 | Lightbox | Scale in | `scaleIn(0.82f, spring(0.6, 300))` + `fadeIn()` |
| 13 | Toast / snackbar | Rise | `slideInVertically { 18 } + fadeIn()`, 350 ms, auto-dismiss 2600 ms |
| 14 | Theme change | Colour crossfade | `animateColorAsState(tween(400))` on background, surface, onSurface |
| 15 | Row press | Lift | translationY -2.dp + border → `primary`, 200 ms |
| 16 | Zoom read-out | Idle fade | to 40 % alpha, 1.2 s after last change, 250 ms |

Reduced motion: read `Settings.Global.ANIMATOR_DURATION_SCALE`; when it is 0, replace every entry animation with a plain `fadeIn(100)` and stop all infinite transitions except the LIVE dot (it carries state, so keep it as a static filled dot instead).

Do not animate: list reordering during a live session, note text as it refines (it must be readable, not sliding), or anything on the reader body beyond the evidence expansion.

---

## 8. Persistence

New file `data/SettingsRepository.kt`, backed by DataStore Preferences:

```kotlin
data class VbSettings(
    val theme: VbThemeMode = VbThemeMode.System,   // System | Dark | Light
    val accent: VbAccent = VbAccent.Blue,
    val readingSize: Int = 1,                      // 0..3
    val readingSerif: Boolean = false,
    val focusMode: Boolean = false,
    val defaultPolicy: NotePolicy = NotePolicy.Runnable,
    val captureIntervalSec: Int = 8,
    val changeThresholdPct: Int = 8,
    val keepRawFrames: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val exportFormat: ExportFormat = ExportFormat.Zip,
    val exportIncludeFlags: Boolean = true,
    val mockMode: Boolean = false,
    val onboarded: Boolean = false,
    val consentAcknowledged: Boolean = false,
)
```

Capture interval and threshold now have two homes: a default in Settings and a per-session override in the Capture disclosure. The override does not write back to the default.

---

## 9. Build order

Each step should compile and run on its own.

1. `Color.kt`, `Theme.kt` (accent + `VbStatus` + `VbShape`/`VbSpace`), `Type.kt`, font resources. Existing screens will look different but keep working.
2. `data/SettingsRepository.kt`; wire `MainActivity` to read theme and accent from it.
3. `nav/VoxBoxNav.kt` + `NavHost` + `ui/VbBottomBar.kt`. Point Home at a stub, keep the old three screens reachable so nothing is lost mid-migration.
4. `settings/SettingsScreen.kt` and `AppearanceScreen.kt`. Verify all four accents in both themes against the fixed status colours.
5. Move the durable blocks out of `CaptureSessionScreen.kt` into `AudioRecoveryScreen.kt`, `ConnectionScreen.kt`, `PrivacyScreen.kt`, `ExportScreen.kt`, `AboutScreen.kt`. Delete each block from capture only once its new page renders real state.
6. Rewrite `CaptureSessionScreen.kt` as the four-decision setup. `CaptureSessionViewModel` is untouched.
7. `camera/VbCameraController.kt`; bind it in the existing camera setup and verify `setZoomRatio` against `ZoomState` bounds on a real device.
8. `session/LiveSessionScreen.kt` including the zoom slider, pinch, read-out, and the reference-frame reset after a zoom. Delete `board/BoardCaptureScreen.kt` and move single-shot capture to a long-press on the Home button.
9. `library/LibraryScreen.kt`, then `reader/NoteReaderScreen.kt` (blocks first, then outline / reading / find / evidence / edit / lightbox / share).
10. `home/HomeScreen.kt` with real recent notes and a real flag count.
11. `onboarding/OnboardingScreen.kt`, gated on `onboarded`.
12. Animation pass in the order of the table in §7, then the reduced-motion path.

## 10. Acceptance checks

- All four accents render legibly in both themes; orange uses the dark on-accent foreground.
- Green never appears on anything unsaved; orange never appears on anything the app changed by itself; red appears only while recording, on a destructive action, or on a failure.
- Capture setup fits one screen at default font size with no scrolling in voice mode.
- Zoom read-out matches `cameraInfo.zoomState.value.zoomRatio` to one decimal place, and the slider reaches both real bounds.
- A single zoom gesture accepts at most one extra frame.
- Reader at Huge size has no clipped text and no horizontal scroll except in code blocks.
- Every review flag shows both the captured and the suggested text, and neither action silently rewrites the note.
- With `ANIMATOR_DURATION_SCALE = 0`, no infinite animation runs and every screen is still fully usable.
- Leaving the live session always confirms first, and always drains saved audio before finishing the note.

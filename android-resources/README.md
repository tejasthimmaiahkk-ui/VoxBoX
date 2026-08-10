# VoxBox redesign — Android resources

Drop-in `res/` tree for the redesign described in `REDESIGN_IMPLEMENTATION.md` (included alongside this file).

## Where things go

Copy the contents of `res/` into `VoxBox/app/src/main/res/`, merging with what is already there.

| Folder | What it holds |
| --- | --- |
| `values/colors.xml` | Every colour token. The five mandatory hues plus both grounds. |
| `values/themes.xml`, `values-night/themes.xml` | `Theme.VoxBox` for window/splash chrome, four accent overlays, and `Theme.VoxBox.Live` for the always-dark capture surface. |
| `values/dimens.xml` | Spacing, radii, control sizes, reader text sizes. |
| `values/integers.xml` | Every animation duration from section 7 of the implementation doc. |
| `values/strings.xml` | All user-facing copy, verbatim from the prototype. |
| `drawable/ic_vb_*.xml` | 29 stroke icons at 24dp, ported from `ui/VoxBoxIcons.kt`. |
| `anim/` | Screen, sheet and list-item transitions plus the two shared path interpolators. |
| `font/` | Downloadable-font declarations for Figtree and Caprasimo. |
| `xml/` | Backup and data-extraction rules — both exclude notes and assets. |

## The app is Compose, so what is the XML for?

- `colors.xml` and `dimens.xml` are the single source of truth. Reference them from Compose with `colorResource(R.color.vb_blue)` and `dimensionResource(R.dimen.vb_button_h)`, or mirror the literals into `ui/theme/Color.kt` as the implementation doc shows. Do not let the two drift.
- `themes.xml` still drives the window background, splash and status/navigation bars. Without it you get a white flash before the first frame in dark mode.
- The drawables are usable from Compose directly: `painterResource(R.drawable.ic_vb_camera)`.
- `anim/` is for activity transitions and any remaining View interop. In-composition motion uses the Compose specs in the implementation doc; the durations in `integers.xml` keep both in step.

## Two things to wire up manually

1. **Downloadable fonts** need `res/values/font_certs.xml` and a `preloaded_fonts` array. Android Studio generates both when you add a downloadable font through the resource picker. If the app must render correctly on first launch with no network, bundle the `.ttf` files in `res/font/` instead.
2. **Backup rules** must be referenced from the manifest:
   `android:fullBackupContent="@xml/backup_rules"` and
   `android:dataExtractionRules="@xml/data_extraction_rules"`.

## Accent switching

`colorPrimary` carries the user's accent; the status colours never do. Read the choice from `SettingsRepository`, pass it to `VoxBoxTheme(accent = …)` for Compose, and apply the matching `ThemeOverlay.VoxBox.*` before `setContent()` so the window chrome agrees. Orange is the one hue that needs a dark on-accent foreground — `vb_on_accent_dark` — and both theme files already account for it.

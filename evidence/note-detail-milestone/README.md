# Note-detail reopening milestone evidence

Date: 2026-07-21

## Automated verification

Command run from `VoxBox/`:

```text
gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain
```

Result: **BUILD SUCCESSFUL** in 28 seconds (53 actionable tasks; 16 executed, 37 up-to-date). The unit suite includes the new saved-pie-chart projection and malformed-chart rejection cases. Debug assembly and Android lint also passed.

## Physical-device verification

- Device: `2411DRN47I` (`5dfb3db8`), Android 16 / API 36.
- Installed the exact `VoxBox/app/build/outputs/apk/debug/app-debug.apk` with `adb install -r`.
- Created `Voice note 5`, loaded `Tejas pie chart 25 percent yellow label wheat`, and saved the preview.
- Force-stopped `me.thimmaiah.voxbox` and launched it again. The library reported `5 saved notes` and listed `Voice note 5` first.
- Selected `Open` for `Voice note 5`. The reopened detail reported `Opened Voice note 5. Saved blocks are read-only in this milestone.` and rendered `Wheat`, `25%`, and `yellow • remainder white` from the stored block.
- Visual capture: `device-note-detail.png` (inspected after capture; the typed pie chart and read-only scope message are visible).

## Scope boundary

This milestone proves ordered typed-block recovery and read-only rendering for the implemented paragraph, heading, bullet and pie-chart kinds. Editing, deletion and reordering of saved blocks are intentionally not implemented yet.

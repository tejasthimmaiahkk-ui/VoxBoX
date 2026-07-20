# Room persistence — physical-device evidence

Date: 2026-07-20

Device: `2411DRN47I` (`5dfb3db8`), Android 16 / API 36

## Reproduction

1. Build the APK with `gradlew.bat testDebugUnitTest assembleDebug --console=plain`.
2. Install `VoxBox/app/build/outputs/apk/debug/app-debug.apk` with `adb install -r`.
3. In VoxBox, create `Voice note 1`, load the built-in 25% yellow wheat pie-chart example, then use **Save preview as a note block**.
4. Inspect the UI hierarchy. It reported `pie chart saved locally.`, `1 saved note`, and `Voice note 1`.
5. Force-stop `me.thimmaiah.voxbox`, relaunch it, then inspect the hierarchy again. It still reported `1 saved note` and `Voice note 1`.
6. Confirm the private Room files with `adb shell run-as me.thimmaiah.voxbox ls -l databases`; the database, WAL and SHM files were present.

## Interpretation

The current library shell establishes durable note creation and block saving. It does not yet provide a note-detail view, so this evidence does not claim visual re-rendering of the stored pie block after restart. That is intentionally deferred to the editor/reopen milestone.

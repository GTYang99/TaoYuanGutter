# Execution Report

## Physical Location Measurement

| Item | Result |
|---|---|
| Device | Sony XQ-AU52, Android 12, serial `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp` |
| App | `com.example.taoyuangutter` version 1.0.0; installed APK source revision NOT VERIFIED |
| Cached Fused location before tap | Available; 16.627 m horizontal accuracy |
| User interaction | Main-map "現在位置" tapped at `2026-09-17 23:50:33.507 +0800` |
| High-accuracy request | Observed at `23:50:33.652`, 145 ms after tap |
| New Fused delivery | Observed at `23:50:39.855`, approximately 6.35 s after tap |
| Follow-up snapshot | Fused/network accuracy 21.038 m |
| Exact camera completion | NOT VERIFIED; no app-owned timing instrumentation exists |

## Commands

- `adb -s adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp shell dumpsys location`
- `adb -s adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp logcat -d -v threadtime`
- A single `adb shell input tap` on the visible current-location control.

## Result

- The device evidence confirms the root cause: the main map waits for a new high-accuracy result despite an available cached location.
- This report does not claim an exact UI camera-completion measurement because the current build does not instrument it.

## Implementation

- Added `LocationFixQualityPolicy`: a cached location is usable for five minutes; a refinement must be newer and improve horizontal accuracy by at least 10 m.
- Updated `MyLocationController` to use the cache first, make one high-accuracy request, and cancel pending work when its owner is destroyed.
- Routed `MapPointPickerActivity` through the shared two-stage behavior.
- Updated form/import lookup to start from a host or usable cached location, then re-query only after a qualified high-accuracy refinement.
- Added lifecycle cancellation for both main-map consumers, the point picker, and form import updates.

## Developer Validation

| Check | Command | Result |
|---|---|---|
| Source whitespace | `git diff --check` | PASS |
| Targeted unit tests | `./gradlew testDebugUnitTest --tests 'com.example.taoyuangutter.map.LocationFixQualityPolicyTest' --tests 'com.example.taoyuangutter.main.MainMapLocationRecenterReloadTrackerTest'` | NOT VERIFIED — Gradle cannot locate a Java Runtime. |
| Debug APK | `./gradlew assembleDebug` | NOT VERIFIED — not attempted after the same Java Runtime failure. |
| Fixed-build device test | Install the committed debug APK and run AC-001 to AC-003 on Sony XQ-AU52 | NOT VERIFIED — no APK could be built locally. |

## Recommended Next Action

- Run the recorded Gradle commands with a supported JDK, install the resulting APK on Sony XQ-AU52, and verify an immediate cached first center/search plus at most one ≥10 m accuracy refinement.

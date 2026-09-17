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

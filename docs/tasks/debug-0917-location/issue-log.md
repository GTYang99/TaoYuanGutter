# Issue Log

## ISS-001

| Field | Value |
|---|---|
| task_id | debug-0917-location |
| phase | debug |
| category | implementation_regression |
| priority | P2 |
| status | monitoring |
| title | Main map ignores an available cached location and waits for high-accuracy delivery |
| impact | Main-map centering was delayed approximately 6.35 seconds on the Sony XQ-AU52 even though a cached Fused position with 16.627 m accuracy existed. |
| evidence | `root-cause.md`, physical-device measurement at 2026-09-17 23:50:33 +0800. |
| next_action | implementation_debug |

## ISS-002

| Field | Value |
|---|---|
| task_id | debug-0917-location |
| phase | validation |
| category | unknown |
| priority | P2 |
| status | open |
| title | Sony validation APK launch reached an App Not Responding window before map interaction |
| impact | AC-001 through AC-003 cannot be physically verified on the fixed APK. |
| evidence | APK installation succeeded; Android reported `Application Not Responding: com.example.taoyuangutter` while the device was asleep, and no app stack trace or reproducible map interaction was captured. |
| next_action | Recheck only if ANR recurs during the authenticated map validation. |

## ISS-003

| Field | Value |
|---|---|
| task_id | debug-0917-location |
| phase | verification |
| category | environment |
| priority | P2 |
| status | open |
| title | Sony device is at the login screen, so authenticated map acceptance cases cannot run |
| impact | AC-001 through AC-003 remain NOT VERIFIED on the fixed APK. |
| evidence | After dismissing the prior ANR dialog and cleanly relaunching, the Sony device showed account, password, and login controls. |
| next_action | User supplies an authenticated test session, then rerun the specified physical validation. |

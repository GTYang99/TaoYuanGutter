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
| status | monitoring |
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
| status | resolved |
| title | Sony device is at the login screen, so authenticated map acceptance cases cannot run |
| impact | Initially prevented AC-001 through AC-003 device verification. |
| evidence | The Sony session was subsequently authenticated on 2026-09-18, enabling main-map and picker physical checks. |
| next_action | Closed; remaining evidence gap is tracked by ISS-004. |

## ISS-004

| Field | Value |
|---|---|
| task_id | debug-0917-location |
| phase | verification |
| category | environment |
| priority | P2 |
| status | open |
| title | Physical validation lacks a materially better fresh GPS fix and full remaining flow access |
| impact | AC-001 is only partially covered; AC-002 and AC-003 remain NOT VERIFIED. |
| evidence | Main map and current picker centered within the 0.25 s capture window on 2026-09-18. Android diagnostics recorded `hAcc=27.637 m` fused network location and registered high-accuracy requests, but no newer result improving accuracy by at least 10 m. The tested import sheet returned HTTP 422. |
| next_action | Repeat the defined physical cases where fresh GPS has a materially better fix and the standalone picker/form nearby lookup are available. |

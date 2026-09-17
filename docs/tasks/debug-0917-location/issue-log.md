# Issue Log

## ISS-001

| Field | Value |
|---|---|
| task_id | debug-0917-location |
| phase | debug |
| category | implementation_regression |
| priority | P2 |
| status | open |
| title | Main map ignores an available cached location and waits for high-accuracy delivery |
| impact | Main-map centering was delayed approximately 6.35 seconds on the Sony XQ-AU52 even though a cached Fused position with 16.627 m accuracy existed. |
| evidence | `root-cause.md`, physical-device measurement at 2026-09-17 23:50:33 +0800. |
| next_action | implementation_debug |

# FEAT-1006 Current Emulator Request Capture

- Date: 2026-10-14 (Asia/Taipei).
- Device: `emulator-5554`, `sdk_gphone16k_arm64`, Android 16 (API 36). Commands explicitly selected this emulator; no physical-device test command was run.
- Scenario: `OfflinePolygonMapHostInstrumentedTest` on the FEAT-1006 branch; `OK (3 tests)`, 75.925 seconds. The test fixture returns the installed app version from the update-check repository so the merged FEAT-1013 prompt cannot cover map-host interactions.
- Capture: app-process `OkHttp` logcat after clearing logcat immediately before the host test.
- Observed 113 outgoing GET requests, all with HTTP 200 responses. Of these, 109 were unrelated `map_ditch_nodes_labels` WMS requests. Requests matching WMTS `regions`, `legacyDitch`, or `roadServey`: 0 each. `UnknownHostException`: 0.
- Network availability: emulator DNS and server responses worked during this run; the unrelated map overlay traffic confirms the shared client was active.
- This is application-level OkHttp request evidence, not a raw packet capture. The host suite verified local GeoPackage provider behavior in all four map hosts.

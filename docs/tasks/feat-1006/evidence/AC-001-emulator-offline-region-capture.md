# Current FEAT-1006 Emulator Request Capture

- Date: 2026-10-14 (Asia/Taipei).
- Device: `emulator-5554`, model `sdk_gphone16k_arm64`, Android 16 (API 36). Commands explicitly targeted this emulator; no physical-phone command was run.
- Runtime scenario: final-source `OfflinePolygonMapHostInstrumentedTest#mapWorkspaceFragmentShowsAndTogglesBothGeoPackageLayers`, `OK (1 test)`.
- Capture: app-process `OkHttp` logcat after clearing logcat immediately before the test.
- Observed 96 outgoing GET entries. Entries matching WMTS `regions`, `legacyDitch`, and `roadServey`: 0 each. Unrelated `map_ditch_nodes_labels` entries: 91. Remaining entries were app-version, scope-search, or supporting map calls.
- Network availability: requests were logged as attempted, but emulator logcat showed `UnknownHostException` for `taipei.srgeo.com.tw`; successful server responses were unavailable. Offline GPKG loading and tile rendering passed separate instrumentation checks.
- This is app-process request evidence, not a raw packet capture. `AC-001-emulator-okhttp-capture.md` records the prior, superseded two-source implementation and its `regions` WMTS requests.

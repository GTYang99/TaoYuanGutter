# AC-001 Emulator Request Capture

- Date: 2026-10-08 (UTC)
- Device: `emulator-5554`, `sdk_gphone64_arm64`, Android 14; no physical phone used.
- Runtime scenario: `OfflinePolygonMapHostInstrumentedTest`, covering MainActivity, MapWorkspaceFragment, GutterFormActivity, and MapPointPickerActivity with offline overlays enabled; result `OK (3 tests)`, 63.11 seconds.
- Capture: app-process-scoped Android logcat from the debug app's `OkHttp` tag (PID 8490). The first through last observed outgoing GET entries span `2026-10-08T04:41:08.116Z` to `2026-10-08T04:42:05.745Z`.
- Observed 374 outgoing OkHttp GET request entries: 242 for WMTS `regions`, 128 for WMS `map_ditch_nodes_labels`, and 0 containing the WMTS feature layer names `legacyDitch` or `roadServey`.
- The visible `regions` and `map_ditch_nodes_labels` requests confirm that request logging was active during the map-host run. Source review also confirms that app call sites construct `Wmts3857TileProvider` only for `REGIONS`; the two feature overlays use the local GeoPackage tile providers.
- This is app-level OkHttp request evidence, not a raw network packet capture. It covers the feature requests routed through the app's shared OkHttp client.

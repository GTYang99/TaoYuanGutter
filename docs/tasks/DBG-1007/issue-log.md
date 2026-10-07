# Issue Log

## ISS-DBG-1007-LOC-001

- Task: DBG-1007
- Category: enhancement_request (classification: current behavior confirmed; defect status depends on product requirement)
- Priority: P3 pending product impact confirmation
- Title: Missing waypoint coordinates use a fixed form-map fallback
- Status: open
- Evidence: `GutterFormActivity.onMapReady()` explicitly selects `LatLng(24.9929, 121.3011)` when the edited waypoint lacks coordinates; it does not use the host location passed in the intent. The separate main map attempts its own asynchronous location recentering.
- Impact: A point without coordinates opens its form map at the fixed Taoyuan-area location even when the shared main map has recentered on the device.
- Next action: implementation

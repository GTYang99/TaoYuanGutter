# Issue Log

## ISS-DBG-1016-1-001

- task_id: debug1016-1
- phase: implementation
- category: enhancement_request
- priority: P2
- title: Empty add form opens its mini-map at the Taoyuan fallback
- status: resolved
- impact: The form's initial map viewport may be unrelated to the user's work location when the new/add waypoint has no saved coordinates.
- repro_steps:
  - Open the new/add gutter sheet.
  - Select a waypoint that has no saved coordinates to open GutterFormActivity.
  - Observe the form mini-map before selecting a coordinate.
- expected: The mini-map viewport starts at the user's current location when a usable location is available.
- actual: A fresh eligible add form now runs the fused-location flow and centers the mini-map on the emulator GPS fix. The fix remains camera-only.
- evidence:
  - AddGutterBottomSheet.kt:1212-1219 reports add mode as not edit mode.
  - GutterFormActivity.kt:1663-1685 selects the Taoyuan fallback and invokes location only through the edit-only policy.
  - EditMapLocationPolicy.kt:5-12 excludes new/add mode.
  - EditMapLocationPolicyTest.kt:7-15 asserts the exclusion.
  - The prior DBG-1007 requirement FR-006 / AC-008 left new-point behavior unchanged.
- Emulator evidence: The location callback ran with a usable fix and the apply guard allowed it, but `animateCamera()` left the map target unchanged. Replacing that update with `moveCamera()` made the focused test pass.
- next_action: verification
- owner: developer

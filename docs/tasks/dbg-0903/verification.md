# Verification

Date: 2026-09-03
Task: DBG-0903
Revision: `c1a278a`

## Acceptance Criteria

- AC-001: After login and initial user-location recenter, the map requests the visible gutter scope without a manual map gesture. **NOT VERIFIED**
  - Source evidence: `MapWorkspaceFragment` now handles location success and camera animation completion with an explicit force reload.
  - Missing evidence: real-device login flow and network request observation.
- AC-002: Tapping any `rvWaypoints` row opens the corresponding gutter form. **NOT VERIFIED**
  - Source evidence: the RecyclerView-level gesture interceptor was removed and the adapter row click remains connected to the host navigation callback.
  - Re-marked cause: the remaining suspected failure is touch dispatch before `layoutForeground.setOnClickListener`, involving RecyclerView child dispatch, ItemTouchHelper gesture interception, or BottomSheet Window callback routing.
  - Missing evidence: real-device touch dispatch and `GutterFormActivity` launch, with event-chain logs.
- AC-003: Existing build and unit-test checks remain green. **PASS**
  - `./gradlew compileDebugKotlin --no-daemon`
  - `./gradlew testDebugUnitTest --no-daemon`
  - `./gradlew assembleDebug --no-daemon`
  - `git diff --check`

## Regression Review

- The existing camera-idle viewport reload path remains unchanged.
- Waypoint row deletion and drag handling remain in `WaypointAdapter` and `ItemTouchHelper`.
- No changes were made to the form layouts or form data contract.

## Result

**NOT VERIFIED**

The implementation compiles, unit tests pass, and a Debug APK was produced. A connected Android device or emulator is still required to verify the two reported UI behaviors.

## Follow-up Verification

- The second implementation binds the click callback to `ViewHolder.itemView`, removing dependence on the swipeable `layoutForeground` event path.
- `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --no-daemon`: **PASS**
- AC-002 remains **NOT VERIFIED** until the real-device touch path reaches the row callback and launches `GutterFormActivity`.

## Confirmed Root Cause Fix

The active BottomSheet is managed by `MapWorkspaceFragment.childFragmentManager`, but the previous code looked for `LocationPickerHost` on `MainShellActivity`. The failed cast caused `openWaypointAt()` to return before navigation. The implementation now resolves `parentFragment` first, with an Activity fallback.

The build and unit-test validation pass, but AC-002 remains **NOT VERIFIED** pending real-device confirmation.

## Latest Validation

- Revision rechecked: `0157768`
- `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --no-daemon`: **PASS**
- `git show --check HEAD`: **PASS**
- `adb devices`: no connected device or emulator
- AC-002: **NOT VERIFIED**

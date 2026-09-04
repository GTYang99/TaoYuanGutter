# Execution Report

Date: 2026-09-03
Task: DBG-0903

## Implementation

- Removed the `RecyclerView`-level `GestureDetector` interception from `AddGutterBottomSheet`.
- Kept waypoint row click handling in `WaypointAdapter`, allowing the existing host callbacks to open `GutterFormActivity`.
- Added a shared location update and camera-finished flow in `MapWorkspaceFragment` so the initial user-location recenter requests a scope reload without requiring a manual map gesture.
- Preserved the existing camera-idle and force-reload guards to avoid changing offline, edit, and inspect flows.

## Validation

- `./gradlew compileDebugKotlin --no-daemon`: PASS
- `./gradlew testDebugUnitTest --no-daemon`: PASS
- `./gradlew assembleDebug --no-daemon`: PASS
- `git diff --check`: PASS
- Real-device login/map and waypoint tap validation: NOT EXECUTED in this environment.

## Limitation

The repository environment does not provide a connected Android device or emulator, so the two reported behaviors still require manual real-device confirmation.

## Follow-up Implementation

- Moved the waypoint click listener from `layoutForeground` to `ViewHolder.itemView` so the click path is not dependent on the swipeable foreground layer.

## Follow-up Validation

- `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --no-daemon`: PASS
- Real-device `rvWaypoints` tap validation: NOT EXECUTED in this environment.

## Confirmed Root Cause Follow-up

- `AddGutterBottomSheet.openWaypointAt()` previously resolved `LocationPickerHost` from the Activity only.
- The active flow is hosted by `MainShellActivity`, while `MapWorkspaceFragment` owns the `LocationPickerHost` implementation through the child fragment manager.
- The failed Activity cast caused an early return before `openWaypointForEdit()` or `openWaypointForInspect()`.
- The fix now resolves the parent fragment first and keeps the Activity fallback for compatibility.

## Follow-up Validation

- `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --no-daemon`: PASS
- Real-device AC-002 validation: NOT EXECUTED.

## Latest Validation Run

- Revision: `0157768`
- `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --no-daemon`: PASS
- No Android device or emulator was available through `adb devices`.

## 2026-09-04 Inspect Route Fit Implementation

- Added an inspect/edit camera helper in `MapWorkspaceFragment` so inspect route fitting consistently preserves the form-visible viewport with `resetPaddingAfter = false`.
- Routed inspect entry, inspect form return, inspect edit entry, and post-update inspect reopen through the same helper.
- Added an inset-change refit hook so the route is recalculated after the bottom sheet reports its real height.

## 2026-09-04 Validation

- `./gradlew compileDebugKotlin testDebugUnitTest assembleDebug --no-daemon`: PASS
- `git diff --check`: PASS
- Real-device `QV710EDR3A` APK install: PASS
- Real-device app launch: PASS, foreground activity confirmed as `com.example.taoyuangutter/.login.LoginActivity`

## 2026-09-04 Limitation

- Manual visual confirmation is still required after login: open an existing gutter and verify the form area leaves the whole route visible above it.

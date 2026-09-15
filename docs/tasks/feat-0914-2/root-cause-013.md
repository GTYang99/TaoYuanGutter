# Root Cause Analysis — AC-008 Failure Routing and Upload Overlay Motion

## Failed behavior

- AC-008 requires every failed multi-gutter submission to show the existing failure Alert and, after the user confirms its close/save action, return to the active 新增側溝清單 while retaining the draft for edit or retry.
- The upload blocking overlay must visibly animate while side-gutter or photo upload is in progress.

## AC-008 root cause

The prior fix in `MapWorkspaceFragment.onStoreDitchNetworkClosed()` only receives failures classified as network failures (including timeout) and the 409 photo-claim conflict. Those paths call the host callback after the Alert closes, save the draft, and return to `returnToMultiGutterListAfterUploadFailure()`.

All other `storeDitch` failures take a different path in `AddGutterBottomSheet.submitNewGutterRequest()`:

1. `UploadFailureClassifier.forStoreDitchError()` or `forStoreDitchException()` creates `showStoreDitchFailureDialog()`.
2. The dialog's `存入草稿` callback is only `dismissAllowingStateLoss()`.
3. No host callback saves the selected multi-gutter draft through the common failure exit or reopens the list.

Therefore AC-008 currently depends on the server error being classified as a network error. A normal HTTP/business error can dismiss the form without returning to the active list, which violates the requirement that *any* failed submission preserves the item and returns after confirmation.

Photo upload before `storeDitch` has the same ownership gap: `ensureWaypointPhotosUploadedBeforeSubmit()` shows a photo failure Alert and returns to the form; it does not use the common multi-gutter failure exit. If photo upload is included in the user-visible upload journey, this also fails the required list-return behavior.

## Upload overlay motion root cause

`android:indeterminate="true"` and `ProgressBar.isIndeterminate = true` establish only the indicator mode. They do not create an app-owned animation or prove that the framework drawable is advancing at runtime. The current overlay relies entirely on the platform `progressBarStyleLarge` indeterminate drawable and the device animation policy.

The codebase currently has no test that triggers the real upload overlay and observes motion over time. `MainShellActivityTest#uploadBlockingIndicatorIsExplicitlyIndeterminate` parses the XML attribute only. The debug long-press simulation in `AddGutterBottomSheet` explicitly shows an Alert with an empty `onClose` callback; it does not start a request, execute a failure return path, or display the upload overlay. Previous physical attempts did not reach a valid upload request, so they cannot prove or disprove spinner movement.

Consequently, there are two evidence-backed contributors:

- Devices with Android animator scale `0` intentionally render the framework indicator without motion.
- At animation scale `1x`, the application still has no runtime contract or observation for the platform-owned `progressBarStyleLarge` animation. A reported static upload spinner cannot be distinguished from a device drawable/policy behavior with the current validation.

## Affected files

- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/main/MainBlockingUiController.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt`

## Classification

`implementation_regression`, P1 for AC-008 because a required failure recovery path is incomplete. The spinner is an `implementation_regression`, P2: its indeterminate state is configured, but runtime animation is delegated to platform/device behavior and has no executable motion validation.

## Evidence

- `submitNewGutterRequest()` lines 1253–1263 and 1291–1298 route non-network errors to `showStoreDitchFailureDialog(... onSaveDraft = { dismissAllowingStateLoss() })`.
- `showStoreDitchFailureDialog()` lines 1304–1317 has no callback to `LocationPickerHost.onStoreDitchNetworkClosed()` or an equivalent common failure exit.
- `onStoreDitchNetworkClosed()` lines 966–972 correctly returns multi-gutter sessions, but only callers that reach it benefit from the fix.
- `ensureWaypointPhotosUploadedBeforeSubmit()` lines 1959–1964 shows a local Alert and returns false, bypassing the active-list return route.
- The current spinner validation asserts XML `indeterminate=true`, not frame-to-frame motion; the test environment previously used animator scale `0` and the physical upload overlay was not reached.

## Implementation result

- Added `LocationPickerHost.onGutterUploadFailureConfirmed()`. Its default return preserves legacy single-gutter dismissal, while `MapWorkspaceFragment` handles multi-gutter failures by saving the selected draft and reopening the active list.
- Routed generic API errors, exceptions, photo upload confirmation, and the debug failure simulation through this common multi-gutter failure exit. Existing network/409 routes continue through the same MapWorkspace handler.
- Replaced the platform-default `ProgressBar` with the repository's existing Material `CircularProgressIndicator`; `MainBlockingUiController` uses `show()` and `hide()` so the component owns its indeterminate drawable lifecycle.
- Debug build, unit tests, and `MainShellActivityTest` passed on XQ-AU52 - 12 and Medium_Phone(AVD) - 14. Runtime upload motion at animation scale 1x and an authenticated failure smoke remain verification work.

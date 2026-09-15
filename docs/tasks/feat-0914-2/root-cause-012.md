# Root Cause Analysis — Upload Loading Indicator Animation

## Failure

During `storeDitch` submission and photo upload, the loading UI appears as a static icon instead of an animated spinner.

## Root cause

Both flows use the same `MainBlockingUiController` overlay and the same `pbInspectLoading` view. `setInspectLoading()` and `beginPhotoUpload()` only update internal flags and toggle the view's `visibility`; they do not explicitly set or start an indeterminate animation.

The corresponding `activity_main.xml` view is a framework `ProgressBar` with `progressBarStyleLarge` and tint attributes, but it does not explicitly declare `android:indeterminate="true"`. The animation is therefore delegated to the platform style/drawable and the device's global animator settings. If the drawable is resolved as a non-animating/static state, or the device animator scale is zero, the user sees only one icon.

The current instrumentation setup explicitly sets `animator_duration_scale`, `transition_animation_scale`, and `window_animation_scale` to `0` in `MainShellActivityTest`. The emulator used for validation also currently reports all three values as `0`. That is sufficient to explain the observed static indicator in the test environment, but it does not guarantee animation on a real device because the production code has no explicit animation contract or animation-state assertion.

## Affected files

- `app/src/main/java/com/example/taoyuangutter/main/MainBlockingUiController.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt`

## Scope of impact

- Side-gutter submission loading: `setInspectLoading(true, ...)`
- Photo upload loading: `beginPhotoUpload(...)`
- Retry and pending-photo upload paths that reuse the same controller

## Evidence

- `MainBlockingUiController.applyBlockingOverlay()` only assigns `visibility` and text; it does not call `setIndeterminate`, `start`, or an explicit animator.
- `pbInspectLoading` uses the platform `ProgressBar` style and tint only; unlike the map loading bar, it lacks an explicit `android:indeterminate="true"` declaration.
- `MainShellActivityTest` sets all Android animation scales to `0`.
- Current emulator values are `0` for all three animation scales.

## Classification

`implementation_regression` / UI behavior. The approved requirement expects a visible animated loading indicator; this is not an API or backend issue.

## Resolution

- The shared `ProgressBar` now explicitly declares and applies indeterminate state.
- `MainBlockingUiController` reasserts `isIndeterminate = true` whenever the shared submission/photo overlay state is applied.
- A layout-state instrumentation test validates the explicit indeterminate attribute on both supported test devices.

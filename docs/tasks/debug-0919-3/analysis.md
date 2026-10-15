# Repository Analysis

## Task and source interpretation

- Task: `debug-0919-3`
- Branch: `fix/debug-0919-3-回報點位難點`
- Source: `/Users/a10362/Desktop/markdown file/ty_debug_0919-3.md`
- The attached note is treated as the observed product problem and expected
  behavior. Its wording about a dialog is a user-facing description; the
  current implementation uses an inline panel, not a `Dialog` window.
- This phase records analysis and root cause only. Production code is not
  changed.

## Current behavior

1. The normal logged-in entry is `LoginActivity → MainShellActivity →
   MapWorkspaceFragment`. `MapWorkspaceFragment` inflates the same
   `activity_main.xml` binding, so the no-ditch panel is hosted inside the
   shell's `shell_container`, above the shell's bottom navigation. The offline
   login path can still launch `MainActivity`, which uses the same layout and
   controller.
2. `activity_main.xml` places `noDitchPanel` as a `CardView` include inside
   the `ConstraintLayout`, constrained to the parent bottom. It is initially
   `GONE` and is shown by `NoDitchModeUiController.enter()`.
3. Both `MainActivity.setupButtons()` and
   `MapWorkspaceFragment.setupButtons()` bind `@+id/btnReportNoDitch` to the
   same no-ditch flow. The flow enters no-ditch mode, waits for a map tap,
   then calls `setPickedLatLng()` to reveal the note field.
3. When the note field receives focus, the panel's window-insets listener
   calculates `max(systemBars.bottom, ime.bottom)` and adds that value to the
   panel's existing bottom margin. The panel remains bottom-constrained, so
   the added IME height moves its top edge upward by the keyboard height.
4. The panel content is a `NestedScrollView` with `wrap_content` height and a
   multi-line input with `minLines="6"`. On a foldable display, the resulting
   panel plus keyboard offset can consume most of the available height and
   visually place the report panel much higher than intended.

## Expected behavior

- In no-ditch report mode on a foldable phone, focusing the note field should
  keep the report panel within the visible area immediately above the IME.
- The panel must not receive the IME displacement twice. Dismissing the IME
  should restore the normal bottom position without accumulating margins.
- The map-pick, note entry, reset, submit, and post-submit flows must remain
  unchanged.

## Reproduction boundary

1. Open the main map.
2. Tap `@+id/btnReportNoDitch`.
3. Tap the map to choose a no-ditch coordinate.
4. Tap the note field and type text while using the foldable device.
5. Observe the report panel's top position relative to the keyboard.

## Evidence reviewed

- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/panel_no_ditch_report.xml`
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/main/NoDitchModeUiController.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/themes.xml`
- `/Users/a10362/Desktop/markdown file/ty_refactor_0917.md` for the earlier
  no-ditch map-tap difficulty that motivated the current interaction design.
- Commit `0652659` (`fix: 修正無側溝點位操作地圖功能，與上傳的動畫疊加的情況`), which introduced the current inline panel and its IME-inset handling.

## Affected modules

- `MainActivity`: no-ditch entry, map-pick state, and submit lifecycle.
- `MainShellActivity` / `MapWorkspaceFragment`: normal logged-in host and
  actual no-ditch entry path.
- `NoDitchModeUiController`: panel visibility, input state, and window-insets
  handling.
- `activity_main.xml`: bottom constraint for the panel.
- `panel_no_ditch_report.xml`: panel height, scroll container, and multi-line
  note field.
- `AndroidManifest.xml` / `themes.xml`: activity window soft-input behavior
  is not explicitly defined for `MainShellActivity` or `MainActivity`.

## Dependencies

- Android IME/window insets dispatch and the activity's soft-input policy.
- `ConstraintLayout` bottom constraint and `MarginLayoutParams`.
- `NestedScrollView` measurement and focus scrolling.
- Foldable device posture, display height, navigation-bar inset, and keyboard
  implementation.
- The shell's bottom-navigation height and its system-bar padding, because the
  fragment panel is already laid out above that navigation container.

## Risks

- Fixing only the panel margin may leave the multi-line panel unbounded on a
  short folded display.
- Applying `ime` inset handling both through the window policy and a manual
  margin/padding would reproduce the same double displacement.
- A broad change to `MainActivity` insets could affect the map FABs,
  measurement panel, or location picker; the minimum fix should remain scoped
  to no-ditch reporting.
- The existing `MeasureModeUiController` has similar inset arithmetic, but it
  is outside this reported flow and should not be changed without separate
  evidence.

## Unknowns and evidence limitations

- No screenshot, logcat output, fold posture, or keyboard/inset measurements
  were supplied with the note. A connected non-foldable Sony XQ-AU52 was
  detected during re-investigation (1080x2520, Android 12), but no runtime
  no-ditch interaction trace was collected.
- The exact runtime soft-input behavior (`adjustPan` versus resize/edge-to-edge
  handling) is therefore not independently measured in this phase.
- The explicit manual IME-margin behavior is confirmed statically. Whether
  the reported device also applies a second platform pan/resize displacement
  remains unverified. Foldable-device acceptance remains `NOT VERIFIED`.

## Historical interaction context

The earlier refactor note (`ty_refactor_0917.md`) reports that, on some
devices, activating `btnReportNoDitch` made the map difficult to tap. Commit
`0652659` replaced the earlier modal bottom-sheet interaction with the current
inline panel so the map could remain draggable/clickable, and added the
manual inset handling in `NoDitchModeUiController`. This explains why the
current code has a panel-specific inset workaround: it was introduced for the
earlier map-pick problem. The 0919-3 input-position problem is a separate
regression risk of that same design, not evidence that the map-pick issue is
the present root cause.

## Re-investigation result without a foldable device

- Source tracing confirms the normal logged-in runtime path is
  `LoginActivity → MainShellActivity → MapWorkspaceFragment`; the attached
  note's `MainActivity.kt` naming is stale relative to the current shell
  architecture.
- The current connected device is a non-foldable Sony XQ-AU52, Android 12,
  physical display `1080x2520`, density `420`. It provides environment
  information only; the no-ditch text-entry flow was not run on it.
- The strongest evidence that does not depend on a foldable device is the
  layout/insets contract itself: the shell places the Fragment above its
  bottom navigation, parent listeners return insets unchanged, and the shared
  controller adds the full IME bottom inset to the already bottom-constrained
  panel. This proves an additional upward offset is introduced by the app.
- The remaining unknown is device-specific: whether the platform also
  resizes or pans the window for the focused editor, and how that combines
  with the manual offset on the reported foldable posture.

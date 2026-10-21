# Root Cause Analysis

## Classification

- Category: `implementation_regression`
- Priority: `P2` — main map controls stay unavailable after closing an unfinished add session.
- Issue: `ISS-DBG-1021-2-001`
- Base revision: `815540aab261a20439ae2ceb39a1de897267e0c9`
- Root-cause confidence: **95%**

## Observed and Expected Behavior

The report describes entering gutter details in `GutterFormActivity`, returning to the upload sheet, dragging the sheet down, and seeing “未上傳側溝草稿將儲存到草稿中”. After either choice, the main map appears but its controls do not recover.

Expected: when this flow returns to the main map, its controls are visible and usable unless another blocking operation is active.

The exact prompt is implemented in `AddGutterListBottomSheet.kt`, not `AddGutterBottomSheet.kt`. In the multi-gutter flow, dismissing `AddGutterBottomSheet` can return the user to the list sheet before this close confirmation occurs. For implementation, the user clarified that Confirm exits to the map while Cancel after a swipe should keep the draft and reopen the list.

## Root Cause

The list sheet starts its close confirmation from `onCancel()`, after BottomSheet cancellation has begun. The list fragment is then detached while the separate confirmation alert remains on screen. The alert's Confirm handler looks up `parentFragment` only when the user taps Confirm, so it no longer finds the `Host` and skips the cleanup callback. Cancel has a null listener and also skips host cleanup.

This leaves the map's target-panel blocking state set:

1. `MapWorkspaceFragment.showAddGutterList()` shows the list and calls `mainBlockingUiController.setTargetPanelVisible(true)`.
2. A bottom-sheet drag to `STATE_HIDDEN` is converted by Material Components 1.12.0's `BottomSheetDialog` callback into `dialog.cancel()`. `AddGutterListBottomSheet.onCancel()` then calls `confirmClose()`.
3. The positive alert listener resolves `(parentFragment as? Host)` at click time. After the list fragment is removed, that lookup is null. The negative listener is `null`, so Cancel only closes the alert.
4. `MapWorkspaceFragment.onAddGutterListConfirmedClose()` is the path that finalizes drafts, clears the multi-gutter session, and calls `restoreMainUiAfterSheetClosed()`. The restoration clears target-panel state and re-enables map controls.
5. Without that host callback, `targetPanelVisible` remains true. `MainBlockingUiController` continues hiding the map buttons under its target-panel policy.

## Evidence

- `AddGutterListBottomSheet.kt:69-85`: the alert is opened by `confirmClose()`; Confirm resolves `parentFragment` when clicked; Cancel has no listener.
- `AddGutterListBottomSheet.kt:150-152`: `onCancel()` calls `confirmClose()`.
- The project pins `com.google.android.material:material:1.12.0`. In its local `BottomSheetDialog` implementation, the bottom-sheet callback calls `cancel()` when state becomes `STATE_HIDDEN` (5), which connects a drag dismissal to the app's `onCancel()` path.
- The project pins AndroidX Fragment 1.8.5. Its `DialogFragment` removes itself through the dialog dismissal lifecycle after cancellation.
- `MapWorkspaceFragment.kt:1072-1080`: opening the list sets `targetPanelVisible` to true.
- `MapWorkspaceFragment.kt:1122-1127`: the Host callback clears session state and invokes UI restoration.
- `MapWorkspaceFragment.kt:1779-1791` and `MainBlockingUiController.kt:64-90`: restoration clears the target-panel policy; while it remains set, the policy hides most map buttons.
- Emulator characterization on `emulator-5554` (Medium_Phone AVD, API 36): a focused instrumentation harness called `dialog.cancel()`, then verified the list fragment was no longer added and had no `parentFragment`, verified the reported alert was visible, and tapped Confirm. The final assertion that the Host had received `onAddGutterListConfirmedClose()` failed. This directly confirms the detached-parent callback failure after the cancellation path reaches the prompt.

## Report-to-Evidence Mapping

- Reported symptom — **Root cause confirmed by source trace and cancellation-path runtime characterization.** The skipped callback leaves the target-panel state active, which keeps the map buttons hidden.
- Full physical swipe through `GutterFormActivity` and the production map screen — **NOT VERIFIED**. An Espresso swipe attempt did not reliably bring the confirmation alert into focus, and the emulator harness used a host stub rather than `MapWorkspaceFragment`.
- No formal `requirement.md` or acceptance-criteria IDs were supplied for this debug task; none are inferred here.

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterListBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/main/MainBlockingUiController.kt`

## Regression Risk

A fix must preserve draft contents and the current Confirm behavior that finalizes the multi-gutter session. Cancel must leave the drafts intact while resolving the UI to a coherent state. Do not finalize drafts for both choices without an approved requirement.

## Evidence Limits

The failing callback behavior was observed on an API 36 emulator through the same `Dialog.cancel()` entry point used by BottomSheet dismissal. The complete form-to-list-to-map gesture was not physically reproduced, and the map button visibility was established by tracing the production cleanup and blocking-policy code rather than asserting it on the emulator. These limits account for the remaining 5% uncertainty.

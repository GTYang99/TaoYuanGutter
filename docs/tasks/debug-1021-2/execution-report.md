# Implementation and Validation Report

## Revision and Environment

- Base revision: `815540aab261a20439ae2ceb39a1de897267e0c9`
- Branch: `codex/debug/debug-1021-2`
- Emulator: `emulator-5554`, Medium_Phone AVD, API 36
- For instrumentation install, the debug package was temporarily suffixed because the emulator had a higher-version package installed. The suffix and local properties symlink were removed after validation.

## Implementation

- `AddGutterListBottomSheet.confirmClose()` captures its Host before the BottomSheet cancellation detaches the fragment. Confirm now reaches the existing close/finalize/restore path after detachment.
- When the list sheet was dismissed by swipe, Cancel notifies a separate Host callback. `MapWorkspaceFragment` reopens the list from the active multi-gutter coordinator without finalizing or clearing drafts. Cancel from the close icon still only dismisses the alert and leaves the current sheet open.
- Added focused instrumentation coverage for detached-sheet Confirm and Cancel behavior, including draft-content preservation when the list is reopened.

## Emulator Validation

Both focused instrumentation cases passed on the emulator:

```text
:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.MainShellActivityTest#canceledListSheetCancelReopensList
BUILD SUCCESSFUL

:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.MainShellActivityTest#canceledListSheetConfirmNotifiesHostAfterDetach
BUILD SUCCESSFUL
```

The Cancel case verifies the detached sheet's prompt can be canceled, a new list sheet is added, and its draft content matches the prior list. The Confirm case verifies the old fragment is detached before the prompt is accepted and the Host callback still runs.

The Confirm case first hit Espresso's default root focus selection while the alert window was open. A single retry scoped Espresso to the dialog root and passed. The test result file is under `app/build/outputs/androidTest-results/connected/debug/` (ignored build output).

## Remaining Validation

- Full physical drag gesture through the production `GutterFormActivity` → upload/list sheet → `MapWorkspaceFragment` flow: **NOT VERIFIED**. Tests invoke `Dialog.cancel()`, the callback entry point Material BottomSheet uses after reaching `STATE_HIDDEN`.
- Production map button visibility was not asserted on the emulator; Confirm delegates to the existing `MapWorkspaceFragment.onAddGutterListConfirmedClose()` restoration path, while Cancel intentionally reopens the blocking list sheet.
- CI and independent Verification have not run.

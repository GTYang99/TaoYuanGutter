# Issue Log

## ISS-DBG-1021-2-001

- task_id: debug-1021-2
- phase: debug
- category: implementation_regression
- priority: P2
- title: Main map controls do not recover after unfinished gutter close confirmation
- status: open
- impact: The user cannot operate most main-map controls after leaving the unfinished multi-gutter upload flow.
- repro_steps:
  - Start adding a gutter and enter details in GutterFormActivity.
  - Return to the upload sheet.
  - Drag the sheet down until the unsent-draft confirmation appears.
  - Select Confirm or Cancel.
  - Observe the main map controls.
- expected: When the main map is shown, its controls are visible and usable unless another blocking operation is active.
- actual: Main map buttons remain hidden or disabled.
- evidence:
  - AddGutterListBottomSheet.kt:77-84, 150-152
  - MapWorkspaceFragment.kt:1072-1080, 1122-1127, 1194-1207, 1779-1791
  - MainBlockingUiController.kt:64-90
  - Emulator characterization: after dialog cancellation the list fragment was detached; the prompt appeared, but Confirm did not reach the Host callback. Full physical swipe and map-button assertion remain NOT VERIFIED.
- next_action: implementation_debug

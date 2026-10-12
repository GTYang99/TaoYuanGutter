# Issue Log

## ISS-1012-001

issue_id: ISS-1012-001
task_id: debug-1012
phase: debug
category: implementation_regression
priority: P2
title: Existing-gutter update failure leaves the blocking spinner active after save draft
status: resolved
impact: Before the fix, the failed update sheet could close while the host loading state remained true, leaving the blocking progress overlay and rotation animation active.
repro_steps:
  - Open an existing gutter in edit mode.
  - Submit the update and have storeDitch return HTTP 403.
  - Choose “存入草稿” in the upload-failure dialog.
expected: The blocking loading animation stops before the failure decision dialog; retry can restart loading, and choosing save draft completes the existing host close flow with loading stopped.
actual: The edit API-error and exception branches now clear host loading before presenting their existing failure UI. Instrumentation on Medium_Phone returned HTTP 403 twice, verified loading stopped after each response, verified retry re-entered loading, and verified save-draft dismissed the sheet.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1275
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1298-1306, 1355-1362, 1380-1400
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1424-1430
  - app/src/main/java/com/example/taoyuangutter/MainActivity.kt:1011-1014
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:999-1005
  - app/src/main/java/com/example/taoyuangutter/main/MainBlockingUiController.kt:159-172
  - app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt:991-1007
  - app/src/main/java/com/example/taoyuangutter/common/UploadFailureClassifier.kt:73-99
next_action: verification
owner: developer

## Notes

- The controlled emulator run used a fake API response; the original production session was not captured.
- Draft persistence remains outside the approved spinner requirement and was not asserted.
- Independent Verification and CI remain pending.


## Investigation Addendum

- MainShellActivity defaults to MapWorkspaceFragment; the standard single-gutter callback returns false and does not clear inspect loading.
- Sheet dismissal recovery does not clear the loading flag; it returns while the blocking controller reports busy.
- ADB device discovery was unavailable because the daemon failed to start with Operation not permitted.
- Separate persistence check: the single-gutter save-draft callback may only dismiss, and autosave can skip when no current/matching draft ID exists. This is not established as the reported issue and remains outside ISS-1012-001.

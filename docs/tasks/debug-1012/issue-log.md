# Issue Log

## ISS-1012-001

issue_id: ISS-1012-001
task_id: debug-1012
phase: debug
category: implementation_regression
priority: P2
title: Existing-gutter update failure leaves the blocking spinner active after save draft
status: open
impact: The failed update sheet can close while the host loading state remains true, leaving the blocking progress overlay and rotation animation active.
repro_steps:
  - Open an existing gutter in edit mode.
  - Submit the update and have storeDitch return HTTP 403.
  - Choose “存入草稿” in the upload-failure dialog.
expected: The update is retained as a draft and the blocking loading animation stops after the decision is handled.
actual: Static control-flow review shows the edit error handler does not clear host loading; the save-draft callback may dismiss the sheet without clearing it.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1275
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1296-1348
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt:1424-1430
  - app/src/main/java/com/example/taoyuangutter/MainActivity.kt:1011-1014
  - app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt:999-1005
  - app/src/main/java/com/example/taoyuangutter/main/MainBlockingUiController.kt:159-172
  - app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt:991-1007
  - app/src/main/java/com/example/taoyuangutter/common/UploadFailureClassifier.kt:73-99
next_action: debug
owner: developer

## Notes

- The source review identifies a possible runtime occurrence, but the reported case was not reproduced on a device.
- The expected terminal state is taken from the user's issue report; no approved task requirement or formal AC file was present.


## Investigation Addendum

- MainShellActivity defaults to MapWorkspaceFragment; the standard single-gutter callback returns false and does not clear inspect loading.
- Sheet dismissal recovery does not clear the loading flag; it returns while the blocking controller reports busy.
- ADB device discovery was unavailable because the daemon failed to start with Operation not permitted.
- Separate persistence check: the single-gutter save-draft callback may only dismiss, and autosave can skip when no current/matching draft ID exists. This is not established as the reported issue and remains outside ISS-1012-001.

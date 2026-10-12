# Analysis

## Task Classification

- Task ID: debug-1012
- Type: debug
- Source revision reviewed: ac6c9dd (feat/feat-1006-gpkg-restart)
- Task branch: codex/debug-1012
- Scope: determine whether a 403 response followed by choosing “存入草稿” can leave the upload spinner running.

## Current Behavior

Static source tracing finds the reported state in the existing-gutter update flow:

1. The edit submit path calls the host's onGutterSubmitting callback before storeDitch. MainActivity and MapWorkspaceFragment set the blocking loading state to visible.
2. A non-network storeDitch API error such as HTTP 403 is classified as a general store failure and opens the retry/save-draft dialog.
3. The edit error handler restores the bottom sheet and submit button, but does not call onGutterSubmitFailed to clear the host loading state.
4. Choosing “存入草稿” calls onGutterUploadFailureConfirmed. MainActivity uses the interface default false result; MapWorkspaceFragment also returns false outside a multi-gutter session. The dialog callback then dismisses the sheet without clearing the loading state.
5. MainBlockingUiController keeps the indeterminate progress overlay and rotation animation active while inspectLoadingVisible remains true.

The create-new-gutter storeDitch error path calls both setSubmitLoading(false) and onGutterSubmitFailed before opening its dialog. That path does not show the same missing-cleanup condition in static review.

## Expected Behavior

After an unsuccessful update is acknowledged with “存入草稿”, the upload loading overlay and its animation should stop, and the form/map should return to the normal state. This is the expected behavior expressed by the current user report; no approved requirement.md or formal acceptance-criteria document exists for task 1012.

## Affected Modules

- app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt — edit submission error and exception paths.
- app/src/main/java/com/example/taoyuangutter/MainActivity.kt — host loading state is enabled and the existing cleanup callback is implemented, but the save-draft callback has no host override there.
- app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt — same loading callback; save-draft cleanup is only handled for multi-gutter sessions.
- app/src/main/java/com/example/taoyuangutter/main/MainBlockingUiController.kt — controls overlay visibility and starts/stops the spinner animator.
- app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt and app/src/main/java/com/example/taoyuangutter/common/UploadFailureClassifier.kt — preserve the HTTP status and route 403 to the general failure classification.

## Dependencies

- LocationPickerHost submission callbacks.
- storeDitch response handling and UploadFailureClassifier.
- The host-specific MainBlockingUiController state.
- Existing draft autosave and sheet dismissal behavior.

## Risks

A fix should clear the loading state for the failed edit request before displaying the decision dialog. The retry action must still re-enter the edit submit flow, which shows loading again. Draft persistence should keep using the current host/session flow; this analysis does not establish whether every production host persists the latest edit at dialog dismissal.

## Unknowns and Limitations

- Static review confirms the code path can retain the loading state; no device reproduction or user-session log was available, so the live occurrence is NOT VERIFIED.
- The reported 403 is assumed to be from storeDitch because the reported dialog offers “重傳” and “存入草稿”. If the status came from a photo endpoint instead, that path needs separate trace evidence.
- Task-specific requirement.md, plan.md, verification.md, logs, and CI results are absent. Formal failed-AC mapping and independent runtime verification are therefore pending.
- ai/implementation-debug.md has unresolved merge-conflict markers around its issue-record output line. The higher-precedence AGENTS.md and ai/issue-management.md require/use an issue log, so this task records one without modifying the rule file.


## Cross-Path Audit

- MainShellActivity selects MapWorkspaceFragment as the default workspace. Its isMultiGutterSession flag defaults to false.
- MapWorkspaceFragment clears loading and saves the draft in onGutterUploadFailureConfirmed only when isMultiGutterSession is true. In the normal single-gutter path it returns false. The edit dialog then dismisses the sheet without clearing loading.
- Both host dismissal/recovery paths check busy state or restore buttons only; neither sets inspect loading false. The busy check therefore preserves the active overlay.
- The only production LocationPickerHost implementations are MainActivity and MapWorkspaceFragment. The shared edit failure callback has no other cleanup implementation.
- A source search found no app/src/test coverage for onGutterSubmitFailed, onGutterUploadFailureConfirmed, or the MainBlockingUiController loading lifecycle.
- The draft choice does not itself persist data in MainActivity's default callback. Edit changes flow through autosave; GutterDraftCoordinator skips a save if there is no active draft ID and no existing draft matching the gutter SPI_NUM. This is an adjacent persistence question and is not needed to explain the spinner.

## Confidence

Confidence is 97% that the reviewed source revision contains the reported spinner-retention path for a standard existing-gutter update returning a non-network 403. This is based on the complete activation, error, host callback, dismiss, and overlay-stop path. Confidence refers to the source-level behavior on ac6c9dd; the live report was not reproduced.

The Android Debug Bridge could not start in this environment: daemon startup failed with Operation not permitted before a device list was returned. Physical-device behavior remains NOT VERIFIED.

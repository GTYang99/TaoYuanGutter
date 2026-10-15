# Root Cause Analysis

## Conclusion

Yes. Static tracing found the unbalanced loading lifecycle, and a controlled Medium_Phone emulator test reproduced the 403 failure/dialog path. It confirmed loading stops after each error, retry activates it again for the next request, and choosing “存入草稿” closes the sheet with loading stopped. The test used an in-process fake API, so it confirms the client path without depending on a production server or account.

The root cause is an unbalanced loading lifecycle in AddGutterBottomSheet.performEditSubmit(): the edit flow turns the host loading state on before storeDitch, but its API-error and generic-exception paths do not turn that state off. The save-draft action can dismiss the sheet without invoking the host's loading cleanup.

## Evidence Chain

1. Edit submission turns loading on. AddGutterBottomSheet.kt:1275 invokes onGutterSubmitting. MainActivity.kt:1011-1014 and MapWorkspaceFragment.kt:832-835 set inspect loading visible.
2. The controller renders the blocking progress overlay and starts the infinite rotation animation while inspect loading is visible. MainBlockingUiController.kt:26-39, 159-172, 192-201.
3. GutterRepository.storeDitch preserves non-success HTTP status codes in ApiResult.Error. For a 403 response without a parsed response body, GutterRepository.kt:1004-1007 returns the response code with the API message or “儲存失敗（403）”.
4. UploadFailureClassifier.forStoreDitchError has specific handling for 401 and 422; other non-network errors, including 403, use the general store failure model. UploadFailureClassifier.kt:73-99. AddGutterBottomSheet.kt:1337-1348 then opens the dialog with “存入草稿” and “重傳”.
5. In the edit ApiResult.Error branch, AddGutterBottomSheet.kt:1296-1302 restores the sheet/button but has no onGutterSubmitFailed callback. The save-draft action at 1343-1347 calls onGutterUploadFailureConfirmed and dismisses when the host returns false.
6. MainActivity does not override onGutterUploadFailureConfirmed, so it inherits the interface default false at AddGutterBottomSheet.kt:120. MapWorkspaceFragment.kt:999-1005 only handles that callback for multi-gutter sessions. Outside that mode it returns false, so the caller dismisses without clearing inspect loading.
7. The create-new-gutter error branch does clear both the form submit loading and host loading at AddGutterBottomSheet.kt:1424-1430. This isolates the missing cleanup to the edit/update error path.

## Why It Occurs

The edit and create submission flows have separate error handlers. The create handler balances the loading state before presenting an error dialog; the edit handler does not. The save-draft callback is expected to hand cleanup to the host, but the common MainActivity host has no implementation for that callback, and the single-gutter MapWorkspaceFragment path also returns false without cleanup. As a result, inspectLoadingVisible remains true after the sheet closes, so the controller continues the indeterminate animation and keeps the blocking overlay visible.

## Classification

- Issue: ISS-1012-001
- Category: implementation_regression (reported behavior conflicts with the expected terminal state of the upload flow; formal acceptance criteria are not available)
- Priority: P2 (local failure-path UI lock affecting existing-gutter updates)
- Affected behavior: failed existing-gutter update followed by save draft.

## Regression Risk

The fix should use the existing host cleanup callback for edit failures and preserve the retry flow. Retrying must show loading again. Draft data handling and HTTP classification should remain unchanged unless a later approved plan identifies a separate gap.

## Verification Limits

The controlled runtime reproduction is recorded in `execution-report.md`. The original production session was not captured, physical-device behavior was not tested, and independent Verification and CI remain pending.


## Cross-Path Audit and Confidence

MainShellActivity's default destination is MapWorkspaceFragment, where isMultiGutterSession starts false. The normal single-gutter save-draft callback returns false without clearing loading; only the multi-gutter branch clears loading and saves before returning true. MainActivity has no override, so it uses the interface default false.

Dismissing the sheet does not clear the state indirectly. AddGutterBottomSheet.onDismiss notifies the host; the MainActivity and MapWorkspaceFragment dismissal paths attempt to restore the UI, but their busy checks see inspectLoadingVisible still true and return without changing it. MainBlockingUiController stops the animation only when inspect loading and photo-upload blocking are both false.

The new-gutter store failure path calls onGutterSubmitFailed before showing its dialog. The post-store photo-upload path also balances its blocking state in a finally block. These checks narrow this finding to the existing-gutter edit/storeDitch generic failure dialog, including HTTP 403.

Confidence: high that the source path in revision ac6c9dd caused the spinner to persist. The client flow was reproduced with an HTTP 403 fake response on the approved Android emulator, and the modified flow passed its retry/save-draft regression test. The user's original production response was not captured, so the conclusion assumes the reported 403 came from `storeDitch`, consistent with the displayed retry/save-draft dialog.

Adjacent persistence note: the single-gutter save-draft callback may only dismiss the sheet. Edited values are autosaved through the normal change callbacks, but GutterDraftCoordinator returns without saving when there is no active session draft ID and no matching existing draft for the SPI_NUM. Whether the user's affected session had such a draft is unknown; this does not change the spinner root cause.

# Root Cause Analysis

## Conclusion

Yes, the code contains a path that can leave the loading animation active after choosing “存入草稿” when an existing gutter update receives HTTP 403. The confirmation is from static control-flow evidence; it has not been reproduced on a device.

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

No runtime logs, device reproduction, task verification report, or formal requirement/plan were available. The static cause is identified, but task state remains in debug because the repository's required inputs and runtime evidence are incomplete.

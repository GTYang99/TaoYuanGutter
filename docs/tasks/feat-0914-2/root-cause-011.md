# Root Cause Analysis — ISS-FEAT-0914-2-011

## Failure

AC-008 fails when a new multi-gutter submission receives a network-classified `storeDitch` failure. After the user closes the failure Alert, the app returns to the main map instead of the active add-gutter list, so the failed gutter cannot be immediately edited or retried.

## Root cause

The network-failure Alert in `AddGutterBottomSheet.submitNewGutterRequest()` invokes `LocationPickerHost.onStoreDitchNetworkClosed()` for both legacy and multi-gutter flows. The callback contract does not carry the workflow boundary; `MapWorkspaceFragment.onStoreDitchNetworkClosed()` therefore applies the legacy cleanup behavior unconditionally:

1. save the waypoints as a pending draft;
2. clear the active sheet callback;
3. dismiss the form;
4. when `spiNum` is null (the normal new-gutter case), restore the main-map UI.

The multi-gutter-specific `returnToMultiGutterListAfterUploadFailure()` function already exists, but it is only used by later photo-upload failure paths. `onStoreDitchNetworkClosed()` never branches on `isMultiGutterSession` and never calls that function. Consequently, the draft is preserved but the navigation state loses the active add-list context.

This is an implementation regression, not a requirement or API error: the approved requirement says every failed submission must return to the same add-gutter list and retain the failed item.

## Evidence

- `AddGutterBottomSheet.kt`: new `storeDitch` network error and exception branches call `onStoreDitchNetworkClosed(...)` after the Alert's `關閉` action.
- `MapWorkspaceFragment.kt`: `onStoreDitchNetworkClosed()` saves the draft, dismisses `activeSheet`, and routes the null-`spiNum` branch through legacy main-map restoration.
- `MapWorkspaceFragment.kt`: `returnToMultiGutterListAfterUploadFailure()` already restores the list only when `isMultiGutterSession` is true, but the network callback does not use it.
- `verification.md`: AC-008 source review and authenticated failure-path result classify the failure as an implementation regression.

## Affected acceptance criterion

- AC-008

## Regression risk

The fix must preserve legacy single-gutter behavior, keep the failed multi-gutter draft and its photos, and avoid removing the item from `MultiGutterSessionCoordinator`. Only the post-Alert navigation/cleanup branch should change.

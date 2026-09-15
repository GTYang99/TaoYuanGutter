# Fix Plan — ISS-FEAT-0914-2-011

## Scope

Fix only the network-classified `storeDitch` failure close path for AC-008.

## Minimum change

Update `MapWorkspaceFragment.onStoreDitchNetworkClosed()` so that after saving the failed waypoints:

- multi-gutter sessions call the existing `returnToMultiGutterListAfterUploadFailure()` path;
- legacy sessions retain the current main-map restoration behavior;
- no draft or session item is deleted.

## Validation

- Add or update a deterministic callback-flow test proving multi-gutter failure returns to the list and legacy failure still restores the map.
- Run focused unit tests, full unit tests, `assembleDebug`, and connected instrumentation with emulator animations disabled.
- Perform authenticated physical-device smoke: force a network-classified `storeDitch` failure, close the Alert, verify the same add-list and failed row remain available for retry.

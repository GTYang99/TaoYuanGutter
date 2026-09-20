# Follow-up Implementation Plan

## Scope

Extend `debug-0920-1` with the two explicitly requested form-transition fixes:

- AC-006: warn and clear when switching on `銜接點`.
- AC-007: clear `接管／連結管` when switching on `無法開蓋`.

## Minimum implementation

1. Route `cbConnectPoint` through the existing detail-exemption confirmation and snapshot flow, using the exact requested Alert message.
2. Clear the shared detail fields, measurement photo slots 2/3, and `rgConnectPipe` after confirmation.
3. Include `IS_CONNECTING` in the existing session snapshot and restore the connecting-pipe radio state when the transition is cancelled or reversed.
4. Preserve mutual exclusion, import/view locking, existing cant-open behavior, and draft synchronization.
5. Add focused Android UI coverage and run the relevant JVM, build, and connected-device regressions.

## Acceptance traceability

| AC | Implementation | Validation |
|---|---|---|
| AC-006 | `GutterBasicInfoFragment` transition listener and shared snapshot clear path | tie-in UI test: warning, cancel preservation, confirm clearing |
| AC-007 | clear/snapshot/restore `IS_CONNECTING` and `rgConnectPipe` | cant-open UI test plus full connected suite |

## Risks

- A programmatic prefill or configuration recreation must not show a user warning.
- Cancelling or reversing either mutually exclusive selection must not lose existing field, photo, or connecting-pipe data.
- Clearing the radio group must not alter unrelated API keys or photo upload metadata.

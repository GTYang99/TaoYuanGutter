# Fix Plan

This is the minimum implementation scope after root-cause analysis. It does not authorize unrelated refactoring.

1. Preserve the existing `GutterCompletionPolicy` tie-in-point behavior from `07e3b5a`; add or adjust only focused regression coverage if a runtime/source test exposes a remaining bypass.
2. In `GutterFormActivity.handleImportedNodeDetails()`, remove only the specified missing-photo completion Toast. Preserve import locking, local photo synchronization, upload-state/image-id bookkeeping, loading cleanup, and genuine exception handling.
3. In `GutterInspectActivity`, bypass the `進入編輯確認` dialog for the no-photo and photo-issue warning paths while preserving the separate node-detail failure retry/block path and the existing preload data flow.
4. Align inspection silt rendering with the approved three-option encoding: code `2` must render `嚴重`, and legacy code `3` must also render `嚴重`. Update stale comments/contracts only where needed to make the chosen mapping explicit.
5. In the inspection-to-edit path, keep `tvMeasureIdTitle` visible but locked/read-only, remove its manual-entry/required-marker gate, and preserve any backend-provided `XY_NUM` needed to identify the existing node during update. Do not broadly remove backend identity fields.
6. Reuse the existing tie-in-point and mapper regression tests, and add focused completion-policy coverage for the backend-generated measurement-id contract. Import warning, edit-entry routing, and inspection silt mapping remain UI/runtime checks because the current repository has no direct JVM seam for those private Android flows.
7. Run `git diff --check`, targeted tests, the affected debug build, and record the exact runtime/device evidence and any remaining release-gate limitations.
8. Reuse the existing detail-exemption snapshot for `cbConnectPoint`: show the exact requested confirmation message, clear the shared detail fields/photos/connecting-pipe selection after confirmation, and restore the snapshot on cancellation/uncheck.
9. Extend the cant-open clear/snapshot/restore contract to include `IS_CONNECTING`; add focused UI regression tests for tie-in confirmation/cancellation and connecting-pipe clearing.

## Follow-up API and inspection fix scope

The user clarified that an API "empty value" means the parameter is omitted from the JSON request entirely. This scope is limited to the reported exempt-mode fields:

10. In `StoreDitchNodeRequestMapper`, treat `IS_CANTOPEN || IS_TIEINPOINT` as detail-exempt. Keep the mode flags (`IS_CANTOPEN`, `IS_TIEINPOINT`) and normal identity/location fields, but omit `IS_CONNECTING`, `MAT_TYP`, `NODE_DEP`, `NODE_WID`, `COVER_DEP`, `IS_BROKEN`, `IS_HANGING`, and `IS_SILT` for exempt nodes. Do not use numeric fallbacks for omitted fields.
11. Omit photo association IDs for exempt photo slots 2 and 3 while preserving slot 1, matching the existing cant-open behavior and the documented exempt photo fields.
12. In the inspection point renderer, apply the same detail exemption to `IS_TIEINPOINT` as to `IS_CANTOPEN`, hiding the exempt measurements, detail attributes, and photo slots 2/3.
13. Add mapper JSON regression tests for tie-in and cant-open omission semantics, plus a focused inspection presentation predicate test. Update the stale mapper expectation that currently requires `IS_CONNECTING` for cant-open.

14. Preserve a missing `IS_CONNECTING` value during inspection-to-edit preload and form prefill. Select `rbConnectPipe0` or `rbConnectPipe1` only for an explicit value; otherwise leave both unselected. Remove the field from normalized exempt-mode draft state so later draft synchronization cannot recreate a false selection. Add focused UI coverage for both 銜接點 and 無法開蓋.
15. In `AddGutterBottomSheet.preloadEditWaypointDetails`, preserve an empty `IS_CONNECTING` from `/v1/node/nodeDetails` instead of deriving `0` through `isConnectingAsBoolean`. In `GutterFormContract`, preserve an empty connection value across form and result extras. Keep explicit `0`/`1` values unchanged and do not alter other fields.

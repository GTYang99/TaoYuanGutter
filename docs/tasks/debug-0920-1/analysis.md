# Repository Analysis

## Current Branch and Evidence Boundary

- Branch checked: `fix/debug-0920-1-表單問題`.
- HEAD checked: `b40e79b`.
- Working tree contains an existing untracked `.worktrees/` entry. It is outside this task's scope and was not modified.
- The follow-up implementation is committed in `7c43493`; runtime/device evidence is available from XQ-AU52 / Android 12.

## Follow-up Scope — AC-006 and AC-007

The attached defect report adds two form-transition defects to this task. They are limited to the existing `GutterBasicInfoFragment` state machine and its session snapshot:

### AC-006 — Tie-in point transition has no warning or clear

`setupConnectPointAndPipe()` currently changes the checkbox state, applies mutual exclusion, and notifies the draft immediately. It does not call the existing confirmation flow used by `cbCantOpen`, does not clear the detail fields or measurement photos, and does not retain a snapshot for cancellation/restoration. This is why selecting `銜接點` can leave incompatible lower-section data in the form without warning.

### AC-007 — Cant-open clear omits connecting pipe

`clearCantOpenFieldsAndPhotos()` clears cover thickness, depth, top width, material, damage, hanging, silt, and measurement photo slots 2/3, but never clears `rgConnectPipe`. `CantOpenSessionViewModel.CANT_OPEN_FIELDS` also omits `IS_CONNECTING`, and `restoreCantOpenSessionState()` does not render that field from a restored snapshot. As a result, the UI retains the connecting-pipe choice and the clear/restore model is incomplete for this field.

### Shared transition contract

Both mutually exclusive detail-exemption modes must use the same snapshot/clear/restore boundary. The existing cant-open session holder is the minimal reuse point; adding `IS_CONNECTING` to its captured field set and rendering it on restore preserves existing cancellation and configuration-recreation behavior without introducing a second state store.

## Current Behavior

### 1. Tie-in-point exemptions

The original rule was split across UI state, required indicators, form validation, photo validation, and Add Gutter submission validation. In the current HEAD, commit `07e3b5a` has already introduced the shared `GutterCompletionPolicy` and applies `IS_TIEINPOINT` alongside `IS_CANTOPEN`:

- `GutterBasicInfoFragment.kt:855-876` disables detail fields and photo slots 2/3 when a tie-in point is selected.
- `GutterBasicInfoFragment.kt:922-949` hides the corresponding required indicators.
- `GutterBasicInfoFragment.kt:1142-1166` returns after identity-field validation for tie-in points.
- `GutterBasicInfoFragment.kt:1449-1467` validates only the required photo slots for the selected mode.
- `GutterCompletionPolicy.kt:25-41` and `AddGutterBottomSheet.kt:1763-1791` share the exemption rule for completion/submission checks.

Therefore, the attached tie-in-point defect describes a historical condition that is already addressed in the current branch, but AC-001 still needs focused runtime verification. The current task should avoid re-implementing this behavior unless verification finds a remaining path that bypasses the shared policy.

### 2. Import feedback

`GutterFormActivity.handleImportedNodeDetails()` starts local photo downloads at `:618`. After syncing the downloaded photo state, it builds `missing` at `:714-721` and unconditionally displays the exact reported Toast at `:722-727` whenever a required local photo is absent. The import flow therefore produces the unwanted message even though the import itself and the form lock have already completed.

### 3. Inspection-to-edit entry

`GutterInspectActivity.preloadAllNodeDetailsThenOpenEditForm()` checks whether the server has no photo URL at `:301-303`. When true, it calls `showEditEntryConfirmation()` at `:304-310`. After preload, any `photoIssues` calls the same confirmation helper through `showEditPreloadWarning()` at `:323-330` and `:577-584`. The helper renders the title `進入編輯確認` at `:595-617`.

The unwanted dialog is therefore a deliberate warning branch in the inspection edit entry flow, not an incidental Android dialog or a bottom-sheet behavior.

### 4. Silt level display

The form defines only `無`, `輕度`, `嚴重` in `GutterBasicInfoFragment.kt:216-218` and encodes `嚴重` as `IS_SILT = "2"` at `:2129-2133`. Inspection display is implemented in `GutterInspectPhotosFragment.kt:485-490`, where code `"2"` is still mapped to `中度` and only `"3"` is mapped to `嚴重`.

Consequently, the form's severe selection is serialized as code `2` and then rendered as `中度` during inspection. `GutterInspectBasicFragment` is the user-reported inspection area, but the actual silt label mapping currently lives in `GutterInspectPhotosFragment`.

### 5. Measurement coordinate number in inspection-to-edit

The inspection edit flow carries `XY_NUM` from `GutterInspectActivity` into each `Waypoint` and opens `AddGutterBottomSheet` in edit mode. However, the form fragment treats edit/view mode as requiring the user-facing measurement identifier:

- `GutterBasicInfoFragment.kt:937-939` shows the required marker whenever `ARG_IS_EDIT_MODE` or `ARG_VIEW_MODE` is true.
- `GutterBasicInfoFragment.kt:1148-1163` rejects the form when `XY_NUM` is empty in those modes.
- `GutterBasicInfoFragment.kt:1025-1037` includes `etMeasureId` in the text fields enabled by `setEditable(true)`, so edit mode can also expose the identifier as editable rather than locked.
- `AddGutterBottomSheet.kt:734` and `:1764-1767` propagate `editSpiNum.isNotEmpty()` as a requirement for every waypoint.
- `WaypointAdapter.kt:30-67` uses the same flag in the completion state.

The attached requirement says this identifier is backend-generated and is not a user-entered value in the inspection-to-edit flow. The current validation/completion contract therefore overstates the user's responsibility, even though normal existing records may already carry `XY_NUM` from the API.

The prior approved `docs/tasks/feat-0917/requirement.md:27-28,42` resolves the UI form of this requirement: inspection still displays `XY_NUM`, edit still displays it locked/read-only, and existing edit requests preserve it. This removes the earlier ambiguity between hiding the field and removing the manual-entry gate.

## Expected Behavior

- AC-001 remains consistent across UI enabled state, required markers, form validation, photo validation, list completion, and submit validation.
- Import completion should silently finish photo synchronization with respect to missing optional/unavailable imported photos; the specified missing-photo Toast must not be shown.
- Inspection edit entry should proceed without `進入編輯確認`; genuine detail-fetch failure handling must not be weakened.
- The canonical displayed silt choices are `無`, `輕度`, `嚴重`, and the code emitted for severe must display as `嚴重`.
- Inspection-to-edit must not block on manual `XY_NUM` entry; preserve and use a backend-provided identifier when one exists.

## Affected Modules

- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterCompletionPolicy.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectPhotosFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/WaypointAdapter.kt`
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt` (stale silt-code comments/contracts to review during implementation)

## Dependencies

- `GutterCompletionPolicy` and `PhotoUploadValidator` determine required fields/photos.
- `handleImportedNodeDetails()` owns import download feedback and persisted photo state.
- `GutterInspectActivity` owns preload and edit-entry branching.
- `GutterBasicInfoFragment` owns form silt encoding; inspection fragments own display mapping.
- API `IS_SILT` values and legacy data may contain both `2` and `3`.
- `XY_NUM` is carried from `NodeDetails`/edit snapshots and may be absent in incomplete or newly created points.
- `docs/tasks/feat-0917/requirement.md:27-28,42` is an approved prior decision for display/lock/preservation behavior.

## Risks

- Reworking tie-in-point logic could regress the recently verified `IS_CANTOPEN` and shared completion behavior.
- Suppressing the import Toast must not suppress actual download-error handling or leave the loading indicator active.
- Removing the edit confirmation must not bypass the existing hard stop for failed node-detail loading or cause stale data to be overwritten.
- Changing silt code mapping must preserve legacy value `3` as severe and avoid introducing a fourth visible option.
- Relaxing the user-facing `XY_NUM` requirement must not remove the identifier needed to address an existing backend node during update.

## Unknowns

- No device/log evidence is attached for the current branch, so the already-fixed tie-in-point behavior and the exact dialog timing are not independently runtime verified.
- Runtime/device evidence is still unavailable; static analysis cannot prove the exact dialog timing or post-rebuild UI state.

## Resolved Evidence

- `docs/tasks/feat-0917/requirement.md:27-28,42` explicitly resolves `XY_NUM`: new forms do not request it, inspection/edit displays it locked, and edit preserves it.
- Ancestor commit `f1ddaca` is included in the inspected branch and explicitly changes the form contract to `0=無`, `1=輕度`, `2=嚴重`, with legacy `3` normalized to severe. The remaining defect is isolated to the inspection mapper.
- `GutterCompletionPolicyTest.kt:10-15` directly covers tie-in-point detail/photo exemption; existing tests also cover create omission and edit preservation of `XY_NUM` in `StoreDitchNodeRequestMapperTest.kt:51-65,98-104`.

## Validation Boundary

- Static source and requirement trace: `PASS`.
- Document whitespace check: `PASS`.
- Targeted JVM tests (`GutterCompletionPolicyTest`, `StoreDitchNodeRequestMapperTest`, `InspectionPresentationTest`): `PASS`.
- Debug build: `PASS` via `assembleDebug`.
- Device/UI runtime validation on `Medium_Phone` Android 14: `PASS` for the scoped AC tests and nearby regression tests.

## Implementation Result

- AC-002: removed only the post-import missing-photo completion Toast; photo synchronization, lock state, loading cleanup, and exception handling remain.
- AC-003: removed only the no-photo/photo-issue confirmation routes; node-detail preload failure still uses the retry/block dialog.
- AC-004: inspection code `2` now renders `嚴重`; legacy code `3` remains `嚴重`.
- AC-005: existing/view-mode `XY_NUM` remains displayed and preserved, but the field is read-only and is no longer a manual completion requirement in the edit flow.
- AC-001: no new production change was required because the shared tie-in exemption from `07e3b5a` is already present; the existing focused policy test remains the evidence source.

## API and inspection follow-up

The attached report's unfinished work item clarified that an exempt API value means the JSON parameter is omitted completely. `StoreDitchNodeRequestMapper` previously used only `isCantOpen` for API detail suppression. As a result, a tie-in point could still receive numeric defaults (`MAT_TYP=1`, measurement/detail fields `0`) and both exempt modes could still send `IS_CONNECTING=false` or a stale `true` value. The form layer also normalizes an unselected connecting-pipe radio group to `"0"`, so the mapper must apply the omission rule at the request boundary rather than relying on form state to represent "not applicable".

The inspection renderer had the same predicate gap: `GutterInspectPhotosFragment` rendered measurements, detail rows, and photo slots 2/3 whenever the node was not virtual and not cant-open. It did not check `IS_TIEINPOINT`, so the attached `nodeDetails` example would display exempt values such as depth `89`, width `78`, and the three detail attributes. The fix now shares the exemption condition for the renderer and for inspection-to-edit photo preload.

The minimum fix keeps `IS_CANTOPEN` and `IS_TIEINPOINT` as mode-identification flags, keeps normal-point `IS_CONNECTING=false` behavior, and omits the exempt detail parameters plus photo association slots 2/3 for both exempt modes. Gson's default serialization omits the resulting nullable properties, matching the clarified API contract.

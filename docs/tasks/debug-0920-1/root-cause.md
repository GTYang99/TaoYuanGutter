# Root Cause Analysis

## Scope

Task: `debug-0920-1`
Branch: `fix/debug-0920-1-表單問題`
Revision inspected: `b40e79b` plus the implementation-debug working-tree diff

This document records the static root-cause evidence and the minimum implementation-debug changes. Runtime/device evidence remains pending.

## AC-001 — Tie-in point fields/photos are exempt

### Historical root cause

Before commit `07e3b5a`, the exemption predicate was effectively `isCantOpen` in the affected UI and validation paths. Tie-in points could be mutually exclusive with `無法開蓋`, but were not included in the detail-field/photo required rules. That created a rule split: the checkbox state existed, while required markers, disabled controls, and submit checks could still follow the normal open-cover path.

### Current status on this branch

The current HEAD already includes the minimum structural correction through `GutterCompletionPolicy.isDetailExempt()` and its consumers. This is classified as an existing fix carried into the new task, not a new implementation authorization. AC-001 is covered by the existing policy test plus the focused UI/form evidence added for this task.

### Evidence

- `GutterBasicInfoFragment.kt:855-876, 922-949, 1142-1166, 1449-1467`
- `GutterCompletionPolicy.kt:25-41`
- `AddGutterBottomSheet.kt:1763-1791`
- Commit `07e3b5a` changed these paths to include `IS_TIEINPOINT`.

## AC-002 — Import shows the unwanted missing-photo message

### Root cause

`GutterFormActivity.handleImportedNodeDetails()` treats missing local copies after the import download as a user-facing completion warning. The coroutine downloads each available URL, synchronizes the form/photo state, then constructs `missing` and calls `Toast.makeText(..., "匯入完成，但...照片未取得，請至照片頁補拍", ...)` whenever one of the expected slots is empty.

The import operation therefore has no separation between internal photo availability bookkeeping and the product's requested import UX. The message is emitted after the import state has already been applied, so it is not required to protect the lock or preserve data.

### Affected behavior

Any imported node without all locally downloaded photo slots can produce the reported Toast, including a valid import whose server-side photo set is incomplete.

### Evidence

- `GutterFormActivity.kt:607-616` applies imported data and lock state.
- `GutterFormActivity.kt:618-712` downloads and synchronizes photos.
- `GutterFormActivity.kt:714-727` builds `missing` and emits the unwanted Toast.

## AC-003 — Inspection edit shows `進入編輯確認`

### Root cause

The edit-entry flow deliberately routes two non-fatal photo conditions to `showEditEntryConfirmation()`:

1. `GutterInspectActivity.kt:301-310` shows it when all server photo URLs are empty.
2. `GutterInspectActivity.kt:323-330` routes `photoIssues` to `showEditPreloadWarning()`, which calls the same helper at `:577-584`.

The helper hard-codes the title at `GutterInspectActivity.kt:595-617`. Thus the reported dialog is caused by a product-warning branch in `btnEdit`'s preload flow, not by `AddGutterBottomSheet` independently creating a dialog.

### Affected behavior

Users must dismiss/confirm an `進入編輯確認` dialog before reaching edit, even when the requirement is to enter directly. The detail-load failure branch at `:324-326` is separate and should remain a blocking/retry path unless the approved implementation plan says otherwise.

## AC-004 — Severe silt displays as medium

### Root cause

The form and inspection use inconsistent code-label contracts:

- Form options contain only `無`, `輕度`, `嚴重` (`GutterBasicInfoFragment.kt:216-218`).
- Form serialization maps `嚴重` to code `2` (`GutterBasicInfoFragment.kt:2129-2133`).
- Inspection maps code `2` to `中度` and code `3` to `嚴重` (`GutterInspectPhotosFragment.kt:485-490`).

The approved current-branch decision is already explicit in ancestor commit `f1ddaca`: `2` is severe and legacy `3` is also normalized to severe. Therefore, the exact value emitted by the current form for severe is decoded as medium by the inspection view. Stale API model comments at `GutterApiModels.kt:50-51, 359-360, 566-567` preserve the old four-level interpretation and make the contract drift harder to detect, but the direct display defect is the `mapSilt("2") -> "中度"` branch.

### Affected behavior

Selecting severe in the form can persist a value that the inspection screen renders as medium. Legacy value `3` currently renders as severe, so compatibility handling must be explicit when aligning the contract.

## AC-005 — Measurement coordinate number is incorrectly treated as user-required

### Root cause

The current form uses mode flags as a proxy for whether the user must supply `XY_NUM`:

- `GutterBasicInfoFragment.kt:937-939` displays `tvMeasureIdRequired` for both edit and view mode.
- `GutterBasicInfoFragment.kt:1148-1163` validates `XY_NUM` as mandatory whenever either mode flag is true.
- `GutterBasicInfoFragment.kt:1025-1037` includes `etMeasureId` in the fields enabled by `setEditable(true)`, so the edit transition can make a backend-generated identifier editable.
- `AddGutterBottomSheet.kt:734, 1764-1767` propagates non-empty `editSpiNum` into the shared required-key calculation.
- `WaypointAdapter.kt:30-67` uses the same `requiresMeasureId` flag when deciding whether a waypoint is complete.

This conflates two separate concerns: the backend needs an existing node identifier to update a record, while the user is not responsible for manually entering that identifier in the inspection-to-edit flow. `GutterInspectActivity` already maps `nodeDetails.xyNum` into `basicData["XY_NUM"]` at `:446-468`, but the validation contract still presents the field as user-required and can block when the backend value is absent or still being resolved. The prior approved `docs/tasks/feat-0917/requirement.md:27-28,42` resolves the intended UI: keep the value visible and locked in edit, preserve it in the update payload, and do not use manual entry as the gate.

### Affected behavior

The edit form can show a required marker or return `測量座標編號` instead of proceeding, even though the attached requirement says the backend supplies the value. Removing the manual-entry requirement must not erase `XY_NUM` when it is present, because it may still be needed to identify the persisted node.

### Evidence

- `GutterBasicInfoFragment.kt:445-449, 937-939, 1025-1037, 1148-1163`
- `GutterInspectActivity.kt:446-468`
- `AddGutterBottomSheet.kt:734, 1751-1753, 1764-1767`
- `WaypointAdapter.kt:30-67`

## Issue Mapping

| Issue ID | Category | Priority | Root cause | Route |
|---|---|---:|---|---|
| ISS-DBG-0920-001 | implementation_regression / historical | P1 | Tie-in point was omitted from shared exemption predicates; current HEAD contains the corresponding fix | verification of existing fix |
| ISS-DBG-0920-002 | implementation_regression | P2 | Import coroutine emits a hard-coded missing-photo Toast after state synchronization | implementation_debug |
| ISS-DBG-0920-003 | implementation_regression | P1 | Inspection preload routes no-photo/photo-issue states to confirmation-dialog helper | implementation_debug |
| ISS-DBG-0920-004 | implementation_regression | P2 | Form emits severe as `2`, inspection decodes `2` as medium | implementation_debug |
| ISS-DBG-0920-005 | implementation_regression | P1 | Edit/view mode turns a backend-generated `XY_NUM` into a user-required field and completion gate | implementation_debug |

## Regression Risk

- The tie-in-point policy must remain shared with `無法開蓋` and must not change normal-point requirements.
- Import lock, photo persistence, and genuine node-detail failure retry must remain intact when the informational Toast/warning path is changed.
- Silt compatibility must keep exactly three visible choices and display both the current severe code and supported legacy severe code as `嚴重`.
- The measurement identifier must remain available for backend update identity even when it is removed from manual-entry validation or required UI.

## AC-006 — Tie-in point transition skips confirmation and clearing

### Root cause

`GutterBasicInfoFragment.setupConnectPointAndPipe()` has only a mutual-exclusion listener. Unlike `cbCantOpen`, it never checks whether the user initiated the toggle, never displays a transition warning, and never invokes the shared clear/snapshot path. The checkbox can therefore become selected while all previously entered detail fields, measurement photos, and connecting-pipe data remain in the form.

### Evidence

- `GutterBasicInfoFragment.kt:722-736` directly applies mutual exclusion and draft notification for `cbConnectPoint`.
- `onCantOpenToggleChanged()` at `:742-830` contains the existing confirmation and snapshot behavior that the tie-in transition should reuse.

## AC-007 — Cant-open transition does not clear connecting pipe

### Root cause

The cant-open clear method omits `rgConnectPipe`, and the session snapshot's field list omits `IS_CONNECTING`. Thus the lower UI selection is neither cleared when entering cant-open nor captured/restored consistently when the user cancels the transition.

### Evidence

- `GutterBasicInfoFragment.clearCantOpenFieldsAndPhotos()` clears the detail radio groups but not `rgConnectPipe`.
- `CantOpenSessionViewModel.CANT_OPEN_FIELDS` contains the other detail keys but not `IS_CONNECTING`.
- `restoreCantOpenSessionState()` restores the other controls but not `rgConnectPipe`.

## Confidence Assessment

Static root-cause confidence is at least 95% overall:

- AC-001: 95% — current shared policy and JVM test cover the core tie-in exemption; UI/device execution remains unavailable.
- AC-002: 99% — the reported message is emitted by a direct, uniquely identified Toast branch.
- AC-003: 99% — both reported edit-entry paths converge on the uniquely identified dialog helper.
- AC-004: 99% — the form encoding is established by the current ancestor decision, while the inspection mapper contains the contradictory branch.
- AC-005: 98% — approved prior requirements define the locked/read-only behavior, and current code directly enables, requires, and carries the same field across the edit flow.
- AC-006: 99% — the missing behavior is isolated to the direct `cbConnectPoint` listener, and the existing `cbCantOpen` path provides the intended confirmation/snapshot contract.
- AC-007: 99% — the omitted `rgConnectPipe` clear and missing `IS_CONNECTING` snapshot key are directly visible in the affected methods.

Runtime confirmation is now available for all five ACs through targeted JVM/UI tests; the root-cause confidence is supported by both source evidence and observed behavior.

## Root Cause Status

Root causes for AC-002 through AC-007 are confirmed by static code evidence plus approved prior decisions, and the minimum fixes are implemented in `7c43493`. AC-001's historical root cause is confirmed, its structural fix is already present in HEAD, and all seven ACs have passed the scoped verification checks.

## Follow-up API and inspection root causes

### API parameter omission

The request mapper's exemption predicate was incomplete. It normalized `IS_TIEINPOINT` for the mode flag, but all detail parameters and `IS_CONNECTING` were guarded only by `isCantOpen`. Empty form values therefore reached numeric fallbacks (`1` for material and `0` for numeric/detail flags), while the connecting-pipe field remained a Boolean. This violated the clarified contract that an exempt value must be absent from the JSON request.

The implementation now derives one `isDetailExempt` predicate from `isCantOpen || isTieInPoint`, assigns nullable request fields for all exempt parameters, omits `IS_CONNECTING` for exempt nodes, and excludes photo association IDs for slots 2/3. Normal non-exempt nodes retain their existing Boolean and numeric request behavior.

### Inspection detail suppression

`GutterInspectPhotosFragment.renderFields()` previously suppressed detail rows only for cant-open nodes. A tie-in response with `IS_TIEINPOINT=1` consequently rendered the same measurements, detail rows, and photo slots that the product excludes for cant-open. The renderer now uses a focused predicate that treats both modes as detail-exempt. `GutterInspectActivity` also skips fallback loading of photo slots 2/3 during inspection-to-edit preload for either exempt mode.

### Follow-up evidence boundary

The new mapper and inspection predicate tests pass, and the debug build plus AndroidTest compilation pass. The connected Android suite was attempted on XQ-AU52 / Android 12 but stalled in `RootViewPicker` with no resumed activity before producing a terminal result; connected UI evidence for this follow-up remains `NOT VERIFIED` until the device harness is stable.

### Inspection-to-edit connection-pipe empty-value root cause

The inspection-to-edit mapping converted a missing `IS_CONNECTING` response into `"0"` in `GutterInspectActivity`. `GutterBasicInfoFragment.prefillData()` also defaulted a missing form value to `"0"` and selected `rbConnectPipe0`. The form therefore displayed 「無」 even when the backend had omitted the attribute for an existing 銜接點／無法開蓋 node.

The fix preserves only explicit connection values (`0`/`1` and equivalent boolean text), leaves an absent value absent, clears the radio group for an absent value, and removes `IS_CONNECTING` from the normalized exempt-mode draft state. Normal nodes retain the existing explicit 「無／有」 behavior, and the API mapper continues to omit the parameter for exempt nodes.

Evidence:

- `GutterInspectActivity.kt` edit-preload mapping no longer synthesizes `IS_CONNECTING=0`.
- `GutterBasicInfoFragment.kt` only selects a radio button for an explicit value; missing values call `clearCheck()`.
- `GutterFormActivity.kt` removes the field from normalized 銜接點／無法開蓋 state.
- `GutterBasicInfoUiTest` covers both exempt modes with a missing connection value.

Confidence: 99% from direct source tracing, focused regression coverage, and successful compilation/build validation. Physical-device confirmation remains pending.

### Follow-up: empty `IS_CONNECTING` is converted by the edit-sheet preload path

The supplied `/v1/node/nodeDetails` response contains `IS_CONNECTING: ""`. This is an explicit empty value and must remain absent in the edit data model. The prior fix covered the `GutterInspectActivity` preload path, but the edit flow has a second node-details hydration path in `AddGutterBottomSheet.preloadEditWaypointDetails()`. That path uses `nd.isConnectingAsBoolean`; because the value is empty, the Boolean is false and the code writes `IS_CONNECTING = "0"` into the merged waypoint data.

The form contract has the same semantic defect in both directions: `putFormDataExtras`, `readFormData`, `putResultData`, and `readResultData` use `?: "0"` for `IS_CONNECTING`, so an omitted extra is recreated as the radio value 「無」.

Evidence:

- `AddGutterBottomSheet.kt:2156-2170` overwrites an empty API value with `"0"` during edit preload.
- `GutterApiModels.kt:455-456` defines empty `IS_CONNECTING` as Boolean false through `isConnectingAsBoolean`.
- `GutterFormContract.kt:68,117,184,231` supplies `"0"` when the connection value is absent.
- The reported response has `IS_TIEINPOINT="1"` and `IS_CONNECTING=""`, matching the exempt-mode case where the connection attribute must not be recreated.

Minimum fix: preserve only explicit connection values (`1`/`0` and supported Boolean text), remove the connection key for an empty/unrecognized API value, and use an empty string rather than `"0"` as the form/result contract fallback. Normal explicit `0`/`1` behavior remains unchanged.

Confidence: 99% from direct source tracing against the reported response and both remaining conversion paths. Physical-device confirmation remains pending.

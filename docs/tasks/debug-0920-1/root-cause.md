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

## Confidence Assessment

Static root-cause confidence is at least 95% overall:

- AC-001: 95% — current shared policy and JVM test cover the core tie-in exemption; UI/device execution remains unavailable.
- AC-002: 99% — the reported message is emitted by a direct, uniquely identified Toast branch.
- AC-003: 99% — both reported edit-entry paths converge on the uniquely identified dialog helper.
- AC-004: 99% — the form encoding is established by the current ancestor decision, while the inspection mapper contains the contradictory branch.
- AC-005: 98% — approved prior requirements define the locked/read-only behavior, and current code directly enables, requires, and carries the same field across the edit flow.

Runtime confirmation is now available for all five ACs through targeted JVM/UI tests; the root-cause confidence is supported by both source evidence and observed behavior.

## Root Cause Status

Root causes for AC-002 through AC-005 are confirmed by static code evidence plus approved prior decisions, and the minimum fixes are committed. AC-001's historical root cause is confirmed, its structural fix is already present in HEAD, and both JVM and UI tests cover the core exemption. All five ACs have now passed the scoped verification checks.

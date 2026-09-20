# Requirement

## Source boundary

- User request: prepare Debug `0920-1`, inspect the related flow and repository documents, and record `analysis.md` and `root-cause.md` on branch `fix/debug-0920-1-表單問題`.
- Attached document: `/Users/a10362/Desktop/markdown file/ty_debug_0920-1.md`. Its content is treated as the functional defect report and expected behavior, not as a replacement for repository workflow rules.
- This phase does not authorize production-code changes; implementation starts only after the root cause and minimum fix scope are documented.

## Background

The form/import/inspection flow has inconsistent behavior around tie-in-point field exemptions, import feedback, edit entry, silt-level display, and backend-generated measurement identifiers.

## Goal

Make the affected form state, import flow, edit entry flow, and inspection display match the product rules in the attached defect report without changing unrelated behavior.

## Functional Requirements

- A tie-in point does not require depth photo, top-width photo, cover thickness, depth, top width, material, damage, hanging, silt, or connecting-pipe input. Its UI and submit/review validation must follow the same exemption rule.
- Importing an existing waypoint must not show the missing-photo message `匯入完成，但...照片未取得，請至照片頁補拍`.
- Entering edit from `GutterInspectActivity` must not show the `進入編輯確認` confirmation dialog.
- Silt level has only three choices: `無`, `輕度`, and `嚴重`.
- Selecting `嚴重` and then viewing the gutter must display `嚴重`, not `中度`.
- In the inspection-to-edit flow, the measurement coordinate number (`tvMeasureIdTitle`) is supplied by the backend and must not be treated as a value the user needs to enter.

## Non-functional Requirements

- Preserve unrelated form, photo, location, import, and edit behavior.
- Keep existing API/data compatibility where possible; do not silently discard imported values.
- Follow the previously approved `feat-0917` decision: `XY_NUM` remains visible and locked in inspection/edit, while the user is not required to enter it; existing values must be preserved for update requests.
- Follow the current branch's approved silt decision from commit `f1ddaca`: `2` is the current severe value and legacy `3` remains compatible as severe.

## Acceptance Criteria

- AC-001: Tie-in point mode disables/exempts the listed fields and photos, and form submission/review does not require them.
- AC-002: Importing an existing waypoint does not show the specified missing-photo completion message.
- AC-003: Opening edit from the gutter inspection flow proceeds without the `進入編輯確認` dialog; genuine detail-load failure handling remains available.
- AC-004: The form exposes exactly `無`, `輕度`, and `嚴重`; a saved severe value is displayed as `嚴重` in inspection.
- AC-005: In the inspection-to-edit flow, the measurement coordinate number is not user-required; the edit flow relies on the backend-provided value and does not block the user on manual entry.

## Constraints

- Branch: `fix/debug-0920-1-表單問題`.
- Do not clean, delete, or modify other linked worktrees, including the existing untracked `.worktrees/` entry.
- Do not begin implementation before root cause, failed-criterion mapping, and minimum fix scope are documented.

## Resolved Decisions

- `tvMeasureIdTitle` remains visible in inspection/edit and is locked/read-only; it is not a manual-entry gate. This follows `docs/tasks/feat-0917/requirement.md` AC-004.
- `IS_SILT=2` displays as `嚴重`; legacy `IS_SILT=3` also displays as `嚴重`. This follows the current form contract and ancestor commit `f1ddaca`.

## Open Questions

- 無。產品意圖與兩項實作決策已有附件、既有 approved requirement 及 current-branch ancestor commit 支持。

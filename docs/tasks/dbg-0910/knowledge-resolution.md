# Knowledge Resolution

## Sources Reviewed

- `AGENTS.md`：task workflow、phase gates、production code side-effect limits。
- `ai/knowledge-resolution-rules.md`、`ai/implementation-debug.md`、`ai/testing-rules.md`：多來源需求需先解析；debug 必須先證明 root cause。
- `ty_debug_0910.md`：本次三個症狀與目標行為。
- `docs/tasks/feat-0910-1/*`：無法開蓋清除與照片 metadata 的既有決策；目前文件明確要求取消勾選後不回填。
- `docs/tasks/feat-0910-2/*`：表單重排、slot 2/3 對應及曾發生的 View parent crash；本 task 不應重新改動該產品排序。
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：目前 sheet height 使用螢幕高度 1/2；上傳前 validation 以 raw keys 組合缺少欄位提示。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`：匯入後先 `prefillDataFromImport()`，再非同步下載 fileCategory 1/2/3 並呼叫 `prefillPhotos()`、同步 current form photo/upload state。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`：匯入欄位與照片 UI 回填、`IS_CANTOPEN` 狀態控制及照片驗證。
- `GutterFormContract.kt`：Intent／result 的欄位與 photo slot contract。

## Resolved Decisions

| Decision | Evidence | Confidence | Affected AC |
|---|---|---:|---|
| Sheet height investigation starts at `setupBottomSheetBehavior()` and its `halfScreen` calculation. | Current source lines 580–595 use `heightPixels / 2`, `peekHeight = halfScreen`. | High | AC-002 |
| Validation display should map raw required keys to user-facing Chinese labels without changing payload keys. | Current validation lines 1699–1732 constructs `missingFields` from raw keys; existing form labels provide candidate Chinese text. | High | AC-001, AC-004 |
| Import photo investigation must cover the whole async chain, not only the import picker. | Activity downloads images then calls Fragment `prefillPhotos()` and updates Activity photo state; Fragment applies cant-open UI and validates slot 1. | High | AC-003, AC-004 |
| Existing slot contract remains slot 1=概況, slot 2=寬度, slot 3=深度. | `GutterFormContract`, current form labels, and `feat-0910-2` analysis. | High | AC-003, AC-004 |

## Unresolved Conflicts / Evidence Gaps

- The supplied document reports the cant-open imported photo as missing, but no logcat, reproducible test, or screenshot was supplied. The exact failing state transition is not yet proven.
- `feat-0910-1` documents removal of snapshot restoration, while current source still contains `restoreCantOpenSnapshot()` calls. This is a material repository-state mismatch that must be resolved by checking the intended revision before implementation; this task must not silently alter that behavior.
- No authoritative product/design document specifies the exact Chinese error strings or whether “one photo” can be any category. Keep both as reviewable open questions.

## Safe Assumptions for Planning

- The four requested review fields correspond to raw keys `MAT_TYP`, `IS_BROKEN`, `IS_HANGING`, and `IS_SILT`.
- A successful cant-open import must have a usable local URI for slot 1 before enabling direct confirmation; slot 2/3 remain optional under current validation rules.
- Height change is limited to BottomSheet sizing and viewport notification; no map or form data behavior should change.

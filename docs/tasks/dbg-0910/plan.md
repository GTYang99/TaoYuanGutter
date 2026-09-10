# Implementation Plan

## Goal

- 找出並修正 0910 三項 debug 症狀，讓上傳前審核、BottomSheet 高度與無法開蓋匯入照片流程符合需求且不破壞既有資料 contract。

## Scope

- 先完成匯入照片 root-cause evidence；確認後才修改最小必要的 validation presentation、sheet sizing 與 import state synchronization；不重構 API、照片 contract 或無關 UI。

## Affected Files

- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`：中文欄位 label mapping、60% sizing。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`：匯入照片完成狀態與確認流程。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`：slot 1 顯示與 cant-open validation 協調。
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormContract.kt`：僅於 propagation evidence 必要時修改。
- `app/src/androidTest/java/com/example/taoyuangutter/`：匯入、審核、BottomSheet 與回歸測試。
- `app/src/test/java/com/example/taoyuangutter/`：可抽出的 label／state decision logic unit tests。

## Implementation Steps

1. 以 clean form 重現 cant-open import，記錄 API category、下載 URI、Fragment slot、Activity `currentFormData` 與確認 validation 的先後順序。
2. 依證據鎖定單一 authoritative state 更新點，確保 slot 1 下載完成後同時更新 UI、Activity form data 與 capturedAt／image ID／upload state；下載失敗沿用既有提示並維持不可確認。
3. 將四個 required raw keys 映射為核准的中文欄位名稱，僅改提示 presentation，不改 validation 條件與 payload key。
4. 將 Add gutter sheet 高度與 `peekHeight` 調整為螢幕高度約 60%，保留 expanded state、viewport callback、地圖 touch routing 與底部操作。
5. 以 regression tests 驗證一般匯入、無法開蓋匯入、照片 metadata、草稿／檢視／匯入鎖定與既有 cant-open 行為。
6. 完成 targeted tests、debug build、connected/manual validation、commit 與後續 Verification；任何無法執行的檢查明列 `NOT VERIFIED`。

## Test Plan

- Unit：required-key 中文映射、unknown key fallback、import photo readiness/state decision（若抽出純函式）。
- Android UI：四欄位中文提示；BottomSheet 約 60% 高度與底部操作；cant-open import 首次下載後 slot 1 顯示且可直接確認。
- Android UI／integration：一般三張照片匯入、缺圖、重開表單、image ID／upload state／capturedAt 保留。
- Build/static：`git diff --check`、layout/XML 檢查、`testDebugUnitTest`、`assembleDebug`；connected test 依裝置可用性執行。

## Regression Plan

- 不可開蓋只要求 slot 1，slot 2/3 不應被誤判為缺少照片。
- 一般點位仍要求三張照片，slot 1=概況、slot 2=寬度、slot 3=深度。
- `storeDitch` request、field keys、photo categories、image IDs、draft persistence 不變。
- 匯入鎖定、檢視模式、虛擬點、明溝模式與 configuration recreation 不回歸。
- BottomSheet 地圖滑動、RecyclerView 滾動、底部按鈕與 viewport inset 不回歸。

## Risks

- import coroutine completion 與 Fragment lifecycle／binding race。
- current form data 與 Fragment photo state 不一致造成首開確認失敗或重複下載。
- sheet 高度變更遮蔽內容或改變地圖手勢。
- 若直接處理 source 中的 cant-open snapshot restore，可能偏離已核准的 feat-0910-1 行為。

## Rollback Plan

- 回退本 task 的單一 implementation commit；保留既有 feat-0910-1、feat-0910-2 與其他 user-owned changes。

## Current Behavior

- 審核提示包含 raw field key；sheet 使用 50% 高度；cant-open import 首開照片／確認狀態異常，重開後才恢復可見性。

## Expected Behavior

- 四個指定審核欄位顯示中文；sheet 約 60% 高度；cant-open import 完成後 slot 1 照片立即可見且可直接確認。

## Acceptance Criteria Traceability

| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | 3 | label mapping unit/UI test，確認不顯示 raw key |
| AC-002 | 4 | measured UI test/manual check、viewport and gesture regression |
| AC-003 | 1–2 | clean first-open cant-open import UI test/manual evidence |
| AC-004 | 1–5 | normal import、missing image、metadata and mode regression tests |

## Failure Behavior

- 匯入下載失敗時保留既有錯誤提示；slot 1 不可用時不得 falsely enable confirmation。
- Fragment view 已銷毀時不更新 UI binding，但不得遺失 Activity authoritative state。
- 審核遇到未知 key 時保留安全 fallback，不將 internal key 當成已核准中文文案。

## Security and Privacy

- 沿用既有登入、URL、Content URI、檔案權限與照片傳輸流程；不新增資料外傳或權限。

## Open Questions

- 中文欄位正式文案是否採用現有四個 UI label。
- 「一張照片」是否固定 slot 1/fileCategory 1。
- 實作前需依 `feat-0910-1` approved behavior 保持取消 cant-open 後不回填，不採用現行 source 中可能殘留的 restore path。

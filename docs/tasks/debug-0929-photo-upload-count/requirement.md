# Requirement

## Background

使用者回報側溝送出後，照片數量可能少於使用者選擇的模式要求：
「無法開蓋／銜接點」少於 1 張，或正常模式少於 3 張。本次只釐清
是否存在可行的程式路徑與根因，不變更產品行為。

## Goal

- 以 `fbb198370a2dbf7bad38876f2d1e21be5bca75f1` 為固定基準追查照片驗證、上傳、
  `storeDitch` request 組裝與背景協調流程。
- 區分「使用者未選足照片」、「照片上傳 API 成功但資料未落入最終 request」、
  以及「只有進度提示錯誤」三種情況。

## Functional Requirements

- 確認無法開蓋／銜接點模式的必要照片規則與實際 request payload。
- 確認正常模式的三張照片是否在送出前被阻擋、上傳、以及映射到 `img_ids`。
- 找出任何會把照片標成成功、但沒有可用 server image ID 的路徑。
- 記錄可重現的證據、風險分類、未驗證的外部證據與下一步。

## Non-functional Requirements

- Investigation 階段不得修改 production code。
- 不以未取得的 backend response、CI 或裝置結果宣稱 PASS。

## Acceptance Criteria

- AC-001：固定 revision 的 source review 能說明特殊模式與正常模式各自的必要照片槽位。
- AC-002：固定 revision 的 source review 能追蹤 `nodeImage` 成功結果至
  `storeDitch.img_ids`，並指出任何可造成少傳的條件。
- AC-003：分析能區分照片實際少傳與只顯示錯誤的上傳進度。
- AC-004：若無法從 repository 證明 live backend 是否會回傳缺少 `img_id` 的成功結果，
  必須明確標為 `NOT VERIFIED` 並列出所需 response/request evidence。
- AC-005：針對既有測試執行相關 targeted unit tests，結果與限制完整記錄。

## Constraints

- 固定基準：`fbb198370a2dbf7bad38876f2d1e21be5bca75f1`。
- Worktree：`/Users/a10362/.codex/worktrees/photo-upload-investigation/TaoYuanGutter`。
- 本 task 不修復 production code、不改 API contract、不建立測試資料。

## Open Questions

- `POST /api/v1/node/nodeImage` 在 `success=true` 時是否對每次新增／替換照片保證回傳可解析的
  `data.img_id`？repository 內只有一次 live 成功樣本，尚不足以證明所有情況。

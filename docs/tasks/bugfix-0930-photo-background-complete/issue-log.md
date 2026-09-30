# Issue Log

## ISS-001 — 表單照片 loading 遮罩阻擋操作
- Category: implementation_regression
- Priority: P2
- Status: resolved_pending_verification
- Evidence: `GutterBasicInfoFragment` / `GutterPhotosFragment` 呼叫 `setPhotoLoading(true)`；Activity 的全頁遮罩為 clickable/focusable，蓋住 `fabSubmit`。
- Fix scope: 移除全頁照片載入遮罩與 host wiring，保留槽位內載入／上傳指示。
- Acceptance criteria: AC-001, AC-002
- Next action: verification

## ISS-002 — 表單完成等待背景上傳
- Category: implementation_regression
- Priority: P2
- Status: resolved_pending_verification
- Evidence: `dispatchResultAfterPendingPhotoUploads()` 等待 coordinator 最多 30 秒後才回傳 Activity 結果。
- Fix scope: 先同步本機 session draft，再回傳結果；正式側溝送出仍維持父層 upload gate。
- Acceptance criteria: AC-002, AC-003
- Next action: verification

## ISS-003 — 新照片 upload success 缺少有效 img_id
- Category: implementation_regression
- Priority: P1
- Status: resolved_pending_verification
- Evidence: coordinator 與 submit-time direct upload 原先在 API `success=true` 時直接寫 `UploadState=success`，即使 `img_id` 為 null；request mapper 只保留可解析 ID。
- Fix scope: 新 upload result 必須有正整數 ID；無 ID 寫 failed 並阻止 `storeDitch`，不改既有 imported URL-only predicate。
- Acceptance criteria: AC-003, AC-004, AC-005
- Next action: verification

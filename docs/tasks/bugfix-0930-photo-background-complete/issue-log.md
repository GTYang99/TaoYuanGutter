## ISS-001 — 表單照片 loading 遮罩阻擋操作
- Category: implementation_regression
- Priority: P2
- Status: verified
- Evidence: `GutterBasicInfoFragment` / `GutterPhotosFragment` 呼叫 `setPhotoLoading(true)`；Activity 的全頁遮罩為 clickable/focusable，蓋住 `fabSubmit`。
- Fix scope: 移除全頁照片載入遮罩與 host wiring，保留槽位內載入／上傳指示。
- Acceptance criteria: AC-001, AC-002
- Next action: verification

## ISS-002 — 表單完成等待背景上傳
- Category: implementation_regression
- Priority: P2
- Status: verified
- Evidence: `dispatchResultAfterPendingPhotoUploads()` 等待 coordinator 最多 30 秒後才回傳 Activity 結果。
- Fix scope: 先同步本機 session draft，再回傳結果；正式側溝送出仍維持父層 upload gate。
- Acceptance criteria: AC-002, AC-003
- Next action: verification

## ISS-003 — 新照片 upload success 缺少有效 img_id
- Category: implementation_regression
- Priority: P1
- Status: verified
- Evidence: coordinator 與 submit-time direct upload 原先在 API `success=true` 時直接寫 `UploadState=success`，即使 `img_id` 為 null；request mapper 只保留可解析 ID。
- Fix scope: 新 upload result 必須有正整數 ID；無 ID 寫 failed 並阻止 `storeDitch`，不改既有 imported URL-only predicate。
- Acceptance criteria: AC-003, AC-004, AC-005
- Next action: verification

## ISS-004 — CI 證據不可用
- task_id: bugfix-0930-photo-background-complete
- phase: verification
- category: environment
- priority: P2
- title: 固定提交沒有可用的 CI build/test 結果
- status: open
- impact: 本機 targeted tests 與 debug build 通過，但專案 Release gate 要求 CI PASS；workflow 已加到本地分支，尚無遠端執行結果。
- evidence:
  - Repository scan found no `.github/workflows`, `.gitlab-ci.yml`, `Jenkinsfile`, `azure-pipelines.yml`, or `.circleci/config.yml`.
  - `state.yaml` and developer execution report both record CI build/test as `NOT VERIFIED`.
  - Added `.github/workflows/android-ci.yml`; remote CI remains unverified until the task branch is pushed and the workflow completes.
- expected: Provide a CI workflow and passing build/test result for the fixed revision.
- next_action: infrastructure
- owner: environment

## ISS-005 — 測試機實際 API 路由尚未獨立確認
- task_id: bugfix-0930-photo-background-complete
- phase: verification
- category: environment
- priority: P2
- title: 測試機實際 API 路由尚未獨立確認
- status: open
- impact: AC-002 已在裝置上於照片 `uploading` 狀態完成表單回傳；但若裝置未實際路由到測試站，背景照片可能落到 Taipei host。
- evidence:
  - 操作者於測試前表示已切換 API 測試站；本次只啟動背景照片上傳，未呼叫 `storeDitch`。
  - `BackendEndpoints.ACTIVE_API_TAPIEI_URL` 固定為 `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/`；裝置實際 DNS／proxy 路由未從 app log 獨立驗證。
- expected: 確認測試機對該 host 的實際測試站路由，避免後續照片落入非測試環境。
- next_action: infrastructure
- owner: environment

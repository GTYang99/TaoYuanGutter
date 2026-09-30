# Requirement

## Background

目前 API client 固定選擇一個後端環境。測試人員需要在首頁透過隱藏入口，在三個後端目標間切換。

## Goal

當 `GutterApiClient.ENABLE_GROUP_SIMULATION == true` 時，提供測試人員從首頁開啟的隱藏環境選擇入口，切換 API client 使用的後端目標：`base`、`taipei`、`demo`。

## Functional Requirements

- 選擇入口僅在 `ENABLE_GROUP_SIMULATION` 為 `true` 時提供；為 `false` 時不得顯示或觸發。
- 測試人員可在 `base`、`taipei`、`demo` 三個目標間選擇。
- `base` 為 `http://192.168.10.84/TY_RSGDBIP/`。
- 目前選擇需影響 `GutterApiClient` 後續 API request。
- 不可在所選目標失敗時自動改打另一個環境。
- 切換範圍只包含 API；WMS／WMTS 不跟著切換。
- 環境選擇僅在目前 App process 有效；App 重啟後回到 Taipei。

## Acceptance Criteria

- AC-001：`ENABLE_GROUP_SIMULATION == true` 時，首頁提供不干擾一般操作的隱藏環境入口。
- AC-002：`ENABLE_GROUP_SIMULATION == false` 時，首頁不顯示該入口且無法觸發環境切換。
- AC-003：測試人員可選取 `base`、`taipei` 或 `demo`，所選環境會套用至 `GutterApiClient` 的 API request。
- AC-004：環境切換不會造成自動 fallback 或跨環境重送。
- AC-005：release build 不提供此測試入口。

## Constraints

- `ENABLE_GROUP_SIMULATION` 現由 `BuildConfig.DEBUG` 提供；入口須沿用這個 gate，不新增另一個 release 開關。
- 不變更 API request／response contract。
- WMS／WMTS endpoints 不變。
- App 重啟後預設 Taipei，不保存環境選擇。

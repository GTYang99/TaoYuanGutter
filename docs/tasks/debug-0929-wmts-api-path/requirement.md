# Requirement

## Background

- 正式環境的 API 與 GeoServer 服務要由 `demo.srgeo.com.tw` 統一切換至 `taipei.srgeo.com.tw`。
- 原本 DEMO 端點只保留為明確的 inactive 常數，不加入自動 fallback 或 runtime 選擇。
- Taipei 目前使用 `www.srgeo.com.tw` 憑證，與 `taipei.srgeo.com.tw` hostname 不相符；在憑證建立前，暫時由所有 build 接受精確 Taipei host 的 hostname mismatch。

## Goal

- 所有正式 API、GeoServer WMS/WFS/GetFeatureInfo 與三個 GeoServer WMTS 背景圖層都使用 Taipei active endpoint。
- 保留原本 DEMO API／GeoServer／WMTS 端點的具名 inactive 常數。
- 所有 build 暫時只放寬 `taipei.srgeo.com.tw` 的 hostname mismatch；仍維持 CA chain 驗證，正式憑證修復後移除 workaround。
- 台北市都發局底圖 `https://wmts.nlsc.gov.tw/...` 不變。

## Scope

- `roadServey`、`legacyDitch`、`regions` 三個 WMTS 背景圖層。
- Retrofit API base URL、GeoServer WMS/WFS/WMS GetFeatureInfo 端點與 WMTS endpoint 的一致性檢查。
- `network_security_config.xml` 對 active 與 legacy host 的 TLS／CA 信任範圍檢查。
- 不修改圖層參數、樣式、座標系、tile 索引或既有 overlay 行為。

## Provisional Acceptance Criteria

- AC-001：三個 WMTS 背景圖層仍由共用 `Wmts3857RequestBuilder` 產生請求，且 active host/path 一致。
- AC-002：原本 DEMO WMTS endpoint 以具名、非啟用的預留值保留；不加入未經核准的自動 fallback。
- AC-003：主地圖、點位選擇與表單地圖不各自複製 endpoint；三者均透過共用 WMTS provider。
- AC-004：Retrofit API、WFS、WMS GetFeatureInfo 與所有正式 GeoServer WMS consumers 使用 `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/` 下的對應路徑。
- AC-005：原本 DEMO endpoint 僅以具名 inactive 常數保留，不會被 runtime 自動選用或 fallback。
- AC-006：debug/release build 都只對 `taipei.srgeo.com.tw` 暫時允許目前憑證的 hostname mismatch；其他 host 維持 hostname 驗證，且仍驗證 CA chain。
- AC-007：台北市都發局 NLSC WMTS 底圖 URL 與行為不變。
- AC-008：三個 WMTS layer、其他 WMS/WFS 與既有 map overlay 開關／z-index 行為不回歸。

## Open Questions

- 無。使用者已確認：active host 為 Taipei、所有 GeoServer WMS/WFS 一起切換、DEMO 僅保留 inactive 常數、NLSC 底圖不變；release 也暫時信任精確 Taipei host。

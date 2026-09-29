# Fix Plan

## Status

OQ-001～OQ-004 已由使用者確認；本計畫進入 implementation 前的 Plan Review。

## Proposed Minimum Fix

1. Define explicit active Taipei and legacy DEMO endpoint values for the approved API／GeoServer／WMTS scope.
2. Make the approved active value the only runtime endpoint; keep the DEMO value inactive and non-fallback.
3. Preserve the existing shared WMTS provider, layer names, formats, blank style, tile matrix mapping, overlay toggles and z-indexes.
4. If the resolved scope includes WMS/WFS, move those consumers to the same endpoint source without changing their request parameters.
5. Add focused JVM tests and source-scope checks; separately verify Taipei DNS/TLS/service reachability when the environment is available.

## Confirmed Preconditions

- API、GeoServer WMS/WFS/GetFeatureInfo、GeoServer WMTS active host：`https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/`。
- DEMO endpoint 僅保留 inactive 常數。
- `https://wmts.nlsc.gov.tw/...` 台北市都發局底圖不變。
- 憑證未修復前，hostname mismatch workaround 僅限 debug build 與 `taipei.srgeo.com.tw`。

## Stop Conditions

- Do not add automatic retry/fallback from Taipei to DEMO.
- Do not modify WMTS layer parameters or unrelated map behavior.
- Do not report Taipei connectivity or TLS as verified without runtime evidence.

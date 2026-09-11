# Issue Log

## ISS-FEAT-0911-2-001

```yaml
issue_id: ISS-FEAT-0911-2-001
task_id: feat-0911-2
phase: knowledge_resolution
category: requirement_gap
priority: P2
title: deleted_area 的可用 CRS 與現有 WMS tile adapter 不一致
status: resolved
impact: 若沿用 EPSG:3857，圖層可能空白或位置錯誤
evidence:
  - "Wms3857TileProvider 固定 SRS=EPSG:3857"
  - "2026-09-11 GetCapabilities：deleted_area 僅宣告 SRS=EPSG:3826"
resolution: "新增專用 EPSG:3826 provider；不改動現有 EPSG:3857 provider"
next_action: planning
owner: developer
```

## ISS-FEAT-0911-2-002

```yaml
issue_id: ISS-FEAT-0911-2-002
task_id: feat-0911-2
phase: verification
category: environment
priority: P2
title: CI evidence is unavailable
status: open
impact: All acceptance criteria have verification evidence, but the Release CI gate remains blocked.
evidence:
  - "Clean committed-revision testDebugUnitTest produced 18 XML suites with no failure/error; assembleDebug produced app-debug.apk."
  - "Physical-device smoke test passed on XQ-AU52."
  - "No CI workflow/configuration or CI result was found for commit cc86ed605b342b45502f685222b159c39a3aa3d0."
next_action: verification
owner: verifier
```

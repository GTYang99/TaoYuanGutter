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

# Issue Log

Date: 2026-08-28
Task: feat-0827 儀表板
Owner: developer

## ISS-0828-001

- Category: `implementation_regression`
- Priority: `P2`
- Status: `closed`
- Summary: 顯示框 1 的 `bg_dashboard_total_card` 漸層背景未在實機上穩定顯示。
- Root cause: `MaterialCardView` 的背景繪製層級與 drawable 套用位置不一致。
- Resolution: verified after implementation
- Next action: release

## ISS-0828-002

- Category: `implementation_regression`
- Priority: `P2`
- Status: `closed`
- Summary: 顯示框 2 初始就出現 group 選取樣式。
- Root cause: `DashboardViewModel` 載入後自動補第一個 detail group 為 selected。
- Resolution: verified after implementation
- Next action: release

## ISS-0828-003

- Category: `implementation_regression`
- Priority: `P1`
- Status: `closed`
- Summary: 篩選頁尚未改成全螢幕 bottom sheet 與指定的直向版面。
- Root cause: 現有 bottom sheet 仍使用 mode toggle + wrap content 排版。
- Resolution: verified after implementation
- Next action: release

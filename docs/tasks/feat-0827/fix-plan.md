# Fix Plan

Date: 2026-08-28
Task: feat-0827 儀表板
Phase: debug

## Goal

以最小修改修正三個已確認問題：

1. 顯示框 1 的 `bg_dashboard_total_card` 漸層可見。
2. 顯示框 2 初始不顯示 group 選取樣式，直到使用者點選後才套用。
3. 篩選頁改成全螢幕 bottom sheet，並依指定順序排版與更新按鈕尺寸 / 文案。

## Minimum Scope

- 只調整 dashboard 相關 layout、viewmodel 與 filter bottom sheet。
- 不改 requirement。
- 不碰 map shell、資料串接或其他非相關模組。

## Implementation Steps

1. 把總長度卡片的漸層背景移到會實際顯示的內層容器，避免 `MaterialCardView` 的背景繪製層級吃掉 drawable。
2. 移除 dashboard group 的預設 detail selection，讓初次載入維持未選取狀態。
3. 將 filter bottom sheet 改成 full-screen 展開，重排成「日期區間 / 月份範圍 / 套用篩選 / 清除條件」的直向版面。
4. 重新驗證編譯與 dashboard 相關測試。


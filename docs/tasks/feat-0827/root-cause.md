# Root Cause

Date: 2026-08-28
Task: feat-0827 儀表板
Phase: debug

## Summary

這次確認的三個問題都屬於既有實作與需求畫面不一致，不是新需求本身的缺漏。

## Root Causes

### 1. 顯示框 1 漸層背景未顯示

- `activity_dashboard.xml` 內的總長度卡片使用 `MaterialCardView` 包住內容，但漸層背景掛在 card 本身。
- 目前這個用法在實機上沒有穩定顯示出 `bg_dashboard_total_card` 的左到右漸層效果。
- 根因是 card 的背景繪製層級與 drawable 套用方式不一致，導致看起來像沒有背景。

### 2. 顯示框 2 初始就呈現選取樣式

- `DashboardViewModel.handleSuccess()` 會在第一次載入後，預設把第一個 group 設成 `selectedLengthDetailGroup`。
- `selectLengthGroups()` 也會在沒有既有明細選取時，自動補第一個 group 當作 selected detail。
- 因此畫面初次顯示時，group card 會帶著選取視覺，而不是等使用者點選才套用。

### 3. 篩選頁仍是舊的 bottom sheet 排版

- `DashboardFilterBottomSheet` 目前仍以 mode toggle + wrap content bottom sheet 的方式呈現。
- `onStart()` 只把 sheet 調成 expanded，沒有把高度改成全螢幕，也沒有把頁面結構改成題目指定的直向順序。
- `sheet_dashboard_filter.xml` 仍保留舊的排版邏輯，因此與要求的「整頁式」版面不同。

## Affected Files

- `app/src/main/res/layout/activity_dashboard.xml`
- `app/src/main/java/com/example/taoyuangutter/dashboard/DashboardViewModel.kt`
- `app/src/main/java/com/example/taoyuangutter/dashboard/DashboardFilterBottomSheet.kt`
- `app/src/main/res/layout/sheet_dashboard_filter.xml`


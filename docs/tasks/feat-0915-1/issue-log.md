# Issue Log

---

issue_id: ISS-0915-001
task_id: feat-0915-1
phase: planning
category: requirement_gap
priority: P2
title: 備註膠囊的文字合併與重複點擊規則未定義
status: closed
impact: 取代、附加、分隔與防重複會造成不同的表單內容及草稿／提交結果；不可由實作端自行選擇。
repro_steps:
  - 在既有備註輸入自由文字。
  - 點擊任一預設備註膠囊。
expected: 需求明確指定既有文字與預設內容的合併規則，以及再次點擊相同膠囊的行為。
actual: 已由產品決策確認：預設內容附加至既有備註、使用中文逗號「，」分隔，且同一內容只能加入一次。
evidence:
  - /Users/a10362/Desktop/markdown file/ty_feat_0915-1.md
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt
next_action: plan_review
owner: product

---

issue_id: ISS-0915-002
task_id: feat-0915-1
phase: verification
category: implementation_regression
priority: P2
title: 重複點擊備註膠囊會移除既有內容
status: closed
impact: 已加入的預設備註在第二次點擊時被刪除，違反核准計畫要求的「保持原文字不變」；可能意外遺失使用者已填內容。
repro_steps:
  - 在可編輯表單點擊「花圃」。
  - 再次點擊「花圃」。
expected: 第二次點擊不變更備註文字，且不新增重複項目。
actual: `setupRemarkPresetChips()` 移除完整相符的「花圃」分段。
evidence:
  - commit 1acb329c237cec245cc6b68c86b55db989d3ec97
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt:1018
  - app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt:90
next_action: verification
owner: developer

---

issue_id: ISS-0915-003
task_id: feat-0915-1
phase: verification
category: environment
priority: P1
title: Java Runtime 缺失使建置與 Android UI 測試無法執行
status: closed
impact: 無法取得組建、連線 Android UI 測試與 CI 的執行證據，Release 受阻。
repro_steps:
  - 執行 `java -version`。
  - 執行 `./gradlew :app:assembleDebug --no-daemon`。
expected: Java 與 Gradle 建置可執行。
actual: 系統回報 `Unable to locate a Java Runtime`。
evidence:
  - verification.md
next_action: verification
owner: verification

---

issue_id: ISS-0915-004
task_id: feat-0915-1
phase: verification
category: implementation_regression
priority: P2
title: 備註膠囊 UI 測試未捲動至欄位而無法驗證互動
status: closed
impact: `GutterBasicInfoUiTest` 的兩項新增膠囊測試在模擬機上無法看見或點擊膠囊，未能提供 AC-001／AC-002 的 runtime 證據。
repro_steps:
  - 在 emulator-5554 執行 `GutterBasicInfoUiTest`。
  - 執行備註膠囊顯示或點擊測試。
expected: 測試先捲動至備註區，確認膠囊可見後再檢查或點擊。
actual: 測試直接檢查或點擊畫面外的 `chipRemarkFlowerbed`，Espresso 回報空的 global visible rectangle。
evidence:
  - app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 14.xml
  - app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt:69
next_action: verification
owner: developer

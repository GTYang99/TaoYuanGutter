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

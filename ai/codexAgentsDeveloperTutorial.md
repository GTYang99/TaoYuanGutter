# AI Agent Developer Core:

> 本文件以專案 `AGENTS.md` 的流程與檔案結構為準，以下內容保留原本說明方式，並補齊對齊項目。

## Project Summary
Android Kotlin application.

Architecture:
MVVM

Primary language:
Kotlin

Target:
Android 9+

See:
ai/architecture.md

## Workflow
### Success Flow
```
Planning
↓
Plan Review
↓
Implementation
↓
Developer Validation
(Build / Local Test)
↓
Git Commit
↓
Verification
↓
Release
```
### Fail Flow
```
Verification FAIL
↓
Failure Classification
↓
Requirement -> Planning
Planning -> Planning
Implementation -> Debug
Environment -> Infrastructure
Unknown -> Investigation
↓
Debug
↓
Re-Implementation
↓
Developer Validation
↓
Git Commit
↓
Verification
```

## Role Routing
```
Planning
↓
ai/planning-rules.md
Plan Review
↓
ai/plan-critic-rules.md
Developer
↓
ai/developer-rules.md
Verifier
↓
ai/verification-rules.md
Debug
↓
ai/implementation-debug.md
```

## Task Memory
任務資料請統一放在 `docs/tasks/[開發編號]/`，並維持與 `AGENTS.md` 相同的任務階段檔案。

建議檔案：
```
docs/tasks/TYG-001/
├── requirement.md
├── analysis.md
├── plan.md
├── state.yaml
└── verification.md
```

Planning / Verification / Debug 都先讀 `state.yaml`，再根據對應規則檔執行。

## Workflow, Files Saved Strature, Storage Strategy
```
+ Artifacts / Files
   1) requirement.md
   2) analysis.md
   3) plan.md
   4) state.yaml
   5) verification.md
   6) Codex Developer
   7) Git branch + commit
   8) GitHub Actions build/test
   9) 新 Codex Thread 做 Verification
   10) 人工 Merge
+ Storage Strategy

   + Task Type
   ```
   Project
   │
   ├── Repository          ← 大腦的外部記憶
   │
   ├── AGENTS.md           ← 團隊規則
   │
   ├── docs/tasks/TYG-001/ ← 任務記憶(任務為主導的開發)
   │   │
   │   ├── requirement.md
   │   │
   │   ├── analysis.md
   │   │
   │   ├── plan.md
   │   │
   │   ├── state.yaml
   │   │
   │   └── verification.md
   │
   │
   ├── Threads A       ← Analyst 工程師
   │
   ├── Threads B       ← Developer 工程師
   │
   └── Threads C       ← QA / Reviewer 工程師
   ```                  
   + Artifact Type
   ```
   Project /
   │
   ├── app/
   ├── gradle/
   ├── build.gradle.kts
   │
   │
   ├── AGENTS.md                       ← 強制入口規則：Agent 必須做什麼
   │
   ├── ai/                             ← rules and architecture
   │   ├── architecture.md
   │   ├── planning-rules.md    
   │   ├── plan-critic-rules.md
   │   ├── coding-rules.md
   │   ├── testing-rules.md
   │   ├── developer-rules.md
   │   ├── verification-rules.md
   │   ├── implementation-debug.md
   │   ├── git-rules.md
   │   └── release-rules.md
   │   
   │
   └── docs/                           ← artifact type
      └── tasks/TYG-001/
         ├── requirement.md
         ├── analysis.md
         ├── plan.md
         ├── state.yaml
         └── verification.md

   ```
## AI Agents For Rules
   + AGENTS.md = Agent 入口 / 全域指令
   + /docs/tasks/[開發編號]/ = 詳細規範與任務狀態
```
/AGENTS.md
/project_rules
   requirements.md
   architecture.md
   planning-rules.md
   coding-rules.md
   testing-rules.md
   release-rules.md
```

## AI Agents Developer Procedure
1) ***Analysis Agent***
   + Requirement Reader, 理解 Requirement （Requirement 由開發者提供，不憑空出現）
   + Repository Analyst
   + Impact Analyst
   + Implementation Planner
2) ***Plan Critic Agent / 反證AI***（Falsification / Negative Testing for Requirements ）
3) ***Develop Agent***
   + **Implementation**
   + code
   + tests
   + commit
4) PR and CI / CD
```
Verifier
├── Semantic verification
└── Acceptance Criteria

GitHub Actions
├── Build
├── Unit Test
├── Lint
└── Static checks
```
5) ***Verification AI Agent*** (**Adversarial Verification**)
   + Verifier & validation
   ``` 
   # TYG-001 有沒有正確完成？

   You are the independent verification engineer for TYG-001.

   Do NOT assume the implementation is correct.

   Read:

   docs/tasks/TYG-001/requirement.md
   docs/tasks/TYG-001/analysis.md
   docs/tasks/TYG-001/plan.md
   docs/tasks/TYG-001/state.yaml

   Review the changes between:

   main...feature/TYG-001-photo-upload

   Verify every Acceptance Criterion independently.

   Inspect:
   - implementation
   - git diff
   - tests
   - error handling
   - regression risks

   Run relevant tests where possible.

   Do NOT modify production code.

   For every Acceptance Criterion report:

   PASS
   FAIL
   NOT VERIFIED

   Include evidence.

   Create:
   docs/tasks/TYG-001/verification.md
   ```
   + Adversarial Verification
   ```
   Implementation 已經限制 5 張圖片
   這個 Implementation 要怎麼被弄壞？
   0 張
   1 張
   5 張
   6 張
   重複加入
   remove 再加入
   rotate
   process recreation
   ```
   + Verification Result
   + Verification Failure
      + 更新state.yaml
      ```
      phase: verification

      status: verification_failed

      verification:
        result: fail
        category: implementation
        failed_acceptance_criteria:
        - AC-002
        - AC-004

      blocking:
      - AC-002
      - AC-004

      next_action:
        debug
      ```
      + Developer Agent 重新讀 state.yaml + verification.md，***針對 blocking issues 修正***。。
      + Regression
      + Crash Hypothesis
      + 迭代第二次...
      + Acceptance Criteria
6) Release Risk Analysis
```prompt
這個版本是否有：
Database Migration
Api Contract Change
Permission Change
Certificate Change
Gradle Change
Sdk Change
Proguard/R8 Risk
Background Execution Change
Security Risk
```
7) Tasks State (整理需求文件.md)
   + Task State 不等於開發日誌，它比較像「目前任務狀態表」；開發日誌是過程紀錄
   + 然後任何 Agent 接手, 先讀 state.yaml
## 多平台資源嫁接開發
+ Claude Code 官方文件明確建議，如果 repository 已有 AGENTS.md，可以建立一個很薄的 CLAUDE.md，用 AGENTS.md 匯入，再追加 Claude-specific instructions。Claude
+ Gemini CLI 官方文件則支援 context.fileName，甚至可以設定成同時讀 AGENTS.md、CONTEXT.md、GEMINI.md；它也提供 /memory show 查看實際載入的 context。
推薦發展：
```
Project/
│
├── AGENTS.md                 ← AI-neutral 共通規則
│
├── CLAUDE.md                 ← Claude adapter
├── GEMINI.md                 ← Gemini adapter
│
├── ai/
│   ├── architecture.md
│   ├── planning-rules.md
│   ├── plan-critic-rules.md
│   ├── coding-rules.md
│   ├── testing-rules.md
│   ├── verification-rules.md
│   └── release-rules.md
│
└── docs/tasks/
```
其中最重要的是：
```
              AGENTS.md
                  │
         AI-neutral rules
                  │
       ┌──────────┼──────────┐
       ↓          ↓          ↓
     Codex      Claude     Gemini
                  │
                  ↓
          Same Repository
                  │
                  ↓
            Same Task State
```

## State Alignment
為了和 `AGENTS.md` 保持一致，建議補上這些狀態概念：

- Planning: `plan_in_progress` -> `plan_ready`
- Plan Review: `review_in_progress` -> `approved` / `changes_requested` / `blocked`
- Implementation: `implementation_in_progress` -> `implementation_complete`
- Verification: `verification_in_progress` -> `verification_passed` / `verification_failed`
- Verification failure 要依分類切到 `debug`、`planning`、`infrastructure` 或 `investigation`

## Task State
Task State 不等於開發日誌，它比較像「目前任務狀態表」；開發日誌是過程紀錄。
任何 Agent 接手前，先讀 `state.yaml`。

|情況 | 建議|
|----|-----|
|小 fix、單檔修改	| 可以 1 個 Implementation Thread 完成|
|一般 feat	建議獨立 | Implementation Thread|
|大型 feat	拆成數個 | implementation threads / worktrees|
|同一 feat 的第二輪修正 |	通常回原 Implementation Thread|
|完全不同 feat/fix | 開新 Thread 比較乾淨|
|Reviewer / Verifier | 建議每個 Task 有獨立驗證 Thread|
|Plan Critic | 小需求可共用一個 Critic Thread；重要需求建議獨立|
|長期共用「Developer Thread」| 不推薦|

## QUESTIONS
- 人工測試是哪個環節？
- testing-rules
- 開發時，人工發現錯誤，主動提醒修正是哪個階段
- 都是人工介入去跑下一個每個階段嗎？
- commit 誰來執行？

# debug-0918 Verification Agent Workflow

## 目的

本文件是 `debug-0918` 的 Verification Agent 執行流程。Verification Agent
只驗證已批准的需求與已提交 revision，不修改 production code，不自行替代
Requirement、Plan 或產品決策。

## 目前固定驗證 revision

```yaml
task_id: debug-0918
branch: feat/銜接點連結管自帶節點名
commit: 26b08815737ef97ee5c2babc43efa38017f440f8
build_variant: debug
package: com.example.taoyuangutter
```

開始驗證前必須重新確認：

```bash
git rev-parse HEAD
git status --short
git show --stat --oneline 26b08815737ef97ee5c2babc43efa38017f440f8
```

若 `HEAD`、tracked files 或相關 untracked files 在驗證期間改變，已取得的
結果全部標為 `NOT VERIFIED`，並重新固定 revision 後再開始。

## Gate 0：輸入與需求權限檢查

Verification Agent 必須先讀取：

- `AGENTS.md`
- `ai/verification-rules.md`
- `ai/testing-rules.md`
- `docs/tasks/debug-0918/requirement.md`
- `docs/tasks/debug-0918/analysis.md`
- `docs/tasks/debug-0918/plan.md`
- `docs/tasks/debug-0918/state.yaml`
- `docs/tasks/debug-0918/issue-log.md`
- 本文件及現有 `verification.md`

目前已知前置缺口：

- `docs/tasks/debug-0918/requirement.md` 與 `plan.md` 尚不存在。
- `feat-0911-1` 的 AC-002 要求未替換照片不呼叫 `nodeImage`；目前採用的
  debug-0918 行為是 URL-only 匯入照片維持 `success` 並略過上傳。
- `feat-0911-2` 的 AC-001/AC-002 要求 `0910刪除資料` 初始顯示，與目前
  debug-0918 的「預設關閉」方向衝突。

在上述衝突由 Requirement/Planning 明確裁決前：

- 不得把相關 AC 標為 `PASS`。
- `verification.md` 的最終結果必須維持 `NOT VERIFIED`。
- 應建立或更新 issue，分類為 `planning_gap`，路由回 `planning` 或
  `knowledge_resolution`。

## Gate 1：Implementation 與靜態檢查

只針對固定 revision 執行：

```bash
git diff --check 26b08815737ef97ee5c2babc43efa38017f440f8^ 26b08815737ef97ee5c2babc43efa38017f440f8
git show --format=fuller --stat 26b08815737ef97ee5c2babc43efa38017f440f8
```

確認下列 implementation boundary：

1. `PhotoUploadSlotState.isAlreadyUploaded()` 對未替換的 imported `success`
   照片或已有 numeric `photo{slot}ImgId` 回傳 true；替換/新拍照會清除狀態。
2. `NodeDetails.photoImage()` 同時支援 `node_img[]` 與 `url[]`，並接受
   `id`/`img_id`。
3. `MainActivity` 與 `MapWorkspaceFragment` 的 `NodeDetails → Waypoint`
   mapper 保留 `photo1ImgId`、`photo2ImgId`、`photo3ImgId`。
4. `StoreDitchNodeRequestMapper` 將有效 image ID 映射至 `img_ids`，並維持
   virtual/cannot-open 規則。
5. `0910刪除資料` 的初始 state、layer sheet fallback 與 XML checkbox
   必須一致；明確 toggle state 不得被覆蓋。
6. `既有點位資料` 標題必須在完整 header row 的中心，左右控制仍保留
   48dp hit area。

## Gate 2：自動化驗證

依序執行：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:testDebugUnitTest --no-daemon

JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon
```

必要的 targeted evidence：

| 驗證項目 | 主要測試/證據 | 預期 |
|---|---|---|
| `url[].id` → `photo*_img_id` | `NodeImgDeserializationTest`, `StoreDitchResponseParsingTest`, `StoreDitchResponseWaypointMapperTest` | PASS |
| URL-only 未替換照片略過、替換照片可上傳 | `PhotoUploadCandidateResolverTest` | PASS |
| image ID fallback | `PhotoImgIdResolverTest` | PASS |
| deleted-area state | `MapOverlayControllerStateTest`, source review | PASS 或依批准 AC 判定 |
| instrumentation artifact | `assembleDebugAndroidTest` | PASS；不等同 runtime PASS |

所有未執行或僅完成 compile 的項目，均不得標為 runtime `PASS`。

## Gate 3：裝置與 UI runtime 驗證

先確認裝置：

```bash
adb devices -l
```

至少需要記錄：serial、model、Android version、package、tested commit。
若沒有 device/emulator，以下 AC 全部是 `NOT VERIFIED`，不得以 APK build
代替：

### Photo flow

1. 以既有點位資料載入一個 URL 有值但無 `id` 的照片。
2. 進入編輯表單，確認照片仍顯示。
3. 儲存且不替換照片，確認不呼叫 `nodeImage`。
4. 替換照片後，確認 `nodeImage` 回傳的 `img_id` 被帶入
   `storeDitch.img_ids`。
5. 替換 slot 1、2、3 各一張照片，確認只上傳被替換 slot。
6. 覆蓋草稿回復、無法開蓋、虛擬點與重新開啟編輯流程。

### Deleted-area layer

1. 冷啟動主地圖，確認初始勾選/顯示狀態依批准 AC。
2. 開啟圖層選單，切換 `0910刪除資料`。
3. 確認取消勾選移除 overlay，再勾選恢復 overlay。
4. 切換底圖、重建地圖畫面、重新開啟圖層選單，確認狀態一致。

### Existing-waypoint title

1. 開啟「既有點位資料」頁面。
2. 以截圖或 UI assertion 確認標題相對完整 header row 水平置中。
3. 確認返回與右側 optional control 的 48dp 操作區仍可用。

每個 case 最多重試一次；重試後仍失敗即停止該 case，記錄最小必要
logcat/screenshot 並分類，不進行無關 exploratory testing。

## Gate 4：Acceptance Criteria 結果矩陣

Verification Agent 必須在 `verification.md` 逐項填寫 `PASS`、`FAIL` 或
`NOT VERIFIED`，不可使用模糊的「大致通過」：

| AC | 必要證據 | 目前狀態 |
|---|---|---|
| Photo ID preservation/upload rule | approved requirement + unit + device/API trace | NOT VERIFIED，待需求裁決及 runtime |
| `0910刪除資料` initial state/toggle | approved requirement + source/unit + device UI | NOT VERIFIED，待需求裁決及 runtime |
| `既有點位資料` title centering | approved requirement + XML review + device screenshot/assertion | NOT VERIFIED，待 runtime |
| Regression: existing photo, replacement, draft, virtual/cannot-open | related tests + device flow | NOT VERIFIED，待 runtime |

若需求裁決後發現 implementation 與 approved AC 不一致，分類為
`implementation`，建立/更新 issue，`next_action: debug`；不得直接修改程式
再重新驗證。

## Gate 5：Verification state 更新規則

Verification 開始：

```yaml
phase: verification
status: verification_in_progress
next_action: verification
```

需求/計畫缺失或衝突：

```yaml
phase: verification
status: verification_not_verified
verification:
  result: not_verified
  category: planning
next_action: planning
```

只有在所有批准的 AC 均有充分證據、runtime/CI 限制已解除且沒有未分類
問題時，才可使用：

```yaml
phase: verification
status: verification_passed
verification:
  result: pass
next_action: release
```

## Verification Agent 最終輸出

更新 `docs/tasks/debug-0918/verification.md`，至少包含：

- 固定 commit、branch、worktree 狀態
- 讀取過的 inputs
- 每個 AC 的結果、步驟、實際結果與 evidence
- 自動化測試與 build 命令/結果
- device context 或明確的 environment blocker
- regression review
- issues、failure classification、next action
- 唯一一個 final result：`PASS`、`FAIL` 或 `NOT VERIFIED`

禁止：

- 以 `assembleDebugAndroidTest` 宣稱 UI runtime PASS。
- 以 source review 取代需要畫面或 API trace 的驗證。
- 在 requirement/plan 缺失或衝突時自行選邊。
- 修改 production code 來讓 Verification 通過。

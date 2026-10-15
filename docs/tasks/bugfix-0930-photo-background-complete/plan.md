# Implementation Plan

## Goal
- 讓照片上傳留在背景且表單可立即完成，同時避免新照片缺少有效 image ID 時送出。

## Scope
- 移除拍照預覽全頁觸控遮罩；表單完成改為同步草稿後立即回傳；將新照片上傳成功條件收斂為正整數 `img_id`，正式送出遇到無 ID 結果時停止並保留重試狀態。

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterPhotosFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/PhotoLoadingHost.kt`
- `app/src/main/res/layout/activity_gutter_form.xml`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoSlotUploadCoordinator.kt`
- `app/src/main/java/com/example/taoyuangutter/common/PhotoUploadSlotState.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/test/java/com/example/taoyuangutter/common/PhotoUploadSlotStateTest.kt`
- `app/src/test/java/com/example/taoyuangutter/gutter/PhotoUploadCandidateResolverTest.kt` (only if regression assertion needs extension)

## Implementation Steps
- 移除 BasicInfo/Photos 對全頁 loading overlay 的 true/false 通知與 host/layout wiring，保留槽位 Glide/progress UI。
- 將結果 dispatch 改為只同步 session draft 後立刻回傳，維持 `resultDispatchInProgress` 防重複與 coordinator listener 清理。
- 加入共用有效 image ID 判定；background coordinator 與正式送出前直接上傳僅在正整數 ID 時寫 success，否則寫 failed 並讓正式送出停下；編輯模式移除「上傳中就要求使用者重試」的前置拒絕，改用同一送出 gate 等待。
- 新增 ID null/0/負數/正數的 focused unit tests；驗證 URL-only 匯入照片的舊測試不回歸。

## Test Plan
- `./gradlew testDebugUnitTest --tests 'com.example.taoyuangutter.common.PhotoUploadSlotStateTest' --tests 'com.example.taoyuangutter.gutter.PhotoUploadCandidateResolverTest' --tests 'com.example.taoyuangutter.api.StoreDitchNodeRequestMapperTest'`
- `./gradlew assembleDebug`
- 靜態檢查拍照 fragments 不再呼叫全頁 overlay；結果 dispatch 路徑不再 await coordinator；`storeDitch` 入口仍受上傳 gate 保護。

### Physical Device Test Scope
- Requires physical device: No
- Device/environment: JVM unit tests and local Android debug build
- In-scope Acceptance Criteria: AC-004, AC-005, AC-006; AC-001–AC-003 by source/data-flow evidence
- Regression risk: form return path, photo draft persistence, final upload gate, image ID mapping
- Full regression required: No
- Full regression trigger: 無
- Stop condition: targeted tests、debug build 與靜態 gate review 均完成；若失敗按 issue route 停止

| AC | Environment | Steps | Expected | Evidence |
|---|---|---|---|---|
| AC-001 | Source/build | Inspect all PhotoLoadingHost references and layout | No full-page photo touch blocker; slot progress retained | rg/source review + assembleDebug |
| AC-002 | Source/build | Follow fabSubmit result path during upload | Draft sync then result dispatch; no network wait | source review + assembleDebug |
| AC-003 | Unit/source | Exercise missing-ID upload classification and inspect submit gate | Missing/failed upload does not reach storeDitch | focused unit tests + source review |
| AC-004 | JVM | Test null, zero, negative, positive IDs | Only positive ID is success | PhotoUploadSlotStateTest |
| AC-005 | JVM | Run imported URL-only state tests | Existing behavior unchanged | PhotoUploadCandidateResolverTest |
| AC-006 | JVM/build | Run targeted tests and assemble debug | All selected checks pass | Gradle output |

## Regression Plan
- 保留每槽位預覽與上傳 progress rendering。
- 保留 URL-only imported photo upload exclusion tests。
- 確認 `storeDitch` mapper 只收到已取得的 server IDs；new upload 無 ID 路徑被拒絕。

## Risks
- 缺 ID 的 API success 需轉成可重試錯誤；不得把匯入既有 URL 的狀態規則全域改掉。
- process death 不保證 coordinator 繼續；使用者稍後送出時應由父層 gate 重新處理持久草稿。

## Rollback Plan
- 回退本 task commit；不涉及 schema migration 或不可逆資料變更。

## Current Behavior
- 全頁遮罩攔截相機返回後的操作，表單完成等待上傳最多 30 秒；缺少 `img_id` 仍可能被標成成功。

## Expected Behavior
- 可立即完成表單並保留正在上傳的照片草稿；正式側溝送出仍受有效 image ID gate 保護。

## Acceptance Criteria Traceability
| AC | Implementation Step | Validation |
|---|---|---|
| AC-001 | Step 1 | Source review + assembleDebug |
| AC-002 | Step 2 | Source review + assembleDebug |
| AC-003 | Step 3 | Targeted tests + submit-gate source review |
| AC-004 | Step 3–4 | PhotoUploadSlotStateTest |
| AC-005 | No behavior change to imported state | Existing resolver unit tests |
| AC-006 | Step 4 | Targeted Gradle tests + assembleDebug |

## Failure Behavior
- 上傳 HTTP error、逾時或 success response 缺少正整數 ID：記為 failed；正式送出不進入 `storeDitch`，使用者可在父層重試，照片本機 URI 與草稿保留。

## Security and Privacy
- 無權限、憑證或資料用途改變；照片仍留在 app-owned URI/session draft。

## Open Questions
- 無

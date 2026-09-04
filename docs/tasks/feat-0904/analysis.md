# Repository Analysis

## Current Behavior
- `AuthNavigator.clearAuthAndGoLogin()` 已能清除本機登入資訊並以清空 task stack 的方式導回 `LoginActivity`。
- `DashboardFragment` 目前會在 `DashboardUiState.errorCode == 401` 時呼叫 `AuthNavigator.clearAuthAndGoLogin()`，但這是單一畫面的處理。
- `GutterRepository` 多個已登入 API 會把 HTTP 401 包成 `ApiResult.Error(code=401, message="尚未登入，請重新登入")`，呼叫端多半只顯示錯誤，不一定強制登出。
- `logout()` 已把 HTTP 401 視為成功登出，符合「token 已失效也要清本機登入狀態」的產品行為。
- `login()` 的 401 是登入失敗情境，不應被納入已登入後的 token 過期強制登出。

## Expected Behavior
- App 需要一個共用 auth expired 判定與處理入口，讓所有已登入 API 收到 401 都能觸發同一個強制登出流程。
- UI 層不需要分散判斷每個 `ApiResult.Error.code == 401`，而是透過共用處理器、包裝函式或事件通道完成一次性 Dialog 與導頁。
- 401 發生時若有編輯中的側溝資料，需先復用既有草稿流程保存，再顯示「登入狀態已失效，請重新登入」Dialog。
- 非 auth 錯誤仍維持原本各流程的錯誤呈現。

## Known Backend Return Codes
- `200`：成功回應，通常 `success=true`。
- `401`：目前唯一已確認的 token 未登入、失效或過期代碼；dashboard 需求文件範例 body 為 `success=false`、`message=尚未登入`、`errors=null`。
- `404`：既有 repository 註解列為查無側溝或查無點位，非 token 過期。
- `409`：既有 debug/verification 文件提到新增側溝衝突類錯誤，非 token 過期。
- `422`：既有 repository 註解與錯誤分類列為欄位驗證失敗，非 token 過期。
- `500`：既有登入、登出與查詢註解列為伺服器錯誤，非 token 過期。

## Affected Modules
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt`
- `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt`
- `app/src/main/java/com/example/taoyuangutter/login/AuthNavigator.kt`
- `app/src/main/java/com/example/taoyuangutter/login/AuthExpiredHandler.kt`
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/dashboard/DashboardFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/main/java/com/example/taoyuangutter/map/ScopeMapCoordinator.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/ImportExistingWaypointActivity.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/ImportExistingWaypointBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/PhotoUploadManager.kt`
- `app/src/main/java/com/example/taoyuangutter/common/UploadFailureClassifier.kt`
- `app/src/main/java/com/example/taoyuangutter/pending/GutterDraftCoordinator.kt`
- `app/src/main/java/com/example/taoyuangutter/pending/GutterSessionRepository.kt`
- `app/src/test/java/com/example/taoyuangutter/api/*`
- `app/src/test/java/com/example/taoyuangutter/common/*`
- `app/src/test/java/com/example/taoyuangutter/pending/*`
- `app/src/androidTest/java/com/example/taoyuangutter/*`

## Authenticated 401 Call-Site Matrix
| Consumer | API / source | Editable data exists? | Draft save action before Dialog | Forced logout behavior |
|---|---|---:|---|---|
| `DashboardFragment` | `GutterRepository.getDashboard()` | No | No callback | Show auth-expired Dialog, confirm, clear auth, go login |
| `ScopeMapCoordinator` via `MainActivity` / `MapWorkspaceFragment` hooks | `GutterRepository.getGuttersByScope()` | Usually no, but map host may have active sheet | Propagate code-aware failure through hooks; host saves active sheet draft if present | Host handles once-only Dialog/logout |
| `MainActivity` | `inspectFlowCoordinator.load()` / `getDitchDetails()` | No for read-only inspect preview; yes if returning from edit/update | If active sheet/session exists, call existing `saveWaypointsAsPendingDraft()` or `draftCoordinator.autoSaveSessionDraft()` | Show Dialog/logout once |
| `MainActivity` | `gutterRepository.updateDitchState()` | Yes, when update follows an edit save path | Save `persistedWaypoints` with current session draft id before Dialog | Show Dialog/logout once |
| `MainActivity` | `gutterRepository.deleteDitch()` | No current edit data for delete confirmation | No callback | Show Dialog/logout once |
| `MainActivity` | `gutterRepository.storeNoDitch()` | No side-gutter edit data | No callback | Show Dialog/logout once |
| `MainActivity` | `PhotoUploadManager.uploadWaypointPhotos()` failures | Yes | Save current `persistedWaypoints` or active waypoints through `saveWaypointsAsPendingDraft()` before Dialog | Show Dialog/logout once |
| `MapWorkspaceFragment` | Same map/delete/no-ditch/inspect/update/upload call sites as shell-hosted map flow | Yes when `activeSheet` or `inspectSheet` exists | Call fragment-local draft coordinator/autosave using current sheet waypoints and draft id | Show Dialog/logout once |
| `AddGutterBottomSheet` | `repository.storeDitch()` in add mode | Yes | Invoke host forced-save callback with `validWaypoints` after latest state sync | Show Dialog/logout via host-aware handler |
| `AddGutterBottomSheet` | `repository.storeDitch()` in edit mode | Yes | Invoke host forced-save callback with `waypoints.toList()` and existing `editSpiNum` | Show Dialog/logout via host-aware handler |
| `AddGutterBottomSheet` | `ensureWaypointPhotosUploadedBeforeSubmit()` / `repository.uploadNodeImage()` | Yes | Mark failed slot in waypoint state, call `onWaypointsChanged(waypoints.toList())`, then host autosaves | Show Dialog/logout via host-aware handler |
| `AddGutterBottomSheet` | `preloadEditWaypointDetails()` / `repository.getNodeDetails()` | Yes | Preserve current loaded waypoints and autosave if edit session has meaningful data | Show Dialog/logout instead of only logging preload error |
| `GutterFormActivity` | Direct photo upload `gutterRepository.uploadNodeImage()` and submit/preload-related authenticated errors | Yes | Call existing draft sync path before Dialog; create a session draft id if missing | Show Dialog/logout once |
| `GutterInspectActivity` | edit preload fallback `repository.getNodeDetails()` | Read-only until edit starts | No callback for pure inspect; if edit preload materializes editable waypoints, save through host/form draft path | Show Dialog/logout once |
| `ImportExistingWaypointActivity` | `getNodeDetailsByXyNum()` | No local edit data in that screen | No callback | Show Dialog/logout once |
| `ImportExistingWaypointBottomSheet` | `getClosestNodeDetails()` / `getNodeDetailsByXyNum()` | Parent sheet may have editable gutter data | Parent host supplies forced-save callback for current add/edit sheet before Dialog | Show Dialog/logout once |
| `PhotoUploadManager` | `uploadNodeImage()` batch upload | Yes in caller-owned waypoint list | Return/mark `ApiResult.Error(code=401)` in batch failures so caller performs forced-save/Dialog/logout | Caller handles Dialog/logout |
| `PhotoSlotUploadCoordinator` | Background `uploadNodeImage()` for draft photo slot | Draft already exists | Keep draft record and mark slot failed with 401; notify listener with error so visible owner can show Dialog/logout | Visible owner handles Dialog/logout once |

## Dependencies
- Existing `ApiResult.Error.code` carries HTTP status code.
- Existing `AuthNavigator.clearAuthAndGoLogin()` already performs the required local cleanup and navigation flags.
- Existing coroutine-based API calls in activities/fragments need a lifecycle-safe way to emit auth expired events.
- Existing upload error classifier must preserve 422/network/timeout user messages while allowing 401 to trigger forced logout.
- Existing `GutterDraftCoordinator.autoSaveSessionDraft()` can save the current waypoint list into the Room-backed draft store.
- Existing `GutterFormActivity.syncCurrentStateToDraft()` style flow can persist in-progress form/photo edits before leaving the screen.

## Risks
- API calls are distributed across map, dashboard, inspect, form, photo upload, and repository helper flows, so missing one call site can leave users stranded on an authenticated screen.
- Concurrent API calls may return multiple 401 responses at once and cause repeated navigation unless guarded.
- Treating all 401 globally could accidentally affect login failure unless the login flow is explicitly excluded.
- Photo upload paths may wrap lower-level failures differently from Retrofit responses, so 401 propagation must be verified there.
- Changing `ApiResult` shape broadly can create unnecessary churn; prefer a small extension/helper if it satisfies the need.
- Draft saving must run before auth cleanup/navigation; navigating too early can destroy Activity/Fragment state before the current edit can be serialized.
- If multiple 401 responses arrive while a draft save is running, duplicate Dialog/navigation must still be suppressed.

## Unknowns
無

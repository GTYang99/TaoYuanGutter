# Implementation Plan

## Goal
- Add a shared forced-logout flow so any authenticated API returning 401 saves in-progress edit data as a draft, shows an auth-expired Dialog, then logs the user out and returns to `LoginActivity`.

## Scope
- Handle authenticated API 401 responses across dashboard, map, inspect, form, ditch, node, no-ditch, delete/update, and photo/upload-related flows.
- Save current editable gutter/session data to the existing draft store before forced logout whenever such data exists.
- Show the required auth-expired Dialog before clearing auth and navigating.
- Preserve login 401 as a normal login failure and preserve non-auth API errors as their existing user-facing failures.

## Affected Files
- `app/src/main/java/com/example/taoyuangutter/api/GutterApiModels.kt` - Add a small auth-expired helper on `ApiResult.Error` or equivalent typed signal.
- `app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt` - Keep authenticated endpoints returning `ApiResult.Error(code=401)` consistently and avoid handling login 401 as auth-expired.
- `app/src/main/java/com/example/taoyuangutter/login/AuthNavigator.kt` - Reuse or extend the existing clear-and-login navigation after the Dialog confirmation.
- `app/src/main/java/com/example/taoyuangutter/login/AuthExpiredHandler.kt` - Add a small shared UI handler for once-only Dialog display, optional draft save callback, and confirmed logout navigation.
- `app/src/main/java/com/example/taoyuangutter/MainActivity.kt` - Handle legacy/main map 401 paths and forced-save active map edits before logout.
- `app/src/main/java/com/example/taoyuangutter/dashboard/DashboardFragment.kt` - Replace local 401 handling with the shared auth-expired handler.
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt` - Route map, logout, delete, inspect reload, store no-ditch, store ditch, and update-state auth failures through the shared handler.
- `app/src/main/java/com/example/taoyuangutter/map/ScopeMapCoordinator.kt` - Surface code-aware failures so hosts can distinguish 401 from ordinary map load errors.
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt` - Route add/edit store, edit preload, and photo upload 401 through host forced-save/logout handling.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterInspectActivity.kt` - Route preload/edit/detail auth failures through the shared handler.
- `app/src/main/java/com/example/taoyuangutter/gutter/GutterFormActivity.kt` - Route form submit/authenticated failures through the shared handler and sync current form/photo state into draft before logout.
- `app/src/main/java/com/example/taoyuangutter/gutter/ImportExistingWaypointActivity.kt` - Route import search 401 through forced logout.
- `app/src/main/java/com/example/taoyuangutter/gutter/ImportExistingWaypointBottomSheet.kt` - Route nearby/search import 401 through forced logout and parent draft save when editing.
- `app/src/main/java/com/example/taoyuangutter/gutter/PhotoUploadManager.kt` - Ensure upload HTTP 401 is surfaced as `ApiResult.Error(code=401)` or equivalent.
- `app/src/main/java/com/example/taoyuangutter/common/UploadFailureClassifier.kt` - Preserve user-facing upload failure categories while allowing 401 to trigger forced logout before or alongside the message.
- `app/src/main/java/com/example/taoyuangutter/pending/GutterDraftCoordinator.kt` - Reuse current autosave rules and only extend if a forced-save entry point is needed for missing draft ids.
- `app/src/main/java/com/example/taoyuangutter/pending/GutterSessionRepository.kt` - Persist forced-logout drafts through the existing Room-backed draft store.
- `app/src/test/java/com/example/taoyuangutter/api/*` - Add auth-expired classification tests.
- `app/src/test/java/com/example/taoyuangutter/common/*` - Add upload/auth failure classification coverage.
- `app/src/test/java/com/example/taoyuangutter/pending/*` - Add forced-logout draft save coverage where possible.
- `app/src/androidTest/java/com/example/taoyuangutter/*` - Add UI coverage for forced logout from an authenticated screen.

## Implementation Steps
- Add a single `isAuthExpired` style helper that returns true only for authenticated API `ApiResult.Error(code=401)`.
- Add a lifecycle-safe `AuthExpiredHandler` that consumes an auth-expired error once, runs an optional `saveCurrentDraft` callback, shows a non-cancelable Dialog with「登入狀態已失效，請重新登入」and a logout confirmation button, then clears stored auth data and navigates to `LoginActivity`.
- Define a small `saveCurrentDraft` callback contract for screens with editable state; screens without editable state can pass no callback.
- Make `ScopeMapCoordinator.Hooks` propagate code-aware failures, either by passing `ApiResult.Error` or adding `onAuthExpired`, so host screens can distinguish 401 from ordinary loading errors.
- Update dashboard to use the shared helper instead of maintaining its own one-off 401 branch.
- Update map/workspace call sites so scope search, delete, no-ditch submit, inspect loading, store ditch, update state, and restore-state flows trigger forced logout on 401; when an add/edit sheet has waypoints, call `GutterDraftCoordinator.autoSaveSessionDraft()` first.
- Update `GutterFormActivity` so submit/upload/preload-related 401 responses call its current draft sync path before showing the forced-logout Dialog.
- Update `AddGutterBottomSheet` so add/edit store, photo upload, and edit preload 401 branches delegate to the host auth-expired handler after pushing the latest waypoint state to the host.
- Update import screens so 401 from `getClosestNodeDetails()` or `getNodeDetailsByXyNum()` uses forced logout instead of normal load-failed rows.
- Update `PhotoSlotUploadCoordinator` so a draft-photo upload 401 is stored in the slot state and surfaced to active listeners with enough information for the visible owner to trigger forced logout.
- Update inspect/form/photo upload flows so preloading and upload-related 401 responses are not left as ordinary dialogs only and preserve any local photo paths in the saved draft.
- Keep `logout()` behavior unchanged: 200 and 401 both result in local auth cleanup and login navigation.
- Keep `login()` behavior unchanged: login 401 remains a login failure message.
- Add tests for 401 inclusion, login exclusion, logout 401 success behavior, draft-save-before-logout, duplicate handling, and non-401 regression cases.

## Draft Save Contract
| Surface | Data source to serialize | Draft id rule | Save method | Failure behavior |
|---|---|---|---|---|
| `MainActivity` active add/edit sheet | `activeSheet` / `inspectSheet` current waypoints and selected `spiTyp` | Use `currentSessionDraftId`; if missing, create with `ensureCurrentSessionDraftId()` before save | `saveWaypointsAsPendingDraft()` or `draftCoordinator.autoSaveSessionDraft()` | Log failure and continue showing Dialog/logout |
| `MapWorkspaceFragment` active add/edit sheet | Fragment sheet waypoints and selected `spiTyp` | Use fragment `currentSessionDraftId`; if missing, create current timestamp draft id before save | Fragment-local `draftCoordinator.autoSaveSessionDraft()` | Log failure and continue showing Dialog/logout |
| `AddGutterBottomSheet` add mode | `validWaypoints` after `syncLatestDraftStateIntoWaypoints()` and photo repair | Host owns draft id | Push latest waypoints through `onWaypointsChanged()` then call host forced-save callback | If host missing, log and continue Dialog/logout |
| `AddGutterBottomSheet` edit mode | `waypoints.toList()` including `editSpiNum`, photo upload states, and pending photo paths | Host owns draft id; preserve existing draft if resumed | Push latest waypoints through host callback and autosave with `editSpiNum` | If save fails, keep local state until Dialog confirmation and log |
| `GutterFormActivity` online/offline form | `sessionWaypoints` plus `currentFormSnapshot()` and normalized photo URIs | Use `sessionDraftId`; if `<=0`, create a timestamp id before sync | Existing `syncSessionDraftNow()` / blocking wrapper | Log save error, then continue Dialog/logout |
| `GutterInspectActivity` read-only inspect | No editable state by default | None | No save callback | Show Dialog/logout |
| `GutterInspectActivity` edit preload | Generated waypoint snapshots if edit preload has begun | Use returned/host draft id if available; otherwise create timestamp id in host edit launch path | Host/form draft sync after preload state is materialized | Log failure and continue Dialog/logout |
| `ImportExistingWaypointBottomSheet` | Parent add/edit sheet waypoints, not the import candidate list | Parent host owns draft id | Call parent/host forced-save callback before Dialog | If parent unavailable, no save and log |
| `PhotoUploadManager` batch upload | Caller-owned `persistedWaypoints` or active `waypoints` with slot state | Caller owns draft id | Caller saves after receiving 401 failure | Batch returns immediately after 401 classification |
| `PhotoSlotUploadCoordinator` background upload | Existing `GutterSessionDraft` row | Existing `draftId`; never create a new draft here | `updateDraftAndNotify()` writes failed slot state with 401 message | If no listener, draft remains saved; next visible owner can react on resume if needed |

## Test Plan
- Add `ApiResultAuthTest` for `isAuthExpired`: 401 true, 404/409/422/500/network false, login failure handled by caller false.
- Add `AuthExpiredHandlerTest` or Robolectric-covered unit tests for once-only handling: duplicate 401 calls run one draft-save callback and show/navigate once.
- Add `GutterRepositoryLogoutTest` or equivalent fake-service test confirming logout 401 still maps to success.
- Add `UploadFailureClassifierTest` coverage confirming upload/store 401 remains distinguishable from validation/network failures.
- Add `GutterDraftCoordinatorTest` coverage for forced save with existing id, generated id caller path, SPI_NUM de-duplication, and local photo path preservation.
- Add `DashboardViewModelTest` coverage confirming dashboard 401 remains exposed as `errorCode=401` for the handler.
- Add or update `ScopeMapCoordinatorTest` so 401 is surfaced through a code-aware hook and does not become a generic loading error only.
- Add instrumentation coverage in `MainShellActivityTest` or a new auth-expired test: launch an authenticated edit surface with a fake 401 repository response, assert draft row exists, assert Dialog text is shown, tap confirm, assert `LoginActivity` is displayed.
- If existing UI tests cannot inject a fake repository cleanly, add a debug/test-only repository provider seam as part of implementation and use it for AC-014 instead of marking the test conditional.
- Run the existing `testDebugUnitTest` suite.

## Regression Plan
- Verify login failure with HTTP 401 still shows the login error and does not enter a redirect loop.
- Verify manual logout with HTTP 401 still clears auth and returns to login.
- Verify 404/409/422/500/network errors still show their existing flow-specific messages and do not force logout.
- Verify draft/edit/photo upload failures do not lose unsaved local draft state or local photo paths except for the required auth cleanup.
- Verify returning after a new login allows the user to find and resume the forced-saved draft.
- Verify dashboard, map, inspect, and form flows do not trigger duplicate login activities when multiple API calls fail together.

## Risks
- Some call sites may swallow repository errors after logging or mapping them into domain-specific UI models.
- Concurrent coroutines can produce repeated 401 events unless the handler is idempotent.
- Photo upload may use lower-level OkHttp paths, so HTTP status propagation must be checked carefully.
- A broad `ApiResult` change could touch many files; keep the implementation small and compatibility-focused.
- Saving a draft from a partially initialized edit screen may require generating or recovering a session draft id before calling existing autosave.
- Dialog display must be lifecycle-safe when the 401 arrives after the Activity/Fragment is stopping.

## Rollback Plan
- Revert the shared auth-expired helper and call-site changes, restoring the previous per-screen 401 behavior.

## Current Behavior 
- Only dashboard currently forces a login redirect when its state has `errorCode == 401`.
- Other authenticated flows often return or display `ApiResult.Error(code=401)` without a shared forced-logout path.
- Logout already treats 401 as a successful local logout.
- In-progress edits can be saved through existing draft paths, but token-expired logout does not yet guarantee draft save before navigation.

## Expected Behavior
- Any authenticated API returning HTTP 401 first saves in-progress editable data to draft when present, then shows the required auth-expired Dialog.
- After the user confirms the Dialog, the app clears local auth data and opens `LoginActivity`.
- Login API 401 remains a normal login failure.
- Non-401 failures are unaffected.

## Open Questions
無

# Fix Plan

Task: feat-0904
Date: 2026-09-04
Phase: debug

## Goal

Resolve verification failures for AC-002, AC-004, AC-006, AC-007, AC-013, and AC-014 with the smallest scoped follow-up implementation.

## Scope

- Fix missing authenticated 401 handling in edit preload and parent-owned import flows.
- Make auth-expired Dialog copy satisfy the exact required phrase.
- Add missing unit and instrumentation evidence.
- Remove unrelated debug flag change from this feature diff.

## Proposed Changes

### 1. AddGutterBottomSheet edit preload 401

Affected file:

- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`

Plan:

- In `preloadEditWaypointDetails()`, when `repository.getNodeDetails(...)` returns `ApiResult.Error(code = 401)`, update current sheet state first:
  - Call `onWaypointsChanged?.invoke(waypoints.toList())`.
  - Call `AuthExpiredHandler.handleIfAuthExpired(result)`.
  - Return from the preload loop/launch so the partial-load Toast is not shown afterward.
- For non-401 preload errors, preserve existing `hasError = true` + log + partial-load Toast behavior.

Acceptance impact:

- Covers AC-002 and part of AC-006.

### 2. Parent draft save for import bottom sheet 401

Affected files:

- `app/src/main/java/com/example/taoyuangutter/gutter/ImportExistingWaypointBottomSheet.kt`
- `app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt`

Plan:

- Extend `ImportExistingWaypointBottomSheet.Callbacks` with a focused callback such as `onAuthExpiredBeforeImportLogout()`.
- In `AddGutterBottomSheet`'s import callbacks, implement that callback by invoking `onWaypointsChanged?.invoke(waypoints.toList())`.
- Before `ImportExistingWaypointBottomSheet` calls `AuthExpiredHandler.handleIfAuthExpired(...)`, invoke the callback so the parent sheet/host saves active edit data through the existing autosave path.
- Keep standalone `ImportExistingWaypointActivity` unchanged, because it has no parent edit data to save.

Acceptance impact:

- Covers the import-bottom-sheet part of AC-006 without introducing a new draft repository dependency into the import sheet.

### 3. Dialog copy

Affected file:

- `app/src/main/java/com/example/taoyuangutter/login/AuthExpiredHandler.kt`

Plan:

- Ensure the visible Dialog message contains the exact phrase `登入狀態已失效，請重新登入`.
- Recommended message: `登入狀態已失效，請重新登入。系統即將登出。`
- Keep the Dialog non-cancelable and keep confirm behavior unchanged.

Acceptance impact:

- Covers AC-004.

### 4. Remove unrelated debug flag change

Affected file:

- `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt`

Plan:

- Restore `ENABLE_GROUP_SIMULATION` to `false`.
- Do not touch the base URL line unless separately confirmed, because the existing diff also changes `.baseUrl(DEMO_URL)` to `.baseUrl(BASE_URL)` and verification only flagged the group simulation flag.

Acceptance impact:

- Resolves ISS-0904-004 and reduces unrelated regression risk.

### 5. Unit test coverage

Affected test files:

- `app/src/test/java/com/example/taoyuangutter/api/ApiResultAuthTest.kt`
- `app/src/test/java/com/example/taoyuangutter/common/UploadFailureClassifierTest.kt`
- New `app/src/test/java/com/example/taoyuangutter/login/AuthExpiredHandlerTest.kt` if Robolectric/test dependencies support it.
- New or updated repository tests for login/logout 401 if fake Retrofit service setup is practical.

Plan:

- Keep existing `isAuthExpired()` tests.
- Add upload classifier coverage proving 401 remains distinguishable from validation/network/photo-processing failures.
- Add handler once-only coverage:
  - first 401 invokes draft-save callback once;
  - duplicate 401 while Dialog is active does not invoke save twice;
  - non-401 returns false and does not save.
- Add logout 401 success coverage around `GutterRepository.logout()` if the project can fake `GutterApiService` cleanly.
- Add login 401 exclusion coverage either at repository result level or at `LoginActivity`/caller level, depending on available test seams.

Acceptance impact:

- Covers AC-013 and supports AC-011/AC-012 regression proof.

### 6. Instrumentation coverage for edit-screen 401

Affected test files:

- `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt` or a new auth-expired instrumentation test.

Plan:

- Add a test-only repository/provider injection point only if current constructors cannot provide fake 401 responses.
- Launch an authenticated editing surface with local fake auth prefs.
- Trigger a fake authenticated API 401 from an edit path.
- Assert in order:
  - draft exists in the draft store;
  - Dialog shows text containing `登入狀態已失效，請重新登入`;
  - tapping confirm clears auth prefs;
  - `LoginActivity` becomes the active screen.
- Include a waypoint/photo state assertion if the test uses photo-bearing draft data.

Acceptance impact:

- Covers AC-007 and AC-014.

## Validation Plan

Run:

```bash
env JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew testDebugUnitTest
```

Run:

```bash
env JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew connectedDebugAndroidTest
```

Expected result:

- Both commands pass.
- Verification can re-check AC-002, AC-004, AC-006, AC-007, AC-013, and AC-014 with code and test evidence.

## Out Of Scope

- No API contract changes.
- No login behavior changes.
- No broad repository refactor.
- No unrelated debug feature enablement.
- No requirement or acceptance-criteria changes.

## Ready For Re-Implementation

Yes. The failure category is implementation, root causes are identified, and each failed AC maps to a minimum follow-up change.

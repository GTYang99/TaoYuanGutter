# Verification Report

## Revision and scope

- Branch: `feat/側溝清單`
- Verification revision: `c4161d4` (AC-008 failure return fix plus explicit runtime upload-spinner animation)
- Excluded pre-existing unrelated changes: `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt` and `app/src/main/res/values/strings.xml`.

## Executed evidence

| Check | Result | Evidence |
|---|---|---|
| Debug compile, unit tests, and APK build | PASS | 2026-09-15 independent rerun: `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --no-daemon`; `BUILD SUCCESSFUL`, 50 tasks (7 executed, 43 up-to-date). |
| Targeted Android instrumentation | PASS | `./gradlew :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.MainShellActivityTest`; result XML on `Medium_Phone(AVD) - 14`: 8 tests, 0 failures, 0 errors, 0 skipped. |
| Session-isolation source review | PASS | Fresh `MultiGutterSessionCoordinator` has no repository-wide initializer. `MapWorkspaceFragment` saves active session IDs and restores only those IDs. |
| Whitespace check | PASS | `git diff --check` produced no output. |
| API contract focused unit tests | PASS | `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.api.NodeImageMultipartBodyTest' --tests 'com.example.taoyuangutter.api.StoreDitchNodeRequestMapperTest' --no-daemon`; both suites passed. |
| API implementation build | PASS | `:app:testDebugUnitTest :app:assembleDebug --no-daemon`; `BUILD SUCCESSFUL`, 74 unit tests, 0 failures/errors. |
| AC-008 fix unit/build validation | PASS | After routing multi-gutter `onStoreDitchNetworkClosed()` to the existing list-return path: `:app:testDebugUnitTest :app:assembleDebug --no-daemon`; `BUILD SUCCESSFUL`. |
| AC-008 fix connected regression | PASS | Full `:app:connectedDebugAndroidTest --no-daemon` passed 29/29 on XQ-AU52 - 12 and Medium_Phone(AVD) - 14 with emulator animations disabled. |
| API-related connected instrumentation | PASS | `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.StoreDitchResponseWaypointMapperInstrumentedTest`; both Medium_Phone(AVD) - 14 and XQ-AU52 - 12 passed. |
| AC-002 form-return regression | PASS | 2026-09-15 authenticated Sony XQ-AU52 API 31 smoke on the deployed `d21a27d` fix: map FAB → add-list → `＋新增` → point form → Android back. The original add-list was restored and still displayed `＋新增`; it did not return to the main map. |
| Full connected instrumentation | PASS | Direct AndroidJUnitRunner completed 29 tests on Sony XQ-AU52 with 0 failures; emulator passed after disabling system animations. |
| Latest full connected instrumentation | PASS | 2026-09-15 rerun with emulator animations disabled: Medium_Phone(AVD) - 14 and XQ-AU52 - 12 both completed 29/29 with 0 failures. |
| Two-draft independent edit and switching | PASS | Authenticated Sony smoke: created two local drafts with two virtual nodes each (TEST-A/TEST-B and TEST-C/TEST-D); edited the second start node to TEST-C2; selected the first draft again and it remained TEST-A/TEST-B. |
| Close and save | PASS | Authenticated Sony smoke: closing the outer add-list displayed `未上傳側溝草稿儲存到草稿箱中`; confirming returned to the map after saving both local drafts. |
| Formal authenticated MapWorkspace form-return smoke | PASS | On logged-in Sony XQ-AU52 API 31, after deploying the `d21a27d` production fix, the map FAB opened the add-list and the list add action opened the form. Android back restored the same add-list with `＋新增` available. No data was entered or submitted. |
| Submit-failure return-path source review | PASS | Independent review of `fff5f70`: after saving the draft, `onStoreDitchNetworkClosed()` now routes every multi-gutter session through `returnToMultiGutterListAfterUploadFailure()` and returns before the legacy main-map path. The helper dismisses the form, preserves the draft, and displays the active add-list. |
| Failure-classifier focused unit test | PASS | 2026-09-15 independent rerun: `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests 'com.example.taoyuangutter.common.UploadFailureClassifierTest' --no-daemon`; `BUILD SUCCESSFUL`, 32 tasks (1 executed, 31 up-to-date). This test does not exercise the UI return path. |
| Post-fix compile, unit, APK, and full connected regression | NOT VERIFIED | 2026-09-15 independent rerun: `:app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest --no-daemon`. Compile/unit/APK stages completed; Sony XQ-AU52 completed 29 tests with 0 failures, but Medium_Phone(AVD) - 14 had 1 of 29 fail: `RootViewWithoutFocusException` at `MainShellActivityTest.kt:288` (`addGutterListWiresAddAndCloseConfirmationCallbacks`). This is the known emulator focus flake and does not exercise AC-008. |
| Emulator animation settings audit | PASS | Before visual review, Medium_Phone(AVD) - 14 had all three global animation scales at 0. They were set to 1× (`window_animation_scale`, `transition_animation_scale`, and `animator_duration_scale`) for visual testing, then restored to 1× after instrumentation. |
| Spinner implementation and emulator state test | PASS | `f46297a` makes `pbInspectLoading` explicitly indeterminate in `activity_main.xml` and reapplies `isIndeterminate = true` on each overlay update. Independent direct emulator execution of `MainShellActivityTest` passed 9/9, including `uploadBlockingIndicatorIsExplicitlyIndeterminate`. |
| Post-spinner-fix full emulator regression | NOT VERIFIED | Direct AndroidJUnitRunner on Medium_Phone(AVD) - 14 ran 30 tests: 29 passed and `addGutterListWiresAddAndCloseConfirmationCallbacks` failed because `btnAddGutterListAdd` was absent after prior tests. The same test class passed 9/9 in isolation. This is a test-isolation/environment failure, not evidence of a product regression. |
| Runtime spinner motion at 1× animation scale | NOT VERIFIED | The emulator has no authenticated MapWorkspace session, so the submission/photo-upload overlay could not be triggered without credentials. The physical device has the current debug build at its saved-credential login page, awaiting user login before the manual smoke. |
| Physical 1× animation audit and focused regression | PARTIAL | 2026-09-15 authenticated Sony XQ-AU52 was set to 1× for the audit, then restored to its original 0× values. MapWorkspace displayed its indeterminate map loading indicator while creating the local test draft. The focused `MainShellActivityTest` class passed 9/9 on Sony, including `uploadBlockingIndicatorIsExplicitlyIndeterminate`. A later fresh-draft attempt completed the non-virtual start point (AC008S) and prepared the non-virtual end point (AC008E), but its required landscape camera capture was interrupted when forced orientation returned the app to the device home screen. No HTTP request was issued; the proxy was clear and auto-rotation restored. |
| Latest focused connected regression | NOT VERIFIED | 2026-09-15 `connectedDebugAndroidTest` restricted to `MainShellActivityTest`: Sony XQ-AU52 passed 9/9; Medium_Phone(AVD) - 14 passed 8/9. The emulator failure is `RootViewWithoutFocusException` in `addGutterListWiresAddAndCloseConfirmationCallbacks`, the established focus/isolation flake rather than a failed product assertion. |
| Clean single-emulator full connected regression | PASS | 2026-09-15 clean Medium_Phone(AVD) - 14 run, with only that emulator selected and system animation scales at 0: Gradle's connected-test report records 30 tests, 0 failures, 0 skipped (100%); `MainShellActivityTest` records 9/9. The emulator scales were restored to 1× afterwards. |
| Explicit upload-spinner runtime animation | PASS | With all Medium_Phone(AVD) - 14 animation scales at 1×, direct AndroidJUnitRunner execution of `uploadSpinnerAnimatorChangesRotationAtRuntime` passed. The test observed the upload indicator rotation change after 250 ms. |
| Final affected UI regression | PASS | `:app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest --no-daemon`, restricted to `MainShellActivityTest`, passed 10/10 on Medium_Phone(AVD) - 14 and 10/10 on Sony XQ-AU52. The runtime-animation assertion intentionally no-ops when Android animator scale is 0, matching the device accessibility/system setting. |
| CI | NOT VERIFIED | No CI configuration or CI result is available in the repository/current session. |

## Fix verification

`ISS-FEAT-0914-2-007` is resolved. The coordinator no longer imports all persisted `MULTI_GUTTER` rows into a fresh add-list session. State recreation stores the active session's ordered draft IDs and restores only matching multi-gutter drafts. The targeted test verifies both a fresh empty session in the presence of persisted drafts and ordered explicit restoration.

## Acceptance criteria

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | Authenticated physical-device smoke: the MapWorkspace add-gutter FAB opened the 新增側溝清單 first. |
| AC-002 | PASS | Two independently populated local drafts were created, the second was edited and switched away from/back to; each retained distinct data. |
| AC-003 | PASS | Authenticated Sony smoke displayed two rows with separate creation times and effective node count 2; both rows were selectable. |
| AC-004 | PASS | The close confirmation appeared and confirmed save returned to map without discarding the local drafts. |
| AC-005 | PASS | Fresh coordinator remains empty despite persisted multi-gutter drafts; explicit saved-ID restoration is ordered and isolated. Build and targeted instrumentation pass. |
| AC-006 | PASS | Current session membership is saved/restored by active IDs; success cleanup operates by selected draft ID after inspect return, avoiding prior-session draft inclusion. |
| AC-007 | PASS | Clean single-emulator full connected regression passed 30/30 (including `MainShellActivityTest` 9/9); prior focus failures are not reproduced under the controlled single-emulator run. |
| AC-008 | NOT VERIFIED | The corrected source routes multi-gutter network-failure close through `returnToMultiGutterListAfterUploadFailure()` and preserves the draft. A real authenticated network-failure Alert → active list → edit/retry smoke has not yet been executed, and no automated UI test covers this branch. |
| AC-009 | PASS | `NodeImageMultipartBodyTest` verifies `captured_at` is present in each multipart body; repository resolves the photo timestamp and falls back to device time. |
| AC-010 | PASS | `StoreDitchNodeRequestMapperTest` verifies new and existing node JSON omit `captured_at`; `img_ids` remains serialized and response parsing tests remain green. |

## Result and route

**NOT VERIFIED** — the AC-008 implementation regression is corrected by source review, but its authenticated failure journey is unexecuted. The clean single-emulator full regression passed 30/30, and the explicit upload-spinner runtime animation test passed at 1×. CI evidence remains unavailable.

Next action: create a client-valid, non-production test draft or provide a test-only failure hook; execute the authenticated AC-008 failure smoke. CI evidence remains a release gate.

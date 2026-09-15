# Verification Report

## Revision and scope

- Branch: `feat/側溝清單`
- Verification revision: `4518596` (API contract implementation: `e22e78e`)
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
| API-related connected instrumentation | PASS | `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.StoreDitchResponseWaypointMapperInstrumentedTest`; both Medium_Phone(AVD) - 14 and XQ-AU52 - 12 passed. |
| AC-002 form-return regression | PASS | 2026-09-15 authenticated Sony XQ-AU52 API 31 smoke on the deployed `d21a27d` fix: map FAB → add-list → `＋新增` → point form → Android back. The original add-list was restored and still displayed `＋新增`; it did not return to the main map. |
| Full connected instrumentation | PASS | Direct AndroidJUnitRunner completed 29 tests on Sony XQ-AU52 with 0 failures; emulator passed after disabling system animations. |
| Latest full connected instrumentation | PASS | 2026-09-15 rerun with emulator animations disabled: Medium_Phone(AVD) - 14 and XQ-AU52 - 12 both completed 29/29 with 0 failures. |
| Two-draft independent edit and switching | PASS | Authenticated Sony smoke: created two local drafts with two virtual nodes each (TEST-A/TEST-B and TEST-C/TEST-D); edited the second start node to TEST-C2; selected the first draft again and it remained TEST-A/TEST-B. |
| Close and save | PASS | Authenticated Sony smoke: closing the outer add-list displayed `未上傳側溝草稿儲存到草稿箱中`; confirming returned to the map after saving both local drafts. |
| Formal authenticated MapWorkspace form-return smoke | PASS | On logged-in Sony XQ-AU52 API 31, after deploying the `d21a27d` production fix, the map FAB opened the add-list and the list add action opened the form. Android back restored the same add-list with `＋新增` available. No data was entered or submitted. |
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
| AC-007 | PASS | Full connected suite passed 29/29 on both Sony XQ-AU52 and Medium_Phone emulator after disabling emulator system animations. |
| AC-008 | NOT VERIFIED | Submit failure → Alert confirmation → list return → retry was not exercised end-to-end. |
| AC-009 | PASS | `NodeImageMultipartBodyTest` verifies `captured_at` is present in each multipart body; repository resolves the photo timestamp and falls back to device time. |
| AC-010 | PASS | `StoreDitchNodeRequestMapperTest` verifies new and existing node JSON omit `captured_at`; `img_ids` remains serialized and response parsing tests remain green. |

## Result and route

**NOT VERIFIED** — API contract, multi-item flow, independent edit/switch, close/save, and dual-device connected regression now pass. Remaining gates are submit-failure return/retry and CI evidence.

Next action: infrastructure evidence collection for the remaining verification gates.

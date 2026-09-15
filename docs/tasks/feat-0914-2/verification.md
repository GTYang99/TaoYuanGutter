# Verification Report

## Revision and scope

- Branch: `feat/側溝清單`
- Verification revision: `d21a27d` (production fix: `d21a27d`)
- Excluded pre-existing unrelated changes: `app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt` and `app/src/main/res/values/strings.xml`.

## Executed evidence

| Check | Result | Evidence |
|---|---|---|
| Debug compile, unit tests, and APK build | PASS | `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --no-daemon`; `BUILD SUCCESSFUL`, 50 tasks up-to-date. |
| Targeted Android instrumentation | PASS | `./gradlew :app:connectedDebugAndroidTest --no-daemon -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.MainShellActivityTest`; result XML on `Medium_Phone(AVD) - 14`: 8 tests, 0 failures, 0 errors, 0 skipped. |
| Session-isolation source review | PASS | Fresh `MultiGutterSessionCoordinator` has no repository-wide initializer. `MapWorkspaceFragment` saves active session IDs and restores only those IDs. |
| Whitespace check | PASS | `git diff --check` produced no output. |
| AC-002 targeted regression | PASS | Compile, unit tests, instrumentation compile, and targeted `MainShellActivityTest` passed on both connected devices after adding the form-handoff guard. Authenticated physical-device smoke must still be repeated. |
| Full connected instrumentation | NOT VERIFIED | A prior committed record reports a two-device pass, but this verifier's targeted rerun retained independent result XML only for the emulator; it is insufficient to mark the full suite PASS. |
| Formal authenticated MapWorkspace smoke | FAIL | On the logged-in Sony XQ-AU52 API 31, the map FAB opened the add-list and the list add action opened the form; returning from that form bypassed the add-list and returned to the main map. |
| CI | NOT VERIFIED | No CI configuration or CI result is available in the repository/current session. |

## Fix verification

`ISS-FEAT-0914-2-007` is resolved. The coordinator no longer imports all persisted `MULTI_GUTTER` rows into a fresh add-list session. State recreation stores the active session's ordered draft IDs and restores only matching multi-gutter drafts. The targeted test verifies both a fresh empty session in the presence of persisted drafts and ordered explicit restoration.

## Acceptance criteria

| AC | Result | Evidence / limitation |
|---|---|---|
| AC-001 | PASS | Authenticated physical-device smoke: the MapWorkspace add-gutter FAB opened the 新增側溝清單 first. |
| AC-002 | FAIL | Authenticated physical-device smoke: 新增 opened the new-gutter form, but its back action returned to the main map instead of the active list. See `ISS-FEAT-0914-2-009`. |
| AC-003 | NOT VERIFIED | Targeted tests cover row formatting, but not a full multi-row selection journey. |
| AC-004 | NOT VERIFIED | Targeted list controls pass, but map-session close/save branching was not exercised end-to-end. |
| AC-005 | PASS | Fresh coordinator remains empty despite persisted multi-gutter drafts; explicit saved-ID restoration is ordered and isolated. Build and targeted instrumentation pass. |
| AC-006 | PASS | Current session membership is saved/restored by active IDs; success cleanup operates by selected draft ID after inspect return, avoiding prior-session draft inclusion. |
| AC-007 | NOT VERIFIED | Build, unit tests, and targeted instrumentation pass, but full regression evidence is not independently retained and the formal map smoke is unavailable. |
| AC-008 | NOT VERIFIED | Submit failure → Alert confirmation → list return → retry was not exercised end-to-end. |

## Result and route

**FAIL** — category: `implementation`. The prior session-isolation failure is fixed and AC-005/AC-006 pass, but the formal authenticated smoke found an AC-002 return-flow regression. Missing full regression and CI evidence remain recorded in `ISS-FEAT-0914-2-008`.

Next action: repeat authenticated MapWorkspace smoke for `ISS-FEAT-0914-2-009` on revision `d21a27d`.

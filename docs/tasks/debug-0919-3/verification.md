# Verification

## Revision Under Test

- Task: `debug-0919-3`
- Environment: managed worktree `debug-0919-3-verification`
- Production revision under test: `9ed2c7cf46288702829d528e95835897b0a88534`
  (`fix: prevent no-ditch panel ime over-offset`)
- AC-001 and AC-002 were verified against the fixed revision above.
- AC-003 was rerun in a user-requested derived test build: only
  `GutterApiService.kt` was changed in the worktree from `DEMO_URL` to
  `BASE_URL` (`http://192.168.10.84/TY_RSGDBIP/`) so the authenticated test
  session used the requested backend. This change is uncommitted and is not
  claimed as evidence for the fixed revision.
- A gitignored `local.properties` with a non-secret placeholder Maps value was
  added only to allow manifest processing in the isolated worktree. The
  original workspace and its real local settings were not changed.

## Requirement and Implementation Review

- `NoDitchModeUiController.setupPanelInsets()` uses the saved base margin and
  the bottom system-bar inset only; the IME bottom inset is not added to the
  panel margin.
- `NoDitchPanelInsetPolicy` clamps negative values and recalculates from the
  base margin on each update.
- The inline `CardView` panel remains in place. API, data format, permissions,
  map selection, and no-ditch state-machine callbacks are outside the
  production diff.
- The production change is limited to the shared no-ditch controller and its
  pure policy helper.

## Runtime Environment

- Independent AVD: `CodexDebug0919_3_Fold`, serial `emulator-5556`.
- Foldable profile: `pixel_fold`, Android 14 / API 34, density 420.
- Tested posture: CLOSED (`cmd device_state state 0`), physical window
  `1080x2092`; application content ended at y=`2029`, navigation bar occupied
  y=`2029..2092`.
- `emulator-5554` was already reserved by another agent and was not touched.
- Fixed-revision APK was used for AC-001 and AC-002.
- Derived BASE_URL APK was built after the explicit endpoint switch for
  AC-003; the endpoint responded to a reachability check with HTTP `200`.

## Acceptance Criteria

| AC | Result | Evidence and reason |
|---|---|---|
| AC-001 | PASS | On `emulator-5556` CLOSED posture, no-ditch entry showed `noDitchPanel` at `[0,1670][1080,2029]`; after map selection it was `[0,1114][1080,2029]`. With the IME visible, the app content resized to y=`1220` and the panel was `[0,305][1080,1220]`; the note field was `[53,659][1027,1051]` and both operation buttons were visible at `[53,1088][1027,1178]`. The panel stayed at the available bottom edge without an extra IME-height margin. |
| AC-002 | PASS | After dismissing the IME, the same selected-point panel returned to `[0,1114][1080,2029]`; the note field returned to `[53,1468][1027,1860]` and buttons to `[53,1897][1027,1987]`, matching the pre-IME bottom-relative position. |
| AC-003 | PASS on derived BASE_URL build | After the user completed login, the flow selected a map point and entered note `authenticated_ac003_base_url`. OkHttp recorded `POST http://192.168.10.84/TY_RSGDBIP/api/v1/map/storeNoDitch` with `200 OK` and response `{"success":true,"message":"新增成功",...,"id":18}`. The no-ditch panel then disappeared and the main shell controls were restored. A second BASE_URL run verified reset: after selecting a point, `重設點位` returned the panel to `請點擊地圖設定無側溝座標`, and `返回` exited to the main shell with `btnReportNoDitch` restored. |

`NOT VERIFIED` is not treated as `PASS`.

## Executed Checks

| Check | Result | Evidence |
|---|---|---|
| `git diff --check` | PASS | No whitespace errors. |
| `state.yaml` parse and fixed revision assertion | PASS | YAML parsed and recorded commit equals `9ed2c7cf46288702829d528e95835897b0a88534`. |
| `./gradlew :app:testDebugUnitTest --tests com.example.taoyuangutter.main.NoDitchPanelInsetPolicyTest` | PASS | Focused policy tests passed with Android Studio JDK 21 and the gitignored placeholder build setting. |
| `./gradlew :app:testDebugUnitTest :app:assembleDebug` | PASS | Full app unit suite and debug APK build completed successfully. |
| `ANDROID_SERIAL=emulator-5556 ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.MainShellActivityTest` | PASS | `TEST-CodexDebug0919_3_Fold(AVD) - 14.xml`: 12 tests, 0 failures, 0 errors; device property `emulator-5556`. This is a shell regression check, not the missing no-ditch controller test. |
| `curl -I http://192.168.10.84/TY_RSGDBIP/` | PASS | Requested BASE_URL returned HTTP `200`. |
| Derived BASE_URL APK build and install | PASS | `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:assembleDebug` completed successfully; APK installed on `emulator-5556` after the endpoint-only test override. |
| Manual no-ditch flow on `emulator-5556` with authenticated BASE_URL session | PASS | Entered no-ditch, selected a map point, filled `etNoDitchNote`, submitted successfully with backend `200`/`success:true`, then verified reset and exit state in a second run. |
| CI result | NOT VERIFIED | No CI result artifact was available in the repository or task inputs for this revision. |

## Test Coverage and Regression Review

The focused unit test covers IME independence, repeated inset updates, and
negative inset safety. The approved plan also names
`app/src/androidTest/java/com/example/taoyuangutter/NoDitchModeUiControllerTest.kt`
for panel positioning and no-ditch state flow; that test file is absent at the
fixed revision. Manual runtime evidence covers the positioning and most of the
state flow, but does not replace the missing focused instrumentation coverage.

The original production diff does not touch API contracts, persistence,
permissions, map picking, or submit behavior. The derived test override changes
only the Retrofit endpoint used for the authenticated verification run; it is
not part of the fixed debug revision. Unit/build checks and the existing
main-shell instrumentation suite pass on the isolated emulator. The focused
no-ditch controller instrumentation test remains absent; manual runtime
evidence covers the full AC-003 state flow.

## Classification and Next Action

- Final verification result: `NOT VERIFIED`
- Category: `environment`
- Acceptance criteria with runtime evidence missing: none. AC-003 is marked
  `PASS` for the derived BASE_URL build, not for the untouched fixed revision.
- Remaining issues: `ISS-debug-0919-3-005`; authenticated-session issue
  `ISS-debug-0919-3-006` is resolved by the BASE_URL run.
- Next action: add the approved focused instrumentation coverage and obtain a
  CI result. If fixed-revision-only evidence is required, repeat AC-003 after
  committing or otherwise approving the endpoint configuration.

Release cannot proceed while the CI result and required focused coverage remain
unverified.

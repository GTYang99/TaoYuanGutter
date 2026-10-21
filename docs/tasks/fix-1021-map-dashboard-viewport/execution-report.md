# Execution Report

## Implementation
- Reused tab fragments by their stable `tab:<menu-id>` tags in `MainShellActivity`.
- Replaced the remove-and-recreate tab transition with `hide()` / `show()` and lifecycle caps (`STARTED` for inactive tabs, `RESUMED` for the selected tab).
- Extended the existing shell instrumentation test to assert fragment identity, mutually exclusive visibility, and lifecycle across repeated Map ↔ Dashboard switches.
- Added a production-map instrumentation test that moves the actual `GoogleMap` to a known center/zoom and checks those values after switching Dashboard → Map, while also checking parent and nested map fragment identity.
- Left `MapWorkspaceFragment.onMapReady()` and explicit location behavior unchanged.

## Validation

### Build
- Command: `/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon --console=plain :app:assembleDebug :app:assembleDebugAndroidTest`
- Result: **PASS** — `BUILD SUCCESSFUL`; debug APK and Android test APK built.
- App APK: `app/build/outputs/apk/debug/app-debug.apk`
- Test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`
- Package: `com.example.taoyuangutter`

### Emulator Instrumentation
- Target: `emulator-5554`, `sdk_gphone16k_arm64`, Android 16 / API 36.
- Physical device testing: **NOT RUN**, as required.
- Installed the debug and instrumentation APKs using `adb -s emulator-5554 install -r`.
- Command: `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.MainShellActivityTest#shellSwitchesBetweenMapAndDashboardTabs com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner`
- Result: **PASS** — `OK (1 test)`.
- Covered: shell transition semantics, selected/inactive lifecycle, mutual visibility, and test-fragment identity. The production-map case below directly verifies the live camera center and zoom.

### Production Map Camera
- Command: `adb -s emulator-5554 shell am instrument -w -e class com.example.taoyuangutter.MainShellActivityTest#productionMapAndNestedMapFragmentsSurviveDashboardSwitches com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner`
- Result: **PASS** — `OK (1 test)`.
- The test used the production `MapWorkspaceFragment`, moved its real `GoogleMap` to center `(25.12345, 121.54321)` at zoom `13.5`, switched to Dashboard and back, and confirmed the exact center/zoom remained within the asserted tolerances. It also confirmed the same parent and nested `SupportMapFragment` instances remained attached.
- Emulator location permissions were temporarily granted to prevent the system permission prompt from pausing the test; both permissions were revoked afterward, restoring the original denied state.
- One initial attempt included a RESUMED-state assertion in this production-map test and failed because the emulator's location permission prompt paused the host activity. That assertion was removed from this case; the dedicated shell test continues to verify selected/inactive lifecycle states without a system permission prompt. The production camera/identity case then passed.

### Static Check
- Command: `git diff --check`
- Result: **PASS**.

### Not Run
- Host unit test execution and full regression were not run. Test execution was restricted to the approved Android emulator; only the two focused instrumentation cases were run.
- CI and independent verification remain pending.

## Outcome
- The regression path is corrected and focused developer validation passed.
- Main implementation commit: `f279e2b`.
- Validated revision: `80726c0` on `codex/fix/map-dashboard-viewport` (includes the real camera instrumentation coverage).
- Remote CI and independent verification remain pending. The configured CI workflow runs JVM tests on a hosted runner; those host tests were not run because this task's approved test scope is the Android test emulator.

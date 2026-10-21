# Execution Report

## Implementation
- Reused tab fragments by their stable `tab:<menu-id>` tags in `MainShellActivity`.
- Replaced the remove-and-recreate tab transition with `hide()` / `show()` and lifecycle caps (`STARTED` for inactive tabs, `RESUMED` for the selected tab).
- Extended the existing shell instrumentation test to assert fragment identity, mutually exclusive visibility, and lifecycle across repeated Map ↔ Dashboard switches.
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
- Covered: AC-001 shell retention mechanism, AC-002 fragment identity/visibility/lifecycle, and AC-003 no tab-return fragment recreation. The test uses shell test fragments; it does not directly sample a live Google Maps camera coordinate. Source inspection confirms the same production `MapWorkspaceFragment` instance and its child map view are retained on return, so its camera is not reinitialized by tab switching.

### Static Check
- Command: `git diff --check`
- Result: **PASS**.

### Not Run
- Host unit test execution and full regression were not run. Test execution was restricted to the approved Android emulator; the focused instrumentation case is the smallest relevant runtime check.
- CI and independent verification remain pending.

## Outcome
- The regression path is corrected and focused developer validation passed.
- Remaining task gates: commit evidence, CI, and independent verification.

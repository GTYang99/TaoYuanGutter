# Issue Log

## ISS-debug-0919-1-001

```yaml
issue_id: ISS-debug-0919-1-001
task_id: debug-0919-1
phase: verification
category: environment
priority: P2
title: Full connected suite has a transient MainShellActivity focus timeout
status: open
impact: Full local instrumentation suite cannot be reported as a stable PASS from one run; the debug-0919-1 scoped tests are unaffected.
repro_steps:
  - Run `:app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest --no-daemon`.
  - Observe `MainShellActivityTest.addGutterListWiresAddAndCloseConfirmationCallbacks`.
expected: Full connected suite completes successfully.
actual: One run failed with Espresso `RootViewWithoutFocusException` after waiting 10 seconds for window focus.
evidence:
  - `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 14.xml`
  - Immediate rerun of the failed method passed with `BUILD SUCCESSFUL in 19s`.
next_action: infrastructure
owner: developer
```

This issue is not classified as an implementation regression because the failed test is outside the debug-0919-1 scope, the failure is a window-focus timeout, and the immediate isolated rerun passed without a code change.

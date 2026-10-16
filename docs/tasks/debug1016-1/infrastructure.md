# Infrastructure Evidence

## Context

- Task: `debug1016-1`
- Originating phase: Verification
- Blocked check: AC-005 no-fix prompt dismissal and fallback map manual-pan behavior
- Fixed implementation revision: `990e60888604da1a5917b72a542effa6649f60f1`
- Task documentation HEAD: `4780ae6064e6ef5b4d51aa1cabbf3fccd1a7db87`; app source is unchanged from the implementation revision.
- Test environment: `emulator-5554`, `sdk_gphone16k_arm64`, Android 16; no physical device was targeted.

## Failure and Remediation

The first AC-005 instrumentation attempt failed while Espresso injected the Continue click. Logcat identified an emulator `InputDispatcher` stale pointer (`Invalid DOWN event - pointers already down`). The emulator was rebooted. With location disabled and animation scales set to zero, the single blocked test was rerun:

```text
adb -s emulator-5554 shell am instrument -w -e class 'com.example.taoyuangutter.gutter.EditMapInitialLocationInstrumentedTest#missingCoordinateEditWithoutUsableFixKeepsFallbackAndShowsUnavailablePrompt' com.example.taoyuangutter.test/androidx.test.runner.AndroidJUnitRunner
```

The rerun successfully clicked Continue, dismissed the prompt, retained the Taoyuan fallback, and passed the no-coordinate-persistence assertions. It then failed at the manual-pan assertion (`Map did not respond to a manual swipe`, test line 128).

The rerun's logcat identifies the remaining cause: Google Maps Android API token request returned `INVALID_ARGUMENT`, followed by Google Maps Android SDK authorization failure for `debug-placeholder-not-a-real-key`. The build used this deliberately non-real placeholder; an authorized test-only Maps SDK key was not available. No credential was stored in task artifacts. Consequently, map-pan behavior cannot be evaluated in this emulator environment.

## Environment Cleanup and Current Status

Immediately after the rerun, the emulator was confirmed restored to location mode `3`, transition/window animation scales `1.0`, animator scale unset, and mock-location app-op denied. A later attempt to repeat the read-only check could not start ADB because the sandbox rejected smartsocket binding (`Operation not permitted`); the earlier post-run restoration check had already succeeded.

No application source was modified. The stale-pointer condition was remediated by reboot and the prompt-dismissal portion now passes. AC-005 remains NOT VERIFIED pending an authorized test-only Maps SDK key and rerun of the same focused case. Remote CI also has no reported status. Keep the task blocked in Infrastructure; return to Verification after the required environment evidence is available.

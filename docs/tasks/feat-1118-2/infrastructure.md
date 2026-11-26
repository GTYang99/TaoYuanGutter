# Infrastructure Evidence — feat-1118-2

## Scope

- Originating phase: `verification`.
- Required check: `OfflinePolygonMapHostInstrumentedTest#mainActivityOfflineMapShowsAndTogglesBothGeoPackageLayers` on fixed commit `d21a49a1d7db0f104e85b2aa06b8c23a69810222`.
- Test source and app source were run from the immutable snapshot `/private/tmp/feat1118-2-verification-r4.d21a49a1`; no production or test source was modified.
- Device: `emulator-5554`, `Medium_Phone(AVD)`, Android 16.

## Environment Diagnosis and Remediation

| Check | Result | Evidence |
|---|---|---|
| Initial AVD network | Unavailable | `adb -s emulator-5554 shell dumpsys connectivity` showed `Active default network: none`; `settings get global wifi_on` returned `0`; DNS was unset; `toybox ping 8.8.8.8` returned `Network is unreachable`. Airplane mode was off. |
| AVD network remediation | Resolved | Ran `adb -s emulator-5554 shell svc wifi enable`. The emulator then reported active Wi-Fi network 100, DNS servers, and successful `toybox ping 8.8.8.8` and `toybox ping maps.google.com`. |
| Workspace Maps configuration | Unavailable | Workspace `local.properties` is absent. The isolated build therefore used `MAPS_API_KEY=ci-placeholder`; no credential was written to task artifacts. |
| Blocked test rerun | Still blocked | The focused connected test exited 1. JUnit reports one failure after 86.401 seconds at `cameraIdle.await(30s)`. The scoped log reports `Error requesting API token. StatusCode=INVALID_ARGUMENT` and Google Maps SDK authorization failure for `ci-placeholder`. |

Rerun command from the fixed snapshot:

```sh
./gradlew --offline --no-daemon :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.map.OfflinePolygonMapHostInstrumentedTest#mainActivityOfflineMapShowsAndTogglesBothGeoPackageLayers
```

The network condition was remediated, and the blocked check was rerun as required. The rerun distinguishes the remaining cause: Maps SDK authorization is unavailable with the temporary placeholder key. Since the camera-idle callback is not delivered, the screenshot and color assertion never run.

## Resolution and Return to Verification

- A valid Maps key is now present in ignored workspace `local.properties`. Only its value was transferred to the isolated snapshot's ignored `local.properties`; no credential value was printed or stored in task artifacts.
- On fixed revision `d21a49a1d7db0f104e85b2aa06b8c23a69810222`, `./gradlew --offline --no-daemon --rerun-tasks :app:assembleDebug :app:assembleDebugAndroidTest` completed successfully. The Debug and AndroidTest manifests merged successfully.
- The previously blocked `mainActivityOfflineMapShowsAndTogglesBothGeoPackageLayers` case then passed on the Android 16 emulator (`OK (1 test)`). It reached the map snapshot and color assertions. ISS-010 is closed.
- The subsequent complete map-host class exposed an intermittent LEGACY snapshot mismatch, recorded as ISS-011. This is a Verification failure requiring Debug, not a remaining credential or network blocker.
- The current worktree's `:app:processDebugMainManifest :app:processDebugAndroidTestManifest` tasks also passed with its existing uncommitted AGP version change; the reported Android Studio manifest merger failure was not reproduced by these CLI checks.

Infrastructure has returned the task to `verification`. No production or test source was changed. Remote CI remains pending as recorded in `verification.md`.

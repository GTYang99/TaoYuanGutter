# Fix Plan — Restore Monotonic Release Version Counter

## Scope

- Failed AC: AC-011
- Related issue: ISS-006
- Production change: `app/version.properties` only
- Preserve the current P1/P2/P3 values; they remain manually managed.

## Steps

1. Restore `P0_VERSION_CODE` to 5, the highest successful Release APK version code recorded in the task evidence. The next Release therefore resolves to code 6.
2. Do not run `:app:assembleRelease` in the primary checkout; it would consume another code and require the resulting P0 to be advanced again. Existing isolated Release evidence already demonstrates the `P0 + 1` persistence behavior.
3. Run `git diff --check`, `:app:testDebugUnitTest`, `:app:assembleDebug`, and the focused `AppVersionUpdateFlowTest` on `emulator-5554` only. Confirm Debug validation leaves P0 at 5 and P1/P2/P3 unchanged.
4. Record results, commit the fix, and obtain CI plus independent Verification for the exact committed revision.

## Expected Results

- P0 remains 5 through Debug build and instrumentation tests.
- P1/P2/P3 remain unchanged.
- A subsequent successful Release assembly uses versionCode 6 and persists P0=6.
- Login/update-flow behavior remains unchanged; simulator tests use the Android emulator only.

## Non-Goals

- Do not change the manually maintained version-name fields.
- Do not alter updater source, tests, API routing, signing configuration, or requirements.
- Do not install or test on a physical device.

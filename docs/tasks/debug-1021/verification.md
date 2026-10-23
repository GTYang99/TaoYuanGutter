# Verification Report — debug-1021

## Inputs
- Requirement: `requirement.md`
- Plan: `plan.md`
- Committed revision under test: `604aa34ff9e468e9c27375aa87f20bf5bf40b22d`
- Verification rounds: API 34 emulator runs recorded at `2026-10-21T14:10:13Z`, plus a fresh API 36 emulator rerun at `2026-10-23T03:03:15Z`.
- CI: Current GitHub combined status lookup returned no checks; the available PR-triggered workflow-run lookup returned no runs.
- Production diff: commit `f9d033d6` resets the submit loading state on the new-mode validation-failure path before returning; it does not reach the photo upload or `storeDitch` path.

## Physical Device Context

```yaml
revision: "604aa34ff9e468e9c27375aa87f20bf5bf40b22d"
device:
  serial: "emulator-5554"
  model: "Medium Phone 2 AVD"
  android_version: "Android 14 / API 34"
app:
  package: "com.example.taoyuangutter"
  build_variant: debug
```

The test was built from a clean `git archive` of the committed revision under `/private/tmp/tyg-debug1021-verify.0m0dWW`; the local working tree's `app/version.properties` and `app/release/` were excluded. A placeholder Maps key was used only in that temporary checkout to configure the build. No production or test source was changed during verification.

Fresh rerun on 2026-10-23 used the same clean snapshot and `emulator-5554` (`Medium Phone (AVD)`, Android 16 / API 36). SHA-256 hashes of the snapshot's production and instrumentation test sources matched `HEAD`. The API 34 runs above remain the plan-targeted emulator evidence; this API 36 run independently repeated all three AC-001 cases.

## Acceptance Criteria

| AC | Steps | Result | Actual Result | Evidence | Retry Count |
|----|-------|--------|---------------|----------|-------------|
| AC-001 | Run the fields-only, photos-only, and combined-incomplete submit cases; dismiss each existing validation dialog. | PASS | Both API 34 rounds and the fresh API 36 rerun passed all three cases. Each displayed “資料/照片不足” with the expected reason; after “確定”, the button displayed “新增側溝”, was enabled, and the host submission count remained zero. | [API 34 round 1 XML](emulator-results/AC-001-verification-604aa34.xml), [API 34 round 2 XML](emulator-results/AC-001-verification-604aa34-r2.xml), [API 36 rerun XML](emulator-results/AC-001-verification-604aa34-api36-20261023.xml). | 0 |

Exact command:

```text
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.Debug1021IncompleteSubmitUiTest --console=plain
```

Result: `BUILD SUCCESSFUL`; 3 tests, 0 failures, 0 errors, 0 skipped. The cases cover missing required fields, missing required photos, and both missing together. Each test asserts the existing dialog and reason, restored button label and enabled state, and zero submissions. Round 2 was run again from the clean snapshot; its XML report timestamp is `2026-10-21T14:10:13Z`.

Fresh rerun command (Android Studio JBR supplied as `JAVA_HOME`):

```text
env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin:$PATH" ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.taoyuangutter.gutter.Debug1021IncompleteSubmitUiTest --console=plain --quiet
```

Result: exit code 0; API 36 report timestamp `2026-10-23T03:03:15Z`; 3 tests, 0 failures, 0 errors, 0 skipped.

## Regression
- Verified the targeted new-mode validation-failure path and the three planned incomplete-input combinations.
- Source review confirms `setSubmitLoading(false)` runs before the validation-failure return and before photo upload or gutter creation.
- Full app regression and edit-mode behavior were not rerun; the approved plan does not require full regression, and this change is limited to the new-mode validation-failure branch.

## Issues
- CI build and unit-test results are absent for the committed revision; tracked as ISS-DBG-1021-002.
- The earlier API 34 emulator access issue is resolved; tracked as resolved ISS-DBG-1021-003.
- The retrospective plan review records the historical chronology deviation; it cannot be changed retroactively.

## Validation Limitations
- GitHub combined commit status for `604aa34ff9e468e9c27375aa87f20bf5bf40b22d` returned no checks. The available workflow-run lookup is PR-triggered only and returned no runs. CI build and unit-test checks therefore remain NOT VERIFIED.
- The first sandbox shell attempt could not start ADB (`Operation not permitted`). ADB device discovery and the fresh connected test subsequently succeeded with the existing Android Studio emulator and host Gradle cache.
- The current checkout also contains user changes in `app/version.properties` and untracked `app/release/`; these were excluded from the clean test snapshot and left untouched.

## Failure Classification
- `environment` — required CI evidence is unavailable. AC-001 itself passed.

## Next Action
- `infrastructure`: obtain CI build and unit-test results for the committed task revision, then return to verification/release readiness.

## Final Result

NOT VERIFIED

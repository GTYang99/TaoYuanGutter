# Root Cause — AC-011 Version Counter Reset

## Revision and Classification

- Task: `feat-1013-app-version-check`
- Failed revision: `2b7d8ba3f27dbfebc3ee7a9004e0f752f6853fca`
- Category: `implementation`
- Failed acceptance criterion: AC-011
- Related issue: ISS-006

## Evidence

- The independent verifier reviewed the exact current HEAD and found `app/version.properties` changed from P0=3 to P0=0.
- `app/build.gradle.kts` computes the next Release version code as `P0_VERSION_CODE + 1` and persists the computed value after a successful `:app:assembleRelease`.
- The committed verification evidence records successful Release artifacts through versionCode 5 (including the same-key update APKs); the task's counter rule says a code consumed by a successful Release assembly must not be reused, even if the APK was not distributed.
- Therefore the current P0=0 would make the next Release use versionCode 1, below the highest recorded consumed code.

## Root Cause

The latest commit reset the tracked release counter from 3 to 0 while recording a new manual version name. Because the Release build derives its next version code solely from P0, this reset discards the monotonic counter history. The Debug build and app-version flow remain functional; the regression is isolated to release version-code state.

## Impact and Regression Risk

- AC-011 fails on the current revision.
- A Release APK built from the current P0 would have a lower versionCode and could not update installations at the already consumed version codes.
- Current-HEAD Release build behavior and same-key update were not run after the reset; AC-001 and AC-008 are not verified for this revision.
- The latest Debug build, JVM suite, connected instrumentation suite, and CI run passed; none exercises Release counter persistence.

## Minimum Fix Direction

Retain P0 at least at the highest successfully assembled Release code recorded by the task evidence (5), leaving P1/P2/P3 untouched as manually managed fields. The next Release would then use code 6. Do not run a Release assembly during this fix unless its resulting consumed code is also persisted and accounted for.

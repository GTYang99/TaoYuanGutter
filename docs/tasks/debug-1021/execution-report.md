# Execution Report — debug-1021

## Change Summary

- Expanded `Debug1021IncompleteSubmitUiTest` to independently cover missing required fields with usable photos, missing required photos with complete required fields, and both missing together.
- Each case verifies the existing dialog, dismisses it, checks `btnSubmitGutter` is enabled with the “新增側溝” label, and asserts the upload host received zero submissions.
- Used the existing `AppVersionUpdateTestHooks` seam to prevent the unrelated live app-version prompt from blocking the form tests.
- Corrected the data-only test fixture to include `SPI_TYP=1`; without it, the existing “請先選擇側溝類型後再送出” precondition correctly stopped submission before required-field validation.
- No production source was changed in this reimplementation.

## Developer Validation

- `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --tests com.example.taoyuangutter.gutter.GutterCompletionPolicyTest` — **BUILD SUCCESSFUL**; the focused JVM suite contains 6 tests with 0 failures.
- `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest` — **BUILD SUCCESSFUL** after adding the update-prompt test hook.
- `./gradlew :app:assembleDebugAndroidTest` — **BUILD SUCCESSFUL** after correcting the data-only fixture.
- API 34 Emulator UI evidence: [AC-001 revalidation](emulator-results/AC-001-api34-reimplementation.md).
- `git diff --check` — PASS.

## Scope and Limitations

- Test target was the temporary API 34 AVD `Codex_Debug1021_API34`, serial `emulator-5554`; commands used the explicit emulator serial. No physical-device tests were performed.
- Build variant: `debug`; app package: `com.example.taoyuangutter`.
- APKs: `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- CI for the task branch and independent Verification of the new committed revision remain pending.
- The original production-fix commit predates the plan review. The retrospective review is recorded and does not change that chronology.

## Git Handoff

- Branch: `codex/debug/debug-1021`.
- Implementation commit: `34f163bb1f882d1eb4153d0b05bdf03ffc2f3a3f`.
- Unrelated pre-existing changes to `app/version.properties` and `app/release/` are preserved and excluded from the task commit.
- Pre-verification worktree status after task artifacts are committed is expected to contain only `M app/version.properties` and `?? app/release/`; these pre-existing changes are not part of the task revision.

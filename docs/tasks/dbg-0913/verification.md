# Verification Report

## Verified Revision
- Branch: `codex/dbg-0913`
- Commits: `ecbe6fe` (original fix), `266fcc0` (default-value regression fix)

## Acceptance Criteria
| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | Android 14 `GutterCantOpenUiTest.emptyClearableFieldsDoNotShowCantOpenDialog` passed against the real new-form state without manually clearing its default UI values. |
| AC-002 | PASS | Unit coverage tests every clearable field and both photo slots; Android 14 existing confirm/cancel/snapshot tests passed. |
| AC-003 | PASS | Android 14 `GutterFormExitUiTest.backButtonWarnsBeforeLeavingIncompleteForm` and `systemBackWarnsBeforeLeavingIncompleteForm` passed. The implementation's sole Alert action calls the existing `buildAndFinishWithResult()` draft-sync/finish path. |
| AC-004 | PASS | Unit exit-policy coverage confirms completed data has no warning; Android 14 `completedVirtualFormLeavesWithoutWarning` passed and observed Activity destruction after toolbar back. |
| AC-005 | PASS | Android 14 `returningFromEditToPreviewDoesNotShowExitWarning` passed and verified the edit button reappears without leaving the Activity. |

## Implementation and Regression Review
- The implementation follows the approved fix plan. It uses the same clear targets as `clearCantOpenFieldsAndPhotos()` and reuses `validateRequiredFields()` / `validateAllPhotos()` instead of duplicating validation requirements.
- The no-data scenario excludes slot 1, while slot 2/3 and their persisted image IDs are included as clear targets.
- Existing cant-open cancellation, confirmation, configuration recreation, and view-mode tests all pass.
- The follow-up verification confirms that new-form defaults 「否／無」 and 明溝的系統 `0` do not count as user-filled clear targets, while actual entered values still retain the existing confirmation behavior.

## Executed Evidence
- PASS: focused unit test for `GutterFormExitRulesTest`.
- PASS: full `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest`.
- PASS: Android 14 emulator targeted `GutterCantOpenUiTest` — 5 tests, 0 failures.
- PASS: Android 14 emulator targeted `GutterFormExitUiTest` — 4 tests, 0 failures.
- PASS: `git diff --check`.
- PASS: follow-up full `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` and Android 14 `GutterCantOpenUiTest` — 5 tests, 0 failures.

## Re-verification — 2026-09-13
- Verified current committed revision `f17049159ad68192b09adbbeb17d28d52e760b93` on branch `codex/dbg-0913`; its production implementation remains `266fcc0`.
- PASS — `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests com.example.taoyuangutter.gutter.GutterFormExitRulesTest`: 3 tests, 0 failures.
- PASS — `JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest`.
- PASS — Android 14 emulator (`emulator-5554`) `GutterFormExitUiTest`: 4 tests, 0 failures. The generated connected-test XML records toolbar back and system back warnings for incomplete forms, no warning on completed virtual forms, and edit-to-preview return behavior.
- PASS — Android 14 emulator (`emulator-5554`) `GutterCantOpenUiTest`: 5 tests, 0 failures. The test runner completed the first four tests in the class; the final no-data case was rerun independently and returned `OK (1 test)`. This covers confirm, cancel, default/no-data suppression, view mode, and configuration-recreation snapshot behavior.
- PASS — `git diff --check` before task-artifact updates.

## CI
- NOT VERIFIED: no remote CI workflow or result is available in the repository. This does not affect the local verification result, but release must not treat CI as passed.

## Final Result
PASS

## Follow-up Verification: ISS-DBG-0913-003
- Verified revision: `7629518`.
- PASS — Android 14 emulator `GutterCantOpenUiTest`: 6 tests, 0 failures.
- The new regression test proves: first empty selection proceeds without confirmation; after unchecking and entering depth, the next selection displays the existing clear confirmation.
- PASS — `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`.
- PASS — `git diff --check` before commit.

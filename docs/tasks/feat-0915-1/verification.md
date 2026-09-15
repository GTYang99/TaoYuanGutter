# Verification Report

Task: feat-0915-1
Verified revision: `1acb329c237cec245cc6b68c86b55db989d3ec97` (`feat/備註欄輔助填寫功能`)
Verification date: 2026-09-15

## Result

**FAIL** — implementation failure. AC-002 does not follow the approved plan: a second tap on a preset removes the existing item, while the approved failure behavior requires the original remark to remain unchanged on a duplicate tap.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | NOT VERIFIED | Static review confirms five required chips and strings. On `emulator-5554`, `remarkPresetChipsAreShownWithExpectedLabels` failed because the chip is outside the scroll viewport (`getGlobalVisibleRect()` is empty); the test does not scroll to the remarks section. |
| AC-002 | FAIL | `setupRemarkPresetChips()` splits `etRemarks` by `，`; when an exact matching preset already exists it calls `filterNot { it == preset }`, removing it. The committed UI test explicitly expects this toggle. This contradicts plan.md’s approved failure behavior: a duplicate tap must leave the original text unchanged. |
| AC-003 | NOT VERIFIED | Static review confirms the chip handler only updates `etRemarks`, `setupDraftWatchers()` observes it, and `collectData()` still maps it to `NODE_NOTE`. No test covers draft save, activity/fragment reconstruction, or submission. |
| AC-004 | NOT VERIFIED | Static review confirms the chip group is added to `setEditable()`, `setVirtualMode()`, and `reorderEditableSections()`. On `emulator-5554`, the virtual-point test passed, but view mode and import lock remain untested. |

## Implementation and Regression Review

- Scope follows the approved files and does not change API fields, draft schema, `GutterFormContract`, or request mapping.
- `setEditable()` and `setImportLocked()` disable the chip group and child chips; the click listener also rejects non-editable, import-locked, and virtual modes.
- Virtual-point mode hides the chip group together with the existing remarks title and input.
- Existing `NODE_NOTE` collection and text-watcher wiring remain present, but their end-to-end behavior is unverified.
- Test coverage is incomplete for manual text after a preset action, draft persistence/rebuild, view mode, import lock, and the required no-op duplicate tap behavior. The two new chip tests also require a scroll action before their visibility checks and clicks can execute on the emulator.

## Executed Evidence

| Check | Result | Actual result |
|---|---|---|
| Revision review | PASS | Reviewed task changes from `0277056` through `1acb329`. |
| `git diff --check 0277056..HEAD` | FAIL | Found pre-existing trailing whitespace in task plan/review Markdown files; no production source whitespace finding was reported. |
| `:app:compileDebugKotlin` | PASS | Built successfully with Android Studio bundled Java 25. |
| `:app:connectedDebugAndroidTest` for `GutterBasicInfoUiTest` | FAIL | Ran on `emulator-5554` (Medium_Phone, Android 14): 5 tests, 3 passed, 2 failed. Both new remark-chip tests failed because the chips were outside the viewport; animations were disabled and the same failures remained. |
| CI result | NOT VERIFIED | No CI configuration (`.gitlab-ci.yml`, `.github/workflows`, Jenkinsfile, Azure or Bitbucket pipeline) or CI result is present in the repository. |

## Issues and Routing

- `ISS-0915-002` is an implementation regression: restore duplicate-tap behavior to a no-op and update the UI test to match the approved rule. Route: `debug`.
- `ISS-0915-003` is closed: Android Studio bundled Java 25 permitted compilation and emulator testing with a non-secret local placeholder.
- `ISS-0915-004` is an implementation/test regression: make the chip tests scroll to the remarks area before asserting or clicking, then rerun on the emulator.

## Next Action

Run Debug for AC-002 and the failed chip test setup before any production-code change. After an approved debug fix is committed, repeat the emulator UI tests and add evidence for draft/rebuild, view mode, and import lock.

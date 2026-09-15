# Verification Report

Task: feat-0915-1
Verified revision: `1acb329c237cec245cc6b68c86b55db989d3ec97` (`feat/備註欄輔助填寫功能`)
Verification date: 2026-09-15

## Result

**FAIL** — implementation failure. AC-002 does not follow the approved plan: a second tap on a preset removes the existing item, while the approved failure behavior requires the original remark to remain unchanged on a duplicate tap.

## Acceptance Criteria

| AC | Result | Evidence |
|---|---|---|
| AC-001 | PASS | The committed layout defines five `Chip` views with the required string resources: 花圃、焊接、車擋、水泥封邊、螺絲固定. `GutterBasicInfoUiTest.remarkPresetChipsAreShownWithExpectedLabels` provides matching UI coverage, but was not executable in this environment. |
| AC-002 | FAIL | `setupRemarkPresetChips()` splits `etRemarks` by `，`; when an exact matching preset already exists it calls `filterNot { it == preset }`, removing it. The committed UI test explicitly expects this toggle. This contradicts plan.md’s approved failure behavior: a duplicate tap must leave the original text unchanged. |
| AC-003 | NOT VERIFIED | Static review confirms the chip handler only updates `etRemarks`, `setupDraftWatchers()` observes it, and `collectData()` still maps it to `NODE_NOTE`. No test covers draft save, activity/fragment reconstruction, or submission, and Android tests could not be run without a Java Runtime. |
| AC-004 | NOT VERIFIED | Static review confirms the chip group is added to `setEditable()`, `setVirtualMode()`, and `reorderEditableSections()`; the unexecuted UI test covers virtual-point visibility. There is no test for view mode or import lock, and UI tests could not run. |

## Implementation and Regression Review

- Scope follows the approved files and does not change API fields, draft schema, `GutterFormContract`, or request mapping.
- `setEditable()` and `setImportLocked()` disable the chip group and child chips; the click listener also rejects non-editable, import-locked, and virtual modes.
- Virtual-point mode hides the chip group together with the existing remarks title and input.
- Existing `NODE_NOTE` collection and text-watcher wiring remain present, but their end-to-end behavior is unverified.
- Test coverage is incomplete for manual text after a preset action, draft persistence/rebuild, view mode, import lock, and the required no-op duplicate tap behavior.

## Executed Evidence

| Check | Result | Actual result |
|---|---|---|
| Revision review | PASS | Reviewed task changes from `0277056` through `1acb329`. |
| `git diff --check 0277056..HEAD` | FAIL | Found pre-existing trailing whitespace in task plan/review Markdown files; no production source whitespace finding was reported. |
| `java -version` | NOT VERIFIED | `Unable to locate a Java Runtime`. |
| `./gradlew :app:assembleDebug --no-daemon` | NOT VERIFIED | Could not start because no Java Runtime is installed. |
| `./gradlew :app:connectedDebugAndroidTest --no-daemon` | NOT VERIFIED | Could not start because no Java Runtime is installed. |
| CI result | NOT VERIFIED | No CI configuration (`.gitlab-ci.yml`, `.github/workflows`, Jenkinsfile, Azure or Bitbucket pipeline) or CI result is present in the repository. |

## Issues and Routing

- `ISS-0915-002` is an implementation regression: restore duplicate-tap behavior to a no-op and update the UI test to match the approved rule. Route: `debug`.
- `ISS-0915-003` is an environment blocker: install/configure a Java Runtime and an Android test target before re-running build and instrumentation evidence. It remains relevant after the implementation fix.

## Next Action

Run Debug for AC-002 before any production-code change. After an approved debug fix is committed, repeat verification with a Java Runtime, build, and connected Android UI tests.

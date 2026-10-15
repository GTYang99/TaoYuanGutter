# Verification

## Verification Round

- Task: `feat-0923`
- Implementation revision under test: `f8f40fe3f3229dda155c24b35b6ae2872ffe9ebe` (`f8f40fe`)
- Scope includes the submitted-retry fix `ee55f08` and pending-draft layout fix `e595311` in the revision ancestry.
- Verification workspace: `/Users/a10362/.codex/worktrees/feat-0923-verification-0003/TaoYuanGutter`
- Package: `com.example.taoyuangutter`
- Build variant: `debug`
- Verification-only setup: ignored `local.properties` with `MAPS_API_KEY=verification-placeholder`; no main-worktree credential was copied.
- Worktree remained clean after checks; `HEAD` remained `f8f40fe`.

### Physical Device Context

```yaml
revision: f8f40fe3f3229dda155c24b35b6ae2872ffe9ebe
device:
  serial: adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp
  model: XQ_AU52
  android_version: Android 12
app:
  package: com.example.taoyuangutter
  build_variant: debug
```

## Executed Checks

| Check | Result | Evidence |
|---|---|---|
| Fixed revision and isolated worktree | PASS | `git rev-parse HEAD` matched `f8f40fe3...`; no tracked worktree changes before or after testing. |
| `./gradlew :app:testDebugUnitTest` | PASS | 40 suites, 130 tests, 0 failures, 0 errors, 0 skipped. |
| `./gradlew :app:assembleDebug` | PASS | Debug APK assembled successfully from `f8f40fe`. |
| `./gradlew :app:compileDebugAndroidTestKotlin` | PASS | Android instrumentation sources compiled successfully. |
| `git diff --check f8f40fe^ f8f40fe` | PASS | No whitespace errors in the implementation commit. |
| `adb devices -l` | PASS | Physical device `XQ_AU52`, Android 12, discovered and used. |
| Focused `PendingDraftAdapterUiTest` | PASS | 3 tests, 0 failures, 0 errors, 0 skipped; includes all three tags and title/subtitle geometry. |
| `./gradlew :app:connectedDebugAndroidTest` | PASS | 53 tests, 0 failures, 0 errors, 0 skipped; report timestamp `2026-09-24T03:59:30`. |
| CI build/test result | NOT VERIFIED | No repository CI workflow or CI result artifact is available for `f8f40fe`. |

## Acceptance Criteria

| AC | Verification scope | Result | Evidence and limitation | Retry count |
|---|---|---|---|---:|
| AC-001 | Submission marker ordering and persistence after API failure, timeout, and restart | NOT VERIFIED | `GutterRepositoryStoreDitchBoundaryTest` and the 130-test JVM suite pass, but no committed runtime case covers failure/timeout/restart after entering `storeDitch`. | 0 |
| AC-002 | Pre-request interruption remains unsubmitted across restart and restore | NOT VERIFIED | Legacy serialization and policy tests pass, but no runtime interruption-before-boundary case is committed. | 0 |
| AC-003 | Three tag texts, position, dimensions, styles, and single-tag behavior, including subtitle layout | PASS | Focused `PendingDraftAdapterUiTest` passes 3/3: submitted, unsubmitted, and existing-gutter tags; measured assertions confirm time subtitle is below `layoutPendingDraftTitle` and node subtitle is below time. Source confirms the shared padding/text sizing and transparent primary-color outline for unsubmitted/existing tags. | 0 |
| AC-004 | Existing `SPI_NUM` precedence plus restore/edit, long-press delete, and successful cleanup regression | NOT VERIFIED | Policy and adapter tests confirm `SPI_NUM` takes precedence and shows `既有側溝編輯中`; the committed suite has no complete existing-gutter draft restore/edit/resubmit/delete/cleanup flow. | 0 |
| AC-005 | Legacy migration, list loading, and restore | NOT VERIFIED | `GutterDraftDatabaseMigrationTest` passes and legacy defaults are covered; list loading and restored content from the migrated row are not independently exercised. | 0 |
| AC-006 | Submitted ordinary draft read-only surfaces with return and re-upload available | NOT VERIFIED | `SubmittedDraftReadOnlyUiTest` passes in the 53-test suite for the covered controls, but it does not cover every required node, attachment, type, import, overlay-boundary, and outer re-upload interaction. | 0 |
| AC-007 | Room-sourced complete re-upload payload and success/failure/timeout/interruption retry behavior | NOT VERIFIED | `SubmittedRetrySnapshotTest` and the 130-test JVM suite pass, and the prior mutable-callback defect is fixed in the tested ancestry; no committed end-to-end runtime case proves payload completeness, no-write failure/timeout/interruption, success cleanup, and retry preservation. | 0 |
| AC-008 | Existing-gutter draft retains content/`SPI_NUM`/tag and remains editable and resubmittable after failure, timeout, or interruption | NOT VERIFIED | `PendingDraftTagPolicyTest` and `SubmittedDraftResumePolicyTest` pass for precedence and editable-policy decisions; no committed runtime case covers existing-gutter restore, edit, resubmit failure/timeout/interruption, and later retry. | 0 |

## Regression Review

- The pending-draft layout regression is covered by `e595311` and passes the measured-layout test: the subtitle now anchors below the direct `layoutPendingDraftTitle` container rather than the nested title child.
- The existing-gutter policy is centralized in `PendingDraftTagPolicy`; tag rendering, delete-title identity, and submitted read-only policy use the same normalized START `SPI_NUM` rule.
- The submitted retry isolation fix is present in the tested revision ancestry and its immutable snapshot unit tests pass.
- Existing form, inspection, import, migration, pending-list, and submitted read-only tests all pass in the 53-test connected suite.

## Issues and Limitations

- CI build/test evidence is unavailable and blocks Release.
- AC-001, AC-002, AC-004, AC-005, AC-006, AC-007, and AC-008 lack complete committed runtime evidence at their required scope.
- Local green tests do not substitute for the missing targeted runtime flows or CI.
- The prior `452915a` AC-007 failure is historical; the current round found no equivalent source regression on `f8f40fe`, but the replacement runtime evidence is still incomplete.

## Failure Classification

- No new implementation failure reproduced in this round.
- `environment` / evidence gap for CI and missing targeted runtime cases.

## Next Action

- `infrastructure`: provide CI evidence and add/run the targeted runtime scenarios for the remaining NOT VERIFIED acceptance criteria, then rerun Verification against a fixed committed revision if implementation changes.

## Final Result

NOT VERIFIED

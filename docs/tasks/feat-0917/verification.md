# Verification Report

## Revision and Worktree

- Production revision under test: `d988f8d9cf5d575af64579f1c9c146962ee14a10` (`feat(feat-0917): update tie-in and connecting pipe contract`).
- Verification started from branch `feat/銜接點連結管自帶節點名`; the later `63c41a2` commit contains task documentation only.
- The only pre-existing worktree item was untracked `.worktrees/`; it was not changed or included in validation.

## Automated Evidence

| Check | Result | Evidence |
|---|---|---|
| Debug Kotlin/JVM build, unit tests, APK build, Android-test compilation | PASS | `JAVA_HOME=... ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --no-daemon -q` completed successfully. The JVM report contains 86 tests with 0 failures and 0 errors. |
| Request/read DTO regression | PASS | `StoreDitchNodeRequestMapperTest` (6) and `StoreDitchResponseParsingTest` (3) passed within the JVM suite: uppercase Boolean request keys, virtual omission, XY_NUM create/edit behavior, String readback and cannot-open precedence are covered. |
| Form UI regression | PASS | Android 14 emulator (`emulator-5554`), `GutterBasicInfoUiTest`: 6 tests passed. It covers required ordering, new-form defaults, Cant Open / Tie-in mutual exclusion, connecting-pipe default, and virtual-mode visibility. |
| Connected suite | NOT VERIFIED | An unfiltered `:app:connectedDebugAndroidTest` run executed `AuthExpiredUiFlowTest.editScreen401SavesDraftShowsDialogAndReturnsToLogin` and initially failed with `MapWorkspaceFragment` detached-context `IllegalStateException`. Its one permitted retry passed. The failure is not attributable to an AC violation from this task, but it prevents treating that run as a clean full-suite result. |
| CI | NOT VERIFIED | No external CI result was available. |

## Acceptance Criteria

| Criterion | Result | Evidence and limitation |
|---|---|---|
| AC-001 | NOT VERIFIED | Form layout and mutual-exclusion/default UI tests pass, and source review finds the new fields wired through form, inspect and adapter. The required draft → inspect → edit flow was not executed end-to-end. |
| AC-002 | NOT VERIFIED | Mapper and DTO parsing tests pass for Boolean request fields, String readback, missing fields, cannot-open precedence and virtual omission. No authenticated `storeDitch` request/response capture verified the live contract. |
| AC-003 | NOT VERIFIED | Source review confirms virtual collection filters out the three fields and the adapter orders `(銜接點)` before `(待架站)`. No persisted old-draft, virtual round-trip, or real inspect-dropdown flow was executed. |
| AC-004 | NOT VERIFIED | Unit tests verify create omits blank `XY_NUM` and edit preserves it; UI test verifies the create required marker is hidden. Generated-name readback and locked edit behavior were not run against the API. |
| AC-005 | NOT VERIFIED | Source review confirms the no-query import endpoint and updated recent-saved-point UI text. The actual import request, response, empty/error states and selection flow were not executed. |

## Regression Review

- No production-code diff whitespace errors were found in `73644b0..d988f8d`.
- The required map lifecycle path has a retry-pass but non-clean connected-test observation: `MapWorkspaceFragment.requestForceScopeReload()` called `requireContext()` after fragment detachment in the first run. It is recorded as a validation limitation; no code change is authorized during Verification.
- The current evidence does not contradict the approved implementation plan, so there is no classified implementation failure or Debug entry from this validation.

## Result

`NOT VERIFIED`

Automated compilation, JVM tests and the direct form UI test pass. Release cannot proceed because the plan requires authenticated end-to-end API, draft, inspect/edit, and import evidence, and the user previously directed that physical-device testing not be performed. CI is also unavailable.

## Next Action

When authorized, run the bounded authenticated device/API cases in `plan.md` for AC-001 through AC-005 on a fixed committed revision, collect the required request/response or UI evidence, then rerun Verification. Obtain CI results before Release.

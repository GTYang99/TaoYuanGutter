# Verification Report

## Revision and Worktree

- Revision under test: `5530da348ce499fcb0244cb2e1e0afa2788b36a9`
- Branch: `feat/銜接點連結管自帶節點名`
- Tracked worktree changes: none
- Untracked `.worktrees/` exists and is unrelated; it was not included in the revision.

## Acceptance Criteria

| Criterion | Result | Evidence and limitation |
|---|---|---|
| AC-001 | PASS | Source review covers form ordering, Cant Open/Connect Point mutual exclusion, Connect Pipe default, virtual-point reset/hide, and inspect-to-edit prefill. `GutterBasicInfoUiTest` and the full `connectedDebugAndroidTest` suite pass on `Medium_Phone` Android 14. |
| AC-002 | NOT VERIFIED | Mapper tests verify integer request serialization and virtual omission; source review verifies Boolean response prefill. Authenticated device request/response evidence for create/draft/edit/virtual flows is unavailable. |
| AC-003 | NOT VERIFIED | Mapper tests verify create omission and edit preservation; source review verifies create hiding/validation and generated XY_NUM mapping. Authenticated create/edit API evidence and independent device confirmation are unavailable. |
| AC-004 | NOT VERIFIED | Source review verifies the no-query endpoint and import-sheet behavior; the full connected suite passes. The planned physical import smoke flow and authenticated endpoint response evidence were not captured. |

## Validation Evidence

- `./gradlew :app:assembleDebug --no-daemon`: PASS.
- `./gradlew :app:testDebugUnitTest --no-daemon`: PASS.
- `./gradlew test --no-daemon`: PASS.
- Latest revision full JVM regression run: `./gradlew test --no-daemon`: PASS, BUILD SUCCESSFUL in 9s.
- `./gradlew :app:compileDebugAndroidTestKotlin --no-daemon`: PASS.
- `./gradlew :app:connectedDebugAndroidTest --no-daemon`: PASS on `emulator-5554`, `Medium_Phone`, Android 14; completed in 4m 36s.
- Targeted `GutterBasicInfoUiTest` after adding connection-control assertions: PASS on `emulator-5554`, `Medium_Phone`, Android 14; `BUILD SUCCESSFUL` in 45s.
- Targeted `StoreDitchNodeRequestMapperTest`: PASS.
- Targeted `StoreDitchResponseParsingTest` plus `StoreDitchNodeRequestMapperTest`: PASS; generated start/node/end `XY_NUM` response parsing and request serialization are covered.
- Bug-fix revision compile and JVM tests: PASS.
- Bug-fix `GutterBasicInfoUiTest`: PASS on `emulator-5554`, `Medium_Phone`, Android 14.

## Regression Review

The full Android connected test suite and JVM test suite passed. Changed mapper, form, inspect-to-edit, import, and response-mapping paths are covered by source review and targeted tests. No implementation failure was observed. CI status and authenticated API evidence remain unavailable.

## Result

`NOT VERIFIED`

The implementation and bug-fix follow-up are committed and developer validation is green, but Release cannot proceed because AC-002 through AC-004 still lack the planned independent device/API evidence and CI results.

## Next Action

Run the planned authenticated emulator flows with request/response capture for AC-002 and AC-003, plus the import smoke flow for AC-004, then update this report and state. Obtain CI results before Release.

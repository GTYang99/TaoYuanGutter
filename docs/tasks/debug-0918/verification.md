# Verification Report

## Verified Revision

- Task: `debug-0918`
- Branch: `feat/銜接點連結管自帶節點名`
- Production revision under test: `26b08815737ef97ee5c2babc43efa38017f440f8`
- The workflow document was committed after the first check as `1e00128`; `git diff 26b0881..1e00128` contains only `docs/tasks/debug-0918/verification-workflow.md`. No production source, test source, or resource changed, so the production revision remains the workflow-fixed `26b0881`.
- Worktree had unrelated changes before verification: `app/src/main/res/values/strings.xml` and two untracked `.worktrees/` directories. They were preserved. Verification updated only task artifacts: this report, `state.yaml`, and `issue-log.md`.

## Inputs and Scope

- Reviewed: `AGENTS.md`, `ai/verification-rules.md`, `ai/testing-rules.md`, `docs/tasks/debug-0918/analysis.md`, `fix-plan.md`, `root-cause.md`, `execution-report.md`, `issue-log.md`, `state.yaml`, the committed implementation diff, related tests, and related parent requirements.
- Reviewed: `docs/tasks/debug-0918/verification-workflow.md` and followed its Gate 0–5 execution rules.
- Missing required task inputs: `docs/tasks/debug-0918/requirement.md` and `docs/tasks/debug-0918/plan.md`.
- `fix-plan.md` describes implementation intent, but it is not a substitute for the missing approved requirement and plan under the Verification rules.
- Product decisions confirmed by the user for this verification: URL-only means an imported existing-waypoint photo whose response has a usable URL but no `id`/`img_id`; `0910刪除資料` is initially off but remains toggleable.
- Related source requirements were reviewed for conflict detection:
  - `docs/tasks/feat-0911-1/requirement.md` AC-002 says unchanged downloaded existing photos must not call `nodeImage`.
  - `docs/tasks/feat-0911-2/requirement.md` AC-001/AC-002 says `0910刪除資料` is checked and visible on first launch.

## Acceptance Criteria

No authoritative acceptance-criteria set exists in `docs/tasks/debug-0918/`, so the task acceptance criteria cannot be evaluated as PASS. The user decisions resolve the intended direction of the two changes, but they do not replace the required approved task artifacts. The following Gate 4 matrix follows `verification-workflow.md`.

| AC / verification target | Result | Evidence and assessment |
|---|---|---|
| AC-001 Photo ID preservation/upload rule | NOT VERIFIED | Unit/source evidence covers `node_img[]`/`url[]`, `id`/`img_id`, API responsibility separation, URL-only unchanged-photo skip, and replacement upload eligibility. Device/API trace is still unavailable, and the approved debug-0918 requirement is missing. |
| AC-002 `0910刪除資料` initial state/toggle | NOT VERIFIED | User decision is initial off; source and `MapOverlayControllerStateTest` support that direction and explicit state propagation. Device UI runtime is unavailable, and the approved debug-0918 requirement is missing. |
| AC-003 `既有點位資料` title centering | NOT VERIFIED | The committed XML uses a full-width `FrameLayout` title with 48dp left/right buttons. No device/UI screenshot or runtime assertion verifies rendered placement. |
| AC-004 Existing-photo, replacement, draft, virtual/cannot-open regression | NOT VERIFIED | Related local tests passed, but no bounded device/API flow was executed for the complete regression matrix. |

## Automated and Static Evidence

| Check | Result | Evidence |
|---|---|---|
| Focused unit tests | PASS | `:app:testDebugUnitTest` with `NodeImgDeserializationTest`, `PhotoImgIdResolverTest`, `PhotoUploadCandidateResolverTest`, `StoreDitchResponseWaypointMapperTest`, `StoreDitchResponseParsingTest`, and `MapOverlayControllerStateTest` completed successfully. |
| Full debug unit suite | PASS | `:app:testDebugUnitTest`; 31 test suites, 95 tests, 0 failures, 0 errors, 0 skipped. |
| Debug APK | PASS | `:app:assembleDebug` completed successfully. |
| Instrumentation APK compilation | PASS | `:app:assembleDebugAndroidTest` completed successfully. Runtime was not executed. |
| Whitespace/diff validation | PASS | `git diff --check HEAD` and `git diff --check c1ea8d0^ HEAD` completed without errors. |
| Source regression review | PASS for reviewed call paths | Upload guards in `GutterFormActivity` and `AddGutterBottomSheet` use the shared unchanged-photo/replacement rule; `storeDitch` mapping remains the existing request boundary; `ditchDetails` and `nodeDetails` are kept as separate API responsibilities; explicit overlay state is still propagated; title controls retain 48dp hit areas. |
| Physical device/UI runtime | NOT VERIFIED | No device/emulator was available. `adb` could not start its daemon because the environment denied the local listener (`Operation not permitted`). |
| CI build/test | NOT VERIFIED | No CI workflow or result is present in the repository. |

## Regression Review

- PASS by unit/source evidence: `node_img[]` and `url[]` photo response shapes, `id`/`img_id` aliases, image-ID fallback, upload eligibility, existing map waypoint handoff, and deleted-layer default state.
- NOT VERIFIED: real import → edit → upload → `storeDitch` request/response flow; unchanged/replaced photo request counts and returned IDs; main-map first-launch overlay rendering; layer toggle/recreation runtime behavior; rendered title centering.
- No implementation failure was found in the executed local checks. The blocking issue is requirement/plan authority plus unavailable runtime/CI evidence.

## Issues

- `ISS-DBG-0918-004`: missing task `requirement.md`/`plan.md`; the user has confirmed the intended product direction, but the required approved artifacts are still absent.
- `ISS-DBG-0918-005`: device runtime and CI evidence unavailable.
- Existing implementation issues `ISS-DBG-0918-001` through `ISS-DBG-0918-003` remain `resolved_pending_verification` until the requirement decision and runtime evidence are complete.

## Validation Limitations

1. The approved debug-0918 requirement and plan are missing. The user decisions are recorded here, but are not yet represented by those required approved artifacts.
2. No connected Android runtime was available for UI and API smoke verification.
3. No CI configuration or result is available.

## Failure Classification

`planning` — verification cannot establish the authoritative acceptance criteria or approved change boundary. Device and CI gaps are additional `environment` limitations.

## Next Action

Planning/requirement resolution: create or provide the approved `docs/tasks/debug-0918/requirement.md` and `plan.md` reflecting the two confirmed decisions. Then rerun the affected verification cases on the workflow-fixed commit; obtain device/UI runtime evidence and CI evidence if release approval is required.

## Final Result

NOT VERIFIED

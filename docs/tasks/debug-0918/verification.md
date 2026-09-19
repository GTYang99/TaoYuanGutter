# Verification Report

## Verified Revision

- Task: `debug-0918`
- Branch: `feat/銜接點連結管自帶節點名`
- Production revision under test: `a8c95208e46b7cb7142f7e28bf70c309bc9726c8`
- The fixed revision contains the production fix and its related tests. The later
  commits `0cb3581` and `93ffa7b` update task evidence and approved inputs only;
  they are not the production revision under test.
- Worktree had unrelated changes before verification: `app/src/main/res/values/strings.xml` and two untracked `.worktrees/` directories. They were preserved. Verification updated only task artifacts: this report, `state.yaml`, and `issue-log.md`.

## Inputs and Scope

- Reviewed: `AGENTS.md`, `ai/verification-rules.md`, `ai/testing-rules.md`, `docs/tasks/debug-0918/analysis.md`, `fix-plan.md`, `root-cause.md`, `execution-report.md`, `issue-log.md`, `state.yaml`, the committed implementation diff, related tests, and related parent requirements.
- Reviewed: `docs/tasks/debug-0918/verification-workflow.md` and followed its Gate 0–5 execution rules.
- Approved task inputs: `docs/tasks/debug-0918/requirement.md` and
  `docs/tasks/debug-0918/plan.md`.
- Product decisions confirmed by the user for this verification: URL-only means an imported existing-waypoint photo whose response has a usable URL but no `id`/`img_id`; `0910刪除資料` is initially off but remains toggleable.
- Related source requirements were reviewed for conflict detection:
  - `docs/tasks/feat-0911-1/requirement.md` AC-002 says unchanged downloaded existing photos must not call `nodeImage`.
  - `docs/tasks/feat-0911-2/requirement.md` AC-001/AC-002 says `0910刪除資料` is checked and visible on first launch.

## Acceptance Criteria

No authoritative acceptance-criteria set exists in `docs/tasks/debug-0918/`, so the task acceptance criteria cannot be evaluated as PASS. The user decisions resolve the intended direction of the two changes, but they do not replace the required approved task artifacts. The following Gate 4 matrix follows `verification-workflow.md`.

| AC / verification target | Result | Evidence and assessment |
|---|---|---|
| AC-001 Photo ID preservation/upload rule | NOT VERIFIED | Unit/source evidence covers `node_img[]`/`url[]`, `id`/`img_id`, API responsibility separation, URL-only unchanged-photo display/skip, and replacement upload eligibility. Device/API trace is unavailable. |
| AC-002 Replacement upload and returned `img_id` | NOT VERIFIED | The approved requirement is present and local tests cover slot-level replacement eligibility and ID propagation, but device/API trace is unavailable. |
| AC-003 `ditchDetails`/`nodeDetails` mapping separation | PASS | Approved requirement, source review, parsing/mapping tests, and fixed-revision targeted tests support separate API responsibilities and no response-order merge. |
| AC-004 `0910刪除資料` initial state/toggle | NOT VERIFIED | Source and `MapOverlayControllerStateTest` support initial off and explicit state propagation. Device UI runtime is unavailable. |
| AC-003 `既有點位資料` title centering | NOT VERIFIED | The committed XML uses a full-width `FrameLayout` title with 48dp left/right buttons. No device/UI screenshot or runtime assertion verifies rendered placement. |
| AC-005 `既有點位資料` title centering | NOT VERIFIED | XML/source review supports full-row centering and 48dp controls. No device/UI screenshot or runtime assertion verifies rendered placement. |
| AC-006 Existing-photo, replacement, draft, virtual/cannot-open regression | NOT VERIFIED | Related local tests passed, but no bounded device/API flow was executed for the complete regression matrix. |

## Automated and Static Evidence

| Check | Result | Evidence |
|---|---|---|
| Focused unit tests | PASS | On clean detached worktree at fixed revision, `:app:testDebugUnitTest` with `NodeImgDeserializationTest`, `PhotoImgIdResolverTest`, `PhotoUploadCandidateResolverTest`, `StoreDitchResponseWaypointMapperTest`, `StoreDitchResponseParsingTest`, and `MapOverlayControllerStateTest` completed successfully. |
| Debug APK | PASS | On the same clean fixed-revision worktree, `:app:assembleDebug` completed successfully. |
| Instrumentation APK compilation | PASS | On the same clean fixed-revision worktree, `:app:assembleDebugAndroidTest` completed successfully. Runtime was not executed. |
| Whitespace/diff validation | PASS | Fixed-revision diff check completed without errors. |
| Source regression review | PASS for reviewed call paths | Upload guards in `GutterFormActivity` and `AddGutterBottomSheet` use the shared unchanged-photo/replacement rule; `storeDitch` mapping remains the existing request boundary; `ditchDetails` and `nodeDetails` are kept as separate API responsibilities; explicit overlay state is still propagated; title controls retain 48dp hit areas. |
| Physical device/UI runtime | NOT VERIFIED | `adb devices -l` returned no attached devices, so no bounded import, layer-toggle, or title-rendering runtime case could be executed. |
| CI build/test | NOT VERIFIED | No CI workflow or result is present in the repository. |

## Regression Review

- PASS by unit/source evidence: `node_img[]` and `url[]` photo response shapes, `id`/`img_id` aliases, image-ID fallback, upload eligibility, existing map waypoint handoff, and deleted-layer default state.
- NOT VERIFIED: real import → edit → upload → `storeDitch` request/response flow; unchanged/replaced photo request counts and returned IDs; main-map first-launch overlay rendering; layer toggle/recreation runtime behavior; rendered title centering.
- No implementation failure was found in the executed local checks. The remaining
  blockers are unavailable device runtime and CI evidence.

## Issues

- `ISS-DBG-0918-004`: resolved; approved task `requirement.md` and `plan.md` now define the acceptance criteria and fixed revision.
- `ISS-DBG-0918-005`: device runtime and CI evidence unavailable.
- Existing implementation issues `ISS-DBG-0918-001` through `ISS-DBG-0918-003` remain `resolved_pending_verification` until the requirement decision and runtime evidence are complete.

## Validation Limitations

1. No connected Android runtime was available for UI and API smoke verification.
2. No CI configuration or result is available.

## Failure Classification

`environment` — fixed-revision automated evidence passed, but device runtime and CI evidence remain unavailable.

## Next Action

Infrastructure/runtime: provide an Android device or emulator and rerun Gate 3 on
the fixed revision. Obtain CI evidence if release approval requires it.

## Final Result

NOT VERIFIED

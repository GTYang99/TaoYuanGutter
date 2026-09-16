# Verification

## Verification Target

- Task: `feat-0911-1`
- Revision: `3f6b302` (`feat/既有點位不上傳照片`)
- Date: 2026-09-12
- Result: **NOT VERIFIED**

## Inputs Reviewed

- `requirement.md`, `analysis.md`, `plan.md`, `plan-review.md`, `execution-report.md`, `issue-log.md`, and `state.yaml`
- Approved task diff: `7b57a48^..3f6b302`
- Relevant production paths: `StoreDitchNodeRequestMapper`, `PhotoUploadCandidateResolver`, `PhotoUploadManager`, `GutterFormActivity`, `MainActivity`, and `MapWorkspaceFragment`
- Existing unit and instrumentation tests

## Executed Evidence

| Check | Result | Evidence |
|---|---|---|
| `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest` | PASS | `BUILD SUCCESSFUL`; complete debug unit-test suite passed at revision `3f6b302`. |
| `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug` | PASS | `BUILD SUCCESSFUL`; debug APK assembled at revision `3f6b302`. |
| `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDebugAndroidTest` | PASS | Emulator `Medium_Phone(AVD) - 14`; `test-result-exit-code.txt` is `0`, and `TEST-Medium_Phone(AVD) - 14.xml` records 14 tests, 0 failures, 0 errors, 0 skipped. |
| `git diff --check` | PASS | No whitespace errors. |
| CI | NOT VERIFIED | No CI configuration or run result is present in the repository or task artifacts. |

## Acceptance Criteria Review

| AC | Result | Evidence and assessment |
|---|---|---|
| AC-001 | PASS | `StoreDitchNodeRequestMapper.map()` assigns `capturedAt` and `imgIds` to `null` when `requestNodeId != null`; the actual Gson assertion in `StoreDitchNodeRequestMapperTest.existingNodeOmitsPhotoMetadataFromJson` confirms both keys are absent, while retaining the in-memory photo metadata. Complete unit suite passed. |
| AC-002 | PASS | `PhotoUploadCandidateResolverTest.unchangedExistingPhotosAreRemovedFromUploadCopyForAllSlots` verifies unchanged slots are removed only from the upload copy while the original form metadata remains. Both save hosts call this resolver before `PhotoUploadManager`, and the manager skips slots already backed by an image ID or successful server state. Existing emulator evidence recorded in `execution-report.md` also confirms imported photos display and no `nodeImage` request occurs during the completed import flow. |
| AC-003 | PASS | `PhotoUploadCandidateResolverTest.replacingOneSlotKeepsOnlyThatSlotAsUploadCandidate` proves that replacing slot 2 leaves only slot 2 in the upload copy; the resolver iterates all three slots, and `PhotoUploadManager` forwards its slot value unchanged as the `nodeImage` category. The same shared resolver is invoked by both `MainActivity` and `MapWorkspaceFragment`. |
| AC-004 | PASS | `StoreDitchNodeRequestMapperTest.newNodeRetainsPhotoMetadataInJson` asserts Gson output retains `captured_at` and `img_ids` for a node without `requestNodeId`. Complete unit suite passed. |
| AC-005 | NOT VERIFIED | Unit tests cover draft-resume candidate preservation and the emulator suite covers cant-open UI state plus response-to-draft photo-ID persistence. However, no current controlled end-to-end evidence covers the full matrix of cant-open, virtual point, draft restore, and reopening an edited existing point while observing network behavior. The real demo-backend `storeDitch` save-and-reopen smoke remains intentionally unrun because it writes persistent backend data. |

## Plan and Regression Review

- Request metadata mapping was extracted as planned; the mapper does not modify waypoint `basicData`.
- The duplicate photo-diff paths in both save hosts were replaced by the shared resolver as planned.
- `PhotoUploadManager` retains slot 1/2/3 categories and additionally treats a successful imported server photo as already uploaded, including when the server omits an image ID.
- The imported-point regression recorded as `ISS-001` is resolved in the implementation: imported node IDs are retained, existing/successful slots are guarded before queuing uploads, and response image IDs are persisted to draft slots.
- No unrelated working-tree changes were present when verification began.

## Missing Evidence and Next Action

1. Obtain authorized, controlled backend smoke evidence for an existing point save/reopen without creating unintended persistent data. It must cover unchanged photos (zero `nodeImage` requests), each replacement slot/category, and the AC-005 special-flow matrix where applicable.
2. Obtain a CI build and test result, or configure the project CI.

Because AC-005 and CI lack sufficient evidence, `NOT VERIFIED` is not a release approval. No implementation defect was observed, so the task remains in verification rather than being routed to Debug.

---

## Verification Round 2 — ISS-002 Fix

### Verification Target

- Production revision: `457ef7b` (`fix(feat-0911-1): honor successful imported photo state`)
- Documentation revision at review start: `c1546ed`
- Date: 2026-09-12
- Result: **NOT VERIFIED**

### Change Review

`AddGutterBottomSheet.countPendingPhotoUploads()` and
`ensureWaypointPhotosUploadedBeforeSubmit()` now both call
`PhotoUploadSlotState.isAlreadyUploaded(...)`. This is the same combined guard
used by the other upload paths: a numeric image ID **or**
`photo*UploadState=success` skips the slot. Consequently, an imported photo
without an ID but with successful server-backed state does not reach
`repository.uploadNodeImage(...)`; an idle replacement remains eligible.

### Executed Evidence

| Check | Result | Expected / actual result |
|---|---|---|
| `./gradlew testDebugUnitTest --rerun-tasks --tests '*PhotoUploadCandidateResolverTest' --tests '*StoreDitchNodeRequestMapperTest'` | PASS | Forced re-execution recorded 2 mapper tests and 7 resolver/state tests, all with 0 failures and 0 errors. Covers AC-001 through AC-004 and the new successful-state eligibility rule. |
| `./gradlew assembleDebug` | PASS | `BUILD SUCCESSFUL`; current debug APK remained assemblable. |
| `./gradlew connectedDebugAndroidTest` | PASS | Fresh emulator report dated 2026-09-12 14:43:33 records 14 tests, 0 failures, 0 errors, 0 skipped; exit code `0` on `Medium_Phone(AVD) - 14`. |
| `git diff --check` | PASS | No whitespace errors. |
| CI | NOT VERIFIED | No CI configuration or run result exists in the repository or task artifacts. |

### Acceptance Criteria Reassessment

| AC | Result | Evidence and assessment |
|---|---|---|
| AC-001 | PASS | Prior Gson request-JSON test remains applicable and was force-rerun; the ISS-002 change does not modify request mapping. |
| AC-002 | PASS | Static review confirms both bottom-sheet pre-submit paths now use the shared server-backed guard. The force-rerun state tests confirm `UploadState=success` without an image ID is already uploaded. |
| AC-003 | PASS | The changed guard only skips already-uploaded state. The force-rerun test confirms an idle replacement is not already uploaded, and the existing resolver/category path remains unchanged. |
| AC-004 | PASS | The new guard does not affect metadata mapping; the new-node Gson test was force-rerun successfully. |
| AC-005 | NOT VERIFIED | The ISS-002 alternate submit path is now statically covered, but there remains no controlled end-to-end save/reopen observation of the complete cant-open, virtual, draft-restore, and existing-photo matrix. |

### Coverage and Regression Assessment

- The regression fix is limited to the two planned bottom-sheet guards; no API contract or unrelated production path changed.
- The added test verifies `PhotoUploadSlotState`, but does **not** instantiate or directly exercise either changed bottom-sheet method. The source-level call-site review is therefore necessary evidence, and a direct bottom-sheet integration test remains a coverage improvement.
- Existing runtime instrumentation validates the APK and adjacent form behavior, but does not make a backend `nodeImage` request assertion.

### Remaining Evidence Gap

The task cannot advance to Release. It still needs an authorized, controlled backend save-and-reopen smoke that observes request bodies/counts without creating unintended persistent data, plus CI build/test evidence. Until then, this task remains `verification_not_verified` with `next_action: verification`.

---

## Verification Round 3 — ISS-003 Fix

### Verification Target

- Production revision: `28ce977` (`fix(feat-0911-1): reupload replaced gutter photos`)
- Branch: `codex/fix-photo-replacement-upload`
- Result: **NOT VERIFIED** for release; local implementation checks PASS.

### Change Review

- A replacement capture calls a new replacement transition before writing the
  new URI. That transition clears only the old server metadata, leaving the
  new URI and captured-at data intact for upload.
- `onPhotoSlotReadyForUpload()` now handles a `null` path before it applies the
  already-uploaded guard. An explicit deletion therefore clears the
  Activity-owned `img_id`, state, error and URI even for a server-backed photo.
- The guard for an unchanged imported photo remains after those two lifecycle
  transitions and continues to prevent duplicate uploads.

### Executed Evidence

| Check | Result | Evidence |
|---|---|---|
| Target unit test + APK builds | PASS | `:app:testDebugUnitTest --tests com.example.taoyuangutter.gutter.PhotoUploadCandidateResolverTest :app:assembleDebug :app:assembleDebugAndroidTest` completed successfully. |
| Full debug unit suite | PASS | `:app:testDebugUnitTest` completed successfully. |
| Whitespace check | PASS | `git diff --check` reported no errors before commit. |
| Controlled replacement-upload smoke | NOT VERIFIED | A real `nodeImage` request would create persistent backend data; no authorized controlled target was supplied. |
| CI | NOT VERIFIED | No CI configuration or run result is available. |

### Regression Coverage

- `clearingFormerServerMetadataMakesReplacementUploadEligible` proves the
  replacement URI is retained while its former successful server state no
  longer blocks upload eligibility.
- Static lifecycle review confirms the explicit delete path now executes before
  the unchanged-import guard.
- Runtime evidence is still required to observe the actual `nodeImage` request
  for a replacement and confirm the server's same-category overwrite behavior.

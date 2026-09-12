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

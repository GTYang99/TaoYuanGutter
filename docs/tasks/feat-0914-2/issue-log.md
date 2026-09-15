# Issue Log

## ISS-FEAT-0914-2-001

```yaml
issue_id: ISS-FEAT-0914-2-001
task_id: feat-0914-2
phase: knowledge_resolution
category: requirement_gap
priority: P2
title: Figma toolbar placement and time precision conflict with requirement text
status: resolved
impact: Could produce a UI that conflicts with the approved behavior and acceptance criteria.
evidence:
  - requirement specifies left add, right close, and seconds in the creation time
  - Figma node 2374:26810 renders left close, right add, and an example time to minutes
resolution: Requirement text takes precedence; Figma is retained as a visual hierarchy reference where it does not conflict.
next_action: plan_review
owner: planning
```

## ISS-FEAT-0914-2-002

```yaml
issue_id: ISS-FEAT-0914-2-002
task_id: feat-0914-2
phase: knowledge_resolution
category: requirement_gap
priority: P1
title: Figma omits the required close-and-save confirmation dialog
status: resolved
impact: Closing could lose drafts or present an unspecified interaction.
evidence:
  - requirement specifies the alert message and confirm/cancel actions
  - Figma section 2374:25786 has no alert node for this flow
resolution: Implement the specified behavior with the existing Material Alert visual convention; no new product behavior is inferred.
next_action: plan_review
owner: planning
```

## ISS-FEAT-0914-2-003

```yaml
issue_id: ISS-FEAT-0914-2-003
task_id: feat-0914-2
phase: plan_review
category: planning_gap
priority: P1
title: Plan targets legacy MainActivity instead of the live map workflow
status: resolved
impact: The production add-gutter entry and form-result flow could remain unchanged, leaving AC-001 through AC-006 unmet.
evidence:
  - LoginActivity launches MainShellActivity
  - MainShellActivity hosts MapWorkspaceFragment for the map tab
  - MapWorkspaceFragment owns the add-gutter FAB and single-draft workflow
resolution: Formal product entry is MapWorkspaceFragment; plan and tests now use its childFragmentManager and activity-result host. MainActivity is documented as legacy only.
next_action: implementation
owner: planning
```

## ISS-FEAT-0914-2-004

```yaml
issue_id: ISS-FEAT-0914-2-004
task_id: feat-0914-2
phase: plan_review
category: planning_gap
priority: P1
title: Plan does not neutralize existing cross-draft SPI_NUM deletion and deduplication
status: resolved
impact: Editing or uploading one gutter can overwrite or remove another unuploaded gutter draft.
evidence:
  - GutterDraftCoordinator.autoSaveSessionDraft reuses and removes drafts by SPI_NUM
  - deleteDraftsBySpiNum is called by MainActivity and MapWorkspaceFragment
resolution: MULTI_GUTTER rows use draft-ID-only upsert/delete; legacy SPI_NUM dedup and batch delete are restricted to LEGACY_SINGLE rows.
next_action: implementation
owner: planning
```

## ISS-FEAT-0914-2-005

```yaml
issue_id: ISS-FEAT-0914-2-005
task_id: feat-0914-2
phase: plan_review
category: planning_gap
priority: P1
title: Plan omits a complete immutable creation-time and collision-safe draft-ID contract
status: resolved
impact: Draft rows can have incorrect creation times or overwrite each other under rapid creation and later updates.
evidence:
  - Current draft ID and savedAt use System.currentTimeMillis
  - Coordinator, form, and photo upload write paths overwrite savedAt
resolution: Plan now requires immutable createdAt, savedAt backfill migration, a repository-backed monotonic allocator with collision retry, and a unified write contract for coordinator, form, and photo writes.
next_action: implementation
owner: planning
```

## ISS-FEAT-0914-2-006

```yaml
issue_id: ISS-FEAT-0914-2-006
task_id: feat-0914-2
phase: verification
category: environment
priority: P1
title: Gradle verification cannot locate a Java Runtime from the default shell environment
status: resolved
impact: Compile, unit-test, APK-build, and instrumentation verification cannot start with the default Java lookup.
evidence:
  - ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --no-daemon returns "Unable to locate a Java Runtime."
  - Android Studio bundled runtime exists at /Applications/Android Studio.app/Contents/jbr.
resolution: Android Studio's bundled JBR was selected explicitly; the same Gradle command then completed successfully.
next_action: verification
owner: verification
```

## ISS-FEAT-0914-2-007

```yaml
issue_id: ISS-FEAT-0914-2-007
task_id: feat-0914-2
phase: debug
category: implementation_regression
priority: P1
title: New multi-gutter sessions restore unrelated persisted multi-gutter drafts
status: classified
impact: Starting a new add-gutter list session can display drafts from prior closed sessions, contrary to the approved session boundary. A later submit can then treat a prior-session draft as an item in the current list, risking incorrect cleanup scope.
evidence:
  - MultiGutterSessionCoordinator.kt lines 12-17 loads every repository row with MULTI_GUTTER ownership during construction.
  - plan.md step 1 requires configuration recreation to restore only saved IDs from the active session and explicitly prohibits mixing all pending drafts after cold start/process death.
  - MainShellActivityTest.kt lines 242-251 asserts that a newly constructed coordinator reloads persisted items, so current coverage enforces the conflicting behavior.
failed_acceptance_criteria:
  - AC-005
  - AC-006
next_action: implementation_debug
owner: verification
```

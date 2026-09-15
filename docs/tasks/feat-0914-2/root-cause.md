# Root Cause Analysis

## Issue

`ISS-FEAT-0914-2-007` causes AC-005 and AC-006 to fail: a newly opened multi-gutter add-list can display drafts from previous sessions.

## Root cause

`MultiGutterSessionCoordinator` treats the Room table as the source of truth for the current in-memory session. Its initializer loads every persisted row with `WORKFLOW_MULTI_GUTTER`:

```kotlin
repo.getAll().filter { it.workflowOwnership == WORKFLOW_MULTI_GUTTER }
```

Room stores drafts across sessions by design, so this query has no active-session boundary. After the user closes one add-list and its drafts are finalized, a later `openAddGutterFlow()` constructs or reuses a coordinator whose item set includes those old rows. The list therefore mixes old pending drafts with the new session. Successful-upload cleanup then operates on an item that is not necessarily owned by the current session, so AC-006 also loses its isolation guarantee.

The approved plan requires two different recovery scopes:

- configuration recreation: restore only the active session's saved draft-ID list;
- cold start/process death: do not auto-import all pending multi-gutter drafts; expose them through the existing pending-drafts flow.

The current implementation has no saved active-ID list in `MapWorkspaceFragment.onSaveInstanceState()` and no restore path for that list. The coordinator's repository-wide initializer is therefore the only recovery path, and it is too broad.

## Affected files

- `app/src/main/java/com/example/taoyuangutter/gutter/MultiGutterSessionCoordinator.kt`
- `app/src/main/java/com/example/taoyuangutter/map/MapWorkspaceFragment.kt`
- `app/src/androidTest/java/com/example/taoyuangutter/MainShellActivityTest.kt`

## Regression risk

Removing the repository-wide auto-load must not hide drafts from the existing pending-drafts screen. It must only stop those drafts from being injected into a newly created add-list session. Configuration recreation must still preserve item order and IDs, and closing the list must still finalize drafts before clearing the in-memory session.

## Failed acceptance criteria

- AC-005: unrelated persisted multi-gutter rows are restored into a new session.
- AC-006: successful cleanup cannot rely on a trustworthy current-session item boundary when foreign rows are mixed in.


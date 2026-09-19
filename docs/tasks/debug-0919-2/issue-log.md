# Issue Log

| Issue ID | Category | Priority | Status | Summary |
|---|---|---:|---|---|
| ISS-debug-0919-2-001 | implementation_regression | P1 | open | Form-result merge removes completed photo metadata; after reversal the submit gate treats the photo as not uploaded and uploads it again. |
| ISS-debug-0919-2-002 | implementation_regression | P1 | open | The same merge removes the newly returned replacement `img_id` and success state during inspect/update. |
| ISS-debug-0919-2-003 | environment | P1 | open | Runtime/network trace and verification artifacts are unavailable; duplicate request count remains unverified. |

## Resolved investigation questions

- OQ-001: The user confirmed all individual photo uploads complete before
  reversal. In-flight completion timing is excluded as the primary cause.
- OQ-002: `_nodeId` / API `nodeId` is the canonical backend update identity;
  `uid` is normally client-side, although one legacy conversion path copies the
  API ID into it. No identity substitution is required for the minimum
  confirmed fix.
- OQ-003: The user confirmed `storeDitch` response order equals submitted
  waypoint order. The existing response mapper is not a root cause.

## ISS-debug-0919-2-001

```yaml
issue_id: ISS-debug-0919-2-001
task_id: debug-0919-2
phase: debug
category: implementation_regression
priority: P1
title: Completed photo metadata is removed before order reversal
status: open
impact: An already uploaded photo becomes a submit-time upload candidate after waypoint reversal.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt
  - app/src/main/java/com/example/taoyuangutter/MainActivity.kt
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterFormContract.kt
next_action: implementation_debug
owner: developer
```

## ISS-debug-0919-2-002

```yaml
issue_id: ISS-debug-0919-2-002
task_id: debug-0919-2
phase: debug
category: implementation_regression
priority: P1
title: Replacement img_id is cleared during form-result merge
status: open
impact: A successful single-photo replacement is uploaded again during gutter update.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/AddGutterBottomSheet.kt
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterFormContract.kt
  - app/src/main/java/com/example/taoyuangutter/common/PhotoSlotUploadCoordinator.kt
next_action: implementation_debug
owner: developer
```

## ISS-debug-0919-2-003

```yaml
issue_id: ISS-debug-0919-2-003
task_id: debug-0919-2
phase: debug
category: environment
priority: P1
title: Runtime evidence is unavailable
status: open
impact: The investigation cannot report runtime duplicate-request counts as verified.
evidence:
  - docs/tasks/debug-0919-2/analysis.md
  - docs/tasks/debug-0919-2/root-cause.md
next_action: infrastructure
owner: developer
```

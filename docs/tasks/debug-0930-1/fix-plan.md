# Fix Plan

## Issue and Root Cause

- `ISS-001` (`enhancement_request`, P2)
- No runtime endpoint selector exists. Retrofit is fixed to Taipei in a lazy singleton and repositories previously captured that service at construction.

## Approved Scope

1. Define three named API targets: base `http://192.168.10.84/TY_RSGDBIP/`, Taipei, and DEMO.
2. Keep Taipei as the default; target selection is process-local and resets on app restart.
3. Expose the selector only when `GutterApiClient.ENABLE_GROUP_SIMULATION` is true.
4. Switch API requests only; leave WMS/WMTS unchanged and do not add cross-target fallback.
5. Keep the selector unavailable while a gutter form, list, inspection, or edit workflow is active.
6. Validate endpoint mapping and service selection without real backend writes.

## Affected Files

See `plan.md`. No API request or response contract changes are required.

## Rollback Boundary

Reverting the task commit restores the fixed Taipei API client and removes the home selector.

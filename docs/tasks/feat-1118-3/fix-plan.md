# Debug Fix Plan

## Issue

- Issue ID: `ISS-feat-1118-3-001`
- Category: `implementation_regression`
- Priority: `P1`
- Failed AC: `AC-003`, `AC-004`, `AC-005`, `AC-006`, `AC-007`, `AC-008`, `AC-009`

## Approved Fix Scope

The re-implementation must preserve the approved removal of tie-in behavior and restore only behavior that was unintentionally removed or is required by the acceptance criteria. No backend/API contract expansion is included.

### Production changes

1. Restore `IS_CONNECTING` compatibility and normal-point behavior from baseline:
   - `GutterApiModels.kt` response/request fields and compatibility accessor.
   - `StoreDitchNodeRequestMapper.kt` mapping for non-virtual, non-`IS_CANTOPEN` normal points.
   - `GutterFormContract.kt` and `GutterFormActivity.kt` Intent/result compatibility.
   - Existing form/inspection rendering and local write-back paths where they are unrelated to the removed tie-in control.
   - Keep `IS_TIEINPOINT` out of the request model and all new write-back paths.

2. Add the AC-006 submitted legacy tie-in exception:
   - Introduce a narrowly scoped predicate over a submitted draft's waypoint snapshots.
   - Unlock only when the draft is submitted, contains an explicit legacy `IS_TIEINPOINT` marker, and is otherwise in the existing submitted-draft category.
   - Keep all other submitted read-only drafts locked.
   - Add JVM policy tests before wiring the UI flow.

3. Normalize legacy markers at draft boundaries:
   - On draft load, treat `IS_TIEINPOINT` as legacy input and remove it from the in-memory write-back snapshot.
   - On draft save, serialize normalized waypoint snapshots without `IS_TIEINPOINT`.
   - Preserve other basic data, photos, submission state, and waypoint identity.

4. Make draft parsing partially tolerant:
   - Parse valid draft/waypoint entries independently where the stored shape permits.
   - Skip malformed waypoint entries, retain valid entries, and keep the draft usable.
   - Do not convert a malformed waypoint into a fabricated valid waypoint.
   - Preserve current behavior for a completely unreadable draft while ensuring it does not prevent other drafts from loading.

### Test changes

1. Rewrite the five failing JVM tests to assert the approved behavior:
   - legacy tie-in marker omitted from request JSON;
   - legacy tie-in follows normal detail/photo completion rules;
   - `IS_CANTOPEN` exemption remains independent;
   - virtual-point rules remain highest priority;
   - inspection displays normal fields for legacy tie-in input.

2. Add/adjust JVM coverage for:
   - `IS_CONNECTING` normal-point request/form compatibility;
   - submitted legacy tie-in unlock versus ordinary submitted read-only lock;
   - draft save removes `IS_TIEINPOINT` while retaining other data;
   - partial-corrupt draft retention and malformed-waypoint skipping.

3. Rewrite instrumentation tests that reference removed tie-in UI/API:
   - remove `cbConnectPoint` interaction and old confirmation-dialog cases;
   - replace them with absence/non-display checks and normal-point behavior;
   - use legacy JSON/Bundle keys only as compatibility input, never as active controls;
   - preserve legacy fixtures in response/draft upgrade tests.

## Validation Order

1. Run targeted JVM tests for mapper, completion, inspection, policy, and draft repository.
2. Run `compileDebugAndroidTestKotlin` to expose stale instrumentation symbols.
3. Run `assembleDebug` and the complete relevant JVM suite.
4. Run source scan confirming `IS_TIEINPOINT` only remains in compatibility input/cleanup code and not active UI/request behavior.
5. Commit the implementation and test changes on the task branch.
6. Re-run verification against the new committed revision; do not use the dirty AGP change as committed-revision evidence.
7. When ADB is available, execute the planned baseline-to-target upgrade cases for AC-001/002/004/005/006/007/008.

## Stop Conditions

- If restoring `IS_CONNECTING` requires a product behavior change beyond the baseline path, stop and route to Knowledge Resolution.
- If draft storage format cannot identify malformed waypoint boundaries safely, preserve data and record AC-008 as `NOT VERIFIED`; do not silently discard the whole draft.
- Do not proceed to Release while any required AC remains `NOT VERIFIED` or any CI/test gate is unavailable.

## Re-entry Gate

Re-implementation may begin only after this root cause and fix plan are accepted by the implementation workflow and `state.yaml` advances to `next_action: implementation_debug`.

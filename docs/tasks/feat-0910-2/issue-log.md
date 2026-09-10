# Issue Log

## ISS-0910-2-01

- Task: feat-0910-2
- Phase: implementation
- Category: environment
- Priority: P1
- Status: open
- Title: Android validation environment has no Java runtime
- Impact: Gradle compilation, unit tests, Android UI tests, build, and lint cannot be executed locally.
- Evidence: `./gradlew compileDebugAndroidTestKotlin --no-daemon` stopped with `Unable to locate a Java Runtime`.
- Next action: verification

## ISS-0910-2-02

- Task: feat-0910-2
- Phase: debug
- Category: `implementation_regression`
- Priority: P0
- Status: open
- Title: Form entry crashes during runtime View reordering
- Impact: Core form validation is blocked and release readiness cannot be established.
- Evidence: Real-device logcat at 2026-09-10 16:12:01: `IllegalStateException: The specified child already has a parent` at `GutterBasicInfoFragment.reorderEditableSections(GutterBasicInfoFragment.kt:511)`, called from `onViewCreated()`.
- Root cause: nested Views remain attached to their original parent when `formContent.addView()` is called.
- Repro steps: Open the feat-0910-2 form on Sony XQ-AU52.
- Next action: debug

## ISS-0910-2-03

- Task: feat-0910-2
- Phase: implementation
- Category: environment
- Priority: P1
- Status: open
- Title: Photo upload API returns server SQL schema error
- Impact: Photo upload completion cannot be confirmed.
- Evidence: Device dialog reports HTTP 500 and SQL Server error `Invalid column name 'xy_num'` while updating `Map_ditch_nodes_images`; SQL includes `node_id`, `xy_num`, `uploaded_by`, and image IDs.
- Client comparison: the Android photo multipart request sends only `file`, `node_id`, and `fileCategory`; the feat-0910-2 diff did not change the photo upload request.
- Classification: backend/API schema mismatch, not a client field rename proven by current evidence.
- Next action: backend owner to align the image table/API query, then rerun photo upload verification.

## ISS-0910-2-04

- Task: feat-0910-2
- Phase: implementation
- Category: `implementation_regression`
- Priority: P1
- Status: open
- Title: Field titles shift after photo cards become visible
- Impact: Measurement fields become visually detached from their labels after all three photos are captured.
- Evidence: Real-device report after capturing photo slots 1-3; affected titles are width, depth, material, broken, hanging, and silt.
- Fix scope: ensure the correct outer title rows are used and explicitly request a form hierarchy remeasure after photo visibility changes.
- Next action: revalidate after installing the current revision.

## ISS-0910-2-05

- Task: feat-0910-2
- Phase: implementation
- Category: `implementation_regression`
- Priority: P1
- Status: open
- Title: Field-title mapping uses virtual wrapper instead of individual rows
- Impact: Width, depth, material, broken, hanging, and silt titles start in the wrong positions.
- Evidence: The affected title rows are direct children of `llVirtualHidden3`; the previous ancestor helper returned the wrapper itself, so the individual rows were not interleaved with their matching controls.
- Fix scope: return the direct child row when its parent is a virtual section, then apply the approved field order.
- Next action: revalidate the initial form order and all three-photo state.

# Plan Review

## Review Scope

Reviewed `AGENTS.md`, the FEAT-1006 requirement, knowledge-resolution record, repository analysis, implementation plan, latest state, architecture, existing overlay implementations, and planning/plan-critic rules.

## Blocking Issues

None identified. The plan covers AC-001 through AC-006, names all four map hosts, specifies the exact trial datasets and colors, addresses parsing/projection/tile rendering and provider lifecycle, and includes packaging, network, unit, UI, and physical-device evidence plus rollback.

## Improvement Suggestions

### Finding 1

- Severity: Suggestion
- Category: Resource Lifecycle Evidence
- Description: AC-005 requires cache and overlay resources owned by the provider to be released when an overlay is disabled or its map host is destroyed.
- Recommendation: In lifecycle tests, explicitly assert the provider-owned tile cache is cleared and resources are released after disabling/removing an overlay, then assert re-enabling does not create duplicate overlays.
- Status: Non-blocking; lifecycle coverage is already in the plan and this makes its evidence precise.

## Checklist

- Requirements and acceptance criteria: Complete; the file-to-layer pairing is explicitly trial-only and OQ-001 remains deferred.
- Repository analysis and affected modules: Complete; the shared main-map controller and separate gutter-form and point-picker paths are identified.
- Dependencies and risks: Complete; source sizes, feature counts, EPSG:3826 conversion, parsing, rendering, cache bounds, and APK size are addressed.
- Implementation steps: Actionable and within scope.
- Test and regression plans: Cover parsing, projection, holes, tile rendering, all four hosts, network requests, lifecycle, existing overlays, and device interaction.
- Rollback: Documented.

## Decision

APPROVED

---

## Addendum Review — Configurable Offline Polygon Outline

### Review Scope

Reviewed the user-approved AC-007 amendment, updated requirement, repository analysis, implementation plan, and existing tile-render instrumentation coverage.

### Blocking Issues

None identified. The change is limited to the shared offline tile renderer and its focused instrumentation test; both map colors and existing fill/hole behavior remain acceptance constraints. A fixed tile-pixel width gives consistent apparent thickness at each zoom because each generated map tile is rendered to the same pixel dimensions.

### Improvement Suggestions

- Severity: Suggestion
- Category: Test Evidence
- Description: The existing real-data test already decodes rendered tile PNGs and can compare an injected alternate width against the 2.0 default.
- Recommendation: Assert that outline pixels change while an interior fill pixel remains the exact existing layer color.
- Status: Accepted; include this assertion in the instrumentation test.

### Checklist

- Requirements and acceptance criteria: AC-007 defines the default, configurability, color, zoom behavior, and preservation of fills/holes.
- Repository analysis and affected modules: Shared offline tile provider and existing instrumentation test identified; no extra dependency or UI change.
- Implementation plan: One style configuration value plus provider injection parameter; bounded to the offline polygon renderer.
- Validation and regression: Existing instrumentation test harness will compare default and alternate rendered widths for both real sources and check fill pixels; existing suite and build remain required.
- Scope and rollback: No API contract change; reverting the focused renderer/test/artifact addendum restores current behavior.

### Decision

APPROVED

---

## Re-review Addendum — Confirmed GPKG Source Pair

### Review Scope

Re-reviewed the amended FEAT-1006 requirement, analysis, plan, asset manifest, state, issue log, execution report, and current offline data-store, geometry decoder, tile provider, and instrumentation coverage.

### Feasibility Assessment

The amended plan is technically feasible. The two source files are available at the documented input location and their SHA-256 values match the amended asset manifest. The current implementation still routes the possible-gutter layer through GeoJSON and only accepts `POLYGON` metadata for the GPKG path; the plan explicitly covers switching it to the `refactored` MultiPolygon table, validating metadata, using the existing cell index, and testing the exact confirmed source. The existing WKB decoder supports MultiPolygon members, so no new parser dependency is identified.

The plan traces AC-001 through AC-007 to concrete implementation and validation work. It also distinguishes historical GeoJSON-pair evidence from evidence that must be repeated for the amended pair.

### Blocking Issues

None that prevent implementation from beginning.

### Environment Gate

`ISS-FEAT-1006-001` remains open. The execution report says AC-001 through AC-005 host scenarios have not been run on a physical device; the current ADB snapshot also could not start the local ADB server. Android 14 AVD and the earlier Android 12 instrumentation results do not replace the planned four-host physical checks. Keep these results unverified until the required device scenarios are run; this environment issue blocks claiming those ACs passed and blocks Release readiness.

### Improvement Suggestions

- Severity: Suggestion
- Category: Lifecycle Evidence
- Description: The existing provider release instrumentation verifies a standalone provider, while AC-005 also covers host teardown and re-enable behavior with the amended GPKG source.
- Recommendation: Include the possible-layer provider and at least one host teardown/reopen path in lifecycle assertions; keep the four-host physical checks as planned.
- Status: Non-blocking; the plan already includes lifecycle tests and host teardown/reopen scenarios.

### Checklist

- Requirements and acceptance criteria: Complete; both GPKG sources, exact layer titles, colors, all four hosts, and AC-007 are represented.
- Repository analysis and affected modules: Sufficient; identifies the current GeoJSON path and the required GPKG metadata/geometry dispatch changes.
- Dependencies and risks: Identifies source hashes/sizes, MultiPolygon parsing, cell indexing, projection/rendering, and obsolete cache handling.
- Implementation steps: Ordered and actionable; the exact input files and table names are specified.
- Test and regression plans: Trace every AC to source, geometry, tile, package, network, UI, lifecycle, and host checks.
- Scope: Limited to the confirmed source substitution, labels, offline renderer, and associated evidence.
- Rollback: Documented.

### Decision

**APPROVED** — implementation may proceed. The open physical-device issue remains an environment gate: AC-001–AC-005 must not be reported as passed, and Release cannot proceed on the current evidence alone.

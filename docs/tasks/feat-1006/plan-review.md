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

---

## Re-review Addendum — Taoyuan Administrative-Area GPKG (AC-008)

### Review Scope

Re-reviewed the latest FEAT-1006 requirement, analysis, plan, state, knowledge-resolution record, issue log, plan-critic rules, and current offline geometry/data-store/tile-provider implementations. This addendum supersedes the earlier APPROVED decision for the narrower source-pair plan and its obsolete physical-device testing note. The current decision applies to AC-001–AC-008 and the emulator-only test constraint.

### Blocking Issues

#### Finding 1 — Major: Geographic CRS path is not specified sufficiently to implement or validate

- Category: Planning Gap
- Description: The existing renderer's query bounds are EPSG:3826 meters (`WebMercatorTileMath.bounds3826`), its cell index assumes 512-meter cells, and its tile projection interprets geometry coordinates as EPSG:3826. The new region table is geographic `GCS_TWD97[2020]` (SRS 100000), with coordinates in degrees. Step 3 only says to “implement/test the geographic ... path”; it does not define source-space query bounds/index units, the coordinate operation to the map tile coordinate system, or expected known-point results. Reusing the current code path as written would query and project degree coordinates as meters and could produce blank or severely misplaced tiles. AC-008 alignment therefore has no reproducible technical or test oracle yet.
- Recommendation: In Planning, define the region-specific end-to-end coordinate path: axis order, how tile bounds are transformed into the GPKG CRS for candidate selection, index units (or an explicitly separate index), and how region coordinates are converted to tile pixels. Record at least two known source-coordinate/control-point to expected map/tile-coordinate results and test those plus a populated-tile bounds/query case. State the datum/coordinate-operation source used for `GCS_TWD97[2020]` rather than assuming the EPSG:3826 legacy transform applies.

#### Finding 2 — Major: “Preserve existing visible style” has no measurable reference

- Category: Planning Gap
- Description: The current `regions` provider requests WMTS `LAYER=regions` with an empty `STYLE`, so the visible style is selected by the server. AC-008 and implementation step 8 require parity with that style, but neither the requirement nor plan records its observable properties or a stable reference capture. The plan cannot distinguish an acceptable local rendering from a visual regression, and the GPKG path may not reproduce raster-only details such as labels or opacity without additional decisions.
- Recommendation: In Planning, record a representative baseline capture at fixed map extent/zoom and describe the appearance that must be retained (boundary color/width/dash, fill/opacity, labels if present, and z-order/transparency). Add a direct side-by-side or pixel/visual comparison procedure using that baseline, and specify how features unsupported by the GPKG source are handled while preserving the approved intent. Do not invent or weaken the user-approved style requirement.

### Improvement Suggestions

None beyond the blocking recommendations.

### Checklist

- Requirements and acceptance criteria: AC-008 is traceable to source, four hosts, toggle, ordering, and no-WMTS request; its alignment and style expectations are not yet objectively testable.
- Repository analysis and affected modules: The three WMTS host paths and existing region z-index are located. The existing data store/index/projection are EPSG:3826-specific; planning must state the distinct geographic path.
- Dependencies and risks: GPKG metadata/source are identified. Geographic CRS operation, index units, and a visual reference are missing from the executable plan.
- Test plan: Emulator-only requirement is correctly reflected. Metadata, all-host toggles, and request absence are covered, but there is no coordinate control-point oracle or measurable style-parity baseline.
- Regression plan: Other map traffic, layer controls/order, and unrelated layers are named; region appearance comparison needs a baseline.
- Implementation steps: Affected hosts and broad source replacement are named, but steps 3 and 8 need the details above before coding.
- Scope and task size: Scope remains the approved three-source offline trial and is appropriately bounded.
- Rollback: Restoring the WMTS is documented and applicable.
- Open questions: The plan says none, but its CRS and style-validation details are planning gaps that can be resolved from the known source and existing WMTS behavior; no external clarification is required by this review.

### Decision

**REQUEST_CHANGES** — return to Planning. Do not begin implementation until Finding 1 and Finding 2 are addressed in `analysis.md`/`plan.md` and their validation steps are traceable to AC-008. No production code or approved requirement was changed by this review.

## Planning Response and Re-review — Geographic Region GPKG

### Response to Blocking Finding 1 — Resolved

The revised `analysis.md` and Plan Steps 3–4 define the geographic coordinate path: GPKG x/y is longitude/latitude; the source WKT declares `GCS_TWD97[2020]`, `Taiwan_Datum_1997`, and GRS80; staging transforms vertices on the background executor into EPSG:3826 with the existing TM2 zone 121 operation (a=6,378,137 m, 1/f=298.257222101, k0=0.9999, central meridian 121°, false easting 250,000 m, false northing 0 m). The normalized coordinates use the existing 512 m portable cell index, `bounds3826` query bounds, exact feature-bound filtering, and `tilePixel` renderer. The plan records two deterministic control-point outputs and a populated-tile/index-query assertion.

### Response to Blocking Finding 2 — Resolved

The user confirmed preserving the current WMTS appearance. A live read-only WMTS tile was captured before any source replacement at z13/x6856/y3508; the PNG and request/hash metadata are checked in under `evidence/`. The sampled tile has transparent fill/background, opaque `#762E77` line core with antialiased edges, and no text labels. Plan Steps 5 and 10 and AC-008 require these properties, style-metric comparison against the saved tile, and preservation of z-index `-0.5f` and zero transparency in all four hosts. Geometry positions use the supplied GPKG as the source of truth, not WMTS raster generalization.

### Re-review Checklist

- Requirements and AC-001–AC-008: covered; the user-confirmed region appearance is recorded in requirement and knowledge resolution.
- Repository analysis and affected modules: identifies the current EPSG:3826/512 m index path and all WMTS host wiring.
- Coordinate operation and spatial query/index units: explicit and tied to repository implementation, with known-point outputs and a populated-tile check.
- Style baseline and visual validation: captured server tile, hash and observed style are recorded; comparison procedure is reproducible.
- Test, regression, lifecycle, network, emulator-only scope, and rollback: present and traced to ACs.
- Blocking issues: none.

### Decision

**APPROVED** — the revised plan is ready for implementation. This review changes planning artifacts only; it does not implement the feature or claim runtime acceptance criteria have passed.

---

## Independent Re-review — AC-008 Planning Response

### Review Scope

Re-reviewed the latest requirement, analysis, plan, state, evidence, issue log, repository rules, and relevant implementation paths. This review evaluates the planner's responses to Findings 1–2 and the full AC-001–AC-008 implementation plan.

### Resolution Evidence

- The source GPKG reports SRS id 100000, `MULTIPOLYGON`, 13 features, and bounds 120.982033033–121.479996161 longitude / 24.586455730–25.123613061 latitude. Its WKT declares longitude then latitude axes, `Taiwan_Datum_1997`, and GRS 1980, matching the analysis.
- The saved WMTS reference exists as a 256 × 256 indexed PNG. Its SHA-256 matches the baseline document (`fa589fdc27d6e7afd37b6c85c8e7477d496df10e90f695e73a0a83e547b9634c`). The baseline documents the request, observed transparent background, `#762E77` stroke metrics, sampled transect, and existing z-index/transparency.
- Independently recalculated the two projection and zoom-16 tile outputs. Both match the plan within its stated 0.01 m / 0.01 px tolerances.
- The plan maps the coordinate normalization to the background staging path, projected-space index, tile queries, populated-tile checks, all four hosts, emulator-only runtime validation, and rollback. AC-008 is traceable to these steps.
- State is `phase: plan_review`, `status: approved`, `plan_review.status: approved`, and `next_action: implementation`. The two prior planning issues are recorded as closed with the evidence above.

### Blocking Issues

None identified. The prior CRS/index-space and missing-style-baseline blockers are resolved sufficiently for implementation to begin.

### Improvement Suggestions

#### Finding 1 — Suggestion: State the normalized cache blob format and decoder explicitly

- Category: Implementation Clarity
- Description: Step 3 permits a normalized geometry/WKB cache, while Step 5 says all three layers use the GeoPackage geometry decoder. The existing code has separate GeoPackage-wrapper and raw-WKB decode entry points. The source formats and normalized cache are clear enough to proceed, but explicitly naming the stored blob format and selected decoder would avoid an implementation mismatch.
- Recommendation: In the implementation detail or test description, state whether the normalized region cache stores raw WKB and uses `fromWkb`, or stores a GeoPackage geometry blob and uses `fromGeoPackage`; cover one cache round-trip in the region rendering test.
- Status: Non-blocking; either representation is supported by the existing decoder and the plan explicitly allows a normalized WKB cache.

### Checklist

- Requirements and acceptance criteria: All eight ACs are represented; no acceptance criterion was lowered.
- Repository analysis and affected modules: All three data sources, existing map providers, four hosts, source-specific CRS handling, and relevant cache/render modules are identified.
- Dependencies and risks: GPKGs, emulator, existing projection, cell index, geometry codec, and baseline evidence are specified; prior high-risk gaps are resolved.
- Test plan: Source metadata/hash, coordinate controls, rendered geometry/style, four-host toggles/network, lifecycle, and emulator-only responsiveness are covered.
- Regression plan: Unrelated map traffic/layers, thematic styling, controls/order, and resource behavior are included.
- Open questions: None remain that block this implementation; the blob/decoder detail is a non-blocking implementation clarification.
- Scope and task size: Bounded to the three trial GPKGs and specified renderer/host behavior.
- Rollback: Documented and appropriate.

### Decision

**APPROVED** — the revised plan is feasible and ready for implementation. This re-review changes planning artifacts only; it does not claim implementation or runtime acceptance criteria have passed.

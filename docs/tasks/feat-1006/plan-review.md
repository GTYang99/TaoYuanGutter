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

# Requirement

## Background

- The `roadServey` GeoServer WMS layer is currently requested with the named style `TY_RSGDBIP_道路調查` in each map entry point.
- The approved request form requires an empty `STYLES` parameter: `STYLES=`.

## Goal

- Remove the named style only from `roadServey` WMS GetMap requests across all supported map entry points.

## Functional Requirements

- The main map, gutter form map, and standalone map point picker must construct `roadServey` requests with `STYLES=`.
- Preserve the existing endpoint, layer name, WMS version, SRS, BBOX, image format, transparency, overlay visibility behavior, and z-index.
- Do not change the styles used by any non-`roadServey` WMS layer.

## Non-functional Requirements

- No new dependency or network/cache policy change.
- This task must not implement the separately deferred WMS in-memory cache work.

## Acceptance Criteria

- AC-001: Opening the main map with the plan-survey layer enabled issues `roadServey` GetMap requests with `STYLES=` and without `TY_RSGDBIP_道路調查`.
- AC-002: Opening the gutter form map and the standalone map point picker with the plan-survey layer enabled issues the same empty-style `roadServey` request.
- AC-003: Existing non-`roadServey` WMS overlays retain their current named styles and their visibility controls continue to work.

## Constraints

- Requirement source: `/Users/a10362/Desktop/markdown file/ty_refctor_0917.md`.
- Scope is limited to removal of `roadServey` style. WMS request-count caching is deferred by user decision.

## Open Questions

- None.

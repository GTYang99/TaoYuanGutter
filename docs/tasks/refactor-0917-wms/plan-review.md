# Plan Review

Task: refactor-0917-wms
Reviewer: Plan Critic Agent
Review Iteration: 2
Review Date: 2026-09-17

---

# Summary

## Review Result

- [x] APPROVED
- [ ] REQUEST_CHANGES
- [ ] BLOCKED

## Summary

The plan is executable and keeps the approved scope to the `roadServey` style parameter. During review, the test approach was corrected: Android `Uri` construction in `Wms3857TileProvider` cannot be reliably covered by the project’s ordinary JVM unit-test setup. The plan now extracts only request serialization into a pure builder, following the existing EPSG:3826 pattern; the tile provider remains the Google Maps adapter.

---

# Review Checklist

| Item | Result | Notes |
|---|---|---|
| Requirement understood | PASS | Only `roadServey` must change from its named style to `STYLES=`. |
| Acceptance Criteria complete | PASS | Covers all three entry points and non-target overlay preservation. |
| Repository analysis complete | PASS | Identifies every current `roadServey` construction and serialization boundary. |
| Architecture impact reasonable | PASS | A pure builder matches existing `Wms3826RequestBuilder`; Google Maps adapter remains in place. |
| Affected modules identified | PASS | Main map, form map, picker, provider, builder, and test are named. |
| Dependencies identified | PASS | Google Maps adapter and GeoServer query compatibility are recorded. |
| Risks evaluated | PASS | Distinguishes empty parameter from omission and protects non-target layers. |
| Test Plan complete | PASS | JVM serialization tests, existing state tests, build, and targeted device checks are included. |
| Regression Plan complete | PASS | Covers all affected map entries and adjacent overlays. |
| Open Questions documented | PASS | No unresolved decision remains. |
| Implementation steps actionable | PASS | Steps state source locations, extraction boundary, and validation. |
| Task size appropriate | PASS | Small, reversible refactor with no cache-policy change. |
| Rollback strategy (if applicable) | PASS | One commit can be reverted. |

---

# Findings

## Finding 1

Severity:
- [ ] Critical
- [ ] Major
- [x] Minor
- [ ] Suggestion

Category: Testability

Description:

The initial plan proposed an ordinary JVM test against `Wms3857TileProvider`, but that class constructs Android `Uri` values. The repository has no JVM Android shadowing configuration, so the test would not provide reliable execution evidence.

Recommendation:

Extract only the URL serialization into a pure Kotlin builder, retain the provider as the Google Maps adapter, and test the builder using the established EPSG:3826 request-builder pattern.

Planning Response:

`analysis.md` and `plan.md` were updated to include `Wms3857RequestBuilder`, delegation from the existing provider, and a focused JVM test.

Status:
- [ ] Open
- [x] Resolved

---

# Blocking Issues

None.

---

# Improvement Suggestions

- Keep the later WMS cache task separate because cache lifetime and invalidation are intentionally deferred.

---

# Decision

## APPROVED

Implementation may begin.

---

# Next Action

- [ ] Planning
- [x] Implementation
- [ ] Requirement Clarification
- [ ] Human Review

---

# Review Notes

- The legacy `MainActivity` has the same WMS configuration, but the active application workspace is `MapWorkspaceFragment` within `MainShellActivity`. The approved requirement names the main map, form map, and standalone picker; implementation will search again before coding and record any additional reachable `roadServey` path as a required scope update rather than changing it silently.

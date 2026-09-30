# Plan Review

## Review Comments

### Finding 1

Severity: Major

Category: API Lifecycle / Regression Plan

Description:
`GutterRepository` currently captures `GutterApiClient.instance` in its constructor, while submissions can issue photo and ditch requests as a multi-step flow. A dynamic client accessor alone could leave existing repository instances on the old target or split a workflow if selection changes between calls.

Recommendation:
The implementation plan now requires repository request-time provider lookup, selector unavailability while an active gutter form/upload workflow is open, and tests proving each target maps to its own cached service while existing/in-flight calls keep their original URL.

Planning Response:
Added this lifecycle boundary to Implementation Step 3 and the Regression Plan. Endpoint routing validation will use a recording `Call.Factory`/interceptor without sending live writes or adding a server dependency.

Status: Resolved

### Finding 2

Severity: Suggestion

Category: Test Plan

Description:
The repository has no MockWebServer dependency, so route assertions should avoid adding a server dependency.

Recommendation:
Use target-to-URL mapping and per-target service identity in unit tests, avoiding a new dependency and all real backend traffic.

Planning Response:
The Test Plan now uses a recording `Call.Factory`/interceptor.

Status: Resolved

## Checklist

- Requirements and acceptance criteria: Pass — AC-001 through AC-005 map to UI gate, endpoint mapping, no-fallback, and release checks.
- Repository analysis: Pass — fixed Retrofit singleton, repository capture, and separate WMS/WMTS clients are documented.
- Affected files and dependencies: Pass — endpoint constants, client, repository, map UI/layout, strings, and tests are listed.
- Implementation steps: Pass — endpoint selection, propagation, UI gate, and tests are ordered.
- Test and regression plan: Pass — target mapping and service identity are asserted without live writes; default Taipei and no-fallback are included.
- Risks and rollback: Pass — wrong target writes, private-network reachability, service propagation, and rollback are addressed.
- Open questions: Pass — none remain.

## Decision

APPROVED

Implementation may begin.

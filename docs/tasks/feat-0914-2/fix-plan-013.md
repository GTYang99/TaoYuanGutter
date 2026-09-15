# Fix Plan — AC-008 Failure Routing and Upload Overlay Motion

## Scope

Resolve `ISS-FEAT-0914-2-013` without changing the approved success flow, API contracts, draft identity rules, or existing legacy single-gutter behavior.

## Minimum implementation

1. Introduce one explicit host callback for a failed multi-gutter upload confirmation, rather than reusing a callback named for network-only failure.
2. Route all terminal add-mode `storeDitch` failure Alert confirmation actions through that callback: network/timeout, 409 photo-claim conflict, generic API error, and exception.
3. Route pre-`storeDitch` photo upload failure confirmation through the same callback after persisting the current waypoint state. Preserve retry behavior where the existing dialog offers it.
4. In `MapWorkspaceFragment`, implement the common callback by saving the selected draft, dismissing the active form, and reopening exactly the current multi-gutter list. Keep legacy single-gutter behavior unchanged.
5. Replace the platform-default-only upload spinner dependency with an explicit app-owned indeterminate drawable/animation contract, or otherwise use a drawable whose runtime animation state can be inspected. Respect the system animation policy rather than forcing motion when Android animations are disabled.
6. Add automated coverage for:
   - generic `storeDitch` error Alert confirmation → retained draft → active list;
   - network/exception/photo-upload failures using the same common exit;
   - overlay visible plus animatable/running state at animation scale `1x`.
7. Run unit tests, targeted instrumentation on both devices, a 1× physical upload-overlay smoke, and an authenticated failure smoke using a non-production failure seam.

## Regression boundaries

- Successful submission → inspect page → inspect close → remove only the successful item remains unchanged.
- No backend delete API is introduced.
- `captured_at` ownership remains unchanged: photo request only, never `storeDitch` nodes.
- System-wide animation scale is not modified by production code.

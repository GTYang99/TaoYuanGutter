# Root Cause Analysis

## Finding

The map/dashboard transition destroys the map tab's fragment instead of retaining it. On return, the shell creates a new `MapWorkspaceFragment`. Its `onMapReady()` unconditionally moves the camera to the fixed Taoyuan coordinate `(24.9929, 121.3011)` before starting asynchronous location lookup. No camera state is saved in `MainViewModel`, which stores only overlay state. This causes a visible jump on each return and can leave the viewport at Taoyuan if location is unavailable.

## Evidence

1. `MainShellActivity.showTab()` creates a fragment for each selection and commits `replace(R.id.shell_container, fragment, tabTag(tabId))`.
2. `MainShellActivity` does not add the transaction to the back stack and does not look up a previously added fragment by tag.
3. `MapWorkspaceFragment.onMapReady()` calls `map.moveCamera(... LatLng(24.9929, 121.3011) ...)` before `requestLocationAndMove()`.
4. `MyLocationController` resolves the preferred fresh fix asynchronously; before a usable callback, the initial fallback is the visible center.
5. `MainViewModel` contains only `overlayState`; no viewport target or zoom exists there.
6. `MainShellActivityTest.shellSwitchesBetweenMapAndDashboardTabs()` checks which fragment class is displayed but does not verify that map and dashboard fragment instances survive tab changes.

## Classification
- Issue: `ISS-FIX-1021-001`
- Category: `implementation_regression`
- Priority: `P2`
- Route: `planning` / bugfix; the cause and minimum correction are source-confirmed.

## Minimum Correction
- Reuse tab fragments by their existing tags and switch visibility with `show()` / `hide()` instead of replacing them.
- Cap hidden tabs at `STARTED` and the visible tab at `RESUMED` so only the active tab is resumed.
- Extend the targeted shell regression test to assert that the original map and dashboard fragment instances are reused after switching.

## Confidence and Limits
- High confidence in the source-level cause: the tab transaction removes the old map fragment, and the replacement map installs the fixed center before location completes.
- Device-level camera behavior remains to be validated on the designated test emulator. The connected physical phone is excluded from testing.

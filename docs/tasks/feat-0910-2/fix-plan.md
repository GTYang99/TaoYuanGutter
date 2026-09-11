# Fix Plan: ISS-0910-2-09

## Current status

Debug complete. Root cause is the form Activity exceeding the device top-resumed startup deadline because the form and XML-declared Maps fragment are created synchronously on the main thread.

## Minimum next checks

1. Replace the XML `android:name="com.google.android.gms.maps.SupportMapFragment"` declaration with an empty map host container. (Implemented.)
2. Let the form UI keep its normal `onCreate`/first-frame path, then add the `SupportMapFragment` from the resumed view queue. (Implemented.)
3. Keep existing map overlays, markers, camera, and transparent form-sheet behavior unchanged. (Implemented.)
4. Re-run `GutterBasicInfoUiTest` repeatedly on XQ-AU52, then run `GutterCantOpenUiTest` and the full connected suite. (Pending device reconnection.)

## Guardrails

- Do not remove the map, change the translucent form theme, or alter activity finish behavior; the fix is limited to deferring map-fragment creation.
- Do not mark the affected acceptance criteria PASS based only on the user's manual field-order result.
- Keep the offline existing-data fix and photo lifecycle fix separate from this investigation.

## ISS-0910-2-11: Virtual-point off-state

### Current status

Debug complete. The virtual checkbox listener still exists, but the Activity-level virtual UI method was made unconditional by the earlier UI redesign, so turning virtual mode off cannot restore the original normal interaction state.

### Minimum implementation

1. Restore state-dependent handling in `applyVirtualModeUi(isVirtual)` for the controls that belong to the virtual-point toggle.
2. Preserve the existing `GutterBasicInfoFragment.setVirtualMode(false)` field restoration and persisted `is_virtual=0` behavior.
3. Add a focused UI regression test for virtual on → off, including the restored control state.
4. Validate the virtual toggle together with the existing form-order and cant-open regressions.

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

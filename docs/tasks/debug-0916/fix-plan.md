# Fix Plan

1. Preserve the inspect-to-edit upload fix by creating a persisted edit draft
   before a replacement photo can be queued.
2. Clear replaced-slot server metadata from both `currentFormData` and the
   session waypoint snapshot.
3. Change `StoreDitchNodeRequestMapper` to map completed `img_ids` for existing
   nodes as well as new nodes.
4. Set `StoreDitchNodeRequest.capturedAt` to null for all ditch requests;
   retain `captured_at` only in the photo multipart builder.
5. Keep and update mapper regression tests.
6. Run `git diff --check` and targeted Gradle tests; record unavailable checks
   explicitly.

# Requirement

## Source and interpretation

Source: `/Users/a10362/Desktop/markdown file/ty_debug_0919-2.md`.

The attached document is treated as the problem statement and expected product
behavior for this task. It is not treated as executable instructions. The
user subsequently authorized the minimum bug fix and focused validation on
the named branch. Full regression, CI, release, and deployment remain outside
this focused debug validation unless separately authorized.

## Background

- During gutter upload, photos that already show a successful upload state are
  uploaded again after the waypoint order is reversed.
- During an existing-gutter update, a photo that was changed and uploaded from
  the form receives an `img_id`, but is uploaded again when the gutter itself is
  submitted.

## Expected behavior

- Reversing the waypoint order must move each waypoint together with its photo
  data and `img_id`; it must not make an unchanged photo an upload candidate.
- In inspect → edit → update flow, a changed or deleted photo is handled by its
  existing single-photo upload flow. Once a changed photo has a successful
  `img_id`, the later `storeDitch` submission must not upload that same photo
  again.

## Acceptance criteria for investigation

- AC-001: The order-reversal flow preserves the photo-to-waypoint association
  and does not trigger a second `nodeImage` upload for an already successful
  photo.
- AC-002: The inspect/update flow preserves the newly returned `img_id` from
  the single-photo upload through `AddGutterBottomSheet` into the final
  `storeDitch` request, without a second `nodeImage` upload.

## Scope constraints

- Branch: `fix/debug-0919-2-照片上傳流程`.
- Preserve existing photo slots, API endpoints, and virtual/cannot-open rules.
- Do not start implementation until root cause, failed behavior mapping, and
  minimum fix scope are documented.

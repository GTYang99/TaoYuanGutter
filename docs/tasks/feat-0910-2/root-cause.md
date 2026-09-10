# Root Cause Analysis

## Issue

- Issue ID: ISS-0910-2-02
- Category: `implementation_regression`
- Priority: P0

## Evidence

Real-device logcat at 2026-09-10 16:12:01:

```text
FATAL EXCEPTION: main
java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.
 at android.view.ViewGroup.addView(...)
 at com.example.taoyuangutter.gutter.GutterBasicInfoFragment.reorderEditableSections(GutterBasicInfoFragment.kt:511)
 at com.example.taoyuangutter.gutter.GutterBasicInfoFragment.onViewCreated(GutterBasicInfoFragment.kt:423)
```

## Cause

`reorderEditableSections()` builds an ordered list of Views, removes each item from `formContent`, and adds it back. Some entries are nested children whose immediate parent is not `formContent`; removing them from `formContent` is a no-op. The subsequent `formContent.addView(child)` therefore attempts to attach a View that still belongs to its original parent, producing the observed `IllegalStateException` during form creation.

## Affected Requirements

- AC-001: entering the form page is blocked by the crash.
- AC-002 and AC-005: the requested order cannot be observed.
- AC-006: regression validation cannot proceed.

## Risk

The crash occurs before the form becomes usable. A minimum fix must preserve existing View IDs, listeners, field bindings, photo-slot mappings, virtual-mode visibility, and view-mode behavior while eliminating runtime cross-parent reparenting.

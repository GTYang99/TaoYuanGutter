## ISS-bugfix-0929-connect-pipe-alpha-001

```yaml
issue_id: ISS-bugfix-0929-connect-pipe-alpha-001
task_id: bugfix-0929-connect-pipe-alpha
phase: implementation
category: implementation_regression
priority: P2
title: Connect pipe disabled state applies alpha twice
status: resolved
impact: Connect pipe controls and title appear darker than equivalent disabled fields.
expected: Only the option group uses the common disabled alpha.
actual: Parent and child alpha both equal 0.5, producing effective child alpha 0.25.
evidence:
  - app/src/main/java/com/example/taoyuangutter/gutter/GutterBasicInfoFragment.kt
  - app/src/androidTest/java/com/example/taoyuangutter/GutterBasicInfoUiTest.kt
resolution: Removed the parent layout alpha and added focused regression coverage.
next_action: verification
owner: developer
```

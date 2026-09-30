# Issue Log

## ISS-001

```yaml
issue_id: ISS-001
task_id: debug-0930-1
phase: implementation
category: enhancement_request
priority: P2
title: Debug testers cannot switch the API backend before login
status: implemented_pending_verification
impact: Testers cannot select the requested backend target at runtime; current API calls use the fixed Taipei endpoint.
repro_steps:
  - Launch the app in a debug build.
  - Open the login screen.
  - Look for an API environment selector.
expected: A hidden tester entry is available when ENABLE_GROUP_SIMULATION is true and can select base, Taipei, or DEMO.
actual: No environment selector exists; GutterApiClient creates its service with the fixed Taipei URL.
evidence:
  - app/src/main/java/com/example/taoyuangutter/api/GutterApiService.kt:288,302-309
  - app/src/main/java/com/example/taoyuangutter/common/BackendEndpoints.kt:9-13
  - app/src/main/java/com/example/taoyuangutter/api/GutterRepository.kt:70
next_action: implementation
owner: planning
```

## Notes

- Classified as an enhancement because no prior working selector or regression evidence was found.
- The user confirmed the `base` URL; implementation is pending independent verification.

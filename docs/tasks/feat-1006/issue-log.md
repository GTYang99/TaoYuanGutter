# FEAT-1006 Issue Log

## ISS-FEAT-1006-001 — Android runtime verification environment unavailable

- Phase: implementation
- Category: environment
- Priority: P1
- Status: open
- Impact: AC-001 through AC-005 require Android runtime/device evidence for local asset loading, visual rendering, host/toggle coverage, pan/zoom responsiveness, and provider lifecycle. No physical device or configured AVD is available in the current environment.
- Evidence: `adb devices -l` returned an empty device list; `emulator -list-avds` returned no AVDs. Instrumented tests compile but have not run.
- Next action: infrastructure
- Remediation: connect an Android 9+ device or make a suitable AVD available, then run the instrumented data-store/provider tests and the plan's focused scenarios in all four map hosts.

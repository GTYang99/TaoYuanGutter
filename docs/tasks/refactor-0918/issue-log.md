# Issue Log

## ISS-001
- task_id: refactor-0918
- phase: verification
- category: environment
- priority: P2
- title: Java Runtime unavailable for Gradle validation
- status: resolved
- impact: Initially blocked focused JVM test and debug assembly.
- evidence: Android Studio's bundled OpenJDK 25 ran the focused test successfully; `assembleDebug` also succeeded with a temporary ignored `MAPS_API_KEY=test` placeholder.
- resolution: Use Android Studio's bundled JDK and a temporary non-secret local manifest placeholder for local validation.
- next_action: verification
- owner: environment

## ISS-002
- task_id: refactor-0918
- phase: verification
- category: environment
- priority: P2
- title: WMTS-only physical failure injection unavailable
- status: open
- impact: The physical Sony XQ-AU52, authenticated session, and functional Maps rendering are available. A safe WMTS-only outage control is not available; the limited all-network outage test cannot prove isolated WMTS failure behavior.
- evidence: Sony XQ-AU52 (Android 12/API 31) installed the current worktree APK, entered `MainShellActivity`, and kept map controls usable after the three affected overlays were toggled while an unreachable local proxy blocked network. Physical screenshot is stored at `docs/tasks/refactor-0918/evidence/ac004-physical-network-failure.png`; proxy settings were removed afterward.
- next_action: infrastructure
- owner: environment

## User Decision
- task_id: refactor-0918
- phase: verification
- decision: Defer AC-004 physical verification; keep the criterion `NOT VERIFIED`.
- production_code_change: none

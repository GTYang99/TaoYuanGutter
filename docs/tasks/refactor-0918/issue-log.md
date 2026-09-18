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
- title: Emulator map-verification prerequisites unavailable
- status: open
- impact: The Android 14 emulator, existing authenticated session, and functional Maps rendering are available. A safe WMTS-only outage control is not available; the limited all-network outage test cannot prove isolated WMTS failure behavior.
- evidence: `emulator-5554` (Android 14/API 34) installed the debug APK, entered `MainShellActivity` through the existing login session, and kept the map controls usable after the three affected overlays were toggled while an unreachable proxy blocked network. Proxy settings were removed afterward. The earlier direct-launch limitation for `MainActivity` remains irrelevant because the normal login flow was used.
- next_action: infrastructure
- owner: environment

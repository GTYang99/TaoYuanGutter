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

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
- impact: The fixed revision starts successfully on Android 14 `sdk_gphone64_arm64`, but it opens the login screen. No test account or authorized functional Maps API key is available, so map overlay behavior cannot be validated.
- evidence: Fixed revision `5fad9e2` passed its focused test and debug assembly, then was installed on the Android 14 emulator. The launcher opens `com.example.taoyuangutter/.login.LoginActivity`; directly starting `MainActivity` is denied because it is not exported. The project Secrets plugin also requires `MAPS_API_KEY` from ignored `local.properties`.
- next_action: infrastructure
- owner: environment

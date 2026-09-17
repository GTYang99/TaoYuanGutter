# Issue Log

## ISS-001

| Field | Value |
|---|---|
| task_id | refactor-0917-wms |
| phase | implementation |
| category | environment |
| priority | P2 |
| status | open |
| title | Local Gradle validation cannot start because no Java Runtime is installed |
| impact | JVM unit tests and debug build are NOT VERIFIED in this worktree. |
| evidence | `./gradlew testDebugUnitTest --tests 'com.example.taoyuangutter.map.Wms3857RequestBuilderTest' --tests 'com.example.taoyuangutter.map.Wms3826RequestBuilderTest' --tests 'com.example.taoyuangutter.map.MapOverlayControllerStateTest'` ended with `Unable to locate a Java Runtime`. |
| next_action | Run the recorded unit tests and `./gradlew assembleDebug` on a machine with a supported JDK before verification. |

# Issue Log

## ISS-001
- task_id: refactor-0918
- phase: verification
- category: environment
- priority: P2
- title: Java Runtime unavailable for Gradle validation
- status: open
- impact: Focused JVM test and debug assembly cannot run in this worktree.
- evidence: `./gradlew testDebugUnitTest --tests com.example.taoyuangutter.map.Wmts3857TileProviderTest --console=plain` ended before Gradle execution with `Unable to locate a Java Runtime`.
- next_action: infrastructure
- owner: environment

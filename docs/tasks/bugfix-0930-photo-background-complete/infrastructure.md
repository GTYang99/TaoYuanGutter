# Infrastructure Report

## Originating Verification Blockers

- `ISS-004`: no CI configuration or passing CI result was available for the fixed revision.
- `ISS-005`: the device's effective API route was not independently confirmed as the test station.

## CI Environment Evidence

- No `.github/workflows`, `.gitlab-ci.yml`, `Jenkinsfile`, Azure Pipelines, or CircleCI configuration exists in the task checkout.
- The local GitHub `origin/main` and GitLab `gitlab-http/master` refs also contain no supported CI workflow file.
- GitHub reported no combined status checks for `bc4f2ab4f0f0a40e2093aea6006bf8b396d67553`; the Actions workflow-run lookup returned no PR-triggered runs. The task branch is not present on GitHub yet, so no push-triggered run could exist.
- The local JDK/Android SDK toolchain and the previous focused JVM test/debug build results are available, but local results do not satisfy the CI gate.

## CI Remediation

- Added `.github/workflows/android-ci.yml` for branch pushes, pull requests targeting `main`, and manual dispatch.
- The job uses JDK 21 and Android SDK 36, has a 30-minute limit, and runs `:app:testDebugUnitTest` plus `:app:assembleDebug`.
- The Maps manifest placeholder is a literal build-only value written on the ephemeral runner. No credential or `local.properties` is committed or read from this machine.
- CI remains `NOT VERIFIED` until this workflow is pushed to GitHub and completes successfully.

## Device API Route Evidence

- Device: `adb-QV710EDR3A-hF5XZF._adb-tls-connect._tcp`, model `XQ-AU52`, Android 12.
- Airplane mode is enabled; Wi-Fi remains connected and validated. Android reports no global HTTP proxy, no VPN transport, Cloudflare Private DNS, and DNS resolvers `8.8.8.8` / `8.8.4.4`.
- `taipei.srgeo.com.tw` resolves from the device to `114.32.90.213`; ICMP reached that address. This is consistent with the source's static endpoint but does not identify the backend database/environment.
- `BackendEndpoints.kt` labels Taipei as the active formal endpoint and `GutterApiService` constructs Retrofit with that constant. The user reported switching to the API test station, but the device exposes no proxy/VPN/DNS override that confirms this change.
- No photo upload or `storeDitch` call was initiated during infrastructure diagnostics.

## Result and Next Action

- CI remediation is prepared locally; push the branch to obtain the required CI result.
- API route remains unresolved pending the authoritative test host or routing mechanism. Do not run further photo uploads until that route is confirmed.
- Keep the task in `infrastructure` until both CI evidence and the test route are resolved, then return to `verification`.

# Release Rules

## Objective

Confirm that a verified revision is ready for merge or deployment and record the release decision without bypassing human authorization.

## Required Inputs

- approved requirement and plan
- committed revision
- CI results
- `verification.md`
- latest `state.yaml`
- known release risks and open issues

## Release Checks

Release MUST confirm:

- Verification result is PASS
- CI build and required tests are PASS
- no acceptance criterion is `NOT VERIFIED`
- no open P0 or P1 issue blocks the release
- database, API contract, permission, certificate, Gradle, SDK, R8/ProGuard, background execution, and security risks are documented when applicable
- rollback or recovery expectations are known for material risk

## State Updates

When ready:

```yaml
phase: release
status: release_ready

release:
  status: approved

next_action: human_release
```

After authorized merge or deployment evidence is available:

```yaml
phase: done
status: done

release:
  status: completed

next_action: none
```

When blocked:

```yaml
phase: release
status: release_blocked

release:
  status: blocked

blocking:
  - Describe the unmet gate

next_action: verification
```

Choose another `next_action` only when the evidence clearly identifies a different owning phase.

## Restrictions

- Do not treat `NOT VERIFIED` as PASS.
- Do not merge, publish, deploy, or alter production without explicit authorization.
- Do not waive an acceptance criterion or release gate.

## Definition of Done

- all release checks have evidence
- the release decision and risks are recorded
- state is `release_ready` with `next_action: human_release`, `done` after authorized release evidence, or routed to the owning phase with blockers

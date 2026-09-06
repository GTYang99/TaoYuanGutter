# Infrastructure Rules

## Objective

Diagnose and resolve environment, CI, SDK, toolchain, device, credential, or service conditions that prevent an otherwise valid task phase from running.

Infrastructure work does not change product behavior or production code.

## Required Inputs

- latest `state.yaml`
- failing command and complete relevant output
- environment details
- originating phase and required check
- related issue record

## Process

- reproduce or confirm the environmental failure
- distinguish repository failure from environment failure
- record the minimum safe remediation
- rerun the blocked check when remediation is available
- return to the originating phase rather than assuming success

## State Updates

While investigating:

```yaml
phase: infrastructure
status: infrastructure_in_progress
next_action: infrastructure
```

When resolved, set `next_action` to the phase whose check was blocked, normally `implementation` or `verification`.

When unresolved, keep the task blocked and record concrete missing access, service, tool, or environment evidence.

## Restrictions

- Do not change application behavior to make an environmental check pass.
- Do not report the blocked check as PASS.
- Do not expose or store credentials in task artifacts.

## Definition of Done

- the environmental cause is evidenced
- remediation is recorded
- the blocked check is rerun or remains explicitly blocked
- state returns to the owning phase

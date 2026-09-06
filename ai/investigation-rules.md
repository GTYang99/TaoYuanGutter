# Investigation Rules

## Objective

Gather enough evidence to classify an unknown failure and route it to the phase that owns the correction.

Investigation MUST NOT modify production code.

## Required Inputs

- latest `state.yaml`
- issue record
- observed and expected behavior
- available logs, tests, screenshots, diffs, and reproduction steps

## Process

- reproduce the problem when possible
- narrow the failing boundary
- test competing hypotheses without changing product behavior
- classify the cause as `requirement`, `planning`, `implementation`, or `environment`
- route the task using `AGENTS.md`

## State Updates

```yaml
phase: investigation
status: investigation_in_progress
next_action: investigation
```

When classified, update the issue evidence and set `next_action` to `knowledge_resolution`, `planning`, `debug`, or `infrastructure`.

If evidence remains insufficient, record the exact missing evidence and keep the task blocked. Do not guess a category.

## Definition of Done

- the failure has an evidence-backed category and route, or
- the task records a precise evidence blocker that prevents classification

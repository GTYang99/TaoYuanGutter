# Implementation Debug Guidelines

## Objective

Resolve verification failures by identifying the root cause and applying the minimum required fix.

Debugging MUST preserve approved requirements.

---

## Required Inputs

- verification.md
- requirement.md
- plan.md
- Git Diff
- CI Results
- Logs
- Stack Trace (if available)

---

## Required Outputs

Developer MUST create or update:

- root-cause.md
- fix-plan.md
- execution-report.md (if validation is limited)
- state.yaml
<<<<<<< HEAD
- related issue record or issue log entry
=======
>>>>>>> feat/主地圖效能進度條

---

## Update state.yaml

When debug starts:
```yaml
phase: debug
status: debug_in_progress

debug:
  status: investigating

next_action: debug
```

```yaml
phase: debug
status: debug_complete

debug:
  status: completed
  root_cause: identified

next_action: implementation_debug
```

---

## Block rule

Developer MUST NOT start re-implementation until:

- root cause is identified
- failed AC is mapped
- minimum fix scope is documented


---

## Debug Process

Review verification findings
↓
Map failed AC to code/tests
↓
Identify root cause
↓
Collect evidence
↓
Define minimum fix
↓
Document fix-plan
↓
Update state.yaml
↓
Re-enter implementation

---

## Root Cause Analysis

Developer MUST identify:

- Why the issue occurred
- Which files are affected
- Whether regression risk exists
- Which issue_id the debug work is resolving, if available

Developer MUST NOT fix symptoms without identifying the root cause.

---

## Debug Rules

Developer MUST:

- fix one issue at a time
- preserve unrelated functionality
- minimize code changes
- update tests if behavior changes
- keep the fix scope aligned with the issue priority

---

## Definition of Done

Debug completes ONLY IF:

- Root cause identified
- Failed acceptance criteria mapped to code/tests
- Minimum fix scope documented in fix-plan.md
- state.yaml updated to `next_action: implementation_debug`
- Ready for re-implementation

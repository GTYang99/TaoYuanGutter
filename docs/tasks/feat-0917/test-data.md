# New API Test Data

All payload assertions below apply to each non-virtual waypoint in `storeDitch`.

## Write Cases

| Case | Point mode | `IS_CANTOPEN` | `IS_TIEINPOINT` | `IS_CONNECTING` | Expected payload |
|---|---|---:|---:|---:|---|
| W-01 | normal | false | false | false | Both new keys exist as Boolean `false` |
| W-02 | normal | false | true | false | Both new keys exist; tie-in `true`, connecting `false`; Cant Open is disabled |
| W-03 | normal | true | false | true | Both new keys exist; tie-in `false`, connecting `true`; tie-in is disabled |
| W-04 | virtual | false | false | false | Omit `IS_CANTOPEN`, `IS_TIEINPOINT`, `IS_CONNECTING` entirely |

`is_connect_point` and `is_connect_pipe` must be absent in every write case. Numeric `0` and `1` are invalid request values.

## Read Cases

| Case | `IS_CANTOPEN` | `IS_TIEINPOINT` | `IS_CONNECTING` | Expected UI/model |
|---|---:|---:|---:|---|
| R-01 | `"0"` | `"0"` | `"0"` | Both new fields false; show 連結管「無」 for normal point |
| R-02 | `"0"` | `"1"` | `"1"` | Tie-in selected; show 連結管「有」; dropdown has `(銜接點)` |
| R-03 | `"1"` | `"1"` | `"0"` | Cant Open selected; normalize tie-in false; dropdown must not show `(銜接點)` |
| R-04 | absent | absent | absent | All new values false; normal point shows 連結管「無」 |
| R-05 | virtual | absent | absent | Hide all three controls and hide 連結管 in view |

When `IS_PENDING_DEPLOY`／`node.isPendingDeploy` is true in R-02, expected name is `起點（E001）(銜接點)(待架站)`. `IS_HANGING` remains an independent「附掛或過路管線」fixture value and must not add a pending-deploy label.

## Draft Cases

| Case | Draft input | Expected restored state |
|---|---|---|
| D-01 | Legacy draft without both new keys | Both false; normal point write later includes both Boolean false values |
| D-02 | Normal draft with both Boolean values | Values and mutual-exclusion state restored unchanged |
| D-03 | Convert normal draft to virtual | Clear three values, omit two new keys in draft and all three in payload |
| D-04 | Convert D-03 back to normal | New values remain false; no historic value is restored |

## Inspect-to-Edit Failure Cases

| Case | Preload condition | Server-side value before edit | Expected result |
|---|---|---|---|
| F-01 | Any node-details request fails | `IS_TIEINPOINT="1"` or `IS_CONNECTING="1"` | Do not open a submittable edit form; offer retry or cancel; do not call `ditchToWaypoints()` as a submit-capable fallback and do not create a false overwrite payload |
| F-02 | All node details succeed; only photo preload fails | Any valid `"0"`／`"1"` combination | Existing warning may allow continuation; opening edit retains the normalized server values |

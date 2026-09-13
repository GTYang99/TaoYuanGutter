# Issue Log

## ISS-DBG-0913-002
- Category: implementation_regression
- Priority: P2
- Status: resolved_pending_validation
- Title: New form defaults trigger the cant-open clear confirmation
- Evidence: A new form can preselect `rbIsBroken0` (否) and `rbIsSilt0` (無); when the default gutter type is 明溝, its UI also forces cover thickness to `0`. The first implementation treated these UI-derived defaults as clearable user content, so the first cant-open selection showed an Alert despite no user input.
- Expected: The initial default selections do not count as user-filled content; the first cant-open selection proceeds without an Alert.
- Fix: Treat only non-default broken/silt selections and non-system-forced cover thickness as confirmation-worthy content, and test the real initial form state without clearing its views.
- Next action: implementation

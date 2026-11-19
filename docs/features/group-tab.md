# Group tab

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

The group's summary: really free money, what to review, warnings and where the money went.

- Group tab (`ui/groupsummary`, `GroupSummaryViewModel(groupId)` with `GroupSummaryRepository`, `AccountDetailRepository`,
  `AccountRepository`, `EnvelopeRepository`; `GroupSummaryPane`): read only, it counts the due dates to review and opens the review
  screen. Top to bottom: (1) the `HeroCard` (see "Look and feel" in [architecture](../architecture.md)): a row "Really
  free today" and "N days left" / "Last day of the period" (`GroupSummaryUiState.daysLeft`, a display count, plural `group_hero_days_left`), the big `free_today` figure (`HeroFigure`: always white, a
  "Below zero" chip when negative), a compact 56 dp `FreeProjectionChart` (`ui/components/charts`, paper line, step-after,
  solid up to today and dashed after, the part below zero in `LocalMoneyColors.negative`, end dot) of `group_free_projection`, a row
  "Projected on <period end>" with `free_projected` (`titleLarge`) and, right-aligned, a label and figure (`MoneyText` Plain, `titleMedium`) with the database figure that
  explains the difference (`GroupSummaryUiState.projectionNote`, nothing is subtracted in the app): `committed_shortfall` ("For
  commitments"), else `pending_out` ("In contributions"), else `pending_income` ("Expected income"), none when all are 0;
  when the projection goes below zero after today, a line "Will drop to X on <day>" in the negative colour (`lowestPoint`: the
  smallest row of the database, a selection, in `HeroColors.negativeOnContainer`). No bars.
  (2) The review card, always visible: with nothing pending a check icon and "All caught up: nothing to review"; otherwise "N to
  review", the oldest due date muted and ONE `PrimaryButton` "Review" that opens `GroupReview(groupId)`. N counts
  `pending_occurrences` without the receiving half of a contribution (`group_transfer_in`, the contributor registers it, FZ018). The Group
  tab icon carries a `BadgedBox` ink badge with the same count (hidden at 0, label "N to review"); `GroupRoute` refreshes it when the
  shell is shown, when the tab is selected and when coming back from the review. (3) "Warnings" card (hidden when empty, tappable rows
  with a warning icon in the negative colour and a chevron): envelopes in `envelope_progress` state `overspent` (opens the envelope
  detail), accounts with `account_forecast.shortfall > 0` (date from `account_projection` with `shortfallDate`, opens the account
  detail) and `committed_shortfall > 0` (selects the Envelopes tab). Under each account warning, one muted line per suggestion of
  `transfer_plan` (`GroupSummaryRepository.getTransferPlan`; `GroupWarning.AccountShortfallOn.hints` as `TransferHint`): "Move N from
  <account> before <date>" plus "· enters the distribution" (donor outside the envelopes, receiver inside) or "· leaves the
  distribution" when it crosses the boundary (`BoundaryCrossing`), or "No other account can cover N" when the plan row has no source;
  each coverable line has a compact "Transfer" button (members who may record) that sends `TransferClicked`, which the Route turns
  into `MovementForm` with `prefillKind = Transfer`, from, to and amount (`MovementPrefill`, date = today); the user reviews and
  saves, nothing is written from the tab. A failed plan read just leaves the suggestions out. (4) "Where this period's money went": a bar per envelope that
  spent in the current period (`SupabaseGroupSummaryRepository.getEnvelopeSpending`: `envelope_periods` rows of `period_start`
  with `activity < 0`, value = -activity), biggest first (ties by name), top 6 (`MAX_SPENDING_BARS`), name left and `MoneyText` Flow
  right, bar length = value / biggest (`spendingFraction`, a display ratio) in the outflow (ink) colour; a bar opens the envelope
  detail and "See all envelopes" selects the Envelopes tab; hidden when nothing was spent. (5) "In and out by period":
  `GroupedColumnChart` of `group_period_flows` (last 6 periods, `PeriodFlow`, inflow and outflow colours; internal account transfers are
  not counted by the database). (6) "Next due items": a `LazyRow` of small cards (weekday and day, name, `MoneyText` Flow signed by
  `DueOccurrence.signedCents`) from `upcoming_occurrences` between tomorrow and the period end; one muted card "Nothing until the
  end of the period" when there are none. The secondary reads (projection, flows, spending, upcoming, warnings) fail alone: the
  card is left out, never the tab. Everything comes from the database; the app only signs, joins names, picks rows and sorts.
- The Group tab's left column ends with a
  "Look ahead" section (`group_outlook(p_until)`, `GroupSummaryRepository.getOutlookUntil`: free today, the three database
  figures that explain it when above 0, and "Free on <day>" emphasised).

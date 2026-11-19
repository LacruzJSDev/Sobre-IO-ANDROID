# Accounts tab

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

The group's accounts, their total and what is outside the envelopes.

- Accounts tab (`ui/accounts`, `AccountsViewModel` with the group id as Koin parameter, `MemberRepository` for holder names): on
  top a `HeroCard` (see "Look and feel" in [architecture](../architecture.md)): "Total in accounts" and the big total (`HeroFigure`) (with "Last 3 months" on the right of the label
  while the chart is shown); no bars. When some accounts are outside the envelopes a footer of two columns: left "Shared into
  envelopes" with `in_envelopes_total` and a muted caption (`HeroLabel` `alpha` 0.6) naming the accounts (the name when it is one,
  otherwise "N accounts"), right-aligned "Outside the envelopes" with `outside_total` and the same caption (no footer when nothing
  is outside); between the two, in the empty middle, a past-only line chart of `group_balance_series` for the last 3 months (about 60 points,
  `AccountRepository.getBalanceSeries`, today from `CalendarRepository.getToday`; it reuses `FreeProjectionChart` with every
  point in the past so there is no dashed projection; 56 dp like the Group card's, hero foreground colour, below-zero part in
  `negativeOnContainer`; a failed read leaves it out, the card keeps its height)
  (`group_overview.accounts_total`, `in_envelopes_total`, `outside_total`, never summed in the app). Then the live accounts in two sections,
  "In envelopes" and "Outside envelopes" (empty ones hidden; the "In envelopes" title only shows when there is an outside
  section): `EntityAvatar` (colour + account icon, the initial when there is no icon), name, kind label (+ " · holder", or
  "Joint", only when the group has several members) and the balance. At the end a collapsed "Archived (N)" section (same
  query, `Account.archivedAt`). Owner and admin see the header "+" and swipe live rows for Edit and Delete and archived rows
  for Reopen (`unarchiveAccount`, snackbar with Undo that archives again); members only read. Delete archives
  (`archived_at`) at once, so FZ008 returns immediately and the row comes back; the row then appears in the Archived section; Undo unarchives. Name
  clashes (SQLSTATE 23505, also when reopening) map to `AppError.DuplicateName`. `SupabaseAccountRepository.getAccounts` reads
  the balances (`account_balances`, with `owner_user_id` and `archived_at`, archived rows included) and the colour and icon
  from `accounts` (the view has neither) in two queries joined by id in the repository. The tab is `AccountsTabRoute` (see [group shell](group-shell.md)).
  Tapping a row opens the detail.
- Accounts rows add a third line from `account_forecast` (`AccountDetailRepository.getForecasts`, all accounts at once, the
  same function behind the `account_outlook` view): "End of period: X" muted, or "Short by X before the period ends" in
  `LocalMoneyColors.negative`.
- On a pane of 720 dp or more the Accounts tab's left column holds, under the hero card,
  "To keep every account above zero" (`transfer_plan` via `GroupSummaryRepository.getTransferPlan`: "Move X from A to B
  before D" and a compact "Transfer" that the Route turns into the prefilled movement form, `AccountsEvent.TransferClicked`;
  "No other account can cover X for B" without a source; hidden when empty) and "Coming up until <period end>"
  (`upcoming_occurrences` from tomorrow: name, weekday and account, signed `MoneyText` Flow) in `ui/accounts/AccountsOutlook.kt`. Phones (one column) do not show these blocks.

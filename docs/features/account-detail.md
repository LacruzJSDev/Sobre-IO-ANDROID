# Account detail

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

One account: balance, chart, forecast, due items, movements, bank tools and commitments.

- Account detail (`ui/accountdetail`, route `AccountDetail(groupId, accountId)`, level 2): opened by tapping an account row
  (swipe actions stay). Header with the account avatar and name, the group as a subtitle (`ScreenHeader(subtitle)`) and a
  pencil (owner and admin, `Group.canEdit`) that opens `AccountForm`; the screen reloads each time it is shown. Flat top
  tabs (`PrimaryTabRow`, ink indicator, no divider): Summary and Commitments (see below).
  Summary: balance with kind, holder (several members only) and scope; balance line chart of the chosen range (1M/3M/6M/1Y,
  default 3M, chosen on the zoomed page) with the dashed projection to the end of the period; the forecast `StatCard`
  (scheduled in, out, projected end; a shortfall line with the date of the lowest point, `shortfallDate`); in/out column chart of the last 6 periods; next 3
  due items; latest 3 movements (`LATEST_MOVEMENTS`) with the movements icon (`ic_list`) at the right of the section title (pushes `AccountMovements`); "Compare with bank" opens a sheet (`AmountField` plus the "In credit / Debt" chips, `CompareState.bankIsNegative`, default Debt when the
  account balance is negative; debounced `compare_with_bank`, re-run when the sign changes) whose "Adjust to this balance" records an `adjustment` with `create_transaction` and shows an Undo
  snackbar that voids it (`transactions.voided_at`). `AccountDetailRepository` (`data/repository`, DTOs and mappers in
  `data/remote`) wraps `account_forecast`, `account_balance_series`, `account_projection`,
  `account_period_flows`, `upcoming_occurrences` (filtered to the account and signed in `UpcomingDto.toUpcomingFor`, the
  only thing done in the app), the `movements` view, `compare_with_bank` and the two writes; nothing is summed in the app.
  The adjustment's amount in the snackbar is formatted by the screen: `UndoableAction`/`UndoMessage` carry `amountCents`.
- The account and envelope details have "Balance on" (`ui/components/BalanceOnDay`: a
  `DateFormTile` up to the group's today and the `account_balance_at` / `envelope_balance_at` answer as a `MoneyText` Balance).
- Lighter details (plan phase 5.2): a detail shows the essentials and leaves exploring to the zoomed page. Account detail: the
  balance chart has no range chips on the detail (a muted line says to enlarge it); its zoomed page holds the range chips,
  the chart, "Balance on" and every point; the latest movements are 3 (`LATEST_MOVEMENTS`).

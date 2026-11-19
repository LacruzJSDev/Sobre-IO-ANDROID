# Envelopes tab

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

Free to distribute and the envelopes by type, with their progress.

- Envelopes tab (`ui/envelopes`, `EnvelopesViewModel` with the group id as Koin parameter, same shape as Accounts): each time
  the tab shows, `materialize_through_today` runs first (failure ignored) so monthly targets are assigned, then the
  `envelope_balances` view (colour, icon and `purpose` from `envelopes`, joined by id), the `envelopes_summary(p_group_id)` RPC
  (one row, `EnvelopeSummary`: `unassigned`, `in_envelopes_total`, `due_this_period`, `reserved_later`, `other_assigned`,
  `overspent`, `covered_on`, `covering_income` (the sum of the expected incomes into Unassigned until `covered_on`); never summed or derived in the app), `envelope_progress` (failure ignored: rows just have no bar)
  and the members are read. The first item is the `EnvelopesHero` (`HeroCard`, see "Look and feel" in [architecture](../architecture.md); the whole card opens the Unassigned
  detail): "Free to distribute" and the big `unassigned` figure (`HeroFigure`, "Below zero" chip when negative); in the middle
  the sum that gives it, rows of label and `MoneyText` Plain (`titleSmall`) of database figures: "In your accounts"
  (`in_envelopes_total`), then only when above 0 "Payments this period" (`due_this_period`), "Set aside for later"
  (`reserved_later`) and "Rest of the envelopes" (`other_assigned`) shown with a display minus, and "Envelopes below zero"
  (`overspent`) with a display plus; the footer is one line chosen by `envelopesHeroLine` (pure): negative and covered "+X of
  expected income covers it on <date>" (`covering_income`, `covered_on`, calendar icon), negative and not covered "No expected
  income covers it" (info icon), positive "Money that is not in any envelope yet", zero "All the money is in envelopes". No bars. "Unassigned" has no row. The envelopes follow in
  sections by type
  (`EnvelopePurpose`: Monthly spending, Bills, Savings, Free, ordered by name, empty ones hidden). Each row: `EntityAvatar`
  (colour + envelope icon), name, "Target X a month" (only when it has no bar) and the holder ("Common" or the member) when the
  group has several members, a 5 dp rounded progress bar (track = soft container) and one muted line, then the balance. The bar
  and line come from `EnvelopeProgress` (`domain/model`): remaining fills in ink "N left of M this month"; reserving fills in the
  inflow colour and follows the NEXT payment only (D-46): line 1 "N of M reserved", line 2 "Payment on <date> · then <date>"
  (`followingDue`; dates short, with the year only when it is not the current one, `formatShortDate`), both one line with
  ellipsis; paid is a full inflow bar and one line "✓ Paid on <date>" in inflow plus "· next <date>"; contributing fills in
  inflow "Contributed N of M this month"; overspent is a full bar and "Overspent: N" in the negative colour; none draws nothing.
  `pending > 0` adds "· N to review in Group" (muted). `EnvelopeProgress.fraction` is only a 0..1 clamp of the two database figures.
  Colours come from `LocalMoneyColors` (colour-blind aware). `external` is filtered out in the mapper. A collapsed "Archived (N)" section at the end; owner and admin swipe
  Reopen (snackbar with Undo). Owner and admin see the "+" (full-screen envelope form) and swipe Edit and Delete (archive with
  Undo, FZ008 restores the row). Any member swipes "Assign" on live rows: a sheet with an amount and the source as chips
  ("Unassigned" by default, each with its balance), which inserts into `envelope_transfers` (no Undo, overdrawing is allowed).
  Tapping a row opens the envelope detail.
- On a pane of 720 dp or more the Envelopes tab's left column holds "Payments until <period end>" (the expenses of `upcoming_occurrences` with an envelope)
  and "Latest moves between envelopes" (5 newest, read only, swap icon to `EnvelopeTransfers`) in
  `ui/envelopes/EnvelopesOutlook.kt`. Phones (one column) do not show these blocks.

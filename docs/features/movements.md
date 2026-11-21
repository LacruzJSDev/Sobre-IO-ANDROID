# Movements

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

The ledger with filters, search, totals, reports and export.

- Movements (`ui/movements`, `MovementsViewModel(groupId, fixedAccountId: String?, fixedEnvelopeId: String?, initialTagIds)`, the fourth Koin parameter optional: `AccountMovements.tagId` starts the list filtered by a bank import's tag; one screen in three places): the Movements
  tab (account null: the whole ledger) and the level 3 route `AccountMovements(groupId, accountId)` that "See all" in the
  account detail pushes above it (title = account name, subtitle = group, back arrow, no Account chip; back returns to the
  detail). Both show `MovementsPane`; the header is the holder's. Right side of the header: only search (magnifier) and "+"
  (`MovementsHeaderActions`). Search turns the header into a compact `SearchField` pill with autofocus and an X
  (`MovementsSearchHeader`, drawn by `MovementsTabRoute` instead of the shell's header), debounced 300 ms, matching notes
  or an amount ("42,30" or "42.30"). "+" opens the movement form (`NewClicked`, intercepted by the Route; on an account's list it preselects that account). Expense and income rows and account transfer rows swipe Edit (`Movement.canEdit`: not adjustments or contribution legs; `EditClicked(id, source)` opens `MovementForm`) and Delete.
  Under the header a horizontally scrolling row of `FilterChipButton`s: Account, Envelope, Type, Dates, Tags, Person (Person
  only with several members, Account hidden on level 3). Idle = soft grey with a chevron; active = ink, the chosen value
  (small avatar for account, envelope, member) and an X that clears it. A chip opens an `EntityPickerSheet` (`FlatBottomSheet`,
  `EntitySelect` rows with a check, search box above 8 options, "All" first; tapping applies and closes); Tags is the only
  multiple choice (`EntityMultiPickerSheet`, "Apply"). Type: Expenses, Income, Transfers, Adjustments, Contributions
  (`MovementType`, sent as `p_kinds` group names). Dates: This period, Previous period (read from `periods`), Last 3 months,
  This year and Custom (`CustomRangeDialog`, a flat `DateRangePicker`); `DatePreset` and `datePresetRange` are pure. Filters
  and search text live in the ViewModel (not saved on disk). The list is grouped by day (`groupByDay`, pure: "Today · 14 Nov",
  "Yesterday · 13 Nov", then "Fri 12 Nov"), 50 per page with load-more near the end and a progress row, and its own empty
  states (no movements, no results with "Clear filters"). Row (`MovementRow`): the ACCOUNT's avatar (colour + account icon), line 1 the notes or the kind label, cut
  (weight without fill) to leave room for "· <envelope>" next to them, baseline aligned (max 130 dp; "Unassigned" by name too;
  not on an envelope's own list; a transfer shows "To/From <account>" there), line 2 ALWAYS filled (22 dp): the tags as small
  chips with their colour dot (`MovementTags`: up to 3, or 2 and "+N") or a muted "No tags", so rows keep one height; the amount
  on the right (unsigned for a transfer in the group ledger). Swipe: Delete only (`deleteAction`), hidden at once with Undo; the void (`voided_at` on
  `transactions` or `account_transfers`) is written only when the snackbar ends without Undo. Contribution legs (`group_transfer`)
  swipe Delete only (no Edit): the row hides with an Undo snackbar "Contribution of X deleted in both groups" and, when it ends
  without Undo, `MovementRepository.voidGroupTransfer` reads the leg's `group_transfers` link and calls `void_group_transfer`,
  which voids both legs (the other one lives in the other group's ledger); the database decides who may (only a member of both
  groups sees the link, otherwise FZ016), and a failure brings the row back with the error.
  `MovementRepository` (`search` = `search_movements` + `search_movements_count` on the first page, `voidMovement`, `voidGroupTransfer`)
  and `TagRepository` (live `tags`).
  Nothing is summed or filtered in the app; the DB function shows an account transfer once in the group ledger (D-45).
- Movement reports (plan phase 5.3, D-54 in the DB repo): `MovementRepository.getTotals` (`search_movements_summary`) and
  `getReport` (`search_movements_by_period` + `search_movements_by_envelope`) take the screen's `MovementFilter`
  (`domain/model/MovementReport.kt`). `MovementsViewModel` reads the totals after each first page (secondary: a failure hides
  the card) and the report on `ReportOpened`. `MovementsUiState.showsTotals`: a filter is on, or the list is an account's,
  envelope's or tag's (the plain ledger shows none). `ui/movements/MovementsTotals.kt`: `TotalsCard` (a `StatCard` "N movements"
  with in, out and net emphasised, and "See report") is a `ZoomableChart` that grows into the report page (by period: in/out
  columns and a `ChartTable`; by envelope: name, amount and a bar). Wide pane: `MovementsPane` is a `TwoColumnSplit` (left:
  `FilterChipsFlow` and the totals card, or a hint without a filter; right: the rows); narrow: the chips row and the card as the
  list's first item. The group shell and `FixedFilterMovementsScreen` are `ContentWidth.TwoColumns` wide. Tapping a tag in Tags
  (any member; editing stays a swipe) opens `TagMovements(groupId, tagId)` (level 4, extra pane): the same pane through
  `TagMovementsRoute`, starting filtered by the tag, so its totals and report are the tag's.
- Export (`ui/export`, D-36): a download icon in the
  Movements tab header opens `ExportHost`'s sheet (`ExportViewModel(groupId)` with `ExportRepository` and `CalendarRepository.getPeriods`: the last 12 `periods`
  as chips, newest chosen); "Export CSV" reads `export_period` and the Route builds the file with `domain/export`'s pure
  `buildExportCsv` (translated header and types, `CsvDialect`: `;` and decimal comma in Spanish, `,` and point otherwise, BOM,
  CRLF) and hands it to `rememberCsvSaver` (expect/actual: Android `CreateDocument("text/csv")`, iOS
  `UIDocumentPickerViewController(forExportingURLs)` from a temporary file); not cached.

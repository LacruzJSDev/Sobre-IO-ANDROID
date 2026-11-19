# Bank import

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

Importing a bank statement CSV into an account.

- Bank import (D-52 in the DB repo, RF11; `ui/bankimport`, route `BankImport(groupId, accountId)`, level 3 of Accounts, full screen
  with no pane metadata, `BankImportViewModel(groupId, accountId)` with `GroupRepository`, `AccountRepository`, `EnvelopeRepository`,
  `AccountDetailRepository`, `BankImportRepository` and `BankFormatStore`). Entry: the account detail's Summary ends with a "Bank" section
  (`BankSection`): a soft tile "Last import" (`bank_import_overview`, date and rows, or "Never") and two `SecondaryButton`s, "Compare"
  (the old Compare with bank sheet) and "Import CSV" (`AccountDetailUiState.canImport`: any member of a live group, live account;
  `BankImportClicked`, handled by the Route). One route, one ViewModel, steps in `ImportStep`: Source (how to export the CSV, last import
  and "export from" date, black "Choose CSV file": `rememberStatementFilePicker` in `ui/bankimport/picker`, expect/actual, the Route
  launches it and sends `FilePicked(PickResult)`; 5 MB limit), Columns (only when `BankFormatStore` has no mapping for this account and
  header: a preview of each column, Date / Description / Amount ("One column" or "Charge and credit") / optional Balance as
  `ExpandableFormTile`s of `FormChip`s, day order and decimal mark as `ChipFormTile`s only when ambiguous; "Continue" saves the mapping),
  Review (a `LazyColumn` of `SoftTile` rows in sections by what will happen: Due items (confirm), Already recorded (link), New, then folded
  Ignored and Already imported; each row: bank description, one muted line from `decisionLine`, `MoneyText` Flow and short date; tap opens
  `RowSheet` (`FlatBottomSheet`: what to do as chips, kind Expense/Income or Transfer, envelope `EntitySelect` with Unassigned first, or the
  other account, notes; the pending or existing candidate as an `EntitySelect`; the envelope also goes to rows with the same description),
  swipe `ignoreAction` with an Undo snackbar; a header icon reopens Columns; a fixed black "Import N rows"), and Result (counts, a
  `StatCard` comparing the CSV's closing balance with `compare_with_bank`, "Undo import" (`void_bank_import`) and "See movements", which
  pops and pushes `AccountMovements(groupId, accountId, tagId)` filtered by the import tag). Back goes one step back; leaving a review with
  rows shows an `AlertDialog` ("Keep reviewing" / "Discard"), the only confirmation dialog of the app, because it loses unsaved work
  (`NavigationBackHandler` in the Route). The CSV is read in `domain/bankimport` (pure, unit tested): `decodeStatement` (UTF-8, else
  Windows-1252), `detectSeparator`, `parseCsv`, `parseStatementDate` / `parseStatementAmount`, `guessMapping` (header row past bank
  preambles, columns by values then names), `readStatementRows` (charges negative, stable `b1:` FNV references with an occurrence number for
  identical rows, `closingBalanceCents`) and `headerSignature`. `BankImportRepository` / `SupabaseBankImportRepository` wrap
  `bank_import_preview`, `apply_bank_import` (tag name in the user's language, `bank_import_tag_name`, colour `TagColor.Default`),
  `void_bank_import` and `bank_import_overview`; a 23505 there is `BANK_ROW_ALREADY_IMPORTED`, FZ024 has its own message. Every
  suggestion comes from the database; the app only reads the file, keeps the user's choices and sorts.

# Movement form

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

Create and edit an expense, an income or a transfer.

- Movement form (`ui/movementform`, route `MovementForm(groupId, movementId?, source?, accountId?, prefillKind?, prefillFromAccountId?, prefillToAccountId?, prefillAmountCents?)`, `MovementFormViewModel` with those
  four as Koin parameters by position): kind row Expense / Income / Transfer (no Adjustment: only "Compare with bank" makes one;
  locked when editing), then the big centred amount (`LargeAmountField`, the keypad opens by itself when creating). Expense
  and income: Account `EntitySelect` (default: the fixed account, else the last one used in this group, stored per user and group in
  `LastAccountStore` on `multiplatform-settings`, set after creating; else none) and Envelope `EntitySelect` with Unassigned first
  (its real system id; expense starts with nothing chosen and requires one, income starts on Unassigned; hidden with a note when the
  account is outside the envelopes). Cover note when creating an expense whose envelope balance (from `envelope_balances`) is below
  the amount, with an optional "Also cover from" envelope sent as `p_cover=[{envelope_id, amount: its full balance}]`; the database
  takes only what is needed. Editing a higher expense shows a note that nothing is covered. Transfer: From / To (different), a
  "To another group" switch on create (destination group of the same currency + its account, `create_group_transfer`, both envelopes
  Unassigned) and a note when it crosses the envelope boundary. Common: Date (`DateFormTile(maxDate = group today)`), Notes and, for
  expense and income, Tags: an expandable multi-select tile (`TagPicker`) with a `SearchField`, a "Did you mean X?" line, a first
  "Create <text>" row (`TagRepository.createTag` with the default colour, selected at once) and the live tags with a colour dot and
  a check; closed, the header shows the chosen tags as small chips (two, then "+N"); the chosen ones show as removable chips at the bottom of the open body. Writes go through
  `MovementFormRepository`: create = `create_transaction` / `create_account_transfer` / `create_group_transfer`; edit = plain update
  of `transactions` (account, signed amount, date, notes, and envelope: "Unassigned" is sent as the system envelope id, an account
  outside the envelopes sends none so the hidden one stays; the account choices keep the stored account's side of the boundary) or of
  `account_transfers`; then the tag step (`transaction_tags` insert / delete). A tag failure keeps the movement, shows the error and
  the next Save only retries the tags. Saved pops the form and the lists reload when shown again.
- Pending match (`match_pending`): while
  creating an expense or income, 400 ms after the amount, account, date or kind change, `PendingMatcher` (`ui/movementform`)
  asks `CommitmentRepository.matchPending` and keeps the first match whose `upcoming_occurrences` row has the same kind;
  `SuggestionCard` (an `InlineNotice` with the Suggestion tone: "Is it <name>?", due date and amount, "No" / "It's this one") under the Money section; yes calls
  `confirm_occurrence` with the typed amount and date (the commitment's envelope) and then the tag step; no is remembered for the
  form.
- History (`ui/history`, D-41): editing a movement ends with `MovementHistorySection` from `MovementHistoryViewModel(groupId,
  movementId, source)` and `AuditRepository` (`audit_log` rows of the transaction or account transfer, oldest first): "Created by
  <who> · <day>" and "<who> changed <fields> · <day>" (bookkeeping columns left out, `visibleHistoryFields`), "Automatic" for the
  daily task and "Former member" for someone who left; a failure leaves it out.
- The movement form's `LargeAmountField`
  writes the currency symbol next to the figure, muted at the same size: after it in Spanish, before it otherwise.

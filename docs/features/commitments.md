# Commitments

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

An account's commitments: the tab, the detail and the form.

- Commitments tab rows (`ui/commitments`): the subtitle says "Paid this period" in the inflow colour when a due date of the current period
  is already registered (`CommitmentRepository.getPaidCommitmentIds` over `commitment_occurrences`, status `materialized`), otherwise
  "Next <date>" as before.
- Commitments tab of the account detail (`ui/commitments`, `AccountCommitmentsViewModel(groupId, accountId)`, `CommitmentRepository` +
  `SupabaseCommitmentRepository`, models in `domain/model/Commitment.kt`, DTOs and mappers `Commitment*`): `AccountDetailScreen` stays
  stateless and takes `commitmentsAction` (the header "+", only on this tab, owner/admin) and `commitmentsContent` slots that
  `AccountDetailRoute` fills from the second ViewModel; it reloads each time the tab shows. The repository reads
  `commitments` with `commitment_versions` embedded (every version, newest first) for the commitments having a version that touches the account;
  the ViewModel keeps those whose current version (in force at the group's today, else the newest) touches it, or, failing that, whose
  first future version does (`Commitment.versionFor`; the row shows that version and "From <date>"); a row whose next version goes to
  another account says "Moves to <account> on <date>" (account names from `AccountRepository`). Sections: Expenses,
  Incomes, Transfers (source), Incoming transfers (read only), Contributions to other groups, Contributions received (read only); empty
  ones hidden, collapsed "Archived (N)" at the end. Row: `EntityAvatar` in ink with the kind icon, name, frequency (plurals
  `frequency_*`) + "Next <date>" (from `upcoming_occurrences`, 400 days) + "Automatic", and the amount signed for this account. Editable
  live rows (owner/admin; a contribution also needs its `commitment_links` row, invisible to members of one group only) swipe
  Edit and Delete (archive at once, Undo unarchives; a contribution uses `archive_linked_commitment` and a single update of both halves to reopen);
  archived rows swipe Reopen. A tap on any row (live, archived or read only) opens the commitment detail (`CommitmentsEvent.OpenClicked`).
- Commitment detail (`ui/commitmentdetail`, route `CommitmentDetail(groupId, accountId, commitmentId)`, level 3, read only,
  `CommitmentDetailViewModel` with Koin params by position): `ScreenHeader` with the kind-icon avatar, the name and the account of the
  version shown as subtitle; a pencil (`Group.canEdit`, live commitment, editable kind, a contribution also needs its link) opens
  `CommitmentForm`; the screen reloads each time it is shown. Content: a line with the signed amount, the frequency and "Archived";
  when the version shown is funded (and the commitment live), a "Reservation" block (`FormInfoTile` in `FormSections.kt`) from
  `CommitmentRepository.getFundingStatus` (`commitment_funding_status`, `CommitmentFunding`; nothing summed in the app; a failure or
  an empty answer leaves the block out; since D-56 "reserved" is what the envelope holds for the next payment, so a cheaper payment
  earlier lowers "this period"): "<this period> this period in <envelope>" and a muted line "<reserved> of <cycle amount> reserved
  for <due date> · N periods left" (plural `commitment_funding_progress`; "due this period" when none are left);
  "History" (a `CollapsibleListSection`, open by default, count in the header; `CommitmentRepository.getOccurrences(id, offset, limit = 20)`
  returns an `OccurrencePage` with the exact total (`count(Count.EXACT)` + `range`) from `commitment_occurrences` with `transactions(...)` / `account_transfers(...)`
  embedded, newest first; the ViewModel appends the next page on `LoadMoreOccurrences` (ignored while loading or when all is loaded; a failure keeps the rows and shows a retry in the section); rows: due date, the registered amount signed for the account and the movement's date when it differs,
  or "Skipped"); "Changes" (a `CollapsibleListSection`, folded unless there is a single version; the versions newest first: "From <date>" and "until <date>" with the day before `effective_to`, amount,
  frequency, the account when it differs from the previous version, the envelope of an expense). It has no pending, confirm or skip
  actions: pending occurrences are reviewed only in the Group review screen, reached from the Group tab.
- Commitment form (`ui/commitmentform`, route `CommitmentForm(groupId, accountId, commitmentId?)`, level 3, Koin params by position):
  avatar with the kind icon + name; "Commitment" (type: expandable chips when creating, read only when editing; amount), "Money" (every choice of an account, envelope or group is an `EntitySelect`: account (locked when creating),
  destination account for a transfer, envelope for an expense, hidden with a note when the account is outside the envelopes so the
  database uses "Outside envelopes"; a contribution: destination group (other live groups with the same currency where the user is
  owner/admin), account, optional source and destination envelope), "Calendar" (`DateFormTile`, repeats switch, every N stepper 1-99,
  unit chips, optional end date), "Registration" (auto register, default off and on for contributions; "funded" only when the DB allows it:
  `CommitmentFormUiState.canFund`), "Notes" (not for contributions, `create_linked_commitment` has none). Editing adds "Apply from" (default
  the group's today); the history of versions lives in the commitment detail. Create calls `create_commitment` or `create_linked_commitment`; edit renames
  (`commitments.name`) when the name changed and then `change_commitment` (only when a rule field changed) or, for a
  contribution, `change_linked_commitment` with the amount only (the other fields are read only, with a note); a retry skips the steps already done.
  A taken name shows under the name field; FZ016/FZ017/FZ018 show under the form. Editing (not a contribution) lets the account change:
  "Account" is an expandable tile of the group's live accounts (a transfer's destination excludes the chosen source, same account is a
  validation error), a note says from when the new account is charged, and choosing an account outside the envelopes clears the envelope
  and funding (expense); the new `accountId` travels in `change_commitment` with "Apply from". Creating and contributions keep it fixed.

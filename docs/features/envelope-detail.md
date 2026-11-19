# Envelope detail

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

One envelope: available, this period, really free, chart, commitments, moves and movements.

- Envelope detail (`ui/envelopedetail`, route `EnvelopeDetail(groupId, envelopeId)`, level 2; Koin params by position): opened by
  tapping any Envelopes row, "Unassigned" and archived ones included (read only: no pencil, no commitments, name from
  resources, inbox icon; the outlook is skipped for them too). `ScreenHeader` with the avatar, name, group subtitle and a
  pencil (`canEditEnvelope`: owner or admin of a live group, live envelope, not Unassigned) that opens `EnvelopeForm`; the screen
  reloads each time it is shown. One scroll: "Available" (`MoneyText` Balance, headline; under it the envelope type, holder with several
  members, "Target X a month"); "This period" tile from the `envelope_periods` row that holds the group's today (brought
  forward Balance, assigned Flow, movements Flow, left Balance emphasised); "Really free" from `envelope_outlook_current`
  (committed Flow negative, free Balance, and when `shortfall > 0` a line in `LocalMoneyColors.negative`); `GroupedColumnChart`
  of the last 6 periods (`EnvelopePeriod.chartColumns()`: assigned and spent, each clamped at 0 because a column cannot be
  negative, a display choice and not a balance); "Commitments that fund it" (`EnvelopeDetailRepository.getFundingCommitments`
  lists the live commitments with a funded version pointing here, `CommitmentRepository.getFundingStatus` keeps only those
  that reserve money for this envelope now; rows tap to `CommitmentDetail(groupId, accountId, commitmentId)`); "Moves between
  envelopes": the 3 newest live `envelope_transfers` of the envelope (`LATEST_TRANSFERS`; origin label, "From/To <envelope>" and
  the day in the group time zone, signed `MoneyText` Flow), READ ONLY, with the swap icon (`Icons.Filled.SwapHoriz`) at the
  right of the section title opening `EnvelopeTransfersOf(groupId, envelopeId)`, the only place where they are edited and
  deleted; and the 3 latest movements (`LATEST_MOVEMENTS`, `MovementRepository.search` with the envelope filter) with the movements icon
  (`ic_list`) at the right of the title opening `EnvelopeMovements`. Nothing is edited or deleted in the detail itself, so it
  has no swipes, no Undo and no "See all" button. `EnvelopeDetailRepository`
  (`data/repository`, DTOs and mappers in `data/remote`, `FakeEnvelopeDetailRepository` in tests) reads `envelope_balances`,
  `envelope_periods`, `envelope_outlook_current` and `envelope_transfers`; the app only signs the moves for this envelope.
- The movements icon of an envelope's detail pushes `EnvelopeMovements(groupId, envelopeId)` (level 3) above the detail: the same
  `MovementsViewModel` and `MovementsPane` with `fixedEnvelopeId` (Koin params `groupId, fixedAccountId, fixedEnvelopeId`; the
  Envelope chip is hidden and the rows show their account instead), through the shared `FixedFilterMovementsScreen` that
  `AccountMovementsScreen` also uses. Back returns to the detail.
- The account and envelope details have "Balance on" (`ui/components/BalanceOnDay`: a
  `DateFormTile` up to the group's today and the `account_balance_at` / `envelope_balance_at` answer as a `MoneyText` Balance).
- "Look until": the envelope detail's "Really free" `StatCard` takes a `DateFormTile` (clearable, back to the period end) that
  reads `envelope_outlook(p_until)` (`EnvelopeDetailRepository.getOutlookUntil`).
- Lighter details (plan phase 5.2): a detail shows the essentials and leaves exploring to the zoomed page. Envelope detail: the "Really free"
  card is tappable (`ZoomableOutlook`, a muted line says so) and grows into a page with the card, "Look until" and "Balance on";
  without an outlook (Unassigned, archived) "Balance on" stays on the detail; the latest moves and movements are 3 each.

# Moves between envelopes

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

Every move between envelopes of the group, or of one envelope.

- Moves between envelopes (`ui/envelopetransfers`, route `EnvelopeTransfers(groupId)`, level 2 of the Envelopes tab, detail pane,
  `EnvelopeTransfersViewModel(groupId)` with `GroupRepository`, `EnvelopeRepository`, `EnvelopeTransferRepository`): opened by the
  swap icon (`IconActionButton`, every member) left of the "+" in the Envelopes tab header. Every live `envelope_transfers` row of
  the group, newest first, 50 per page with load-more: destination envelope avatar, "From → To", origin label and day, `MoneyText`
  Transfer. Members of a live group swipe Edit (manual moves and monthly targets, also a tap) and Delete (manual, target and
  commitment reserve rows, `TransferOrigin.canEdit`/`canVoid`): hidden at once, Undo snackbar, `voided_at` written when it ends
  without Undo; a voided target is not generated again in its period, D-53; covers and releases stay read only. The header "+"
  opens the form for a new manual move. The same screen limited to one envelope is `EnvelopeTransfersOf(groupId, envelopeId)`
  (level 3, extra pane above the envelope detail; `EnvelopeTransfersViewModel(groupId, envelopeId)`,
  `EnvelopeTransferRepository.getTransfers(groupId, envelopeId, …)` filters from or to it; the subtitle names the envelope and
  its "+" starts the form with that envelope as the destination). `EnvelopeTransferForm(groupId, transferId?, toEnvelopeId?)`
  (`EnvelopeTransferFormViewModel`, full screen, `FormScaffold`): amount, From and To `EntitySelect`s ("Unassigned" first, each
  with its balance; a new move starts from Unassigned, and on `toEnvelopeId` when it is a live envelope) and notes; an automatic move (monthly target) shows its envelopes read only with a note, only amount and notes
  change (D-16). Create inserts a manual row, edit is a plain update of `envelope_transfers`. `EnvelopeTransferRepository`
  (`SupabaseEnvelopeTransferRepository`) reads and writes the table; `voidTransfer` only touches `VOIDABLE_ORIGINS`.

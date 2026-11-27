# Group review

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

The only place where due dates are confirmed or skipped: the ones already due and, early, the next ones.

- Group review (`ui/groupreview`, route `GroupReview(groupId)`, level 2 of the Group tab, `GroupReviewViewModel(groupId)` with
  `GroupRepository`, `GroupSummaryRepository`, `AccountRepository`, `EnvelopeRepository`, `CalendarRepository`, `CommitmentRepository`; `GroupReviewScreen`,
  `ConfirmSheet`): the ONLY place where pending occurrences are reviewed. `ScreenHeader` with back arrow, title "Next movements" (it opens from the
  icon of that section of the Group tab) and the group as subtitle. Two `ListSectionTitle` groups, each shown only when it has rows:
  "To review", one `SoftTile` per `pending_occurrences` row (kind-icon avatar in ink, name, "<account> · was due on <date>", `MoneyText`
  Flow signed by `DueOccurrence.signedCents`), and "Coming up: you can record them now", the `upcoming_occurrences` from tomorrow to
  today + 30 days (`NEXT_DUE_WINDOW_DAYS`) with "<account> · due on <date>". Both have the same buttons with compact "Skip" (`SecondaryButton(compact)`) and "Confirm" buttons for any member
  of a live group (archived or pending-deletion groups are read only: no buttons). Confirm opens `ConfirmSheet` (`FlatBottomSheet`):
  `AmountField` with the expected amount (editable: the real charge, for example the electricity bill), `DateFormTile` (due date, or today when it is later, so a next one is registered on today; `maxDate` = group today; `confirm_occurrence`
  accepts a due date that has not come, and once registered or skipped it never comes up for review), and, for an
  expense whose envelope (not Unassigned) on an account inside the envelopes has less than the amount, the movement form's cover note and
  optional "Also cover from" `EntitySelect` (`p_cover` = that envelope's full balance). Opening the sheet also reads
  `CommitmentRepository.getFundingStatus`; when the commitment's next payment is this due date and its reserve is in the same
  envelope (`reservedForDue` in `GroupReviewRules.kt`), an `InlineNotice` (Info) above the cover note says what was set aside for it and, for the
  amount typed (`reserveNoteOf`, `ReserveNote`): "X set aside for this payment", "…The Y left stay in <envelope> and lower the next
  reserve" (less), or "…The Y more come from what <envelope> already had" (more, when the envelope covers it; otherwise the cover note
  explains). A failure of that read only hides the line. The difference is only a sentence: the database refills the envelope (D-56). `confirm_occurrence` returns the created fact;
  the row goes and an Undo snackbar "<name> registered" voids that fact (`transactions` / `account_transfers` `voided_at`,
  `void_group_transfer` for a contribution), after which the database frees the due date. Skip hides the row at once, shows "<name>
  skipped" and writes `skip_occurrence` only when the snackbar ends without Undo (`UndoController`; `hiddenKeys` stop a reload bringing
  the row back meanwhile; the back arrow and `onCleared` make a waiting skip final). A skipped row comes back to its own list on Undo. With nothing in either list it shows "All caught up: nothing to review
  and nothing due in the next 30 days" and a button back to the group. `FlatButton`, `PrimaryButton` and `SecondaryButton` take `compact` for small row buttons.

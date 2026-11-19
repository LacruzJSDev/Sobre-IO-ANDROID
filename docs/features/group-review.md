# Group review

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

The only place where pending due dates are confirmed or skipped.

- Group review (`ui/groupreview`, route `GroupReview(groupId)`, level 2 of the Group tab, `GroupReviewViewModel(groupId)` with
  `GroupRepository`, `GroupSummaryRepository`, `AccountRepository`, `EnvelopeRepository`, `CalendarRepository`; `GroupReviewScreen`,
  `ConfirmSheet`): the ONLY place where pending occurrences are reviewed. `ScreenHeader` with back arrow, title "To review" and the
  group as subtitle. One `SoftTile` per `pending_occurrences` row (kind-icon avatar in ink, name, "<account> - was due on <date>", `MoneyText`
  Flow signed by `DueOccurrence.signedCents`) with compact "Skip" (`SecondaryButton(compact)`) and "Confirm" buttons for any member
  of a live group (archived or pending-deletion groups are read only: no buttons). Confirm opens `ConfirmSheet` (`FlatBottomSheet`):
  `AmountField` with the expected amount, `DateFormTile` (due date, or today when it is later; `maxDate` = group today), and, for an
  expense whose envelope (not Unassigned) on an account inside the envelopes has less than the amount, the movement form's cover note and
  optional "Also cover from" `EntitySelect` (`p_cover` = that envelope's full balance). `confirm_occurrence` returns the created fact;
  the row goes and an Undo snackbar "<name> registered" voids that fact (`transactions` / `account_transfers` `voided_at`,
  `void_group_transfer` for a contribution), after which the database frees the due date. Skip hides the row at once, shows "<name>
  skipped" and writes `skip_occurrence` only when the snackbar ends without Undo (`UndoController`; `hiddenKeys` stop a reload bringing
  the row back meanwhile; the back arrow and `onCleared` make a waiting skip final). With nothing left it shows "All caught up: nothing
  to review" and a button back to the group. `FlatButton`, `PrimaryButton` and `SecondaryButton` take `compact` for small row buttons.

# Create and edit forms

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

The shared form layout and the group, account and envelope forms.

- Full-screen forms (`FormScaffold`: close X on the left, title, "Save" on the right with a spinner while saving; the
  fields scroll above the keyboard with `imePadding()`; system back = the X): `GroupForm(groupId?)` (`ui/groupform`),
  `AccountForm(groupId, accountId?)` (`ui/accountform`) and `EnvelopeForm(groupId, envelopeId?)` (`ui/envelopeform`),
  each with its own ViewModel (ids as Koin parameters by position, `params.get<String?>(1)`) that loads the item when
  editing, validates, calls the repository and sets a `saved` flag the Route turns into back. The list or settings
  screen reloads when shown again (`Refresh`). Forms are laid out with `ui/components/FormSections.kt`:
  `FormSection` (small muted title), `FormTile` (soft tile, label left, value or control right, optional note),
  `ChipFormTile` (label and a row of `FormChip`s for a short fixed choice, optional error under it) and
  `ExpandableFormTile` (header with a preview of the choice and an arrow; content opens under it, one open at a time).
  Group form: the avatar next to the name field; "Appearance" with Colour and Icon as separate expandable tiles
  (`ColorPicker`, `IconPicker` with paper cells); "Currency and period" with the currency (expandable chips when
  creating, read-only tile when editing), the period start day (a "-" / "Day N" / "+" stepper in its tile, 1-28, with
  a note that it only affects periods not created yet, D-29) and the time zone, a tile
  that opens `TimeZonePicker(current)` (`ui/timezone`: back arrow, autofocused `SearchField`, the current zone first and
  highlighted, then every `TimeZone.availableZoneIds` filtered case-insensitively, spaces matching underscores). The
  picked id travels back through a `pickedTimeZone` holder in `AppNavigation` (saved state, above both entries): the
  picker sets it and pops, the still-alive form ViewModel takes it once (`TimeZonePicked`) and clears it. Creating a
  group pops the form and opens the new group.
  Account form (`AccountFormViewModel`, loads members too): the avatar (colour + account icon) next to the name; "Appearance"
  (`ColorFormTile`, `IconFormTile` from `ui/components/AppearanceTiles.kt`, shared by the three forms); "Account": Kind
  (expandable, `FormChip`s), Holder (`EntitySelect` with "Joint" and the members, initial avatar and role label; hidden when the group has one active
  member), "Shares its money into envelopes" (a `Switch` tile with a note; on edit the note adds "From today; if it has a
  balance it moves between Unassigned and the money outside the envelopes") and, only when creating, the opening balance.
  Create sends every field to `create_account` (`p_in_envelopes`, `p_owner_user_id`, `p_color`, `p_icon`). Edit: when the switch
  differs from the stored scope, `changeAccountScope` (`change_account_scope`, a dated event from today) runs first and then
  `updateAccount` writes name, kind, holder, colour and icon (`in_envelopes` can never be updated directly, FZ017); a retry after
  a failed update does not repeat the scope call. FZ017 from the scope call shows "has movements dated today or later..." under
  the switch (`AccountFormUiState.scopeError`). Envelope form (`EnvelopeFormViewModel`): avatar + name, "Appearance" (envelope icons), "Envelope": the type as a
  `ChipFormTile` of four chips (Monthly spending, Bills, Savings, Free; D-46), the holder ("Common" or a member, same visibility
  rule) and the amount field only for Monthly (monthly target, required above 0) and Savings (monthly contribution, optional);
  Bills and Free send 0. For Monthly and Savings a "Start next period" `Switch` tile (D-53) is shown when creating, when the
  stored target was 0 or when the stored `targets_from` is still after today: on sends `targets_from` = the day after
  `current_period`'s end (dates from `CalendarRepository.getToday`/`getCurrentPeriod`; a failed read hides the switch), off
  on an envelope that started later sends today, otherwise nothing is sent and the database keeps its rule. The Envelopes rows
  of an envelope whose target starts later show "Target X a month from <date>" instead of the bar (`Envelope.targetStartsAfter`,
  `EnvelopesUiState.today`). An `InlineNotice` at the very end explains the chosen type. Create inserts and edit updates all fields of
  `envelopes` (`EnvelopeDraft`: `purpose` and the kind that follows it, savings is `saving` and the rest `spending`); the type can
  be changed when editing, and FZ021 (leaving Bills while funded commitments point to it) shows "It has commitments with a
  reserve..." under the type (`purposeError`). Commitment form: when "Set aside each period" is on, the envelope `EntitySelect`
  lists only Bills envelopes (and no "Unassigned" for a contribution), an `InlineNotice` says why, or says to create a Bills envelope
  first when there is none; saving with the reserve on and no Bills envelope chosen fails validation (`envelopeError`); the
  database's FZ021 maps to the same message. Unfunded commitments take any spending envelope.
- `ui/components/DateFormTile`: a `FormTile` that opens a flat Material 3 `DatePickerDialog`; optional clear button for optional dates.

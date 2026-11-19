# Group shell

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

The group's four tabs and the header they share.

- Group shell (phase 6, part E): `GroupRoute` keeps only the group (`GroupViewModel`), the selected tab and the Group tab's
  badge (it reads `GroupSummaryViewModel`, refreshes it when shown, on the Group tab and on a remote change). Each tab is its
  own route in its package (`GroupSummaryTabRoute`, `AccountsTabRoute`, `EnvelopesTabRoute`, `MovementsTabRoute`): it gets the
  loaded `Group` and a `TabHeader` (`ui/components/ScreenHeader.kt`: the shell's title, avatar and menu button, with the tab's
  actions and refresh bar), creates its ViewModel with `koinViewModel` (kept by the group's entry, so a tab keeps its state
  while another is shown), sends it `GroupLoaded` before its first frame, refreshes it each time it is shown and on a remote
  change, and wires its clicks to the navigation lambdas. `GroupScreen` is stateless: `tabContent(tab, group, header)`; while
  the group loads or fails it draws the header alone. Header actions: on the Group tab a gear (`ic_settings`, visible to
  everyone) that opens the group settings (`GroupSettings(groupId)` route, level 2; the Group route reloads the group when it
  is shown again); on Movements search, export and a "+" that opens the movement form (Accounts and Envelopes have their own, see
  [accounts](accounts.md) and [envelopes](envelopes.md)). The export sheet (`ExportHost`) lives in `MovementsTabRoute`.

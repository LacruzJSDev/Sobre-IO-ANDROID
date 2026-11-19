# Group settings

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

Details, tags, members and invitations of a group.

- Group settings (`ui/groupsettings`, `GroupSettingsViewModel` with the group id as Koin parameter): compact, sections
  separated by spacing, no bottom buttons (archive, delete and leave are Groups list swipes). The header has a pencil on the
  right (owner and admin, live group), in the same place as the gear that opened the screen, which opens the full-screen
  group form (`GroupForm(groupId)`). Details: avatar, name and currency ("Archived" next to it when archived), then the
  period start day and time zone as `FormTile`s; the screen reloads when it is shown again. Members (`MemberRepository`): initial avatar, name ("You"
  marker) and role label; an owner swipes a member's row for Role (sheet with owner/admin/member chips,
  `account_group_members.role` update; FZ009 shows inside the sheet; also on their own row; a "Role changed" snackbar follows, without Undo) and, on other members' rows,
  Remove (hidden at once with Undo; `left_at` is written only when the snackbar ends without Undo). Invitations (owner
  and admin, live group), the "+" on the header opens a sheet to choose the role (Member default, Admin; never owner),
  creates the `invitations` row and shows the code with a copy button. Two small sections, each hidden when empty:
  Pending (status pending and not expired by the clock: code, role, "Expires on <date>", tap copies, swipe Revoke with
  Undo, `status = 'revoked'` sent when the snackbar ends without Undo) and Accepted (who, resolved from the members list
  or "Former member", role and "Accepted on <date>"; read only). The query loads `status in (pending, accepted)`.
  Members read Details and Members only. `GroupSettingsUiState.hasSeveralMembers` (and `List<Member>.hasSeveralMembers()`)
  tell Accounts and Envelopes whether to ask for a holder.

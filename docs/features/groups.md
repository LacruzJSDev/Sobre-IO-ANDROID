# Groups

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

The list of the user's groups: create, join, archive, delete and leave.

- Groups screen: header with the title and a "+" (new group), the list of groups as the only scrolling part, and at the
  very bottom, with no divider, one row of icon buttons spread evenly (`IconActionButton`: join with a code, profile and
  sign out in red; each has an accessible name that also shows on long press). Rows show an `EntityAvatar` with the group's colour and icon. Each row swipes left
  (`SwipeActionsRow`, icon actions): Archive and Delete (owner) and Leave (everyone except the sole active owner). There is no Edit
  here: editing lives only in the group settings. The "+" opens the full-screen group form (`GroupForm(null)`, see
  Group form below). Archive, Delete and Leave
  are never confirmed: an "Undo" snackbar follows (`UndoController`, see the Rules of `AGENTS.md`). Archive sets `archived_at` at once (the
  row moves to the Archived section) and Undo reopens it. Owner delete calls `request_group_purge` immediately and Undo
  calls `cancel_group_purge`. Leave is deferred (rejoining needs an invitation): `account_group_members.left_at` is
  updated only when the snackbar ends without Undo. `getGroups` also embeds the active owners (`owners:account_group_members(user_id)`,
  filtered with `owners.role=eq.owner` and `owners.left_at=is.null`, alias of the same table) into `Group.activeOwnerCount`;
  `Group.isSoleOwner` hides Leave, and FZ009 stays as a fallback (the row comes back with the error). Reopen and Cancel
  deletion also show a snackbar with Undo.
  `SupabaseGroupRepository.getGroups` returns every group of the user with its role (PostgREST embedding
  `account_group_members!inner(role)`, filtered to the user and `left_at is null`); the ViewModel splits them: live
  groups in `groups`, and `archivedGroups` (archived first, then those with `purge_requested_at`) for a collapsed
  "Archived (N)" section at the end of the list. Archived rows say "Archived" and open read only; pending rows say
  "Deleted on <purge_requested_at + 7 days>" and cannot be opened. Owners swipe Reopen (`archived_at` to null) or Cancel
  deletion (`cancel_group_purge`); `getGroup` still hides groups pending deletion. `Group.canEdit` is false for archived
  and pending groups, so Accounts and Envelopes become read only.

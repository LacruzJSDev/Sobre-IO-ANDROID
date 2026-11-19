# Profile and account deletion

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

The user's name, the accessibility switch and deleting the account.

- Profile: the name is a field with a Save button (not blank, at most 100); `AuthRepository.updateDisplayName` updates
  `profiles.display_name` (what other members see) and the session's `name` metadata (what the app shows), and the
  screen shows the saved name at once. The email is read-only. At the very bottom a `DestructiveButton` "Delete account"
  pushes `DeleteAccount` (the column scrolls).
- Delete account (decision D-51; `ui/deleteaccount`, route `DeleteAccount`, level 2 of Groups above Profile,
  `DeleteAccountViewModel` with `AccountDeletionRepository`): `ScreenHeader` with back and title, an intro line and, from
  `account_deletion_preview()` (`DeletionPreviewItem`, `DeletionOutcome` Blocked / Delete / Leave, DTO `DeletionPreviewDto`),
  sections hidden when empty, each row an `EntityAvatar` (group colour and icon) and the name: first "First name another owner"
  (blocked: the only owner of a group with other members; a row opens that group's `GroupSettings(groupId)`, where roles change,
  and the preview reloads when the screen is shown again), then "Deleted for good" (the only member) and "You will leave" (with the
  note that their movements stay, signed as "Deleted user"). Then a muted line (email and sign-in removed, cannot be undone), a
  `Switch` tile "I understand this cannot be undone" and a `DestructiveButton` with `isLoading`, enabled only with the switch on,
  the preview loaded and nothing blocked. The switch is the confirmation: no dialog. A load failure shows a retry; a delete failure
  shows under the switch and reads the preview again. `SupabaseAccountDeletionRepository.deleteAccount` calls the Edge Function
  `delete-account` (`supabase-kt` `functions-kt`, installed in `buildSupabaseClient`, POST with the user's JWT, no body) and, on
  success, clears the user's `LastGroupStore` and `LastAccountStore.clear(userId)` entries and closes the session locally
  (`signOut(SignOutScope.LOCAL)`: the user no longer exists, so the server cannot revoke it); the session-driven root then shows the
  sign-in screens. Function errors: the function answers `{"code","message"}`; a `RestException` is mapped by
  `ErrorMapper.fromFunctionBody` (reads `code` from `RestException.description`, else from the response body) with the same rules as
  RPC codes (`auth_delete_failed` and unreadable bodies are `Unknown`, retry is safe); `FZ023` ("a group blocks it") has its own message.

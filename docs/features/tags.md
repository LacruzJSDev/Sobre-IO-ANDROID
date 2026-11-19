# Tags

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

The group's tags: create, edit, merge, archive, and their names.

- Tags (decision D-49 in the DB repo; `ui/tags`, route `Tags(groupId)`, level 3 of the Group tab, `TagsViewModel(groupId)` with
  `TagRepository` and `GroupRepository`; `TagsScreen`, `TagSheet`, `MergeSheet`): opened from the group settings'
  "Tags" section: its title carries the count ("Tags · N") and a pencil, and a soft card previews the tags themselves as
  coloured chips (up to 8, then "+N more"; `GroupSettingsUiState.tags`); the card and the pencil open the screen. With no tags
  the title stands alone with a "+" (any member of a live group, `canManageTags`) that opens it with the create sheet
  (`Tags(groupId, startWithCreate = true)`) and a line explains what tags are for; every member sees the section. Any member of a live group manages tags (`TagsUiState.canEdit`). `ScreenHeader` with back, title, group
  subtitle and "+". Rows come from `tag_usage` (`TagRepository.getUsage`: live and archived tags with the count of live
  movements, counted by the database): a `TagDot` (colour), name and "N movements". Live rows swipe Edit, Merge
  (`mergeAction`, hidden with a single live tag) and Delete (archive at once, Undo unarchives); a collapsed "Archived (N)" section
  with Reopen (a clash with a live name shows `AppError.DuplicateName`). "+" and Edit open `TagSheet` (name `FormTextField`
  at most 50, the `TagColorPicker` swatches, Save; a duplicate name shows under the field). Merge opens `MergeSheet` ("Merge X
  with…": an `EntitySelect` of the other live tags and a note that X will be archived), calls `merge_tags` and ends with a plain
  notice "Tags merged" (`UndoController.notify`, no Undo: it cannot be undone). The tag palette is `ui/theme/TagColor` (10 soft
  colours, light hex stored in `tags.color`, a deeper dark variant, names `tag_color_<key>`), separate from `EntityColor`;
  a new tag starts on `TagColor.Default`.
- Tag names: the database stores them trimmed with inner whitespace collapsed and keeps them unique among live tags ignoring
  case and accents. `domain/TagMatching.kt` mirrors that key (`normalizeTagKey`, `normalizeTagName`, unit tested) and drives the
  picker through `tagPickerOptions(tags, query)`: rows whose key contains the text, a `suggestion` (the same tag, or one within one
  edit when both keys have at least 4 letters) and `createName` (null when the key matches a live tag exactly, or the name
  is over 50 characters).

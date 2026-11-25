# Sobre-IO App · Agent guide

Android and iOS client of Sobre-IO, an envelope-budgeting app for households. Phone, tablet and foldable with one adaptive UI.
All business rules live in the Supabase database (repository `Sobre-IO-DB`, its `AGENTS.md` lists the client API);
this app displays state and calls that API. UI, ViewModels, repositories and models are written once in Kotlin
Multiplatform and shared by both platforms.

Human documentation (Spanish): [README.md](README.md).

## Stack

| Concern | Choice |
| --- | --- |
| Language / UI | Kotlin 2.4 Multiplatform, Compose Multiplatform 1.12 (Material 3 and `material3-adaptive` JetBrains builds) |
| Targets | Android (minSdk 24, targetSdk 37), `iosArm64`, `iosSimulatorArm64` (iOS 18.5+, the version Compose's bundled ICU data is built for) |
| Architecture | MVVM with repositories, UI state in `StateFlow`; JetBrains `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` |
| Navigation | Navigation 3 (`androidx.navigation3-runtime` 1.2 + JetBrains `navigation3-ui`), keys are `@Serializable` `NavKey`s registered in `NavSavedStateConfiguration`; ViewModels scoped per entry with `lifecycle-viewmodel-navigation3` |
| Resources | Compose resources (`composeResources`): strings, fonts and icons, generated `Res` object |
| DI | Koin 4.2 (`koin-core`, `koin-compose-viewmodel`; `koin-android` only on Android), started by the shared `initKoin` |
| Backend client | `supabase-kt` 3.8 (`auth-kt`, `postgrest-kt`, `functions-kt`, `realtime-kt`) on Ktor (OkHttp on Android, Darwin on iOS) + `kotlinx.serialization` JSON |
| Icons | `org.jetbrains.compose.material:material-icons-extended` 1.7.3 (only the ~130 distinct icons of `IconCatalog` are referenced, so R8 strips the rest) |
| Local storage | `com.russhwolf:multiplatform-settings-no-arg` 1.3 (last group per user, last account, bank CSV formats per account); `kotlinx-datetime` 0.8 (device time zone) |
| Files | Android: `androidx.activity:activity-compose` (in `:shared` androidMain) for the system document picker; iOS: `UIDocumentPickerViewController` from Kotlin (no Swift) |
| Tests | `kotlin.test`, `kotlinx-coroutines-test` (`testing/MainDispatcherTest`), run as Android host tests |
| Build | Gradle Kotlin DSL, version catalog `gradle/libs.versions.toml`, AGP 9.4 (`com.android.kotlin.multiplatform.library` for `:shared`), Java 11; iOS project generated with XcodeGen |

No Room, no offline cache, no custom server. The Ktor version must match the one `supabase-kt` is built against.

## Current state (keep this section true)

- Package and application id: `dev.lacruzjs.sobreio` (shared module namespace `dev.lacruzjs.sobreio.shared`).
- Two Gradle modules: `:shared` (all the code) and `:app` (Android application shell). `iosApp/` is the iOS shell.
- Features: sign-in (email and password with 6-digit codes, Google on Android and iOS); Groups (create, join with a code,
  archive, delete, leave); the group shell with the Group, Accounts, Envelopes and Movements tabs; account, envelope and
  commitment details; commitments, movements, moves between envelopes and the bank import; group settings with members,
  invitations and tags; profile and account deletion. Root navigation follows the session (`SessionViewModel`).

What the app does and how, in more detail, lives next to this file and follows the same rule (keep it true, change it
in the same commit as the code):

- `docs/architecture.md`: navigation and back stack, adaptive layout, configuration, data loading (cache, `revalidate`,
  skeletons, refresh bar), calendar, realtime, errors, money formatting and colours, look and feel, charts, help texts, CI.
- `docs/features/<screen>.md`, one per screen or flow: [auth](docs/features/auth.md), [groups](docs/features/groups.md), [group-shell](docs/features/group-shell.md), [group-tab](docs/features/group-tab.md), [group-review](docs/features/group-review.md), [accounts](docs/features/accounts.md), [envelopes](docs/features/envelopes.md), [account-detail](docs/features/account-detail.md), [envelope-detail](docs/features/envelope-detail.md), [envelope-transfers](docs/features/envelope-transfers.md), [commitments](docs/features/commitments.md), [movements](docs/features/movements.md), [movement-form](docs/features/movement-form.md), [forms](docs/features/forms.md), [group-settings](docs/features/group-settings.md), [tags](docs/features/tags.md), [profile](docs/features/profile.md), [bank-import](docs/features/bank-import.md).

## Target structure

```
settings.gradle.kts           include(":shared", ":app")
shared/                       Kotlin Multiplatform library (everything shareable)
  src/commonMain/kotlin/dev/lacruzjs/sobreio/
    App.kt                    Root composable: SobreioTheme { NavigationWrapper() }
    di/                       Koin modules (one per layer) and initKoin
    navigation/               Routes (NavKey), NavSavedStateConfiguration, NavigationWrapper, adaptive scaffold
    ui/
      theme/                  Colors, typography, shapes
      format/                 Money and date formatting (Spanish locale)
      components/             Reusable composables (amount text, avatar, bottom sheet keypad)
      <feature>/              <Feature>Screen.kt, <Feature>ViewModel.kt, <Feature>UiState.kt, <Feature>Event.kt
    data/
      remote/                 supabase-kt client setup, @Serializable DTOs, RPC wrappers
      repository/             <Thing>Repository (interface) + Supabase<Thing>Repository (implementation)
    domain/                   Plain data classes (amounts as Long cents) and pure functions
  src/commonMain/composeResources/   values/strings.xml (English), values-es-rES/strings.xml (Spain Spanish), drawable/, font/
  src/androidMain/kotlin/     Android actuals: Credential Manager Google sign-in, supabase URL
  src/iosMain/kotlin/         iOS actuals: MainViewController, setUpKoin, supabase URL, Google sign-in bridge (`IosGoogleSignInProvider` implemented in Swift)
  src/commonTest/kotlin/      Unit tests with fakes
app/                          Android application only: MainActivity, SobreioApplication, manifest, launcher icons,
                              network security configs, res/xml/locales_config.xml (hand-written language list)
iosApp/                       project.yml (XcodeGen) + generated iosApp.xcodeproj + SwiftUI shell (iOSApp.swift, ContentView.swift)
```

Features follow the product navigation: `auth`, `groups`, `group` (shell), `groupsummary`, `groupreview`, `groupsettings`, `tags`, `groupform` / `accountform` / `envelopeform` / `timezone`, `profile` / `deleteaccount`, `accounts`, `envelopes`, `envelopedetail`, `envelopetransfers`, `movements`, `bankimport`,
plus `movementform` (the new / edit movement form).

## Rules

- Screen structure follows the workspace `docs/agents/navigation.md`; read it before adding or changing a screen.
- Before changing a screen, read its `docs/features/<screen>.md`: it says what the screen shows, what it reads and
  writes, and the decisions behind it.
- Editing a live movement is a plain update validated by the DB guards and recorded in `audit_log`; deleting is voiding.
- Common first: write code in `shared/src/commonMain`. Platform code goes only in `androidMain` / `iosMain`, behind an
  `expect`/`actual` declaration or an interface that common code owns (for example `rememberGoogleSignInLauncher`,
  `supabaseUrl`). `:app` and `iosApp` stay thin shells: no screens, no business logic.
- No `java.*`, `android.*` or `androidx.compose.ui.res` in `commonMain`; check with
  `./gradlew :shared:compileKotlinIosSimulatorArm64` when you touch it.
- Resources (strings, fonts, drawables) live in `shared/src/commonMain/composeResources` and are used through the
  generated `Res` object (`stringResource(Res.string.x)`). Vector drawables there must use literal colors
  (`#FFFFFFFF`), not `@android:color/...`: the Compose parser does not know Android references. UI state holds a
  `StringResource?` for messages; ViewModels still import no Compose UI, only the generated `Res` and the
  `StringResource` type (the same role `R.string` ids had).
- Loading: every ViewModel that reads loads through `revalidate` (see "Data loading" in `docs/architecture.md`) and every new
  read a screen needs gets a cached override in `CachedRepositories.kt`; a new read-only RPC goes into `READ_ONLY_RPCS`.
  No full-screen `CircularProgressIndicator`: skeleton for a first load, `isRefreshing` on the header for the rest.
- Shared pieces live in `ui/components` or `ui/validation`, never inside one screen's package for others to import:
  `SoftTile` (the soft surface a section sits on), `ListSectionTitle` (the muted title that splits a list), `Envelope.displayName()`
  (the translated name of the system envelope "Unassigned", stored in Spanish; never compare `kind == System` by hand to name
  it), `ui/validation/Validation.kt` (`validateRequired`, `validateName(name, maxLength)`, `validatePositiveAmount`, `validateTagName`,
  email, password, 6-digit code) and `ui/format/MoneyInput.kt` (`parseMoneyInput` and its inverse `formatMoneyInput`, the text
  that prefills an amount field).
- A form's rules are pure functions of its UI state in `<feature>/<Feature>Rules.kt`, each with a `<Feature>RulesTest`: what
  Save finds wrong (`validate<Feature>Form` returning `<Feature>Errors` and `withErrors`), the draft it sends (`…DraftOf`), how a
  choice changes the other fields and how a stored row fills the form (`MovementFormRules`, `CommitmentFormRules`,
  `BankImportRules`; the movement form's tag step is `TagSync`). The ViewModel keeps the flow: state, coroutines, repositories.
- A screen receives `state` and `onEvent`; it never calls a repository, Supabase or the back stack directly.
- ViewModels depend on repository interfaces only; tests use fakes.
- Writes that change money go through the database RPCs (`create_transaction`, `create_account_transfer`, …).
  Never chain several table writes to emulate one operation, and never recompute balances in the app.
- Errors: data maps any failure to an `AppError` in `data/remote/ErrorMapper.kt` (database codes `FZ001`–`FZ024` become
  `BusinessRule`); ui maps `AppError` to string resources in `ui/error/ErrorMessages.kt`. Each happens in one place.
  Repository implementations wrap calls with `safeCall`.
- Every standalone money figure is a `MoneyText` with its role (Flow, Transfer, Balance, Plain); never `formatMoney` plus a
  hand-picked colour. Inflow green / outflow ink / transfer muted / negative balance red; the colour-blind preference
  swaps green to blue and red to orange. Amounts inside a sentence stay plain `formatMoney` text.
- Amounts are `Long` cents everywhere; format them only in the UI with `formatMoney(…, languageTag)`, passing the
  language tag of the current configuration (`1.298,15 €` in Spanish, `€1,298.15` in English).
- The app is bilingual. Every user-visible string is a resource with the same key in `composeResources/values/strings.xml` (English,
  the default for any other device language) and `composeResources/values-es-rES/strings.xml` (Spanish from Spain: the region makes the per-app language setting say
  "Español (España)", and devices in other Spanish locales fall back to it). No literal user text in
  composables, ViewModels or anywhere in Kotlin, not even a `contentDescription`.
  Add both translations in the same change; `StringsParityTest` fails otherwise (lint no longer sees Compose resources). Android 13+ shows the per-app language setting from
  `app/src/main/res/xml/locales_config.xml`, which is written by hand (the Android resources no longer hold the strings):
  update it, and `CFBundleLocalizations` in `iosApp/project.yml`, when a language is added. Code, comments and test names are English.
- Transient UI (open swipe rows, chart tooltips) closes on any press outside it, through the app-wide outside-press host;
  never re-implement it per screen. The `OutsidePressHost` that `App` installs once at the root (`OutsidePressHostProvider`,
  `ui/components/OutsidePressHost.kt`) sees every press first; an open `SwipeActionsRow` registers with it, and a chart
  uses `Modifier.closeOnOutsidePress(active) { clear }`. The press is not consumed, so the touched control still works.
  Lists keep at most one row open.
- Figures: two or more related amounts are ONE `StatCard` (`ui/components/StatCard.kt`: rows of label left and
  `MoneyText` right, optional emphasised result row, optional title and shortfall warning line), never one tile per figure.
  `FormTile` is for inputs only.
- Funded commitments only point to Recibos (Bills) envelopes; the envelope type decides its fields and its progress bar (D-46).
  The app never decides progress: it draws what `envelope_progress` returns.
- Search boxes are a `SearchField` (`ui/components/SearchField.kt`: 48 dp pill, magnifier, placeholder instead of a floating
  label, clear X), never a `FormTextField`; pass `containerColor = background` on a grey tile.
- Notices: a note that explains a consequence or a state (outside envelopes, cover, automatic move, locked fields, final
  warnings) is an `InlineNotice` (`ui/components/InlineNotice.kt`, tones Info / Warning / Suggestion, optional title and
  actions). `FormNote` is only for a one-line caption directly under a field.
- Text input: fields are filled soft fields (surfaceContainerHigh, no outline or indicator, `softTextFieldColors()`, shape `tileShape()`), never outlined. Every field is a `FormTextField` (keyboard closes on Done, Next moves to the next field) and every
  money amount an `AmountField` (numeric pad with decimal separator). The input components (`FormTextField`,
  `AmountField`, `LargeAmountField`, `PasswordField`) own a `TextFieldState` kept in step with the outside String by
  `rememberSyncedTextFieldState` / `TextSync` (`ui/components/TextSync.kt`): never hold the text in a value-based field
  fed from a `StateFlow`, the late round trip makes the cursor jump. Screens and ViewModels keep `value` / `onValueChange(String)`.
  Amounts are always typed positive (`AmountField` rejects "-"); a real negative balance is chosen with the "In credit /
  Debt" chips (`BalanceSignChips`): the account form's opening balance (`openingBalanceIsDebt`, Debt by default for a
  credit card until the user taps a chip) and Compare with bank; the ViewModel applies the sign. The 6-digit codes use
  `digitsOnly(maxLength)`. Names and notes pass `capitalization = KeyboardCapitalization.Sentences` to `FormTextField`
  (the default is none, for emails, codes and searches). Screens with fields add `Modifier.clearFocusOnTap()` (already in `FormScaffold`, `AuthScaffold`,
  `FlatBottomSheet` and Profile) so a tap outside any field closes the keyboard.
- Every create/edit form uses the same layout, built only from `ui/components/FormSections.kt` and `FormScaffold`:
  avatar (colour + icon) next to the name field at the top, then `FormSection`s of `FormTile`s. A section is ONE segmented block: tiles 2 dp apart,
  outer corners 14 dp (the section clips its group with `shapes.large`), inner corners 4 dp (`LocalTileShape`, read by every tile through `tileShape()`,
  which is `shapes.medium` outside a section; a new tile-like component must use it), 24 dp between sections; captions and errors between tiles keep 4 dp
  of vertical padding. A section whose content is not tiles (a figure, plain rows, a chart) passes `segmented = false`; choices with many
  options (colour, icon, kind, currency, holder) are `ExpandableFormTile`s, one open at a time (header and content are ONE grey surface, `ExpandableSurface`, the building
  block of `ExpandableFormTile`, `EntitySelect`, `TagPicker` and `CollapsibleListSection`: no second card, no gap, no border); long lists open a
  picker screen with search (`TimeZonePicker`). Settings screens edit with a pencil `IconActionButton` at the top
  right, never a large button.
- Create and edit are full-screen forms (Navigation 3 routes with their own ViewModel, `FormScaffold`); bottom sheets are
  only for short one-step actions (Assign, role change, invite plus code, join with a code).
- Choosing one thing of the group (account, envelope, group, member) is an `EntitySelect` (`ui/components/EntitySelect.kt`: avatar + name,
  up to 8 options the list opens inside the same grey surface, under a divider, with its own scroll and no search; with
  more the tap opens a bottom sheet with search (`EntityPickerSheet`); a select that already sits in a bottom sheet passes
  `insideSheet = true` and keeps a long list inline with a search box, never a sheet over a sheet; `EntityOption` with optional special first options such as
  "Unassigned" via `unassignedOption`; filter in `filterEntityOptions`); `FormChip` only for fixed short enums (kind, unit, role, currency, period).
- A detail screen shows, never manages: a section of items that are edited elsewhere shows the latest few, read only, and
  an icon at the right of its title (`FormSection(action = …)`, an `IconActionButton` of 24 dp) opens the screen where they
  are all listed and managed. No "See all" button under a section, and no item managed in two places.
- Long lists inside a detail screen are `CollapsibleListSection`s (`ui/components/CollapsibleListSection.kt`: count in the header, rows inside the same surface under the divider
  with thin inset lines between them and no background of their own, bounded height with their own scroll, paginated with `onLoadMore`, `isLoadingMore` and `loadMoreFailed`).
- Swipe actions are icons without text, defined once in `ui/components/SwipeActions.kt` (factory functions: Edit pencil,
  Archive, Reopen unarchive, Cancel deletion restore-from-trash, Change role admin shield, Assign move-to-inbox in soft
  grey, Ignore eye-off in soft grey (a bank statement row), Leave logout in the muted orange `ColorScheme.leaveAction` from `ui/theme/RoleColors.kt`, Delete bin in red,
  Remove member and Revoke invitation in red) and used by every list; never build a `SwipeAction` by hand. Each has an
  accessible label shown on long press (`LabelTooltip`, Material3 `TooltipBox`). Every swipe action (and the role
  change) that does not open a screen or sheet shows a bottom snackbar saying what happened, with Undo when possible
  (`UndoController.show`, or `notify` for a plain notice).
- Lists: rows swipe left for Edit/Delete with `SwipeActionsRow`; creation is the "+" in the header; there are no
  per-item settings screens. Destructive actions are never confirmed with a dialog: the item is hidden at once (soft
  delete) and an Undo snackbar follows. ViewModels use `UndoController` (`ui/undo`): `show(UndoableAction(commit, undo,
  restore))`; `commit` runs when the snackbar ends, another destructive action starts, the screen is shown again or the
  ViewModel is cleared; failures restore the row and show the error. The screen shows `UndoMessage` with `UndoSnackbarHost`.
  A commit made as a screen closes (`commitDetached`) is registered in `PendingUndoCommits`, and the screens that reload
  money when shown (Groups, Accounts, Envelopes, Group tab, account and envelope details, movements) call
  `PendingUndoCommits.await()` before reading, so they never show figures from before the delete. Navigation 3 clears a
  popped entry's ViewModel only after its exit animation, so a level 2 screen with deletes commits on back itself: its
  `BackClicked` calls `commitDetached` and its Route adds `CommitUndoOnBack(pending = state.undoMessage != null)`, which
  sends system back and the gesture the same way while a delete waits (envelope moves, account detail, review).
- Adaptive layout uses `NavigationSuiteScaffold` and our list-detail scene `AdaptiveListDetail` (one scene per root, so panes move instead of scenes cross-fading) (see "Adaptive layout" in `docs/architecture.md`); decide widths by the pane's own width (`BoxWithConstraints`, `contentMaxWidth`), never the window's; check phone, tablet and foldable previews or emulators.
- Connection settings come from `local.properties` (git-ignored) through the generated `AppConfig`:
  `SUPABASE_URL`, `SUPABASE_URL_IOS` (optional), `SUPABASE_PUBLISHABLE_KEY` and `GOOGLE_WEB_CLIENT_ID` (the Google *web* client id, optional; Google sign-in is hidden when blank). Only the publishable (anon) key ever ships in the app;
  the secret / service_role key never appears in this repository.
- From the emulator, the local Supabase stack is at `http://10.0.2.2:54321`; it is also the default when
  `SUPABASE_URL` is missing. The key has no default: the client is created lazily by Koin and fails on first use
  without one. Changing `local.properties` needs a rebuild.
- The iOS simulator shares the Mac's network, so there the local stack is `http://127.0.0.1:54321`. `SUPABASE_URL_IOS`
  sets it; when missing it is `SUPABASE_URL` with `10.0.2.2` replaced by `127.0.0.1`. A physical iPhone needs the Mac's
  LAN address (or the hosted project) in `SUPABASE_URL_IOS`.

## Navigation pattern (user actions and results)

- A click that only navigates (link, back) is intercepted by the screen's `XxxRoute`, which calls its navigation lambda;
  the ViewModel ignores it. The Route is the only place that holds the back stack lambdas, Context or platform APIs.
- A result that leads to another screen (sign-up created -> code screen) is state: the ViewModel sets a field such as
  `navigateToConfirm: String?`; the Route reacts in a `LaunchedEffect`, calls the lambda and sends `NavigationHandled`
  to clear it, so rotation does not navigate twice.
- A change of session is never handled by a screen: the root swaps `AuthNavigation` / `AppNavigation`.
- Route arguments reach ViewModels as Koin parameters (`koinViewModel { parametersOf(...) }`), because Navigation 3 keys are not in a `SavedStateHandle`.

## Commands

```bash
./gradlew :app:assembleDebug                 # build the debug APK
./gradlew :shared:testAndroidHostTest        # unit tests (commonTest, run on the JVM)
./gradlew :app:installDebug                  # install on the connected device or emulator
./gradlew :app:lintDebug                     # Android lint
./gradlew :app:checkIosVersion              # iOS version equals sobreio.version (gradle.properties)
tools/release-notes.sh 1.0.0                 # notes of one version from CHANGELOG.md (or: unreleased)
./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64   # iOS compile check, no Xcode UI needed
cd iosApp && xcodegen                        # regenerate iosApp.xcodeproj after editing project.yml
```

iOS app: open `iosApp/iosApp.xcodeproj` in Xcode, pick your own Personal Team under Signing & Capabilities (the
repository commits none), choose a simulator or iPhone and press Run. Profiles of a free Apple ID expire after 7 days;
run the app again from Xcode to renew it. From the terminal, `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp
-sdk iphonesimulator -destination 'id=<simulator udid>' build` builds without signing.

## Conventions

- Kotlin official style; names: `XxxScreen`, `XxxViewModel`, `XxxUiState`, `XxxEvent`, `XxxRepository`.
- No `!!`; errors become UI state. State classes are immutable.
- Commits: Conventional Commits in English with scopes `app`, `ui`, `data`, `nav`, `build`; for example `feat(ui): add movements list`.
- Several agents work on this repository at once: work in your own git worktree, never in the main checkout. From
  this folder, `git worktree add ../worktrees/app-<topic> -b <type>/<topic> origin/main`, copy `local.properties` into it,
  and commit only to your own branch. Never switch branches, stash or commit in a checkout you did not create. Before
  deleting a branch, `git log origin/<branch>..<branch>` must be empty; after the merge, `git worktree remove` it and bring
  the main checkout up to date when it is on `main` (`git pull --ff-only`), the one thing done there, so the author builds
  the merged code.
- Versions and release notes (full procedure in the README section "Versiones"): SemVer, written only in `sobreio.version`
  (`gradle.properties`); `versionCode` is major * 10000 + minor * 100 + patch, and `iosApp/project.yml` plus
  `iosApp/iosApp/Info.plist` carry the same pair (`./gradlew :app:checkIosVersion`, run by CI). Change the version only in
  a `build(app): release vX.Y.Z` PR. Every `feat` or `fix` PR the user notices adds one Spanish line, written for the user,
  under "Sin publicar" in `CHANGELOG.md` (Novedades / Mejoras / Arreglos); a fix of something never released adds none
  and gets the `no-changelog` label (`.github/workflows/changelog.yml` enforces it). The GitHub Release text is
  `tools/release-notes.sh X.Y.Z`. Never move or delete a published tag.
- When you add a dependency, add it to the version catalog and to the "Stack" section above (or `docs/architecture.md`).
- When you add or change a screen, update its `docs/features/<screen>.md` (a new screen gets a new file, listed in
  "Current state") in the same change; a layer rule goes into this file, a cross-cutting mechanism into `docs/architecture.md`.

## Domain names

Use the English names: group, member, account, envelope, movement, transaction (income / expense / adjustment),
account transfer, envelope transfer, cover, period, commitment, occurrence, tag. "Sin asignar" is the Unassigned
system envelope. The full glossary is in the workspace `docs/agents/glossary.md`.

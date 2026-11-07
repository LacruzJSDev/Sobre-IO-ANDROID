# Sobre-IO App · Agent guide

Android client of Sobre-IO, an envelope-budgeting app for households. Phone, tablet and foldable with one adaptive UI.
All business rules live in the Supabase database (repository `Sobre-IO-DB`, its `AGENTS.md` lists the client API);
this app displays state and calls that API.

Human documentation (Spanish): [README.md](README.md).

## Stack

| Concern | Choice |
| --- | --- |
| Language / UI | Kotlin 2.4, Jetpack Compose (BOM 2026.09), Material 3 + `material3-adaptive` (layout, navigation3, navigation suite) |
| Architecture | MVVM with repositories, UI state in `StateFlow`; `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` |
| Navigation | Navigation 3 (`androidx.navigation3` 1.2), keys are `@Serializable` `NavKey`s; ViewModels scoped per entry with `lifecycle-viewmodel-navigation3` |
| DI | Koin 4.2 (`koin-android`, `koin-androidx-compose`), started in `SobreioApplication` |
| Backend client | `supabase-kt` 3.8 (`auth-kt`, `postgrest-kt`) on Ktor OkHttp + `kotlinx.serialization` JSON |
| Google sign-in | `androidx.credentials` 1.6 (`credentials`, `credentials-play-services-auth`) + `googleid` 1.2: Credential Manager returns a Google ID token that is handed to Supabase |
| Tests | JUnit 4, `kotlinx-coroutines-test` (`testing/MainDispatcherRule`) |
| Build | Gradle Kotlin DSL, version catalog `gradle/libs.versions.toml`, AGP 9.4, minSdk 24, targetSdk 37, Java 11 |

No Room, no offline cache, no custom server. The Ktor version must match the one `supabase-kt` is built against.

## Current state (keep this section true)

- Package and application id: `dev.lacruzjs.sobreio`.
- Authentication is implemented (RF1, D-43); there are no other features yet:
  - Screens in `ui/auth/<screen>/`: `signin`, `signup`, `confirm` (6-digit email code with a 60 s resend cooldown),
    `recovery` (email step, then code + new password step); shared pieces in `ui/components/`
    (`AuthScaffold`, `LoadingButton`, `FormTextField`, `PasswordField`, `ErrorText`). Field validation is UI-level only (`ui/auth/AuthValidation.kt`).
  - `ui/session/SessionViewModel` exposes the `AuthState` (Loading / SignedOut / SignedIn); `navigation/NavigationWrapper`
    shows `AuthNavigation` (starts at `SignIn`) or `AppNavigation` (starts at `Home`) from it, each with its own back stack.
    Screens never navigate on sign-in success: the session changes and the root swaps the tree.
  - `ui/home/HomeScreen` is a **temporary** signed-in screen ("Signed in as ..." + sign out); the Groups screen replaces it.
  - `data/repository/AuthRepository` + `SupabaseAuthRepository`; `data/remote/ErrorMapper` also maps GoTrue error codes to the auth `AppError`s;
    `data/remote/mapper/UserMapper` builds the domain `User` (name from user metadata `name` or `full_name`).
    `resetPassword` verifies the recovery code and sets the password in one call; the session stays hidden until the password is set.
  - Google: `ui/auth/google/GoogleSignInRequester` (Credential Manager, nonce pair) is called by the sign-in Route.
    The button shows only when `GOOGLE_WEB_CLIENT_ID` is set in `local.properties`. Not testable end to end until the Google credentials exist.
  - `SobreioApplication` starts Koin with `di/appModules` (`dataModule`, `uiModule`).
  - `data/remote/SupabaseClient.kt` builds the client from `BuildConfig`; `ErrorMapper.kt` maps `FZ0xx` codes,
    auth codes, network failures and anything else to string resources through `ui/error/ErrorMessages.kt`.
  - `ui/format/MoneyFormat.kt`: `formatMoney(amountCents, currencyCode)` -> `1.298,15 €`.
  - Theme in `ui/theme` (brand inks, Lexend with `ss01`/`zero`, Inter with tabular figures).
  - `res/drawable/ic_sobreio_mark.xml`: the brand mark as a vector; the adaptive launcher icon uses it and follows dark mode.
- Debug builds allow cleartext HTTP only to `10.0.2.2`, `localhost` and `127.0.0.1` (`src/debug/res/xml/network_security_config.xml`);
  release builds are HTTPS only.
- The `ic_*` drawables in `res/drawable` are icons prepared for the coming screens.

## Target structure

```
dev.lacruzjs.sobreio/
  SobreioApplication.kt, MainActivity.kt
  di/            AppModules.kt, DataModule.kt (client, repositories), UiModule.kt (ViewModels)
  navigation/    Routes.kt (NavKey), NavigationWrapper.kt (NavDisplay), adaptive scaffold
  domain/
    model/       Xxx.kt: plain data classes (amounts as Long cents)
    error/       AppError.kt
  data/
    remote/      SupabaseClient.kt, ErrorMapper.kt, SafeCall.kt
      dto/       XxxDto.kt: @Serializable, @SerialName("snake_case")
      mapper/    XxxMapper.kt: DTO -> domain
    repository/  XxxRepository.kt (interface) + SupabaseXxxRepository.kt
  ui/
    theme/       Colors, typography (Lexend, Inter with tabular figures), shapes
    format/      Money and date formatting in the app language
    error/       ErrorMessages.kt: AppError -> string resource
    components/  Reusable composables (amount text, avatar, bottom sheet keypad)
    <feature>/<screen>/
      XxxScreen.kt     stateless XxxScreen(state, onEvent, modifier) with @Preview, plus a thin XxxRoute
                       (here or in XxxRoute.kt) that gets the ViewModel with koinViewModel and collects
                       the state with collectAsStateWithLifecycle
      XxxViewModel.kt, XxxUiState.kt, XxxEvent.kt
```

Features follow the product navigation: `auth`, `groups`, `group` (summary), `accounts`, `envelopes`, `movements`,
plus the `newmovement` bottom sheet. Tests mirror the main packages; fake repositories live in `test/.../fake/`.

| Layer | Does | Never |
| --- | --- | --- |
| ui | Draws state, emits events | Calls repositories, Supabase or the back stack |
| ViewModel | Handles events, calls repository interfaces, exposes `StateFlow<UiState>` | Imports Compose or Supabase |
| data | Talks to Supabase, maps DTOs to domain and exceptions to `AppError` through `safeCall` | Knows screens or user text |
| domain | Plain Kotlin models and `AppError` | Imports Android or Supabase |

- `data` and `domain` are organized by entity (shared across screens); `ui` by screen.
- Root navigation is driven by the session (signed in -> app, signed out -> auth), so screens never navigate on success.
- There is no use-case layer: the rules live in the database. Single Gradle module.

## Rules

- A screen receives `state` and `onEvent`; it never calls a repository, Supabase or the back stack directly.
- ViewModels depend on repository interfaces only; tests use fakes.
- Writes that change money go through the database RPCs (`create_transaction`, `create_account_transfer`, …).
  Never chain several table writes to emulate one operation, and never recompute balances in the app.
- Errors: data maps any failure to an `AppError` in `data/remote/ErrorMapper.kt` (database codes `FZ001`–`FZ020` become
  `BusinessRule`); ui maps `AppError` to string resources in `ui/error/ErrorMessages.kt`. Each happens in one place.
  Repository implementations wrap calls with `safeCall`.
- Amounts are `Long` cents everywhere; format them only in the UI with `formatMoney(…, locale)`, passing the locale of
  the current configuration (`1.298,15 €` in Spanish, `€1,298.15` in English).
- The app is bilingual. Every user-visible string is a resource with the same key in `res/values/strings.xml` (English,
  the default for any other device language) and `res/values-es-rES/strings.xml` (Spanish from Spain: the region makes the per-app language setting say
  "Español (España)", and devices in other Spanish locales fall back to it). No literal user text in
  composables, ViewModels or anywhere in Kotlin, not even a `contentDescription`.
  Add both translations in the same change; lint `MissingTranslation` fails otherwise. `generateLocaleConfig` gives
  Android 13+ a per-app language setting. Code, comments and test names are English.
- Adaptive layout uses `NavigationSuiteScaffold` and `ListDetailPaneScaffold`; check phone, tablet and foldable previews or emulators.
- Connection settings come from `local.properties` (git-ignored) through `BuildConfig`:
  `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` and `GOOGLE_WEB_CLIENT_ID` (the Google *web* client id, optional; Google sign-in is hidden when blank). Only the publishable (anon) key ever ships in the app;
  the secret / service_role key never appears in this repository.
- From the emulator, the local Supabase stack is at `http://10.0.2.2:54321`; it is also the default when
  `SUPABASE_URL` is missing. The key has no default: the client is created lazily by Koin and fails on first use
  without one. Changing `local.properties` needs a rebuild.

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
./gradlew assembleDebug     # build the debug APK
./gradlew test              # unit tests
./gradlew installDebug      # install on the connected device or emulator
./gradlew lint              # Android lint
```

## Conventions

- Kotlin official style; names: `XxxScreen`, `XxxViewModel`, `XxxUiState`, `XxxEvent`, `XxxRepository`.
- No `!!`; errors become UI state. State classes are immutable.
- Commits: Conventional Commits in English with scopes `app`, `ui`, `data`, `nav`; for example `feat(ui): add movements list`.
- When you add a dependency, add it to the version catalog and to the "Stack" or "Current state" section above.
- When you add a screen or a layer rule, update this file in the same change.

## Domain names

Use the English names: group, member, account, envelope, movement, transaction (income / expense / adjustment),
account transfer, envelope transfer, cover, period, commitment, occurrence, tag. "Sin asignar" is the Unassigned
system envelope. The full glossary is in the workspace `docs/agents/glossary.md`.

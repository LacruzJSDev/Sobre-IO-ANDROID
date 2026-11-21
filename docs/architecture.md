# Architecture

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

What every screen relies on. The screens themselves are described in `docs/features/`.

## Navigation and back stack

- Signed-in back stack: `[Group(last)]` when `LastGroupStore` holds a group for this user, else `[Groups]`
  (`initialBackStack`, a synchronous read before the first frame, so no flash and no animation). A group is a root, not a
  page above Groups: its header has a menu button (`MenuButton`, three lines), never a back arrow, and system back or the
  back gesture never lead to Groups (back from the root leaves the app). Groups is a root too, with no back arrow: the menu
  button and a group that is gone replace the stack with `[Groups]` (`showOnlyGroups`), and picking or creating a group
  replaces it with that group (`openGroup`). Groups stays a screen of its own so a wide layout can show it as a pane. `GroupViewModel`
  stores the group as last once it loads, and clears it (then shows only Groups) when it does not load because it is
  gone or the user is no longer a member (`AppError.NotFound`). The store is `SettingsLastGroupStore` on
  `multiplatform-settings-no-arg` 1.3.0, one key per user id. `kotlinx-datetime` 0.8.0 gives the device time zone that
  `SupabaseGroupRepository.createGroup` sends to `create_group`.

## Adaptive layout

- Adaptive layout (tablet and foldable): `AppNavigation` passes `sceneStrategies = listOf(AdaptiveListDetail(directive, listWidth),
  SinglePaneSceneStrategy())` to `NavDisplay`, with `directive = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(currentWindowAdaptiveInfoV2())` capped at two horizontal partitions (V2 would allow a third from 1200 dp)
  (`adaptive-layout` 1.3, experimental opt-in). `AdaptiveListDetail` (`navigation/AdaptiveListDetail.kt`) is our own scene on
  `ListDetailPaneScaffold`, not the library's `ListDetailSceneStrategy` (dropped: a lone list kept an empty detail pane, and
  switching to a single-pane scene to avoid it made NavDisplay cross-fade and reload the list on every close). It is ONE scene
  per root (key = the list entry, so a group and Groups still animate apart, and its metadata is the list entry's, so Groups keeps
  its back-like arrival): a list alone is laid out with one partition (full width), a detail narrows it and slides in, closing
  widens it while the detail slides out WITH its content (the popped entry is kept on screen until its pane has left;
  Navigation 3 cleans up an entry only once it leaves composition), and system back from a detail is a predictive gesture that
  scrubs the panes and then pops (`previousEntries` = the stack minus the top, so back from a detail never leaves the app).
  On a phone (one partition) it gives no scene. Entry metadata (ours, same file): `listPane` for `Group` and `Groups` (`Groups`
  also keeps its back-animation `transitionSpec`, combined with `+`); `detailPane` for `AccountDetail`, `EnvelopeDetail`, `EnvelopeTransfers`,
  `GroupSettings`, `GroupReview` and `Profile`; `extraPane` for `CommitmentDetail`, `AccountMovements`, `EnvelopeMovements`,
  `EnvelopeTransfersOf` and `Tags`. Forms, `TimeZonePicker`, `DeleteAccount` and `BankImport` have no pane metadata (full-screen scenes). Beside a detail the list takes half of a medium
  window and 420 dp from expanded (`listWidth`).
  Window classes: compact < 600 dp, medium 600-840, expanded >= 840. Two panes from medium (a tablet in portrait, an open foldable; the directive keeps them off a
  vertical hinge), so only phones keep one pane, and the back stack rules do not change. `MutableList<NavKey>.openDetail(key)` (next to
  `openGroup` in `InitialBackStack.kt`, unit tested; `NavKey.isPaneDetail()` lists the pane keys) replaces any detail or extra
  pane on top instead of stacking, and is what the Group route's open-account/envelope/settings/review and Groups' profile use.
  `closeDetails()` (same file, unit tested) leaves the list alone: every tab change of the group shell (the tab bar, the
  Commitments warning and "See all envelopes") goes through `selectTab` in `GroupRoute`, which calls it through `onCloseDetails`,
  so a detail never stays beside a tab it does not belong to.
  Content width, by the pane's own width: `ui/components/ContentWidth.kt` (`ContentWidth.Form` 640, `List` 720, `TwoColumns` 1120,
  `TwoColumnsMinPane` 720 dp, so each column gets about a phone's content width and `Modifier.contentMaxWidth(max)`, which centres and caps while the `Surface` keeps the full width).
  Applied once on the root column of `FormScaffold`, the group shell (List, or TwoColumns on the Group tab), Groups, group settings,
  review, tags, movements lists, commitment detail, profile and the time zone picker. Two columns (decided 2026-11-18, `ui/components/TwoColumnLayout.kt`): a pane of 720 dp or more
  (`ContentWidth.TwoColumnsMinPane`, the pane's own width) shows two columns, EACH WITH ITS OWN SCROLL, so a short column stays
  put while the other scrolls; below it the same pieces stack in one scrolling column, left first. Left = what sums the screen
  up, right = what is gone through. `TwoColumnLayout(left, right, top)` scrolls plain columns; `TwoColumnSplit(narrow, left,
  right)` is for a lazy list (the right column a `LazyColumn` with `RightColumnPadding`, the narrow version the same list with the
  summary as its first items). Screens: Group tab (left: hero, review, warnings; right: where the money went, in and out, next
  due items), Accounts (left: hero; right: the list; weights 5/6), Envelopes (same), account detail Summary (left: balance, chart,
  forecast; right: in/out, next due, latest movements, bank), envelope detail (left: available, this period, really free, chart;
  right: commitments, transfers, latest movements), commitment detail (left: amount, reservation, changes; right: history) and
  group settings (left: details and tags; right: members and invitations). The group shell, commitment detail and group settings
  are held to `ContentWidth.TwoColumns`; Movements stays one column (`ContentWidth.List`) until a filter summary gives its left
  side content; forms and Profile stay `ContentWidth.Form`. `FlatBottomSheet` sets `sheetMaxWidth` to 640 dp.

## Configuration

- `AppConfig` is generated by `:shared:generateAppConfig` from `local.properties` (replaces Android's `BuildConfig`).
- Debug builds of the Android app allow cleartext HTTP only to `10.0.2.2`, `localhost` and `127.0.0.1`
  (`app/src/debug/res/xml/network_security_config.xml`); release builds are HTTPS only. The iOS app allows local
  networking (`NSAllowsLocalNetworking`).

## Data loading

- Data loading (decided 2026-11-18): `data/cache/QueryCache` keeps the last answer of every read in memory (key = read name
  and arguments, `QueryKey`). A value is fresh for 30 s (`QueryCache.DEFAULT_MAX_AGE`); two screens reading one key share
  one request; failures are never stored; no disk (offline is out of scope). Every repository a screen reads is wrapped in
  `data/cache/CachedRepositories.kt` (`Cached<Thing>Repository(remote, cache)`, reads go through `cache.get`, writes are
  delegated with `by remote`). Writes never invalidate by hand: `cacheInvalidationPlugin` on the Supabase HTTP client sees
  every request and `cacheEffectOf` (pure, tested) decides: GET/HEAD and the RPCs in `READ_ONLY_RPCS` change nothing, any
  other request invalidates everything (values stay to be painted, the next read asks again), and sign-in/sign-out
  requests clear it (a token refresh does not). `materialize_through_today` is cached as a read for 30 s. ViewModels load
  with `ui/loading/revalidate(onRefreshing) { read() }`: `read` runs first against the cache only (`CacheOnlyPass`, no
  network, no suspension when all is cached, so starting it from `init` fills the state before the first frame) and then,
  if anything was missing or stale, against the database with `isRefreshing = true`. A read that finds cached data
  inconsistent (an id missing from a stale list) calls `stopIfCachePass()` before reporting NotFound. UI contract: the first
  load with nothing to show is a skeleton (`ListSkeleton`, `DetailSkeleton`, `FormSkeleton` in `ui/components/Skeleton.kt`),
  never a spinner on a blank page; a refresh keeps the content and shows `RefreshBar` (2 dp ink line drawn over the header's
  bottom edge by `ScreenHeader(isRefreshing)` / `MovementsSearchHeader(isRefreshing)`, so it never moves the content; hidden
  for refreshes under 250 ms); a failed refresh keeps the content and reports `actionError`. The group shell shows the bar
  of the tab on screen. Spinners remain only inside buttons, "load more" rows and the bank import's file step.
- Every RPC that only reads is in `READ_ONLY_RPCS` (`data/cache/CacheInvalidation.kt`), among them the ones behind a balance
  on a day, an outlook until a day, the export and the pending match (`account_balance_at`, `envelope_balance_at`,
  `envelope_outlook`, `group_outlook`, `export_period`, `match_pending`): calling them invalidates nothing.

## Calendar

- Calendar (phase 6, part B): `data/repository/CalendarRepository` (`SupabaseCalendarRepository`, cached as `CachedCalendarRepository`)
  is the only way to the group's dates: `getToday` (`group_today`), `getCurrentPeriod` (`current_period`), `getDateContext` (today,
  current period and the previous `periods` row, for the Movements date presets) and `getPeriods(limit)` (newest `periods` rows,
  for Export). Models in `domain/model/Calendar.kt` (`PeriodRange`, `GroupPeriod`, `DateContext`). Every ViewModel that needs
  "today" or a period takes it as `calendar`; tests use `FakeCalendarRepository` (today 14 Nov 2026, period November).

## Realtime

- Realtime (plan phase 2, D-55 in the DB repo): `buildSupabaseClient` installs `Realtime`. `data/realtime/GroupChangeFeed`
  (`SupabaseGroupChangeFeed`): `follow(groupId)` joins the private channel `group:<id>` and listens to its `change` broadcasts
  (the database sends one per group and transaction, with no data); notices are debounced 500 ms, then `QueryCache.invalidateAll()`
  and an emission of `remoteChanges`; after a reconnection (a second SUBSCRIBED) everything is read again; a failure retries every
  5 s and the channel is removed when collection stops. `GroupViewModel` keeps `follow` collected while the group's entry is in
  the back stack (so it keeps listening under a detail) and reads the group again on each notice. Routes place
  `ui/realtime/RefreshOnRemoteChange(paused) { Refresh }`: while the screen is on display a notice sends its Refresh, and
  `paused` (an Undo snackbar waiting) holds it until the snackbar ends, so a reload never takes the snackbar away. Used by the
  group shell (the Group tab and the tab on display), account detail (and its Commitments tab), envelope and commitment details,
  review, group settings and the account, envelope and tag movement lists. Checked on an emulator against the local stack: a
  movement inserted elsewhere showed within 3 s.

## Errors

- `data/remote/ErrorMapper.kt` maps `FZ0xx` codes, network failures and anything else to `AppError`; `ui/error/ErrorMessages.kt`
  maps `AppError` to string resources.

## Money and formatting

- `ui/format/MoneyFormat.kt`: `formatMoney(amountCents, currencyCode, languageTag)` gives `1.298,15 €` for `es`,
  `€1,298.15` otherwise; only the euro has a symbol, other currencies show their ISO code (`USD 1,298.15`).
- Money colours (`ui/theme/MoneyColors.kt`): `MoneyColors(inflow, outflow, neutral, negative)` provided by `SobreioTheme` through
  `LocalMoneyColors`, chosen by dark/light and the colour-blind preference (`moneyColors(dark, colorBlindSafe)`); `moneyColorFor(role,
  amount, colors, ink)` is the pure rule behind `ui/components/MoneyText` (Flow: positive inflow with "+", otherwise outflow;
  Transfer: neutral; Balance: ink, `negative` below zero; Plain: ink; `ink` overrides that colour, paper text on the dark hero card). The destructive red (`error`) never changes. The preference
  lives in `AppPreferencesStore` (`data/local`, `SettingsAppPreferencesStore`, per device); `App` observes it and the Profile screen
  has the "Accessibility" switch. Option lists (`EntityOption.balance`: `MoneyAmount`) show balances as `MoneyText`.

## Look and feel

- Search and notices: every search box is a `SearchField` (compact pill, placeholder, clear X); a note about a consequence
  or state is an `InlineNotice` (flat tint, icon, tones Info / Warning / Suggestion, warning amber from
  `noticeWarningContainer` in `RoleColors.kt`); `FormNote` stays for one-line captions under a field.
- Visual style: flat, no shadows or tonal elevation (`surfaceTint` equals the paper colour, sheets and dialogs set
  elevation 0); one white/paper background everywhere and the brand black clearly present (`PrimaryButton`, avatars, the
  selected tab with an ink indicator); red (`error`) only for destructive actions (`DestructiveButton`, the Delete and
  Leave swipe action). No dividers and no settings-style rows with chevrons: separate with spacing (the one exception is the thin
  inset line inside an `ExpandableSurface` between its header and its body, and between the rows of a `CollapsibleListSection`). Corners come from
  `SobreioShapes` (`medium`, 10 dp, is the group avatar's radius at 40 dp) for buttons, fields, snackbars and swipe actions.
  Forms: a `FormSection` is one segmented block (tiles 2 dp apart, outer corners `large` 14 dp, inner corners 4 dp through `LocalTileShape` / `tileShape()`, 24 dp between sections); text fields are filled soft fields in the tile grey with no outline (`softTextFieldColors()`).
  There is no FAB anywhere. Components: `PrimaryButton`, `SecondaryButton` (soft grey), `DestructiveButton` (all on
  `FlatButton`, with `isLoading`), `FlatBottomSheet`, `FlatSnackbarHost`, `UndoSnackbarHost`, `SwipeActionsRow`.
- Expand and collapse: one pattern, `ExpandableSurface` (`ui/components`, corners `large` 14 dp alone, the section's inner 4 dp inside a `FormSection`): header (label, preview, chevron) and body are
  the same grey surface, the body starts under a divider. `ExpandableFormTile`, `EntitySelect` (up to 8 options inline without search;
  more open an `EntityPickerSheet` with search, or stay inline with a search box when `insideSheet`), `TagPicker` (chosen tags as chips in the header, removable chips in the body) and
  `CollapsibleListSection` are built on it. The selected option is marked with a primary check and a medium-weight name, no grey row.
- Hero card (`ui/components/HeroCard.kt`, tokens in `ui/theme/HeroColors.kt`): the main figure of a tab (Group: really free money;
  Accounts: total in accounts; Envelopes: free to distribute, i.e. Unassigned) lives in a `HeroCard`, never in a loose header. `HeroCard` is a fixed `HeroCardHeight` (232 dp, the
  Group tab's content decides it) dark card with `SobreioShapes.large` that spreads its content top to bottom; `HeroLabel` is the
  card's text at 85 % alpha; `HeroFigure` draws the big figure through `MoneyText(Plain, ink = onHeroContainer)`, so it is ALWAYS
  white, and a negative one gets a "Below zero" chip (`showsBelowZeroChip`; light background, text in the LIGHT palette's negative
  colour so it contrasts); any other figure on the card is a `MoneyText` Plain with `ink = onHeroContainer`; the cards have no bars: label and figure on top,
  how it is made in the middle, a footer of `HeroLabel` (labelMedium) above `MoneyText` Plain (`titleMedium`) pairs (the Group projected
  figure is `titleLarge`); `HeroLabel(alpha)` gives muted captions. Tokens: `MaterialTheme.colorScheme.heroContainer` / `onHeroContainer`, from `HeroColors` provided by `SobreioTheme`
  (`LocalHeroColors`, `heroColors(dark, colorBlindSafe)`): light `#0C0C0C` / paper; dark `#2D2D2A` / `#F4F4F4` (`primary` is near-white
  in dark mode, so it is not used); `negativeOnContainer` is the dark palette's negative for lines and text drawn on the card.
- Theme in `ui/theme` (one `LightPaper`/`DarkPaper` background for `background`, `surface` and the lower containers;
  `LightSoft`/`DarkSoft` for higher containers, chips and secondary buttons; `LightRed`/`DarkRed` for `error`; colours as
  in the prototype, no hardcoded colours in composables). Fonts: Lexend with `ss01`/`zero`, Inter with tabular figures; the typography is built in a
  composable because fonts are Compose resources).
- Appearance components (colour and icon are inline in the group form; accounts and envelopes will use them too): `ui/theme/EntityColor` is the fixed 12-colour palette (the DB
  `color` column stores the light `#RRGGBB`, `fromHex` is case-insensitive and null for unknown; `dark` is a lighter
  variant); `ui/components/IconCatalog` holds the icon sections per `IconCategory` (Group 20, Account 20, Envelope 32;
  keys are snake_case English, stored in the DB `icon` column, never rename them; `iconFor` is null for unknown keys).
  Components: `EntityAvatar` (`GroupAvatar` delegates to it), `ColorPicker`, `IconPicker` (used directly in the
  forms, no sheet) and `EntitySelect` (see the Rules of `AGENTS.md`; the holder is "Joint"/"Common" plus one option per member, hidden with a single member). Members come from `MemberRepository.getMembers` (`account_group_members`
  with `profiles!inner(display_name)`, current members only). Icon, section and colour names are string resources
  (`icon_<key>`, `icon_section_<id>`, `color_<key>`).
- The `ic_*` drawables in `composeResources/drawable` are icons prepared for the coming screens.

## Charts

- Charts (`ui/components/charts`, Canvas only, no library): `BalanceLineChart(past, projection, todayX, texts: LineChartTexts,
  contentDescription)` (2 dp ink line, 10 % area, dashed lighter projection, today hairline, end dot with ring, crosshair
  and tooltip on touch or drag; the selection clears on a press outside the chart or when the page scrolls) and `GroupedColumnChart(series: List<ColumnSeries>, groups: List<ColumnGroup>, formatY,
  formatValue, contentDescription)` (columns up to 24 dp, 4 dp rounded free end, 2 dp gap, legend above, tooltip per group).
  Shared: `ChartTooltip`, `ChartLegend`, `chartTouch`, and the pure maths in `ChartMath.kt` (`niceTicks`, `compactAmount`,
  `LinearScale`, `nearestIndex`, `evenPositions`, `slotAt`), unit tested. Series colours are the money colours (`LocalMoneyColors`): In = `inflow`, Out =
  `outflow` (ink); the balance line uses `primary` and the part of the line and its area below zero is drawn in
  `negative` (clipped at the zero line). Gridlines are `outlineVariant`; axis text is muted, never in the series colour.
- Chart zoom (plan phase 5.1, `ui/components/charts/ChartZoom.kt`): `App` installs `ChartZoomHost` once around the navigation
  (a `SharedTransitionLayout` and one full-screen page drawn above everything). A chart card is wrapped in
  `ZoomableChart(key)`; `rememberChartZoom(key, title) { content }` returns the function that opens the page; the card and the
  page share their bounds (`sharedElementWithCallerManagedVisibility`, 350 ms), so the card grows into the page and shrinks back
  on the X, system back or the gesture (`NavigationBackHandler` while open). Section charts open from `ChartZoomButton` (an
  "Enlarge chart" icon in `FormSection(action = …)`): account balance (range chips, chart at `ZoomedChartHeight` 360 dp and every
  point), account and Group in/out and the envelope chart (each period's figures in a `ChartTable`). The small hero charts
  (Group really free, Accounts total) open on tap and show a `DayLineChart` (a `BalanceLineChart` with axes and crosshair) and
  every day's figure. `ChartTable` only shows the rows the chart draws; nothing is computed. `BalanceLineChart` and
  `GroupedColumnChart` take `height` (defaults `BalanceChartHeight` 220 dp and `FlowsChartHeight` 200 dp).

## Help texts

- Help texts (plan phase 5.4): every icon-only button shows its accessible name on long press (`LabelTooltip`; `BackButton`,
  `MenuButton`, `AddIconButton`, the header gear, search and close, steppers, clear date, password eye and the zoom's X were
  wrapped). `ui/components/InfoButton.kt`: `InfoButton(title, paragraphs)` is a 16 dp ⓘ (32 dp target) that opens a
  `FlatBottomSheet` with string-resource paragraphs; `HeroLabelWithInfo` puts one after a hero label in the card's colours, and
  `StatCard(info = …)` after its title. Used for really free today, free to distribute, total in accounts, the account forecast,
  an envelope's "This period" and "Really free" (`info_*` strings).

## Continuous integration

- CI builds Android only (`:app:assembleDebug :shared:testAndroidHostTest :app:lintDebug`). The iOS app needs Xcode on a Mac.

# Terms, privacy and your data

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

Accepting the terms of use and the privacy policy (D-57) and downloading all of one's data (D-58).

- The texts live on the website (`Sobre-IO-WEB`); the database holds each published version and its Spanish and English
  links (`legal_documents`), so the app has no URL of its own. `LegalRepository` (`SupabaseLegalRepository`, cached by
  `CachedLegalRepository`): `getDocumentsInForce()` (`legal_documents_in_force`, works without a session),
  `getStatus()` (`legal_status`: per document in force, the version accepted and `needsAcceptance`) and
  `accept(terms, privacy)` (`accept_legal`, FZ025 when a version is no longer in force). `LegalDocument.urlFor(languageTag)`
  picks the Spanish link for any Spanish locale and the English one otherwise. Both reads are in `READ_ONLY_RPCS`.
- Acceptance gate (`ui/legal`, `LegalGate`, `LegalGateViewModel` with `LegalRepository` and `AuthRepository`): sits in
  `NavigationWrapper` between `AuthState.SignedIn` and `AppNavigation`, keyed by user. While `legal_status` is read the
  root keeps the splash. `legalGateStateOf` (pure, `LegalGateRulesTest`) lets the app show when nothing needs accepting,
  and also when only one document is in force (the database accepts both in one call, so there would be nothing to
  accept). Otherwise `LegalAcceptanceScreen` (`AuthScaffold`): a welcome line for someone who accepted nothing, or an
  `InlineNotice` "the documents changed" for someone who accepted an older version; "Read them here" with a tile per
  document that opens it in the browser (`LegalDocumentTile`, `LocalUriHandler` in the Route); a switch tile "I have read
  and accept…" (the confirmation, no dialog); Continue (`accept`, enabled only with the switch on); a note and Sign out.
  One mechanism covers a new account (email or Google), a user of an earlier version and a new version of a document.
  A failed read lets the user in and the gate asks again on the next start: an outage, or a database without D-57,
  never locks anyone out. FZ025 on Continue reads the status again and shows the new version unticked with the reason.
- Sign-up (`SignUpViewModel` reads `getDocumentsInForce`): under the fields, a muted line and two text buttons with the
  terms and the privacy policy, so the user is informed before giving their data (GDPR art. 13). The acceptance itself is
  the gate after the email code. Without published documents the line is not shown.
- Profile, section "Privacy and your data": a tile per document with "Accepted on <day>" (from `legal_status`, hidden
  while the version in force is not accepted) that opens it, and "Download my data". Without published documents only the
  download shows. Download (`ExportRepository.exportMyData`, `export_my_data`, never cached): the tile shows a small
  progress while reading; the JSON goes to the state as `pendingData` (`DataFile(day, content)`) and the Route opens the
  system save dialog once (`rememberTextFileSaver("application/json")`, the same platform code as the CSV export) as
  `sobre-io-datos-AAAA-MM-DD.json` / `sobre-io-data-YYYY-MM-DD.json`, then sends `DataFileHandled`: "File saved", "The file
  could not be saved", or nothing when cancelled. A failed read shows the error under the section.

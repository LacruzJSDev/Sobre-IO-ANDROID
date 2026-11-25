# Sign-in

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

Email and password with 6-digit codes, and Google sign-in on both platforms.

- Google sign-in on iOS: the SDK is Swift only (Swift Package Manager in `iosApp/project.yml`, Kotlin does not link it).
  `iosApp/GoogleSignInProvider.swift` implements the Kotlin interface `IosGoogleSignInProvider` and `iOSApp.swift` passes
  it to `setUpKoin(googleSignIn:)`; `GoogleSignInLauncher.ios.kt` generates the nonce and calls it. The iOS OAuth client
  id (`GIDClientID`) and its URL scheme are in `iosApp/project.yml` (public values); `GOOGLE_WEB_CLIENT_ID` only turns
  the button on. Supabase accepts both the web and the iOS client as token audience.
- Google sign-in errors: closing the account picker shows nothing (`GoogleSignInCancelledException`); any other failure
  sends `GoogleFailed` and the screen shows the generic error. On Android, Credential Manager reports some failures as a
  user cancel too (an app whose package and signing key are not an Android OAuth client: "[16] Account reauth failed."),
  so `GoogleSignInRequester` treats a `GetCredentialCancellationException` as a cancel only when `isUserCancellation`
  (`ui/auth/google/CredentialCancellation.kt`) finds "cancelled by user" or no message. On iOS the Swift provider checks
  `GIDSignInError.canceled`.

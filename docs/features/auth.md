# Sign-in

<!-- Behaviour of the code as it is: keep it true. Change it in the same commit as the code it describes. -->

Email and password with 6-digit codes, and Google sign-in on both platforms.

- Google sign-in on iOS: the SDK is Swift only (Swift Package Manager in `iosApp/project.yml`, Kotlin does not link it).
  `iosApp/GoogleSignInProvider.swift` implements the Kotlin interface `IosGoogleSignInProvider` and `iOSApp.swift` passes
  it to `setUpKoin(googleSignIn:)`; `GoogleSignInLauncher.ios.kt` generates the nonce and calls it. The iOS OAuth client
  id (`GIDClientID`) and its URL scheme are in `iosApp/project.yml` (public values); `GOOGLE_WEB_CLIENT_ID` only turns
  the button on. Supabase accepts both the web and the iOS client as token audience.

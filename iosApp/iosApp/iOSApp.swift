import GoogleSignIn
import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Dependency injection lives in the shared module; Google sign-in is the one part that needs the Swift SDK
        IosKoinKt.setUpKoin(googleSignIn: GoogleSignInProvider())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Google returns to the app through its URL scheme
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}

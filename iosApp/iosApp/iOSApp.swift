import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Dependency injection lives in the shared module
        IosKoinKt.setUpKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

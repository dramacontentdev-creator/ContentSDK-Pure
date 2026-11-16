import SwiftUI
import ContentSdk

@main
struct ContentSdkDemoApp: App {

    /// Content SDK - integration Step 2 (initialize)
    ///
    /// Call `initialize` exactly ONCE when your app launches. `App.init` (SwiftUI) or
    /// `application(_:didFinishLaunchingWithOptions:)` (UIKit) are good places.
    /// It only stores your configuration (no network or player start here), so it is cheap.
    /// The credentials come from `SdkConfig` in DemoEdition.swift.
    /// Do NOT call it from a SwiftUI `body`, which can run many times.
    init() {
        // (Optional) Print SDK status messages to the Xcode console. Keep it off in your release builds.
        #if DEBUG
        IosContentSdk.shared.setLoggingEnabled(enabled: true)
        #endif

        IosContentSdk.shared.initialize(
            appId: SdkConfig.appId,
            appSecret: SdkConfig.appSecret,
            userId: SdkConfig.userId,
            // (Optional) Content language, e.g. "en-US". Pass "" to follow your app's locale.
            language: ""
        )
    }

    var body: some Scene {
        WindowGroup {
            DemoHomeView()
        }
    }
}

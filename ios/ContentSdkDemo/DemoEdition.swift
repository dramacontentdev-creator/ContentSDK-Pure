import UIKit
import ContentSdk

// =================================================================================================
//  PURE edition. Swift files that differ from the commercial demo: this file,
//  Catalog/EditionApiCatalog.swift, and Customization/EditionCustomization.swift; the commercial demo
//  also has its own unlock UI. Every other Swift file is identical in the commercial and pure demos.
// =================================================================================================

/// What the demo UI shows about this edition. Demo only.
enum DemoEdition {
    static let name = "Pure"
    static let summary = "No SDK ads: your app monetizes on its own and reports the revenue."
    static let startingDetail = "Booting the SDK."

    /// The SDK's `setAutoPlay` default in this edition: an episode is prepared and waits for a play.
    static let autoPlayDefault = false

    /// The integration checklist on the demo home screen; labels match the `Step` comments in code.
    static let integrationSteps: [IntegrationStep] = [
        IntegrationStep(label: "1", title: "Install the SDK Pod", file: "Podfile"),
        IntegrationStep(label: "2", title: "Initialize the SDK", file: "ContentSdkDemoApp.swift"),
        IntegrationStep(label: "3", title: "Start the SDK", file: "DemoEdition.swift"),
        IntegrationStep(label: "4", title: "Open the SDK page", file: "SdkIntegration.swift"),
    ]
}

/// Your SDK credentials for the PURE edition.
///
/// The values below are the DEMO credentials. The commercial and the pure demo, on Android and on
/// iOS, all use this one set, and it is for the demos only. Replace it with YOUR App ID / App
/// Secret, which you get at https://open.lucky-chick.com.
/// Do not hard-code your production secret in a public repository.
enum SdkConfig {
    /// YOUR Content App ID.
    static let appId = "17effbdf-99fc-4e44-8850-ba8ed6dfb2e7"

    /// YOUR Content App Secret.
    static let appSecret = "a0e44f2ecdaa5b2cc573ae98f0bf98e815376fc3e167d85187e2636bb73d505c"

    /// YOUR logged-in user's id, so watch history follows the user across devices.
    /// Pass "" if your app has no login; the SDK then creates an anonymous user.
    static let userId = ""
}

/// Content SDK - integration Step 3 (start)
///
/// `start` boots the SDK (network, player). Call it once; do NOT show SDK pages before it succeeds.
/// The pure edition shows no SDK ads, so there is no ad consent step.
///
/// The callback runs on the main thread.
@MainActor
func startContentSdk(onResult: @escaping (DemoSdkStatus) -> Void) {
    IosContentSdk.shared.start { error in
        if let error {
            // Show a failure message or retry entry.
            print("Content SDK start failed: \(error.code) \(error.msg)")
            onResult(.failed(code: error.code, message: error.msg))
            return
        }
        onResult(.ready)
    }
}

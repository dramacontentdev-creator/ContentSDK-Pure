import ContentSdk

// What the demo screens show. None of this is part of the SDK API.

/// What the home screen shows about the SDK.
enum DemoSdkStatus: Equatable {
    case notStarted
    case starting
    case ready
    case failed(code: String, message: String)
}

/// One row of the integration checklist on the home screen.
struct IntegrationStep {
    let label: String
    let title: String
    let file: String
}

/// The screens the demo can push onto its navigation stack.
enum SdkRoute: Hashable {
    /// The SDK's drama home.
    case dramaHome
    /// The SDK's player page for one drama.
    case dramaPlayer(Drama.PlayerParams)
    /// The demo's player lab (Lab/).
    case lab
}

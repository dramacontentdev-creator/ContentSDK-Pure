import SwiftUI
import UIKit
import ContentSdk

// =============================================================================
// Everything related to the SDK lives in this file:
//   - start()            -> calls startContentSdk (DemoEdition.swift, Step 3)
//   - openDramaHome()    -> Step 4: show the SDK page
//   - SdkDestination     -> how SDK pages are placed in a SwiftUI NavigationStack
// UI/ and Lab/ are only demo UI.
// =============================================================================

@MainActor
final class ContentSdkDemo: ObservableObject {

    /// Shown on the demo home screen.
    @Published private(set) var status: DemoSdkStatus = .notStarted
    /// SwiftUI navigation stack. SDK pages are pushed onto it.
    @Published var path: [SdkRoute] = []

    func onOpenDramaHomeTapped() {
        whenSdkReady { [weak self] in self?.openDramaHome() }
    }

    func onOpenLabTapped() {
        whenSdkReady { [weak self] in self?.path.append(.lab) }
    }

    /// Leaves the top screen. A quick second tap on a back button finds the stack already empty.
    func goBack() {
        if !path.isEmpty { path.removeLast() }
    }

    /// Runs `action` once the SDK is started, starting it first if needed.
    private func whenSdkReady(_ action: @escaping () -> Void) {
        switch status {
        // Already started: run the action directly. `start` only needs to succeed once per launch.
        case .ready: action()
        // A start request is running; ignore repeated taps.
        case .starting: break
        // Not started yet (or the last attempt failed): start first, then run the action.
        case .notStarted, .failed: start(onReady: action)
        }
    }

    /// Starts the SDK through `startContentSdk` (DemoEdition.swift): the commercial edition asks
    /// for ad consent first, the pure edition starts directly.
    /// Tip: you can also start early (e.g. right after launch) so the SDK is ready when needed.
    private func start(onReady: @escaping () -> Void) {
        status = .starting
        startContentSdk { [weak self] result in
            self?.status = result
            if result == .ready { onReady() }
        }
    }

    /// Content SDK - integration Step 4 (show the SDK page)
    ///
    /// Pushes the SDK drama home onto this demo's SwiftUI NavigationStack; `SdkDestination` below
    /// builds the screen. The SDK handles browsing and episode switching. On iOS your
    /// app owns navigation, so the player is pushed when the user taps a drama.
    ///
    /// Using UIKit navigation, embedding the page in a tab, or opening the player directly?
    /// See SdkApiCatalog.swift.
    private func openDramaHome() {
        path.append(.dramaHome)
    }
}

/// Builds the SwiftUI screen for each `SdkRoute` (used with `.navigationDestination`).
struct SdkDestination: View {
    let route: SdkRoute
    @ObservedObject var demo: ContentSdkDemo

    var body: some View {
        switch route {
        case .dramaHome:
            // Content SDK - integration Step 4: the SDK drama home (see `openDramaHome` above).
            SdkControllerView {
                IosContentSdk.shared.dramaHome(
                    // nil hides the SDK title bar, because this NavigationStack already shows one.
                    title: nil,
                    // The navigation bar covers the top; let the SDK pad only the bottom edge.
                    insetsMode: .navigationBarsPadding
                ) { [weak demo] drama in
                    // The user tapped a drama: open the SDK player for it.
                    demo?.path.append(.dramaPlayer(drama.asPlayerParams()))
                }
            }
            .ignoresSafeArea(edges: .bottom)
            .navigationTitle("Dramas")
            .navigationBarTitleDisplayMode(.inline)

        case .dramaPlayer(let params):
            SdkControllerView {
                IosContentSdk.shared.dramaPlayer(params: params) { [weak demo] in
                    // The player's own back button was tapped: leave the player.
                    demo?.goBack()
                }
            }
            .ignoresSafeArea()
            // The player draws its own controls, including a back button.
            .toolbar(.hidden, for: .navigationBar)
            // A custom info region draws its own back button; this is how it leaves (see CustomRegions.swift).
            .onAppear { PlayerPageCloser.handler = { [weak demo] in demo?.goBack() } }

        case .lab:
            PlayerLabView(onBack: { [weak demo] in demo?.goBack() })
                .toolbar(.hidden, for: .navigationBar)
        }
    }
}

/// Wraps an SDK `UIViewController` so it can be used in SwiftUI.
/// The controller is created once; never create SDK controllers inside `updateUIViewController`.
struct SdkControllerView: UIViewControllerRepresentable {
    let factory: () -> UIViewController

    func makeUIViewController(context: Context) -> UIViewController { factory() }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}

extension UIApplication {
    /// The view controller currently on screen; used to present dialogs (ad consent, your paywall).
    var topViewController: UIViewController? {
        let scene = connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var top = scene?.keyWindow?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

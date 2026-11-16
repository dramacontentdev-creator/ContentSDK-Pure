import ContentSdk
import SwiftUI

/// Content SDK - player lab (demo only)
///
/// The SDK player page fills the screen, as it does in an app, and the demo draws its own controls
/// over it, the way an app with its own player UI would:
///
/// - the button at the top right opens the **control panel**: playback controls (play / pause, seek,
///   speed, quality, episodes) and settings (playback options, page UI style, region visibility),
///   every one a `ContentPlayerController` or `IosContentSdk` call;
/// - the strip at the top left is the **log**: every SDK call made here and every playback event,
///   from `setPlayerEventListener`.
///
/// The controller comes from `setPlayerControllerListener`; the SDK calls are in `LabActions` and the
/// switches' state in `LabSettings`. The other files of this folder are only the screen.
struct PlayerLabView: View {
    let onBack: () -> Void

    @ObservedObject private var settings = LabSettings.shared
    // State input: the page's controller and the drama list from the SDK; the picked drama.
    @State private var controller: ContentPlayerController?
    @State private var dramas: [Drama] = []
    @State private var selected: Drama?
    // Chrome state.
    @State private var panelOpen = false
    @State private var section: PanelSection = .controls

    private let actions = LabActions.shared

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let drama = selected {
                player(for: drama)
            } else {
                DramaPicker(dramas: dramas, onPick: pick, onReload: reload,
                            onHistory: { actions.loadHistory() }, onBack: onBack)
            }
        }
        .preferredColorScheme(.dark)
        .tint(DemoColors.accent)
        // Listeners are process-wide and keep one each: start them here, stop them when the screen goes.
        .onAppear {
            actions.startListening { controller = $0 }
            reload()
        }
        .onDisappear { actions.stopListening() }
        // While the panel covers the page, keep the page controls shown; let them hide again when it closes.
        .onChange(of: panelOpen) { open in
            if let controller { actions.holdControls(controller, held: open) }
        }
        .onChange(of: controller.map { ObjectIdentifier($0 as AnyObject) }) { _ in
            if panelOpen, let controller { actions.holdControls(controller, held: true) }
        }
    }

    /// The SDK player page with the lab's chrome over it: log overlay, panel button, control panel.
    private func player(for drama: Drama) -> some View {
        ZStack(alignment: .topTrailing) {
            // The SDK player page, full screen. Its own back button returns to the picker.
            SdkControllerView {
                IosContentSdk.shared.dramaPlayer(
                    params: Drama.PlayerParams(id: drama.id, episodeOrder: 0, drama: nil)
                ) { closePlayer() }
            }
            .id(drama.id)
            .ignoresSafeArea()
            // A custom info region draws its own back button; this is how it leaves (see CustomRegions.swift).
            .onAppear { PlayerPageCloser.handler = { closePlayer() } }

            if settings.logOverlay { LogOverlay() }
            // Top right, where the SDK page draws nothing.
            RoundIconButton(symbol: panelOpen ? "xmark" : "slider.horizontal.3",
                            label: panelOpen ? "Close panel" : "Open panel", filled: panelOpen) { panelOpen.toggle() }
                .padding(.top, 8)
                .padding(.trailing, LabLayout.edgeMargin)

            if panelOpen {
                VStack {
                    Spacer()
                    ControlPanel(controller: controller, section: $section)
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.2), value: panelOpen)
    }

    // MARK: - Actions

    private func pick(_ drama: Drama) {
        selected = drama
        LabLog.shared.add(.api, "open \(drama.name)")
    }

    private func reload() {
        actions.loadDramas { dramas = $0 }
    }

    private func closePlayer() {
        selected = nil
        panelOpen = false
    }
}

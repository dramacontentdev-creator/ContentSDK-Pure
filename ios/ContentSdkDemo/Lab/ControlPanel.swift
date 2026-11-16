import ContentSdk
import SwiftUI

/// The two pages of the control panel.
enum PanelSection: CaseIterable {
    case controls, settings

    var label: String {
        switch self {
        case .controls: return "Controls"
        case .settings: return "Settings"
        }
    }
}

/// The lab's control panel: a sheet over the bottom of the SDK player page, drawn entirely by the
/// demo and driven through `ContentPlayerController`. It is what an app's own player UI looks like
/// when the SDK page keeps the video and the app keeps the controls.
///
/// `controller` is the controller of the page shown now, or nil while the page is loading; the
/// controls then say so and the settings still work.
struct ControlPanel: View {
    let controller: ContentPlayerController?
    @Binding var section: PanelSection

    var body: some View {
        VStack(spacing: 0) {
            // Section switch
            HStack(spacing: 6) {
                ForEach(PanelSection.allCases, id: \.self) { entry in
                    Chip(label: entry.label, selected: entry == section) { section = entry }
                }
                Spacer(minLength: 0)
            }
            .padding(12)
            // Section body
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    switch section {
                    case .controls:
                        if let controller {
                            ControlsSection(controller: controller)
                        } else {
                            Note("The player page is not ready yet. The controls use the controller of the page that is shown.")
                        }
                    case .settings:
                        SettingsSection(controller: controller)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
        }
        .frame(maxHeight: UIScreen.main.bounds.height * LabLayout.panelMaxHeightFraction)
        .background(DemoColors.surface.opacity(0.96), in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(DemoColors.outline, lineWidth: 1))
        // Touches on the panel stay on the panel and do not reach the player behind it.
        .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .onTapGesture {}
        .padding(LabLayout.edgeMargin)
    }
}

/// Playback controls of the page on screen: status, timeline, transport, speed, quality, and episodes.
/// Every control is one `ContentPlayerController` call in `LabActions`; the state it shows comes from
/// `PlayerFeed`, so it needs no listener of its own.
private struct ControlsSection: View {
    let controller: ContentPlayerController
    @ObservedObject private var feed: PlayerFeed
    @ObservedObject private var actions = LabActions.shared
    @State private var dragging: Double?

    init(controller: ContentPlayerController) {
        self.controller = controller
        feed = PlayerFeed.of(controller)
    }

    var body: some View {
        let state = feed.state
        let progress = feed.progress
        let resource = state.resource
        // A locked episode shows the SDK's unlock or unavailable layer; playback commands would be ignored.
        let playable = !state.episodeLocked

        // Status line: what the controller reports right now.
        Text(statusText(state, resource))
            .font(.system(size: 12, design: .monospaced))
            .foregroundStyle(DemoColors.textPrimary)

        // Timeline: follows playback, seeks once when the finger lifts. Off until the duration is known.
        VStack(spacing: 4) {
            Slider(
                value: Binding(get: { dragging ?? progress.fraction }, set: { dragging = $0 }),
                in: 0...1,
                onEditingChanged: { editing in
                    if !editing, let fraction = dragging {
                        actions.seekTo(controller, positionMs: Int64(fraction * Double(progress.durationMs)))
                        dragging = nil
                    }
                }
            )
            .tint(DemoColors.accent)
            .disabled(progress.durationMs <= 0 || !playable)
            HStack {
                let shown = dragging.map { Int64($0 * Double(progress.durationMs)) } ?? progress.positionMs
                timeText(formatTime(shown))
                Spacer()
                timeText(formatTime(progress.durationMs))
            }
        }

        // Transport: previous, back 10 s, play / pause (by the play intention), forward 10 s, next.
        HStack {
            RoundIconButton(symbol: "chevron.left", label: "Previous episode",
                            enabled: (resource?.episodeNumber ?? 0) > 1) { actions.episode(controller, delta: -1) }
            Spacer()
            RoundIconButton(symbol: "gobackward.10", label: "Back 10 seconds", enabled: playable) {
                actions.seekBy(controller, deltaMs: -10_000)
            }
            Spacer()
            RoundIconButton(symbol: state.playWhenReady ? "pause.fill" : "play.fill",
                            label: state.playWhenReady ? "Pause" : "Play",
                            size: LabLayout.playButton, filled: true, enabled: playable) { actions.togglePlay(controller) }
            Spacer()
            RoundIconButton(symbol: "goforward.10", label: "Forward 10 seconds", enabled: playable) {
                actions.seekBy(controller, deltaMs: 10_000)
            }
            Spacer()
            RoundIconButton(symbol: "chevron.right", label: "Next episode",
                            enabled: resource.map { $0.episodeNumber < $0.episodeCount } ?? false) {
                actions.episode(controller, delta: 1)
            }
        }

        // Speed and quality: one row each, the current choice is highlighted.
        Segmented(
            title: "Speed",
            options: [Float(1), 1.5, 2].map { (value: $0, label: speedLabel($0)) },
            enabled: playable,
            isSelected: { $0 == state.playbackSpeed },
            onSelect: { actions.setSpeed(controller, $0) }
        )
        QualityRow(
            heights: state.availableQualityHeights.map { $0.intValue },
            selected: state.quality,
            resolved: actions.resolvedQuality,
            enabled: playable
        ) { actions.setQuality(controller, $0) }

        // Episodes: jump straight to one; a locked one shows a lock.
        if !feed.content.episodes.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                SectionLabel("Episodes")
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(feed.content.episodes, id: \.episodeId) { episode in
                            Chip(
                                label: episode.locked ? "🔒 \(episode.episodeNumber)" : "\(episode.episodeNumber)",
                                selected: episode.episodeNumber == resource?.episodeNumber
                            ) { actions.selectEpisode(controller, number: Int(episode.episodeNumber)) }
                        }
                    }
                }
            }
        }
    }

    private func statusText(_ state: ContentPlayerState, _ resource: ContentResource?) -> String {
        var text = "\(state.playbackState)"
        text += state.playWhenReady ? "  ·  play intent on" : "  ·  play intent off"
        if state.episodeLocked { text += "  ·  locked" }
        let episode = resource.map { "\($0.dramaName)  EP \($0.episodeNumber)/\($0.episodeCount)" } ?? "no episode yet"
        return text + "\n" + episode
    }

    private func timeText(_ text: String) -> some View {
        Text(text).font(.system(size: 11, design: .monospaced)).foregroundStyle(DemoColors.textSecondary)
    }
}

/// The resolutions the stream really offers, and the one playing. When there is nothing to switch to
/// it says so; a tap then still reports the failure in the log, which is what `LabActions.setQuality` does.
private struct QualityRow: View {
    let heights: [Int]
    let selected: PlayerQualitySelection
    let resolved: PlayerResolvedQuality?
    let enabled: Bool
    let onSelect: (PlayerQualitySelection) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Segmented(
                title: "Quality",
                options: [(value: LabActions.autoQuality, label: "Auto")]
                    + heights.map { (value: LabActions.manualQuality($0), label: "\($0)p") },
                enabled: enabled,
                isSelected: { $0.mode == selected.mode && $0.requestedHeight == selected.requestedHeight },
                onSelect: onSelect
            )
            Note(note)
        }
    }

    private var note: String {
        let playing = resolved.map { LabActions.describeStream($0) }
        let only = heights.count == 1 ? heights[0] : (heights.isEmpty ? resolved.map { LabActions.shortEdge($0) } : nil)
        if let only {
            return "Playing \(playing ?? "\(only)p"). This stream has one resolution, so it cannot be switched."
        }
        if heights.isEmpty { return "The resolutions of this stream are not known yet." }
        return "Playing \(playing ?? "…"). Offered: \(heights.map { "\($0)p" }.joined(separator: ", "))."
    }
}

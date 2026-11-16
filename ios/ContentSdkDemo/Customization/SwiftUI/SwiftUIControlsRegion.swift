import ContentSdk
import SwiftUI

/// SwiftUI version of the custom CONTROLS region (see CustomRegions.swift): a thin timeline with
/// its 44 pt touch band, then one row of 40 pt controls, where the SDK draws its own.
///
/// This is the whole connection to the SDK. Each tap is one controller call:
/// `seekTo`, `selectEpisode`, `setPlaybackSpeed`; the episode pill opens the episode list.
struct SwiftUIControlsRegion: View {
    private let controller: ContentPlayerController
    @ObservedObject private var feed: PlayerFeed

    init(controller: ContentPlayerController) {
        self.controller = controller
        feed = PlayerFeed.of(controller)
    }

    var body: some View {
        // A locked episode shows the SDK's unlock or unavailable layer; seeking and switching would be ignored.
        if !feed.state.episodeLocked {
            let ui = feed.controlsUi()
            ControlsRegionContent(
                ui: ui,
                onSeek: { controller.seekTo(positionMs: Int64($0 * Double(ui.durationMs))) },
                onPrevious: { controller.selectEpisode(episodeNumber: Int32(ui.episodeNumber - 1)) },
                onNext: { controller.selectEpisode(episodeNumber: Int32(ui.episodeNumber + 1)) },
                onEpisodes: { EpisodeListPresenter.showSwiftUI(controller: controller, feed: feed) },
                onSpeed: { controller.setPlaybackSpeed(speed: nextSpeed(ui.speed)) }
            )
        }
    }
}

/// The controls region as pure UI: no SDK types, so it can be drawn from fixed data.
struct ControlsRegionContent: View {
    let ui: ControlsUi
    let onSeek: (Double) -> Void
    let onPrevious: () -> Void
    let onNext: () -> Void
    let onEpisodes: () -> Void
    let onSpeed: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            VStack(spacing: 0) {
                TimelineBand(ui: ui, onSeek: onSeek)
                // The SDK's gap between the timeline and the row; the times sit in the band above it.
                Color.clear.frame(height: RegionSlots.rowTopGap)
                HStack(spacing: RegionSlots.rowGap) {
                    circleButton("chevron.left", "Previous episode", enabled: ui.hasPrevious, action: onPrevious)
                    episodePill
                    circleButton("chevron.right", "Next episode", enabled: ui.hasNext, action: onNext)
                    speedPill
                }
                .padding(.horizontal, RegionSlots.rowPaddingH)
                .frame(height: RegionSlots.rowHeight)
            }
            // The shade reaches under the home indicator; the controls stay above it.
            .background(
                LinearGradient(
                    colors: [.clear, Color(argb: RegionColors.scrimBottom)],
                    startPoint: .top, endPoint: .bottom
                )
                .ignoresSafeArea(edges: .bottom)
            )
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// Opens the episode list. Shows where you are, `EP 3 / 24`, with a list icon.
    private var episodePill: some View {
        Button(action: onEpisodes) {
            HStack(spacing: 8) {
                Image(systemName: "list.bullet").font(.system(size: 15, weight: .semibold))
                Text(ui.episodeCount > 0 ? "EP \(ui.episodeNumber) / \(ui.episodeCount)" : "Episodes")
                    .font(.system(size: 14, weight: .semibold))
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: RegionSlots.rowHeight)
            .background(Color(argb: RegionColors.pill), in: Capsule())
        }
        .disabled(ui.episodeCount == 0)
    }

    /// Cycles 1x, 1.5x, 2x. The label is the current speed.
    private var speedPill: some View {
        Button(action: onSpeed) {
            Text(speedLabel(ui.speed))
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .frame(height: RegionSlots.rowHeight)
                .background(Color(argb: RegionColors.pill), in: Capsule())
        }
    }

    private func circleButton(_ symbol: String, _ label: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(.white.opacity(enabled ? 1 : 0.35))
                .frame(width: RegionSlots.buttonSize, height: RegionSlots.buttonSize)
                .background(Color(argb: RegionColors.pill), in: Circle())
        }
        .disabled(!enabled)
        .accessibilityLabel(label)
    }
}

/// Timeline with the SDK's geometry: a 44 pt touch band whose line sits at its bottom edge, 2 pt
/// thick and 6 pt while dragging. The elapsed and total time sit in the band, just above the line.
/// While the player waits for data a light runs along the line.
///
/// A drag that starts on the band is taken by the band, so a vertical swipe that begins there does
/// not change the episode. Keep touch areas as small as this one.
private struct TimelineBand: View {
    let ui: ControlsUi
    let onSeek: (Double) -> Void

    @State private var dragFraction: Double?
    @State private var lightPhase: CGFloat = 0

    var body: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let shown = dragFraction ?? ui.fraction
            let lineHeight = dragFraction == nil ? RegionSlots.timelineLineHeight : RegionSlots.timelineLineHeightDragging
            ZStack(alignment: .bottomLeading) {
                // Line: the unplayed track, the played part with the accent gradient, the loading light.
                ZStack(alignment: .leading) {
                    Rectangle().fill(Color(argb: RegionColors.track))
                    Rectangle()
                        .fill(LinearGradient(
                            colors: [Color(argb: RegionColors.accent), Color(argb: RegionColors.accentEnd)],
                            startPoint: .leading, endPoint: .trailing
                        ))
                        .frame(width: width * shown)
                    if ui.loading { loadingLight(width: width) }
                }
                .frame(height: lineHeight)
                .clipped()

                if let dragFraction {
                    // The time under the finger while dragging, above the line.
                    dragBubble(fraction: dragFraction, width: width)
                } else {
                    HStack {
                        timeText(formatTime(ui.positionMs))
                        Spacer()
                        timeText(formatTime(ui.durationMs))
                    }
                    .padding(.horizontal, RegionSlots.rowPaddingH)
                    .padding(.bottom, RegionSlots.timelineLineHeight + 6)
                }
            }
            .frame(width: width, height: RegionSlots.timelineTouchHeight, alignment: .bottomLeading)
            .contentShape(Rectangle())
            // Horizontal drags only count once the duration is known.
            .gesture(
                DragGesture(minimumDistance: 6)
                    .onChanged { value in
                        guard ui.seekable, abs(value.translation.width) > abs(value.translation.height) || dragFraction != nil else { return }
                        dragFraction = min(max(value.location.x / max(width, 1), 0), 1)
                    }
                    .onEnded { _ in
                        if let fraction = dragFraction { onSeek(fraction) }
                        dragFraction = nil
                    }
            )
        }
        .frame(height: RegionSlots.timelineTouchHeight)
    }

    private func timeText(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 10, design: .monospaced))
            .foregroundStyle(Color(argb: RegionColors.textSecondary))
    }

    private func dragBubble(fraction: Double, width: CGFloat) -> some View {
        let bubbleWidth: CGFloat = 56
        let x = min(max(CGFloat(fraction) * width - bubbleWidth / 2, 0), max(width - bubbleWidth, 0))
        return Text(formatTime(Int64(fraction * Double(ui.durationMs))))
            .font(.system(size: 12, weight: .semibold, design: .monospaced))
            .foregroundStyle(.white)
            .frame(width: bubbleWidth)
            .padding(.vertical, 4)
            .background(Color(argb: RegionColors.glass), in: Capsule())
            .offset(x: x, y: -14)
    }

    /// A light that runs along the line while the player waits for data, like the SDK's own.
    private func loadingLight(width: CGFloat) -> some View {
        let light = width * 0.3
        return Rectangle()
            .fill(LinearGradient(
                colors: [.clear, .white.opacity(0.9), .clear], startPoint: .leading, endPoint: .trailing
            ))
            .frame(width: light)
            .offset(x: (width + light) * lightPhase - light)
            .onAppear {
                withAnimation(.linear(duration: 1.1).repeatForever(autoreverses: false)) { lightPhase = 1 }
            }
    }
}

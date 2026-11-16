import ContentSdk
import SwiftUI

/// SwiftUI version of the custom INFO region (see CustomRegions.swift).
///
/// It draws what the SDK draws in this region, where the SDK draws it: the back button in the top
/// bar, the episode intro at the bottom left. The SDK's back button belongs to this region, so
/// replacing the region means offering your own, as here.
///
/// Connected to the SDK in two lines: the controller gives the `PlayerFeed`, the feed gives the `InfoUi`.
struct SwiftUIInfoRegion: View {
    @ObservedObject private var feed: PlayerFeed

    init(controller: ContentPlayerController) {
        feed = PlayerFeed.of(controller)
    }

    var body: some View {
        // Closing the player page is the app's decision: see PlayerPageCloser.
        InfoRegionContent(ui: feed.infoUi(), onBack: PlayerPageCloser.close)
    }
}

/// The info region as pure UI: no SDK types, so it can be drawn from fixed data.
struct InfoRegionContent: View {
    let ui: InfoUi
    let onBack: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Top bar: same height, padding, and button size as the SDK's. Only the button takes touches.
            HStack {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: RegionSlots.buttonSize, height: RegionSlots.buttonSize)
                        .background(Color(argb: RegionColors.glass), in: Circle())
                }
                .accessibilityLabel("Back")
                Spacer()
            }
            .padding(.horizontal, RegionSlots.topBarPaddingH)
            .padding(.vertical, RegionSlots.topBarPaddingV)
            .frame(height: RegionSlots.topBarHeight)

            Spacer(minLength: 0)

            if ui.hasEpisode { intro }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// Bottom left, above the controls row. Has no touch handler, so taps reach the page.
    private var intro: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Text(ui.dramaName)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .shadow(color: .black.opacity(0.6), radius: 3)
                Text("CUSTOM")
                    .font(.system(size: 9, weight: .bold))
                    .kerning(0.8)
                    .foregroundStyle(Color(argb: RegionColors.accent))
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2)
                    .background(Color(argb: RegionColors.accent).opacity(0.18), in: RoundedRectangle(cornerRadius: 4))
            }
            HStack(spacing: 8) {
                Text("EP \(ui.episodeNumber)")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(Color(argb: RegionColors.accent), in: Capsule())
                Text(ui.episodeTitle)
                    .font(.system(size: 13))
                    .foregroundStyle(Color(argb: RegionColors.textSecondary))
                    .lineLimit(1)
                    .shadow(color: .black.opacity(0.6), radius: 3)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, RegionSlots.introPaddingH)
        .padding(.bottom, RegionSlots.controlsHeight + RegionSlots.introGapAboveControls)
    }
}

import ContentSdk
import SwiftUI

/// SwiftUI version of the custom EPISODE LIST, opened from the episode pill of the controls region.
///
/// It is shown in its own presented sheet, not drawn inside the controls region: that region hides
/// together with the SDK controls, and the list must stay while you choose.
extension EpisodeListPresenter {
    /// Presents the list on top of the player page. The list follows the player through `feed`.
    static func showSwiftUI(controller: ContentPlayerController, feed: PlayerFeed) {
        present(
            UIHostingController(rootView: SwiftUIEpisodeList(controller: controller, feed: feed)),
            backgroundColor: UIColor(argb: RegionColors.panel)
        )
    }
}

private struct SwiftUIEpisodeList: View {
    let controller: ContentPlayerController
    @ObservedObject var feed: PlayerFeed
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        EpisodeListContent(
            ui: feed.episodeListUi(),
            onSelect: { number in
                // The SDK stops at the first locked episode when you select past it.
                controller.selectEpisode(episodeNumber: Int32(number))
                dismiss()
            },
            onDismiss: { dismiss() }
        )
        // Keeps the controls behind the sheet shown, so they are still there when it closes.
        .onAppear { controller.setControlsHeld(held: true) }
        .onDisappear { controller.setControlsHeld(held: false) }
    }
}

/// The episode list as pure UI: no SDK types, so it can be drawn from fixed data.
struct EpisodeListContent: View {
    let ui: EpisodeListUi
    let onSelect: (Int) -> Void
    let onDismiss: () -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 10), count: RegionSlots.episodeColumns)

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text(ui.dramaName)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 32, height: 32)
                        .background(Color(argb: RegionColors.pill), in: Circle())
                }
                .accessibilityLabel("Close")
            }
            ScrollView {
                LazyVGrid(columns: columns, spacing: 10) {
                    ForEach(ui.episodes) { episode in
                        cell(episode)
                    }
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Color(argb: RegionColors.panel).ignoresSafeArea())
    }

    private func cell(_ episode: EpisodeCellUi) -> some View {
        let current = episode.number == ui.currentNumber
        return Button { onSelect(episode.number) } label: {
            ZStack(alignment: .topTrailing) {
                Text("\(episode.number)")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white.opacity(episode.locked ? 0.5 : 1))
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(
                        current ? AnyShapeStyle(DemoColors.accentGradient) : AnyShapeStyle(Color(argb: RegionColors.cell)),
                        in: RoundedRectangle(cornerRadius: 10, style: .continuous)
                    )
                if episode.locked {
                    Image(systemName: "lock.fill")
                        .font(.system(size: 9))
                        .foregroundStyle(.white.opacity(0.7))
                        .padding(4)
                }
            }
        }
    }
}

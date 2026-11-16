import ContentSdk
import SwiftUI

/// The first page of the lab: the dramas of `getDramaList`, to pick the one the player opens.
///
/// `getPlaybackHistory` answers in the log below the list, as does the drama list itself.
struct DramaPicker: View {
    let dramas: [Drama]
    let onPick: (Drama) -> Void
    let onReload: () -> Void
    let onHistory: () -> Void
    let onBack: () -> Void

    /// Log lines shown under the list: the answers of `getDramaList` and `getPlaybackHistory`.
    private let logLines = 12

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 12) {
                    RoundIconButton(symbol: "arrow.left", label: "Back", action: onBack)
                    Text("Player lab")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(DemoColors.textPrimary)
                }
                Text("Pick a drama. The SDK player page opens full screen; the button at the top right opens the lab's own controls and settings, all driven through the player controller.")
                    .font(.system(size: 14))
                    .foregroundStyle(DemoColors.textSecondary)
                ButtonRow {
                    Chip(label: "Reload dramas", action: onReload)
                    Chip(label: "Playback history", action: onHistory)
                }
                SectionLabel("Dramas")
                if dramas.isEmpty {
                    Text("No dramas yet.").font(.system(size: 13)).foregroundStyle(DemoColors.textSecondary)
                } else {
                    ForEach(dramas, id: \.id) { drama in dramaRow(drama) }
                }
                SectionLabel("Log")
                LogLines(max: logLines)
            }
            .padding(20)
        }
        .background(DemoColors.background.ignoresSafeArea())
    }

    /// One drama of `getDramaList`: the cover, the name, and the summary fields the list already
    /// carries (`episodeSize`, `freeCount`, `tags`, `description`). Reading them takes no extra call.
    private func dramaRow(_ drama: Drama) -> some View {
        Button { onPick(drama) } label: {
            HStack(alignment: .top, spacing: 14) {
                AsyncImage(url: URL(string: drama.coverImageUrl)) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    DemoColors.outline
                }
                .frame(width: 72, height: 96)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                VStack(alignment: .leading, spacing: 6) {
                    Text(drama.name)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(DemoColors.textPrimary)
                        .lineLimit(2)
                    Text("\(drama.episodeSize) episodes · first \(drama.freeCount) free")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(DemoColors.accent)
                    if !drama.tags.isEmpty {
                        Text(drama.tags.joined(separator: "  ·  "))
                            .font(.system(size: 11))
                            .foregroundStyle(DemoColors.textSecondary)
                            .lineLimit(1)
                    }
                    if !drama.description_.isEmpty {
                        Text(drama.description_)
                            .font(.system(size: 12))
                            .foregroundStyle(DemoColors.textSecondary)
                            .lineLimit(3)
                            .multilineTextAlignment(.leading)
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(12)
            .background(DemoColors.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(DemoColors.outline, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

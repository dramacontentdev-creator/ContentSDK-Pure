import ContentSdk
import SwiftUI

/*
 * Small building blocks the lab screens share: buttons, option rows, switches, and the log lines.
 * No SDK calls here.
 */

/// Sizes the lab's chrome shares; the panel and the overlay use them to stay out of each other's way.
enum LabLayout {
    /// Round buttons over the video, and their distance from the screen edge.
    static let roundButton: CGFloat = 44
    static let edgeMargin: CGFloat = 12
    /// The control panel never takes more than this share of the screen height, so the video stays visible.
    static let panelMaxHeightFraction: CGFloat = 0.6
    /// The play button is the largest of the transport buttons.
    static let playButton: CGFloat = 60
}

/// A round button with an SF Symbol; `filled` paints it with the accent, otherwise it is dark glass over the video.
struct RoundIconButton: View {
    let symbol: String
    let label: String
    var size: CGFloat = LabLayout.roundButton
    var filled = false
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: size * 0.4, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: size, height: size)
                .background(filled ? DemoColors.accent : Color.black.opacity(0.55), in: Circle())
                .overlay(Circle().stroke(.white.opacity(0.22), lineWidth: 1))
        }
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.4)
        .accessibilityLabel(label)
    }
}

/// A pill-shaped button; `selected` paints it with the accent.
struct Chip: View {
    let label: String
    var selected = false
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(selected ? Color.white : DemoColors.textPrimary)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(selected ? DemoColors.accent : Color.white.opacity(0.1), in: Capsule())
        }
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.4)
    }
}

/// Buttons that wrap onto the next line when the row is full.
struct ButtonRow<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        FlowLayout(spacing: 8) { content }
    }
}

/// Lays its children out left to right and wraps to the next line when the row is full.
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        arrange(in: proposal.width ?? .infinity, subviews: subviews).size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let placed = arrange(in: bounds.width, subviews: subviews)
        for (index, frame) in placed.frames.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY), proposal: .unspecified)
        }
    }

    private func arrange(in width: CGFloat, subviews: Subviews) -> (frames: [CGRect], size: CGSize) {
        var frames: [CGRect] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var maxX: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            frames.append(CGRect(origin: CGPoint(x: x, y: y), size: size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
            maxX = max(maxX, x - spacing)
        }
        return (frames, CGSize(width: maxX, height: y + rowHeight))
    }
}

/// A small grey title above a group of controls.
struct SectionLabel: View {
    let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text.uppercased())
            .font(.system(size: 11))
            .kerning(1.2)
            .foregroundStyle(DemoColors.textSecondary)
    }
}

/// A one-line explanation under a control.
struct Note: View {
    let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text)
            .font(.system(size: 11))
            .foregroundStyle(DemoColors.textSecondary)
            .fixedSize(horizontal: false, vertical: true)
    }
}

/// A titled group of options where the current one is highlighted; one tap switches. Wraps when it is full.
struct Segmented<Value>: View {
    let title: String
    let options: [(value: Value, label: String)]
    var enabled = true
    let isSelected: (Value) -> Bool
    let onSelect: (Value) -> Void

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            Text(title)
                .font(.system(size: 12))
                .foregroundStyle(DemoColors.textSecondary)
                .frame(width: 60, alignment: .leading)
            ButtonRow {
                ForEach(options.indices, id: \.self) { index in
                    Chip(label: options[index].label, selected: isSelected(options[index].value), enabled: enabled) {
                        onSelect(options[index].value)
                    }
                }
            }
        }
    }
}

/// One setting: a title, a one-line explanation, and a switch at the end. `detail` says what the switch
/// does in the SDK's words, so the panel reads like the API it drives.
struct SwitchRow: View {
    let title: String
    let detail: String
    let isOn: Bool
    var enabled = true
    let onChange: (Bool) -> Void

    var body: some View {
        Toggle(isOn: Binding(get: { isOn }, set: onChange)) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(DemoColors.textPrimary)
                Text(detail)
                    .font(.system(size: 11))
                    .foregroundStyle(DemoColors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .tint(DemoColors.accent)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.4)
    }
}

extension LogKind {
    /// The color a log line of this kind is drawn in.
    var color: Color {
        switch self {
        case .api: return DemoColors.info
        case .event: return DemoColors.success
        case .error: return DemoColors.error
        }
    }

    /// The tag in front of a log line of this kind.
    var tag: String {
        switch self {
        case .api: return "API"
        case .event: return "EVENT"
        case .error: return "ERROR"
        }
    }
}

/// The newest `max` log lines, each with a colored tag.
struct LogLines: View {
    @ObservedObject private var log = LabLog.shared
    let max: Int

    var body: some View {
        if log.lines.isEmpty {
            Text("Nothing yet. Events and calls appear here.")
                .font(.system(size: 12))
                .foregroundStyle(DemoColors.textSecondary)
        } else {
            VStack(alignment: .leading, spacing: 6) {
                ForEach(log.lines.prefix(max)) { LogLineRow(line: $0) }
            }
        }
    }
}

/// One log line: time, colored tag, message.
struct LogLineRow: View {
    let line: LogLine

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text(line.time).foregroundStyle(DemoColors.textSecondary)
            Text(line.kind.tag).fontWeight(.bold).foregroundStyle(line.kind.color)
            Text(line.message).foregroundStyle(DemoColors.textPrimary)
        }
        .font(.system(size: 11, design: .monospaced))
    }
}

/// One line of the strip over the video: a colored dot and the message, two lines at most.
struct LiveLine: View {
    let line: LogLine

    var body: some View {
        HStack(spacing: 6) {
            Circle().fill(line.kind.color).frame(width: 6, height: 6)
            Text(line.message)
                .font(.system(size: 10, design: .monospaced))
                .foregroundStyle(.white.opacity(0.92))
                .lineLimit(2)
        }
    }
}

extension PlaybackProgress {
    /// Played part of the episode, 0...1; 0 while the duration is not known yet.
    var fraction: Double {
        durationMs > 0 ? min(max(Double(positionMs) / Double(durationMs), 0), 1) : 0
    }
}

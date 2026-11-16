import SwiftUI

/// The lab's log, in the top-left corner of the SDK player page.
///
/// Collapsed, it is a small strip with the newest lines; only its header takes touches, so taps and
/// swipes on the lines still reach the page. Expanded, it is a scrollable panel with every line and a
/// clear button, and it keeps its touches to itself.
struct LogOverlay: View {
    @ObservedObject private var log = LabLog.shared
    @State private var expanded = false

    /// Below the SDK top bar (56 pt), which holds the back button of the page.
    private let topOffset: CGFloat = 56
    /// Lines the collapsed strip shows, and its width cap so it leaves most of the video visible.
    private let collapsedLines = 3
    private let collapsedMaxWidth: CGFloat = 260
    /// The expanded panel stops short of the right edge, where the panel button sits.
    private let expandedEndClearance = LabLayout.edgeMargin * 2 + LabLayout.roundButton
    private let expandedMaxHeightFraction: CGFloat = 0.45

    var body: some View {
        Group {
            if expanded { expandedLog } else { collapsedLog }
        }
        .padding(.top, topOffset)
        .padding(.leading, LabLayout.edgeMargin)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    /// The strip: a header that opens the panel, then the newest lines, which take no touches.
    private var collapsedLog: some View {
        VStack(alignment: .leading, spacing: 3) {
            header
            VStack(alignment: .leading, spacing: 3) {
                ForEach(log.lines.prefix(collapsedLines)) { LiveLine(line: $0) }
            }
            .allowsHitTesting(false)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .frame(maxWidth: collapsedMaxWidth, alignment: .leading)
        .background(Color.black.opacity(0.55), in: RoundedRectangle(cornerRadius: 12))
        .fixedSize(horizontal: true, vertical: false)
    }

    /// The panel: header with clear and collapse, then every line in a scrollable list.
    private var expandedLog: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                header
                Chip(label: "Clear") { log.clear() }
            }
            if log.lines.isEmpty {
                Note("Nothing yet. SDK calls and playback events appear here.")
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 4) {
                        ForEach(log.lines) { LogLineRow(line: $0) }
                    }
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .frame(maxHeight: UIScreen.main.bounds.height * expandedMaxHeightFraction, alignment: .top)
        .background(DemoColors.surface.opacity(0.96), in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(DemoColors.outline, lineWidth: 1))
        // Touches on the panel stay on the panel and do not reach the player behind it.
        .contentShape(RoundedRectangle(cornerRadius: 16))
        .onTapGesture {}
        .padding(.trailing, expandedEndClearance)
    }

    /// `LOG · n` and a chevron; tapping it toggles between strip and panel.
    private var header: some View {
        Button { expanded.toggle() } label: {
            HStack(spacing: 6) {
                Text("LOG").font(.system(size: 10, weight: .bold)).kerning(1.2)
                Text("· \(log.lines.count)").font(.system(size: 10))
                Spacer(minLength: 8)
                Image(systemName: expanded ? "chevron.up" : "chevron.down").font(.system(size: 10, weight: .semibold))
            }
            .foregroundStyle(DemoColors.textSecondary)
            .padding(.vertical, 2)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(expanded ? "Collapse log" : "Expand log")
    }
}

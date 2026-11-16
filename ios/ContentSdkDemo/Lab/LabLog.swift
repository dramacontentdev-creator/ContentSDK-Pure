import Combine
import Foundation

/// What a log line is about; the lab colors by it.
enum LogKind {
    /// A call the lab made into the SDK, and its direct result.
    case api
    /// A playback event the SDK delivered through `setPlayerEventListener`.
    case event
    /// A failed call or a playback error.
    case error
}

/// One line of the lab's log.
struct LogLine: Identifiable {
    let id = UUID()
    let kind: LogKind
    let time: String
    let message: String
}

/// The lines the lab shows, newest first. Published, so the screen follows it. Capped, so a long
/// session cannot grow it without bound.
final class LabLog: ObservableObject {
    static let shared = LabLog()

    private static let maxLines = 200
    private let clock: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter
    }()

    @Published private(set) var lines: [LogLine] = []

    func add(_ kind: LogKind, _ message: String) {
        guard !message.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        lines.insert(LogLine(kind: kind, time: clock.string(from: Date()), message: message), at: 0)
        if lines.count > Self.maxLines { lines.removeLast(lines.count - Self.maxLines) }
    }

    func clear() {
        lines.removeAll()
    }
}

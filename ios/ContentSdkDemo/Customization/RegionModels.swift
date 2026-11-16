import Foundation

/*
 * What the custom regions draw, as plain data.
 *
 * The UI files in SwiftUI/ and UIKit/ take these types and know nothing about the SDK. The mapping
 * from the SDK's controller state to these types is in PlayerFeed.swift, so "what the SDK gives you"
 * and "what the screen shows" are two separate steps you can read one at a time.
 */

/// What the info region draws: the back button is static, the intro changes with the episode.
struct InfoUi {
    let dramaName: String
    let episodeNumber: Int
    let episodeTitle: String

    /// false until the SDK has bound an episode; the intro is then left out.
    var hasEpisode: Bool { episodeNumber > 0 }
}

/// What the controls region draws.
struct ControlsUi {
    let episodeNumber: Int
    let episodeCount: Int
    let positionMs: Int64
    let durationMs: Int64
    let speed: Float
    /// true while the player waits for data; the timeline shows a moving light.
    let loading: Bool

    var hasPrevious: Bool { episodeNumber > 1 }
    var hasNext: Bool { episodeNumber >= 1 && episodeNumber < episodeCount }

    /// Played part of the episode, 0...1; 0 while the duration is not known yet.
    var fraction: Double {
        durationMs > 0 ? min(max(Double(positionMs) / Double(durationMs), 0), 1) : 0
    }

    /// Seeking needs a known duration; before that the timeline is display only.
    var seekable: Bool { durationMs > 0 }
}

/// One cell of the episode list.
struct EpisodeCellUi: Identifiable, Equatable {
    let number: Int
    let locked: Bool
    var id: Int { number }
}

/// What the episode list draws.
struct EpisodeListUi: Equatable {
    let dramaName: String
    let currentNumber: Int
    let episodes: [EpisodeCellUi]
}

/// Playback speeds the speed button cycles through, in order.
private let speedSteps: [Float] = [1, 1.5, 2]

/// The speed after `current` in 1x, 1.5x, 2x, back to 1x.
func nextSpeed(_ current: Float) -> Float {
    let index = speedSteps.firstIndex(of: current) ?? -1
    return speedSteps[(index + 1) % speedSteps.count]
}

/// `ms` as `m:ss`.
func formatTime(_ ms: Int64) -> String {
    let seconds = max(ms / 1000, 0)
    return String(format: "%d:%02d", Int(seconds / 60), Int(seconds % 60))
}

/// `speed` as the button label, `1x` / `1.5x` / `2x`.
func speedLabel(_ speed: Float) -> String {
    speed.truncatingRemainder(dividingBy: 1) == 0 ? "\(Int(speed))x" : "\(speed)x"
}

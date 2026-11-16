import Combine
import Foundation

/// The switches of the lab's settings panel that outlive one player page.
///
/// The playback options are SDK-wide and the SDK offers no getter, so the lab remembers what it set;
/// the first value of each is the SDK default, and setting one makes the SDK call. The player page
/// style and `autoHide` live in `CustomRegions`, because the SDK keeps regions for the whole process;
/// the panel reads them there.
final class LabSettings: ObservableObject {
    static let shared = LabSettings()

    private let actions = LabActions.shared

    /// Play as soon as an episode is ready. SDK default: `DemoEdition.autoPlayDefault`.
    @Published private(set) var autoPlay = DemoEdition.autoPlayDefault

    /// Move to the next episode when one ends. SDK default: on.
    @Published private(set) var autoAdvance = true

    /// Open a drama where it was left when no episode is named. SDK default: on.
    @Published private(set) var resumeFromHistory = true

    /// Whether the log overlay is drawn over the video. Demo only.
    @Published var logOverlay = true

    func changeAutoPlay(_ enabled: Bool) {
        autoPlay = enabled
        actions.setAutoPlay(enabled)
    }

    func changeAutoAdvance(_ enabled: Bool) {
        autoAdvance = enabled
        actions.setAutoAdvance(enabled)
    }

    func changeResumeFromHistory(_ enabled: Bool) {
        resumeFromHistory = enabled
        actions.setResumeFromHistory(enabled)
    }
}

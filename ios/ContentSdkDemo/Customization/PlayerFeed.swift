import Combine
import ContentSdk
import Foundation

/// Listens to one player controller, once, for every custom region to read.
///
/// **Why one place.** A controller keeps ONE listener of each kind: `setListener`,
/// `setProgressListener`, and `setContentListener` each replace the previous one. The info region,
/// the controls region, the episode list, and the player lab all need playback state, so if each
/// registered its own, the next would silence the one before. Register here and let them all read.
///
/// **How to read it.**
/// - SwiftUI observes the feed (`@ObservedObject`) and reads `state`, `progress`, and `content`.
/// - UIKit views call `observe(_:)` and render again in the callback.
///
/// The SDK hands the same controller to every episode's region, so one feed lives for the whole page
/// and survives episode switches. It forgets the controller when the page is released.
///
/// Call everything on the main thread, as the SDK does.
final class PlayerFeed: NSObject, ObservableObject, ContentPlayerListener, ContentPlayerProgressListener, ContentPlayerContentListener {

    /// Status, play intention, episode, lock, speed, and quality. Changes rarely.
    @Published private(set) var state: ContentPlayerState

    /// Position, duration, and buffered position. Changes about every 300 ms while playing.
    @Published private(set) var progress: PlaybackProgress

    /// The open drama and its episodes, with the lock state of each.
    @Published private(set) var content: ContentPlayerContent

    private let controller: ContentPlayerController
    private var observers: [UUID: () -> Void] = [:]

    private init(controller: ContentPlayerController) {
        self.controller = controller
        state = controller.state
        progress = controller.state.progress
        content = controller.content
        super.init()
        // Every setter calls back once at once with the current value, then on every change.
        controller.setListener(listener: self)
        controller.setProgressListener(listener: self)
        controller.setContentListener(listener: self)
    }

    // MARK: - SDK callbacks (main thread)

    func onStateChanged(state: ContentPlayerState) {
        self.state = state
        if state.playbackState == .released { PlayerFeed.feeds.removeValue(forKey: key(of: controller)) }
        notifyObservers()
    }

    func onProgress(progress: PlaybackProgress) {
        self.progress = progress
        notifyObservers()
    }

    func onContentChanged(content: ContentPlayerContent) {
        self.content = content
        notifyObservers()
    }

    /// Calls `onChange` on the main thread after every update. Call the returned function to stop it.
    func observe(_ onChange: @escaping () -> Void) -> () -> Void {
        let id = UUID()
        observers[id] = onChange
        return { [weak self] in self?.observers.removeValue(forKey: id) }
    }

    private func notifyObservers() {
        Array(observers.values).forEach { $0() }
    }

    // MARK: - From SDK types to what the screens draw

    /// What the info region draws now.
    func infoUi() -> InfoUi {
        let resource = state.resource
        return InfoUi(
            dramaName: resource?.dramaName ?? "",
            episodeNumber: Int(resource?.episodeNumber ?? 0),
            episodeTitle: resource?.episodeName ?? ""
        )
    }

    /// What the controls region draws now.
    func controlsUi() -> ControlsUi {
        let resource = state.resource
        return ControlsUi(
            episodeNumber: Int(resource?.episodeNumber ?? 0),
            episodeCount: Int(resource?.episodeCount ?? 0),
            positionMs: progress.positionMs,
            durationMs: progress.durationMs,
            speed: state.playbackSpeed,
            // The SDK's own timeline shows the same moving light while it waits for data.
            loading: state.playbackState == .preparing || state.playbackState == .buffering
        )
    }

    /// What the episode list draws now.
    func episodeListUi() -> EpisodeListUi {
        EpisodeListUi(
            dramaName: content.drama?.name ?? "",
            currentNumber: Int(state.resource?.episodeNumber ?? 0),
            episodes: content.episodes.map { EpisodeCellUi(number: Int($0.episodeNumber), locked: $0.locked) }
        )
    }

    // MARK: - One feed per controller

    private static var feeds: [ObjectIdentifier: PlayerFeed] = [:]

    /// The feed of `controller`, created on first use. Call on the main thread.
    static func of(_ controller: ContentPlayerController) -> PlayerFeed {
        let id = ObjectIdentifier(controller as AnyObject)
        if let feed = feeds[id] { return feed }
        let feed = PlayerFeed(controller: controller)
        feeds[id] = feed
        return feed
    }

    private func key(of controller: ContentPlayerController) -> ObjectIdentifier {
        ObjectIdentifier(controller as AnyObject)
    }
}

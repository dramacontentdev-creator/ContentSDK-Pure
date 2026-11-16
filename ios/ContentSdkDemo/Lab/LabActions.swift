import Combine
import ContentSdk
import Foundation

/// Content SDK - the player lab's SDK calls (demo only)
///
/// Every SDK call the lab makes, and nothing else: no UI, so each function is a few lines you can
/// copy. Each call writes what it did to `LabLog`, which the log overlay shows. The same calls with
/// explanations are in Catalog/SdkApiCatalog.swift (sections 6 to 11).
///
/// Run everything on the main thread, as the SDK requires for callbacks and listeners.
final class LabActions: ObservableObject {
    static let shared = LabActions()

    private let log = LabLog.shared
    private let sdk = IosContentSdk.shared

    /// The listeners the SDK holds; kept here so they stay alive.
    private var eventHandler: LabEventHandler?
    private var controllerHandler: LabControllerHandler?

    // MARK: - Events and controller (SdkApiCatalog sections 6 and 8)

    /// Starts listening to the SDK. `setPlayerEventListener` and `setPlayerControllerListener` each keep
    /// ONE process-wide listener: setting again replaces the previous, `nil` stops. `onController` gets
    /// the controller of the SDK player page that is shown now, and `nil` when none is.
    func startListening(onController: @escaping (ContentPlayerController?) -> Void) {
        let events = LabEventHandler(actions: self)
        let controllers = LabControllerHandler { [weak self] controller in
            self?.log.add(.api, controller == nil ? "no SDK player page" : "page controller ready")
            self?.resetForPage(controller)
            onController(controller)
        }
        eventHandler = events
        controllerHandler = controllers
        sdk.setPlayerEventListener(listener: events)
        sdk.setPlayerControllerListener(listener: controllers)
        log.add(.api, "setPlayerEventListener + setPlayerControllerListener")
    }

    func stopListening() {
        sdk.setPlayerEventListener(listener: nil)
        sdk.setPlayerControllerListener(listener: nil)
        eventHandler = nil
        controllerHandler = nil
        resetForPage(nil)
        log.add(.api, "listeners removed")
    }

    // MARK: - Playback commands (SdkApiCatalog section 8.2)

    /// A `pause()` holds until the next play from your app or the user.
    func play(_ controller: ContentPlayerController) {
        controller.play()
        log.add(.api, "play")
    }

    func pause(_ controller: ContentPlayerController) {
        controller.pause()
        log.add(.api, "pause")
    }

    /// Play or pause by the play INTENTION (`playWhenReady`), not by the playback state: the intention
    /// stays true while the player waits for data, so a tap while buffering pauses instead of "playing again".
    func togglePlay(_ controller: ContentPlayerController) {
        if controller.state.playWhenReady { pause(controller) } else { play(controller) }
    }

    func seekTo(_ controller: ContentPlayerController, positionMs: Int64) {
        controller.seekTo(positionMs: positionMs)
        log.add(.api, "seekTo \(formatTime(positionMs))")
    }

    func seekBy(_ controller: ContentPlayerController, deltaMs: Int64) {
        controller.seekTo(positionMs: max(controller.state.progress.positionMs + deltaMs, 0))
        log.add(.api, "seekTo \(deltaMs > 0 ? "+" : "")\(deltaMs / 1000)s")
    }

    func selectEpisode(_ controller: ContentPlayerController, number: Int) {
        controller.selectEpisode(episodeNumber: Int32(number))
        log.add(.api, "selectEpisode \(number)")
    }

    /// Steps `delta` episodes from the one on screen (-1 previous, 1 next).
    func episode(_ controller: ContentPlayerController, delta: Int) {
        guard let resource = controller.state.resource else {
            return log.add(.api, "selectEpisode ignored: no episode yet")
        }
        selectEpisode(controller, number: Int(resource.episodeNumber) + delta)
    }

    func setSpeed(_ controller: ContentPlayerController, _ speed: Float) {
        controller.setPlaybackSpeed(speed: speed)
        log.add(.api, "setPlaybackSpeed \(speed)")
    }

    // MARK: - Quality

    /// The stream the player resolved last; the controls show it as the real resolution.
    @Published private(set) var resolvedQuality: PlayerResolvedQuality?

    /// The manual request waiting for its `QualityResolved` event.
    private var pendingQuality: PlayerQualitySelection?

    /// Requests a quality, but only when the stream offers something to switch to. `setQuality` itself
    /// cannot fail: a height the stream lacks quietly resolves to the closest lower one. So this
    /// checks `state.availableQualityHeights` first and reports a failure in the log when there is
    /// nothing to switch to, and `onQualityResolved` reports what the player really ended up with.
    func setQuality(_ controller: ContentPlayerController, _ selection: PlayerQualitySelection) {
        let heights = controller.state.availableQualityHeights.map { $0.intValue }
        let wanted = describe(selection)
        // A stream that lists no other rendition plays one fixed resolution: the one it resolved to.
        let only = heights.count == 1 ? heights[0] : (heights.isEmpty ? resolvedQuality.map { Self.shortEdge($0) } : nil)
        if let only {
            log.add(.error, "Switch to \(wanted) failed: this stream has only one resolution (\(only)p), nothing to switch to")
        } else if heights.isEmpty {
            log.add(.error, "Switch to \(wanted) failed: the stream has not reported its resolutions yet")
        } else if selection.mode == .manual, !heights.contains(Int(selection.requestedHeight)) {
            log.add(.error, "Switch to \(wanted) failed: the stream offers \(heights.map { "\($0)p" }.joined(separator: ", "))")
        } else {
            pendingQuality = selection
            controller.setQuality(selection: selection)
            log.add(.api, "setQuality \(wanted)")
        }
    }

    /// Selection of the Auto button: unconstrained, so the height is 0.
    static let autoQuality = PlayerQualitySelection(mode: .auto_, requestedHeight: 0)

    static func manualQuality(_ height: Int) -> PlayerQualitySelection {
        PlayerQualitySelection(mode: .manual, requestedHeight: Int32(height))
    }

    fileprivate func onQualityResolved(_ episode: String, _ quality: PlayerResolvedQuality) {
        resolvedQuality = quality
        let line = "\(episode) playing \(Self.describeStream(quality))"
        let asked = pendingQuality
        pendingQuality = nil
        if let asked, asked.mode == .manual, Int(asked.requestedHeight) != Self.shortEdge(quality) {
            log.add(.error, "\(line), but \(asked.requestedHeight)p was requested: switch failed")
        } else {
            log.add(.event, line)
        }
    }

    private func describe(_ selection: PlayerQualitySelection) -> String {
        selection.mode == .auto_ ? "Auto" : "\(selection.requestedHeight)p"
    }

    /// The P label of a stream: its shorter side, as in 1080 for a 1080x1920 portrait video.
    static func shortEdge(_ quality: PlayerResolvedQuality) -> Int {
        Int(min(quality.width, quality.height))
    }

    /// The stream as `<P label> (<width>x<height>, <bitrate> Mbps)`, read from the player event.
    static func describeStream(_ quality: PlayerResolvedQuality) -> String {
        let rate = quality.bitrate > 0 ? String(format: ", %.1f Mbps", Double(quality.bitrate) / 1_000_000) : ""
        return "\(shortEdge(quality))p (\(quality.width)x\(quality.height)\(rate))"
    }

    // MARK: - Page controls (SdkApiCatalog sections 8 and 11)

    /// Keeps the page controls shown while your own panel covers the page, and lets them hide again
    /// when it closes. The lab calls it when its control panel opens and closes.
    func holdControls(_ controller: ContentPlayerController, held: Bool) {
        controller.setControlsHeld(held: held)
        log.add(.api, "setControlsHeld(\(held))")
    }

    /// Shows or hides your custom version of `region`. Only a region set with `autoHide: false`
    /// follows this at once; one that hides with the SDK controls keeps the value for later.
    func showRegion(_ controller: ContentPlayerController, _ region: PlayerPageRegion, visible: Bool) {
        if controller === regionPage {
            if visible { hiddenRegions.remove(region) } else { hiddenRegions.insert(region) }
        }
        controller.setRegionVisible(region: region, visible: visible)
        log.add(.api, "setRegionVisible(\(region), \(visible))")
    }

    /// What the lab last set for `region` on the page on screen. The SDK keeps it per page, across
    /// style and `autoHide` changes, and has no getter, so the lab remembers it for its switches.
    func isRegionShown(_ region: PlayerPageRegion) -> Bool {
        !hiddenRegions.contains(region)
    }

    @Published private var hiddenRegions: Set<PlayerPageRegion> = []

    /// The page `hiddenRegions` belongs to.
    private var regionPage: ContentPlayerController?

    /// A new page starts with both regions shown and no stream resolved yet; `nil` drops the old page.
    private func resetForPage(_ controller: ContentPlayerController?) {
        guard controller !== regionPage else { return }
        regionPage = controller
        hiddenRegions = []
        resolvedQuality = nil
        pendingQuality = nil
    }

    // MARK: - Playback options (SdkApiCatalog section 9). Each applies from the next episode of each player.

    func setAutoPlay(_ enabled: Bool) {
        sdk.setAutoPlay(enabled: enabled)
        log.add(.api, "setAutoPlay(\(enabled))")
    }

    func setAutoAdvance(_ enabled: Bool) {
        sdk.setAutoAdvance(enabled: enabled)
        log.add(.api, "setAutoAdvance(\(enabled))")
    }

    func setResumeFromHistory(_ enabled: Bool) {
        sdk.setResumeFromHistory(enabled: enabled)
        log.add(.api, "setResumeFromHistory(\(enabled))")
    }

    // MARK: - Content and history (SdkApiCatalog sections 7 and 10)

    /// `getDramaList`. An empty list means no more data; errors are `SdkException`.
    func loadDramas(completion: @escaping ([Drama]) -> Void) {
        sdk.getDramaList(page: 1, pageSize: 10) { [weak self] dramas, error in
            if let error {
                self?.log.add(.error, "getDramaList failed: \(error.code) \(error.msg)")
            } else {
                self?.log.add(.api, "getDramaList -> \(dramas?.count ?? 0) dramas")
            }
            completion(dramas ?? [])
        }
    }

    /// `getPlaybackHistory`: newest first, per signed-in user on this device.
    func loadHistory() {
        sdk.getPlaybackHistory(limit: 10) { [weak self] records in
            self?.log.add(.api, "getPlaybackHistory -> \(records.count) records")
            for record in records {
                self?.log.add(.api, "\(record.dramaName): episode \(record.episodeNumber)/\(record.episodeCount) at \(record.positionMs / 1000)s")
            }
        }
    }
}

// MARK: - SDK listeners

/// Playback events: one readable log line each; the high-frequency progress ticks are left out.
/// Kotlin sealed classes become Swift subclasses: check them with `as?`.
private final class LabEventHandler: NSObject, PlayerEventListener {
    private weak var actions: LabActions?
    private let log = LabLog.shared

    init(actions: LabActions) {
        self.actions = actions
    }

    func onPlayerEvent(event: PlayerEvent) {
        let episode = event.resource.map { "#\($0.episodeNumber)" } ?? ""
        if let e = event as? PlayerEvent.FirstFrameRendered {
            log.add(.event, "\(episode) first frame in \(e.startupDurationMs) ms")
        } else if let e = event as? PlayerEvent.PlaybackStateChanged {
            log.add(.event, "\(episode) \(e.previousState) -> \(e.state)")
        } else if event is PlayerEvent.PlaybackEnded {
            log.add(.event, "\(episode) ended")
        } else if let e = event as? PlayerEvent.PlaybackError {
            log.add(.error, "\(episode) error \(e.error.code) recoverable=\(e.recoverable)")
        } else if let e = event as? PlayerEvent.PositionDiscontinuity {
            log.add(.event, "\(episode) jump to \(e.progress.positionMs) ms (\(e.reason))")
        } else if let e = event as? PlayerEvent.PlaybackSpeedChanged {
            log.add(.event, "\(episode) speed \(e.speed)x")
        } else if let e = event as? PlayerEvent.QualityChanged {
            log.add(.event, "\(episode) quality requested: \(e.selection.mode == .auto_ ? "Auto" : "\(e.selection.requestedHeight)p")")
        } else if let e = event as? PlayerEvent.QualityResolved {
            actions?.onQualityResolved(episode, e.quality)
        } else if let e = event as? PlayerEvent.ControlDispatched {
            log.add(.event, "\(episode) command \(e.control)")
        }
        // PlayerEvent.PlaybackProgressUpdated is left out on purpose.
    }
}

/// Receives the controller of the SDK player page on screen; nil when the page leaves.
private final class LabControllerHandler: NSObject, PlayerControllerListener {
    private let onChange: (ContentPlayerController?) -> Void

    init(onChange: @escaping (ContentPlayerController?) -> Void) {
        self.onChange = onChange
    }

    func onPlayerControllerChanged(controller: ContentPlayerController?) {
        onChange(controller)
    }
}

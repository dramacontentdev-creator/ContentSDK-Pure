import SwiftUI
import UIKit
import ContentSdk

/*
 =================================================================================================
  Content SDK - API catalog (iOS), shared by both editions
 =================================================================================================

  Every public call of `IosContentSdk.shared` that both editions have, what it is for, and an
  example to copy. Edition-only calls (ad consent, ad events, unlock UI, or your own monetization
  flow) are in EditionApiCatalog.swift. The functions here compile but the demo never calls them.

  Rules that apply to every call:
  - Callbacks and listeners are always delivered on the MAIN thread.
  - `initialize` once at launch; `start` must succeed before any SDK page, `getDramaList`, or
    `createPlayer`. `login` only needs `initialize`.
  - Errors are `SdkException` with `code` and `msg`.

  The catalog is grouped in blocks. Every section title below starts with its block.

  | Block     | What it covers                                                   | Sections                 |
  |-----------|------------------------------------------------------------------|--------------------------|
  | Setup     | initialize, start, logging, strategy, theme, user, options       | Steps 2-3, 1, 2, 3, 5, 9 |
  | Pages     | show the SDK's pages: home, player, UIKit, embedded              | Step 4, 4                |
  | Requests  | ask the SDK for data: drama list, playback history               | 7, 10                    |
  | Callbacks | listen to what happens: playback events, player state, progress  | 6, 8                     |
  | Player    | control a player, replace parts of its page, or use your own     | 8, 11, 12                |
  | Ads       | EditionApiCatalog.swift (commercial: SDK ads; pure: your own)    | C1-C2 / P1               |
  | Reporting | tell the SDK about revenue your app earns itself                 | 13                       |

  By section:

  | API                                        | Required | When to call                        | Section |
  |--------------------------------------------|----------|-------------------------------------|---------|
  | initialize(appId:appSecret:userId:language:)| yes     | app launch, once                    | Step 2  |
  | start(onComplete:)                         | yes      | once per launch                     | Step 3  |
  | dramaHome(title:insetsMode:onDramaClick:)  | yes *    | after start succeeded               | Step 4  |
  | setLoggingEnabled(enabled:)                | optional | any time, usually before initialize | 1       |
  | setStrategy(strategy:)                     | optional | before start                        | 2       |
  | setColorPalette(colors:) / resetColorPalette() | optional | any time; also applies to open pages | 3 |
  | setSubtitleStyle(style:) / resetSubtitleStyle() | optional | any time; overrides subtitle files | 3 |
  | dramaHome(onDramaClick:) variants          | optional | other title / inset combinations    | 4.1     |
  | dramaPlayer(params:onCloseRequest:)        | optional | open one drama directly             | 4.2     |
  | UIKit navigation                           | optional | your app uses UINavigationController| 4.3     |
  | Embedded in your layout (tab)              | optional | SDK page as one tab                 | 4.4     |
  | login(userId:onComplete:)                  | optional | user logs in / switches / logs out  | 5.1     |
  | setLanguage(language:onComplete:)          | optional | your app language changed           | 5.2     |
  | setPlayerEventListener(listener:)          | optional | playback events                     | 6.1     |
  | getDramaList(page:pageSize:...)            | optional | build your own drama entry UI       | 7       |
  | setPlayerControllerListener(listener:)     | optional | control SDK player pages            | 8       |
  | controller.setControlsHeld(held:)          | optional | your sheet covers the player page   | 8.4     |
  | setAutoPlay / setAutoAdvance / setResume...| optional | playback options                    | 9       |
  | getPlaybackHistory / remove... / clear...  | optional | "continue watching" UI              | 10      |
  | setVideoRegion / setInfoRegion / setControls...| optional | replace parts of the player page | 11      |
  | DramaPlayerRegion(autoHide:) / setRegionVisible| optional | show and hide your views yourself | 11     |
  | createPlayer() / playerViewController(...) | optional | the player inside your own UI       | 12      |
  | reportAdRevenue / reportPurchaseRevenue    | optional | revenue your app earns itself       | 13      |

  * Step 4 or any of 4.1 - 4.4 is enough to show SDK content.
 =================================================================================================
 */
enum SdkApiCatalog {

    // MARK: - 1. [Setup] Logging

    /// Prints SDK status lines to the Xcode console, e.g. `I/[ContentSdk][Lifecycle] start_succeeded`.
    /// Secrets and URLs are never printed. Default: off.
    ///
    /// Usually inside `#if DEBUG`, before `initialize` (see ContentSdkDemoApp.swift).
    /// Attach these lines when you report an issue.
    static func enableLogs(_ enabled: Bool) {
        IosContentSdk.shared.setLoggingEnabled(enabled: enabled)
    }

    // MARK: - 2. [Setup] Resource strategy

    /// Chooses how much memory, disk and network the SDK may use. Call BEFORE `start`.
    ///
    /// - `.adaptive` (default, recommended): adjusts to the device automatically. No call needed.
    /// - `.standard`: fixed, balanced for mainstream devices.
    /// - `.lowPerformance`: fixed, lowest memory / disk / data usage.
    /// - `.highPerformance`: fixed, fastest episode switching on high-end devices.
    static func chooseStrategy() {
        IosContentSdk.shared.setStrategy(strategy: ContentSdkStrategy.adaptive)
    }

    // MARK: - 3. [Setup] Theme colors

    /// Switches SDK pages to the built-in dark palette; also applies to SDK pages already open.
    /// Built-in palettes: `Colors.shared.light` (default) and `Colors.shared.dark`.
    static func useDarkTheme() {
        IosContentSdk.shared.setColorPalette(colors: Colors.shared.dark)
    }

    /// Back to the default light palette.
    static func useDefaultTheme() {
        IosContentSdk.shared.resetColorPalette()
    }

    /// Subtitle appearance for all SDK players. Each field you set wins over the subtitle file;
    /// unset fields keep the file's styling or the SDK default. Applies to subtitles already on screen.
    /// Fields: fontScale (0.5...2.0), textColor, backgroundColor, outlineColor, bold, bottomMargin (0...0.4 of the player height).
    static func useLargeSubtitles() {
        let style = SubtitleStyle(
            fontScale: 1.2,
            textColor: Colors.shared.argb(argb: 0xFFFFFF00),
            // Colors.shared.unspecified = not set: keep the subtitle file / SDK default.
            backgroundColor: Colors.shared.unspecified,
            outlineColor: Colors.shared.unspecified,
            bold: nil,
            bottomMargin: nil
        )
        IosContentSdk.shared.setSubtitleStyle(style: style)
        // Back to file styles and SDK defaults: IosContentSdk.shared.resetSubtitleStyle()
    }

    // MARK: - 4. [Pages] Other ways to show SDK pages (pick one; all need `start` to have succeeded)

    /// 4.1 Drama home variants. All return a `UIViewController` you place with your own navigation.
    ///
    /// - title: text in the SDK title bar; `nil` hides the bar (use it under your own navigation bar).
    ///   The variants without `title` keep the SDK default title.
    /// - insetsMode: `.systemBarsPadding` (default) | `.statusBarsPadding` | `.navigationBarsPadding` | `.none`.
    /// - onDramaClick: called when the user taps a drama; open the player there (4.2).
    ///   Without it nothing happens on tap.
    static func makeHomeVariants(onDramaClick: @escaping (Drama) -> Void) -> [UIViewController] {
        [
            IosContentSdk.shared.dramaHome(onDramaClick: onDramaClick),
            IosContentSdk.shared.dramaHome(title: "Dramas", onDramaClick: onDramaClick),
            IosContentSdk.shared.dramaHome(insetsMode: .navigationBarsPadding, onDramaClick: onDramaClick),
            IosContentSdk.shared.dramaHome(title: nil, insetsMode: .navigationBarsPadding, onDramaClick: onDramaClick),
        ]
    }

    /// 4.2 Creates the player for one drama, e.g. from your own list (section 7), a push
    /// notification or a deep link. Push or present the returned controller yourself.
    ///
    /// `Drama.PlayerParams`:
    /// - id: drama id.
    /// - episodeOrder: episode to start from, 1-based. 0 (default) = first episode; above the count = last episode.
    /// - drama: (optional) a Drama you already have, so title and cover show without waiting.
    ///   With a Drama from `getDramaList`, `drama.asPlayerParams()` fills all three.
    ///
    /// - Parameter onClose: the player's back button was tapped; pop / dismiss here.
    static func makePlayer(dramaId: String, onClose: @escaping () -> Void) -> UIViewController {
        IosContentSdk.shared.dramaPlayer(
            params: Drama.PlayerParams(id: dramaId, episodeOrder: 1, drama: nil),
            onCloseRequest: onClose
        )
    }

    /// 4.3 UIKit navigation: push home, then push the player when a drama is tapped.
    static func pushHome(onto navigationController: UINavigationController) {
        // `title: nil` because UINavigationController already shows a bar.
        let home = IosContentSdk.shared.dramaHome(title: nil, insetsMode: .navigationBarsPadding) { [weak navigationController] drama in
            let player = IosContentSdk.shared.dramaPlayer(params: drama.asPlayerParams()) {
                navigationController?.popViewController(animated: true)
            }
            navigationController?.pushViewController(player, animated: true)
        }
        navigationController.pushViewController(home, animated: true)
    }
}

/// 4.4 SDK home as one tab of a SwiftUI `TabView`. Make sure `start` succeeded before it appears.
/// `SdkControllerView` (SdkIntegration.swift) wraps the SDK view controller for SwiftUI.
struct SdkHomeTabExample: View {
    var body: some View {
        TabView {
            SdkControllerView {
                IosContentSdk.shared.dramaHome { drama in
                    // Open the player with your own navigation, see 4.2.
                }
            }
            .ignoresSafeArea()
            .tabItem { Label("Dramas", systemImage: "play.rectangle") }
        }
    }
}

extension SdkApiCatalog {

    // MARK: - 5. [Setup] User and language

    /// 5.1 Tells the SDK which of YOUR users is active, so watch history and unlocks follow them.
    /// Call on login, account switch or logout. Not needed at launch if `initialize` got the userId.
    ///
    /// - Parameter userId: any stable id from your account system; "" = anonymous (use on logout).
    /// - Completion: `error == nil` = new identity active; otherwise the previous identity stays.
    static func onUserChanged(userId: String) {
        IosContentSdk.shared.login(userId: userId) { error in
            if let error { print("login failed: \(error.code) \(error.msg)") }
        }
    }

    /// 5.2 Your app language changed; later content requests use it.
    ///
    /// - Parameter language: BCP-47 code such as "en-US" or "zh-CN". Unsupported codes fall back to "en-US".
    /// - Completion: `effectiveLanguage` is the language the SDK really uses.
    static func onAppLanguageChanged(language: String) {
        IosContentSdk.shared.setLanguage(language: language) { effectiveLanguage, error in
            print("SDK language = \(effectiveLanguage ?? "-"), error = \(error?.code ?? "none")")
        }
    }

    // MARK: - 6. [Callbacks] Event listeners (one per process; setting again replaces; nil stops)

    /// 6.1 Playback events. The SDK keeps a strong reference; call `stopPlayerEvents()` when done.
    ///
    /// Every event has `resource` (nil before an episode is bound): dramaId, dramaName, episodeId,
    /// episodeNumber (1-based), episodeName, episodeCount. Most also have `progress`.
    static func listenToPlayerEvents() {
        IosContentSdk.shared.setPlayerEventListener(listener: PlayerEventHandler())
    }

    /// Stops playback events, e.g. on logout.
    static func stopPlayerEvents() {
        IosContentSdk.shared.setPlayerEventListener(listener: nil)
    }

    // MARK: - 7. [Requests] Drama list (your own entry UI, then open the player with 4.2)

    /// Loads one page of dramas.
    ///
    /// - page: 1-based (>= 1). pageSize: >= 1.
    /// - dramaIds: (optional) only these ids; nil or empty = no filter.
    /// - recentDays: (optional) only dramas published in the last N days (>= 0); nil = no filter.
    ///   Kotlin `Int?` is `KotlinInt?` in Swift: pass `KotlinInt(int: 7)`.
    ///
    /// Each Drama: id, name, coverImageUrl, episodeSize, freeCount, description, tags.
    /// Empty array = no more data. Invalid arguments return code `INVALID_ARGUMENT`.
    static func loadDramas(completion: @escaping ([Drama]) -> Void) {
        IosContentSdk.shared.getDramaList(page: 1, pageSize: 20) { dramas, error in
            if let error { print("getDramaList failed: \(error.code) \(error.msg)") }
            completion(dramas ?? [])
        }
        // Filtered:
        // IosContentSdk.shared.getDramaList(page: 1, pageSize: 20, dramaIds: ["YOUR_DRAMA_ID"], recentDays: KotlinInt(int: 7)) { dramas, error in ... }
    }
}

/// Player listener. Kotlin sealed classes become Swift subclasses: check them with `as?`.
private final class PlayerEventHandler: NSObject, PlayerEventListener {
    func onPlayerEvent(event: PlayerEvent) {
        let episode = event.resource.map { "\($0.dramaName) #\($0.episodeNumber)" } ?? "-"

        if let e = event as? PlayerEvent.FirstFrameRendered {
            // First frame on screen; startupDurationMs = prepare to first frame.
            print("[\(episode)] first frame in \(e.startupDurationMs) ms")
        } else if let e = event as? PlayerEvent.PlaybackStateChanged {
            // Playing, paused, buffering, ended, ...
            if e.state == .playing { print("[\(episode)] playing") }
        } else if let e = event as? PlayerEvent.PlaybackProgressUpdated {
            // About every 300 ms while playing (best-effort, not a precise timer).
            _ = (e.progress.positionMs, e.progress.durationMs)
        } else if event is PlayerEvent.PlaybackEnded {
            print("[\(episode)] finished")
        } else if let e = event as? PlayerEvent.PlaybackError {
            // recoverable = the SDK may retry by itself.
            print("[\(episode)] error \(e.error.code) recoverable=\(e.recoverable)")
        }
        // Also: PositionDiscontinuity (seek), QualityChanged, QualityResolved,
        // PlaybackSpeedChanged, ControlDispatched.
    }
}

// MARK: - 8. [Player] Playback control of SDK player pages

/// 8.1 Receives the controller of the SDK player page on screen; nil when the page leaves.
/// Register once: `IosContentSdk.shared.setPlayerControllerListener(listener: SdkPlayerPages.shared)`.
final class SdkPlayerPages: NSObject, PlayerControllerListener, ContentPlayerListener {
    static let shared = SdkPlayerPages()

    /// The controller of the SDK player page on screen.
    private(set) var controller: ContentPlayerController?

    func onPlayerControllerChanged(controller: ContentPlayerController?) {
        self.controller = controller
        // Called when status, play intention, episode, lock, speed, quality, or duration changes;
        // not for position (use 8.3).
        controller?.setListener(listener: self)
    }

    func onStateChanged(state: ContentPlayerState) {
        print("state=\(state.playbackState) playWhenReady=\(state.playWhenReady) " +
              "episode=\(state.resource?.episodeNumber ?? 0) locked=\(state.episodeLocked)")
    }

    /// 8.2 Commands. A `pause()` holds until the next play from your app or the user: returning to
    /// the foreground, an ad ending, or an episode switch does not resume playback.
    func sendCommands() {
        guard let controller else { return }
        controller.pause()
        controller.play()
        controller.seekTo(positionMs: 30_000)
        controller.setPlaybackSpeed(speed: 1.5)
        controller.setQuality(selection: PlayerQualitySelection(mode: .manual, requestedHeight: 720))
        controller.selectEpisode(episodeNumber: 3)
    }

    /// 8.3 Content for your own intro and episode list, and progress for your own progress bar.
    func readContentAndProgress() {
        guard let controller else { return }
        let content = controller.content
        print("drama=\(content.drama?.name ?? "-") episodes=\(content.episodes.count)")
        controller.setContentListener(listener: EpisodeListPrinter())
        // About every 300 ms while playing; set it only while your progress bar is visible.
        controller.setProgressListener(listener: ProgressPrinter())
        // controller.setProgressListener(listener: nil) when your progress bar goes away.
    }

    /// 8.4 Keep the page controls shown while your own alert or sheet covers the page: they neither
    /// hide while playing nor on a tap. Pass false when it closes. Your custom info and controls
    /// views (section 11) stay shown with them.
    func holdControlsWhileSheetOpen(_ sheetOpen: Bool) {
        controller?.setControlsHeld(held: sheetOpen)
    }
}

private final class EpisodeListPrinter: NSObject, ContentPlayerContentListener {
    func onContentChanged(content: ContentPlayerContent) {
        for episode in content.episodes {
            print("#\(episode.episodeNumber) \(episode.title) locked=\(episode.locked)")
        }
    }
}

private final class ProgressPrinter: NSObject, ContentPlayerProgressListener {
    func onProgress(progress: PlaybackProgress) {
        print("\(progress.positionMs)/\(progress.durationMs)")
    }
}

extension SdkApiCatalog {

    // MARK: - 9. [Setup] Playback options (apply from each player's next episode)

    static func choosePlaybackOptions() {
        // Start playing once an episode is ready. Default: true (commercial), false (pure).
        IosContentSdk.shared.setAutoPlay(enabled: true)
        // Move to the next episode when one finishes. Default: true.
        IosContentSdk.shared.setAutoAdvance(enabled: true)
        // Open a drama without an episode at the recorded episode and position. Default: true.
        IosContentSdk.shared.setResumeFromHistory(enabled: true)
    }

    // MARK: - 10. [Requests] Playback history (per signed-in user, on this device)

    /// Newest first. The SDK keeps the 50 most recently watched dramas and drops a drama not
    /// watched for 30 days. Empty before login.
    static func loadContinueWatching() {
        IosContentSdk.shared.getPlaybackHistory(limit: 20) { records in
            for record in records {
                print("\(record.dramaName): episode \(record.episodeNumber)/\(record.episodeCount) at \(record.positionMs) ms")
            }
        }
    }

    static func forgetHistory(dramaId: String) {
        IosContentSdk.shared.removePlaybackHistory(dramaId: dramaId)
        // Everything for the current user: IosContentSdk.shared.clearPlaybackHistory()
    }

    // MARK: - 11. [Player] Player page regions (video / info / controls; each set on its own)

    /// Each region: keep the SDK version, replace it with your view, or hide it.
    /// Your view covers the whole page and is created again for each episode. Vertical swipes still
    /// switch episodes; taps on your view go to your view.
    static func customizeRegions() {
        // Controls: your own play/pause button instead of the SDK progress bar and episode list.
        IosContentSdk.shared.setControlsRegion(region: DramaPlayerRegion(useDefault: true, view: PlayPauseRegion()))
        // Info: hide the SDK intro and back button.
        IosContentSdk.shared.setInfoRegion(region: DramaPlayerRegion(useDefault: false, view: nil))
        // Video: back to the SDK version.
        IosContentSdk.shared.setVideoRegion(region: DramaPlayerRegion(useDefault: true, view: nil))
    }

    /// Your app decides when the controls show; the info region keeps hiding with the SDK controls.
    /// Each region has its own `autoHide`. A region with `autoHide: false` starts shown.
    static func controlRegionVisibility(controller: ContentPlayerController, showControls: Bool) {
        IosContentSdk.shared.setControlsRegion(region: DramaPlayerRegion(
            useDefault: true, view: PlayPauseRegion(), bottomInsetDp: 0, autoHide: false
        ))
        // Later, for example from your own button or timer:
        controller.setRegionVisible(region: .controls, visible: showControls)
        // Follow the SDK controls in part if you like: controller.state.controlsVisible
    }

    // MARK: - 12. [Player] Your own player (no SDK player page)

    /// Embeds a player in `parent` as a child view controller. The SDK still selects, authorizes,
    /// and preloads episodes. Playback pauses when the view leaves its window and resumes when it
    /// comes back.
    ///
    /// - Returns: the player; call `release()` when your screen goes away.
    static func showOwnPlayer(in parent: UIViewController, container: UIView, dramaId: String) -> ContentPlayer {
        let player = IosContentSdk.shared.createPlayer()
        player.open(params: Drama.PlayerParams(id: dramaId, episodeOrder: 1, drama: nil))
        // .default_ draws the SDK loading, play/pause, progress, and error overlays; .none draws only video and subtitles.
        let playerVC = IosContentSdk.shared.playerViewController(player: player, overlays: .default_)
        parent.addChild(playerVC)
        playerVC.view.frame = container.bounds
        playerVC.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        container.addSubview(playerVC.view)
        playerVC.didMove(toParent: parent)
        // `player` is also a ContentPlayerController: draw your own controls and call play(), seekTo(positionMs:), ...
        return player
    }

    // MARK: - 13. [Reporting] Revenue your app earns itself (every field is required; amounts in US dollars)

    /// 13.1 One impression of an ad YOUR app showed, from the ad platform's paid callback.
    /// `dedupeId` must be the same each time this impression is reported (e.g. AdMob responseIdentifier).
    /// Ads the SDK shows itself (commercial edition) are already reported; do not report them.
    static func reportAdImpression(controller: ContentPlayerController, valueUsd: Double, adSourceName: String, responseId: String) {
        guard let resource = controller.state.resource else { return }
        IosContentSdk.shared.reportAdRevenue(report: AdRevenueReport(
            aboutUnlock: true,
            resource: resource,
            valueUsd: valueUsd,
            precision: .precise,
            adNetworkName: adSourceName.isEmpty ? "unknown" : adSourceName,
            adType: .rewarded,
            dedupeId: responseId
        ))
    }

    /// 13.2 One successful purchase or subscription payment; `dedupeId` is the store transaction id.
    static func reportPurchase(controller: ContentPlayerController, paidUsd: Double, transactionId: String) {
        guard let resource = controller.state.resource else { return }
        IosContentSdk.shared.reportPurchaseRevenue(report: PurchaseRevenueReport(
            aboutUnlock: true,
            resource: resource,
            revenueUsd: paidUsd,
            dedupeId: transactionId
        ))
    }
}

/// Region view for 11: a play/pause button in the bottom-right corner; the rest stays transparent.
private final class PlayPauseRegion: NSObject, DramaPlayerRegionView {
    func createView(controller: ContentPlayerController) -> UIView {
        let container = PassThroughView()
        let button = UIButton(type: .system, primaryAction: UIAction(title: "Play / Pause") { _ in
            if controller.state.playWhenReady { controller.pause() } else { controller.play() }
        })
        button.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(button)
        NSLayoutConstraint.activate([
            button.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -16),
            button.bottomAnchor.constraint(equalTo: container.safeAreaLayoutGuide.bottomAnchor, constant: -16),
        ])
        return container
    }
}

/// Full-page container that lets touches outside its subviews reach the SDK player page.
private final class PassThroughView: UIView {
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        let hit = super.hitTest(point, with: event)
        return hit === self ? nil : hit
    }
}

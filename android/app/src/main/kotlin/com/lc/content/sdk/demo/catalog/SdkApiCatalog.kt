package com.lc.content.sdk.demo.catalog

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lc.content.sdk.AndroidContentSdk
import com.lc.content.sdk.DramaPlayerRegion
import com.lc.content.sdk.DramaPlayerRegionView
import com.lc.content.sdk.api.Drama
import com.lc.content.sdk.api.SdkException
import com.lc.content.sdk.api.event.AdRevenuePrecision
import com.lc.content.sdk.api.event.PlayerEvent
import com.lc.content.sdk.api.event.PlayerPlaybackState
import com.lc.content.sdk.api.event.PlayerQualityMode
import com.lc.content.sdk.api.event.PlayerQualitySelection
import com.lc.content.sdk.api.player.ContentPlayer
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.api.player.ContentPlayerOverlays
import com.lc.content.sdk.api.player.PlayerPageRegion
import com.lc.content.sdk.api.revenue.AdRevenueReport
import com.lc.content.sdk.api.revenue.PurchaseRevenueReport
import com.lc.content.sdk.api.revenue.RevenueAdType
import com.lc.content.sdk.api.strategy.ContentSdkStrategy
import com.lc.content.sdk.api.theme.Colors
import com.lc.content.sdk.api.theme.SubtitleStyle
import com.lc.content.sdk.ui.DramaHomeFragment

/*
 * =================================================================================================
 *  Content SDK - API catalog (Android), shared by both editions
 * =================================================================================================
 *
 *  Every public call of `AndroidContentSdk` that both editions have, what it is for, and an
 *  example to copy. Edition-only calls (ad consent, ad events, unlock UI, or your own monetization
 *  flow) are in EditionApiCatalog.kt. The functions here compile but the demo never calls them.
 *
 *  Rules for every call:
 *  - Callbacks and listeners run on the MAIN thread.
 *  - `initialize` once in Application.onCreate; `start` must succeed before any SDK page,
 *    `getDramaList`, or `createPlayer`. `login` only needs `initialize`.
 *  - Errors are `SdkException` with `code` and `msg`.
 *
 *  The catalog is grouped in blocks. Every section title below starts with its block.
 *
 *  | Block     | What it covers                                                   | Sections                 |
 *  |-----------|------------------------------------------------------------------|--------------------------|
 *  | Setup     | initialize, start, logging, strategy, theme, user, options       | Steps 3-4, 1, 2, 3, 5, 9 |
 *  | Pages     | show the SDK's pages: home, player, Fragment, Compose            | Step 5, 4                |
 *  | Requests  | ask the SDK for data: drama list, playback history               | 7, 10                    |
 *  | Callbacks | listen to what happens: playback events, player state, progress  | 6, 8.1, 8.3              |
 *  | Player    | control a player, replace parts of its page, or use your own     | 8, 11, 12                |
 *  | Ads       | EditionApiCatalog.kt (commercial: SDK ads; pure: your own)       | C1-C3 / P1-P2            |
 *  | Reporting | tell the SDK about revenue your app earns itself                 | 13                       |
 *
 *  By section:
 *
 *  | API                                           | When to call                          | Section |
 *  |-----------------------------------------------|---------------------------------------|---------|
 *  | initialize(application, appId, ...)           | Application.onCreate, once            | Step 3  |
 *  | start()                                       | once per process                      | Step 4  |
 *  | openDramaHomePage(context)                    | after start succeeded                 | Step 5  |
 *  | setLoggingEnabled(enabled)                    | any time, usually before initialize   | 1       |
 *  | setStrategy(strategy)                         | before start                          | 2       |
 *  | setColorPalette / setSubtitleStyle            | any time; also applies to open pages  | 3       |
 *  | openDramaPlayerPage / Fragments / Composables | other ways to show SDK pages          | 4       |
 *  | login(userId) / setLanguage(language)         | user or app language changed          | 5       |
 *  | setPlayerEventListener(listener)              | playback events                       | 6       |
 *  | getDramaList(page, pageSize, ...)             | your own drama entry UI               | 7       |
 *  | setPlayerControllerListener(listener)         | control SDK player pages              | 8       |
 *  | controller.setControlsHeld(held)              | your dialog covers the player page    | 8.4     |
 *  | setAutoPlay / setAutoAdvance / setResume...   | playback options                      | 9       |
 *  | getPlaybackHistory / remove... / clear...     | "continue watching" UI                | 10      |
 *  | setVideoRegion / setInfoRegion / setControls..| replace parts of the SDK player page  | 11      |
 *  | DramaPlayerRegion(autoHide) / setRegionVisible| show and hide your regions yourself   | 11      |
 *  | createPlayer / ContentPlayerView              | the player inside your own UI         | 12      |
 *  | reportAdRevenue / reportPurchaseRevenue       | revenue your app earns itself         | 13      |
 * =================================================================================================
 */
@Suppress("unused")
object SdkApiCatalog {

    private const val TAG = "ContentSdkDemo"

    // ---------------------------------------------------------------------------------------------
    // 1. [Setup] Logging
    // ---------------------------------------------------------------------------------------------

    /**
     * Prints SDK status lines to Logcat (tag "ContentSdk"), e.g. `[Lifecycle] start_succeeded`.
     * Secrets and URLs are never printed. Default: off.
     * Usually `setLoggingEnabled(BuildConfig.DEBUG)` before `initialize` (see DemoApplication.kt).
     */
    fun enableLogs(enabled: Boolean) {
        AndroidContentSdk.setLoggingEnabled(enabled)
    }

    // ---------------------------------------------------------------------------------------------
    // 2. [Setup] Resource strategy
    // ---------------------------------------------------------------------------------------------

    /**
     * How much memory, disk, and network the SDK may use. Call BEFORE `start`.
     * Adaptive (default) adjusts to the device; Standard / LowPerformance / HighPerformance are fixed.
     */
    fun chooseStrategy() {
        AndroidContentSdk.setStrategy(ContentSdkStrategy.Adaptive)
    }

    // ---------------------------------------------------------------------------------------------
    // 3. [Setup] Theme colors and subtitles
    // ---------------------------------------------------------------------------------------------

    /** Built-in palettes: `Colors.light` (default) and `Colors.dark`. Also applies to open pages. */
    fun useDarkTheme() {
        AndroidContentSdk.setColorPalette(Colors.dark)
        // Back to the default: AndroidContentSdk.resetColorPalette()
    }

    /**
     * Fields you set win over the subtitle file; unset fields keep the file or SDK default.
     * fontScale 0.5..2.0, textColor, backgroundColor, outlineColor, bold, bottomMargin 0..0.4.
     */
    fun useLargeSubtitles() {
        AndroidContentSdk.setSubtitleStyle(SubtitleStyle(fontScale = 1.2f, textColor = Colors.argb(0xFFFFFF00)))
        // Back to file styles and SDK defaults: AndroidContentSdk.resetSubtitleStyle()
    }

    // ---------------------------------------------------------------------------------------------
    // 4. [Pages] Other ways to show SDK pages (pick one; all need `start` to have succeeded)
    // ---------------------------------------------------------------------------------------------

    /** 4.1 Full-screen home with a title bar. `title = null` (the default) shows no title bar. */
    fun openHomeWithTitle(context: Context) {
        AndroidContentSdk.openDramaHomePage(context, title = "Dramas")
    }

    /**
     * 4.2 Opens the player full screen for one drama, e.g. from your own list, a push, or a deep link.
     * `Drama.PlayerParams` fields:
     * - `id`: the drama to open (`Drama.id` from `getDramaList`). Required.
     * - `episodeOrder`: 1-based; 0 (default) opens the first episode, or the recorded one (section 10).
     * - `drama`: optional. The `Drama` you already hold from `getDramaList`; the page uses its
     *   summary (such as the name) while the details load. `drama.asPlayerParams()` fills `id` and this.
     */
    fun openPlayer(context: Context, dramaId: String) {
        AndroidContentSdk.openDramaPlayerPage(context, Drama.PlayerParams(id = dramaId, episodeOrder = 1))
        // With a Drama from getDramaList: AndroidContentSdk.openDramaPlayerPage(context, drama.asPlayerParams())
    }

    /**
     * 4.3 Home as a Fragment inside your own screen. Tapping a drama opens the SDK player page.
     *
     * - `title`: text of the title bar; `null` (default) shows none.
     * - `insetsMode`: which system bars the fragment keeps clear of with padding. Pick the one that
     *   matches what your screen already pads: `SystemBarsPadding` (default; status and navigation
     *   bar), `StatusBarsPadding`, `NavigationBarsPadding`, or `None` (draw under the bars).
     */
    fun showHomeFragment(activity: FragmentActivity, containerId: Int) {
        // Add only once; after a rotation the FragmentManager restores it.
        if (activity.supportFragmentManager.findFragmentById(containerId) != null) return
        val fragment = AndroidContentSdk.createDramaHomeFragment(
            title = null,
            insetsMode = DramaHomeFragment.InsetsMode.SystemBarsPadding,
        )
        activity.supportFragmentManager.beginTransaction().replace(containerId, fragment).commit()
    }

    /** 4.4 Player as a Fragment inside your own screen. */
    fun showPlayerFragment(activity: FragmentActivity, containerId: Int, dramaId: String) {
        if (activity.supportFragmentManager.findFragmentById(containerId) != null) return
        val fragment = AndroidContentSdk.createDramaPlayerFragment(Drama.PlayerParams(id = dramaId))
        activity.supportFragmentManager.beginTransaction().replace(containerId, fragment).commit()
    }

    /** 4.5 Home as a Composable. Without `onDramaClick`, the SDK opens its own player page. */
    @Composable
    fun HomeInCompose() {
        AndroidContentSdk.DramaHomeScreen(modifier = Modifier.fillMaxSize())
    }

    /** 4.6 Player as a Composable. `onCloseRequest`: the player asked to close; pop your navigation. */
    @Composable
    fun PlayerInCompose(dramaId: String, onClose: () -> Unit) {
        AndroidContentSdk.DramaPlayerScreen(
            params = Drama.PlayerParams(id = dramaId),
            onCloseRequest = onClose,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // 5. [Setup] User and language
    // ---------------------------------------------------------------------------------------------

    /**
     * 5.1 Which of YOUR users is active, so watch history follows them. Call on login, account
     * switch, or logout ("" = anonymous). Not needed at launch if `initialize` got the user id.
     */
    fun onUserChanged(userId: String) {
        AndroidContentSdk.login(userId) { error ->
            if (error != null) Log.w(TAG, "login failed: ${error.code} ${error.msg}")
        }
    }

    /** 5.2 Your app language changed (BCP-47, e.g. "en-US"). Later content requests use it. */
    fun onAppLanguageChanged(language: String) {
        AndroidContentSdk.setLanguage(language) { effectiveLanguage, error ->
            Log.d(TAG, "SDK language = $effectiveLanguage, error = ${error?.code}")
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 6. [Callbacks] Playback events (one listener per process; setting again replaces; null stops)
    // ---------------------------------------------------------------------------------------------

    /**
     * Every event has `resource` (null before an episode is bound): dramaId, dramaName, episodeId,
     * episodeNumber (1-based), episodeName, episodeCount. Most also have `progress`.
     */
    fun listenToPlayerEvents() {
        AndroidContentSdk.setPlayerEventListener { event ->
            val episode = event.resource?.let { "${it.dramaName} #${it.episodeNumber}" } ?: "-"
            when (event) {
                is PlayerEvent.FirstFrameRendered -> Log.d(TAG, "[$episode] first frame in ${event.startupDurationMs} ms")
                is PlayerEvent.PlaybackStateChanged ->
                    if (event.state == PlayerPlaybackState.Playing) Log.d(TAG, "[$episode] playing")
                is PlayerEvent.PlaybackEnded -> Log.d(TAG, "[$episode] finished")
                is PlayerEvent.PlaybackError -> Log.w(TAG, "[$episode] error ${event.error.code} recoverable=${event.recoverable}")
                // Also: PlaybackProgressUpdated, PositionDiscontinuity, QualityChanged,
                // QualityResolved, PlaybackSpeedChanged, ControlDispatched.
                else -> Unit
            }
        }
    }

    /** Stops playback events, e.g. on logout. */
    fun stopPlayerEvents() {
        AndroidContentSdk.setPlayerEventListener(null)
    }

    // ---------------------------------------------------------------------------------------------
    // 7. [Requests] Drama list (your own entry UI, then open the player with 4.2)
    // ---------------------------------------------------------------------------------------------

    /**
     * page and pageSize start at 1. Optional filters: dramaIds, recentDays.
     * Each Drama: id, name, coverImageUrl, episodeSize, freeCount, description, tags.
     * An empty list means no more data.
     */
    suspend fun loadDramas(): List<Drama> = try {
        AndroidContentSdk.getDramaList(page = 1, pageSize = 20)
        // Filtered: AndroidContentSdk.getDramaList(page = 1, pageSize = 20, dramaIds = listOf("YOUR_DRAMA_ID"), recentDays = 7)
    } catch (error: SdkException) {
        Log.w(TAG, "getDramaList failed: ${error.code} ${error.msg}")
        emptyList()
    }

    // ---------------------------------------------------------------------------------------------
    // 8. [Player] Playback control of SDK player pages
    // ---------------------------------------------------------------------------------------------

    /** The controller of the SDK player page on screen; null when none is shown. */
    private var playerController: ContentPlayerController? = null

    /**
     * 8.1 Get the controller of the SDK player page on screen (Activity, Fragment, or Compose).
     * Set it once, e.g. in Application.onCreate; it survives Activity recreation.
     * The listener gets a new controller for each page and null when the page leaves.
     */
    fun followSdkPlayerPages() {
        AndroidContentSdk.setPlayerControllerListener { controller ->
            playerController = controller
            // Called when status, play intention, episode, lock, speed, quality, or duration changes;
            // not for position (use 8.3).
            controller?.setListener { state ->
                Log.d(
                    TAG,
                    "state=${state.playbackState} playWhenReady=${state.playWhenReady} " +
                        "episode=${state.resource?.episodeNumber} locked=${state.episodeLocked}",
                )
            }
        }
    }

    /**
     * 8.2 Commands. A `pause()` holds until the next play from your app or the user: returning to
     * the foreground, an ad ending, or an episode switch does not resume playback.
     */
    fun sendCommands() {
        val controller = playerController ?: return
        controller.pause()
        controller.play()
        controller.seekTo(30_000)
        controller.setPlaybackSpeed(1.5f)
        controller.selectEpisode(3)
    }

    /**
     * 8.2b Quality. Offer only what the stream really has: `state.availableQualityHeights` lists the
     * short-edge heights it offers (highest first; empty until the stream is inspected). `setQuality`
     * never fails: a height the stream lacks quietly resolves to the closest lower one, so check the
     * list first. With one entry there is nothing to switch to. The resolution that actually plays
     * arrives as `PlayerEvent.QualityResolved` (`width`, `height`, `bitrate`); the player lab shows it.
     */
    fun switchQuality() {
        val controller = playerController ?: return
        val offered = controller.state.availableQualityHeights
        if (offered.size > 1) {
            controller.setQuality(PlayerQualitySelection(PlayerQualityMode.Manual, requestedHeight = offered.last()))
        }
        // Back to automatic: controller.setQuality(PlayerQualitySelection(PlayerQualityMode.Auto, requestedHeight = 0))
    }

    /** 8.3 Content for your own intro and episode list, and progress for your own progress bar. */
    fun readContentAndProgress() {
        val controller = playerController ?: return
        val content = controller.content
        Log.d(TAG, "drama=${content.drama?.name} episodes=${content.episodes.size}")
        controller.setContentListener { changed ->
            changed.episodes.forEach { Log.d(TAG, "#${it.episodeNumber} ${it.title} locked=${it.locked}") }
        }
        // About every 300 ms while playing; set it only while your progress bar is visible.
        controller.setProgressListener { progress -> Log.d(TAG, "${progress.positionMs}/${progress.durationMs}") }
        // controller.setProgressListener(null) when your progress bar goes away.
    }

    /**
     * 8.4 Keep the page controls shown while your own dialog or panel covers the page: they neither
     * hide while playing nor on a tap. Pass false when it closes. Your custom info and controls
     * content (section 11) stays shown with them.
     */
    fun holdControlsWhileDialogOpen(dialogOpen: Boolean) {
        playerController?.setControlsHeld(dialogOpen)
    }

    // ---------------------------------------------------------------------------------------------
    // 9. [Setup] Playback options (apply from each player's next episode)
    // ---------------------------------------------------------------------------------------------

    fun choosePlaybackOptions() {
        // Start playing once an episode is ready. Default: true (commercial), false (pure).
        AndroidContentSdk.setAutoPlay(true)
        // Move to the next episode when one finishes. Default: true.
        AndroidContentSdk.setAutoAdvance(true)
        // Open a drama without an episode at the recorded episode and position. Default: true.
        AndroidContentSdk.setResumeFromHistory(true)
    }

    // ---------------------------------------------------------------------------------------------
    // 10. [Requests] Playback history (per signed-in user, on this device)
    // ---------------------------------------------------------------------------------------------

    /**
     * Newest first. The SDK keeps the 50 most recently watched dramas and drops a drama not
     * watched for 30 days. Empty before login.
     */
    suspend fun loadContinueWatching() {
        val records = AndroidContentSdk.getPlaybackHistory(limit = 20)
        records.forEach { record ->
            Log.d(TAG, "${record.dramaName}: episode ${record.episodeNumber}/${record.episodeCount} at ${record.positionMs} ms")
        }
        // Callback version: AndroidContentSdk.getPlaybackHistory(20) { records -> ... }
    }

    fun forgetHistory(dramaId: String) {
        AndroidContentSdk.removePlaybackHistory(dramaId)
        // Everything for the current user: AndroidContentSdk.clearPlaybackHistory()
    }

    // ---------------------------------------------------------------------------------------------
    // 11. [Player] Player page regions (video / info / controls; each set on its own)
    // ---------------------------------------------------------------------------------------------

    /**
     * Each region: keep the SDK version, replace it with your view, or hide it.
     * Your view covers the whole page and is created again for each episode. Leave empty areas
     * without click listeners, so swipes and taps still reach the page.
     *
     * `DramaPlayerRegion` parameters:
     * - `useDefault`: `true` (default) keeps the SDK version when you give no `content` or `view`;
     *   `false` leaves the region empty.
     * - `content`: Compose content of the region. Wins over `view` when both are given.
     * - `view`: a `DramaPlayerRegionView { context, controller -> View }` factory for Android Views.
     * - `bottomInsetDp`: how much of the page's bottom edge, system bars included, your content
     *   occupies. Subtitles stay above it. The SDK keeps subtitles 124 dp above the bottom edge
     *   (112 dp of clearance plus a 12 dp gap); your value replaces the 112 when it is larger and the
     *   12 dp gap is added on top. Set it only when your content is taller. Default 0.
     * - `autoHide`: `true` (default) shows and hides your info or controls content with the SDK
     *   controls; `false` keeps it shown until you call `controller.setRegionVisible` (see
     *   [controlRegionVisibility]).
     *
     * Regions: `video` (cover, center play button, error), `info` (back button and intro),
     * `controls` (timeline, episode and speed buttons). The info and controls content show and hide
     * with the SDK controls unless `autoHide = false`; the video region stays.
     *
     * A runnable version with custom info and controls regions (View and Compose) is in
     * customization/CustomRegions.kt: pick a style on the demo home screen, then open the player.
     */
    fun customizeRegions() {
        // Controls: your own play/pause button instead of the SDK progress bar and episode list.
        AndroidContentSdk.setControlsRegion(
            DramaPlayerRegion(
                view = DramaPlayerRegionView { context, controller ->
                    FrameLayout(context).apply {
                        addView(
                            Button(context).apply {
                                text = "Play / Pause"
                                setOnClickListener {
                                    if (controller.state.playWhenReady) controller.pause() else controller.play()
                                }
                            },
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                Gravity.BOTTOM or Gravity.END,
                            ),
                        )
                    }
                },
            ),
        )
        // Info: hide the SDK intro and back button.
        AndroidContentSdk.setInfoRegion(DramaPlayerRegion(useDefault = false))
        // Video: back to the SDK version.
        AndroidContentSdk.setVideoRegion(DramaPlayerRegion())
    }

    /** Same with Compose content; `content` wins over `view` when both are given. */
    fun customizeRegionsWithCompose() {
        AndroidContentSdk.setControlsRegion(
            DramaPlayerRegion(
                content = { controller ->
                    Box(Modifier.fillMaxSize()) {
                        androidx.compose.material3.Button(
                            onClick = { if (controller.state.playWhenReady) controller.pause() else controller.play() },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        ) {
                            Text("Play / Pause")
                        }
                    }
                },
            ),
        )
    }

    /**
     * Your app decides when the controls show; the info region keeps hiding with the SDK controls.
     * Each region has its own `autoHide`. A region with `autoHide = false` starts shown.
     */
    fun controlRegionVisibility(controller: ContentPlayerController, showControls: Boolean) {
        AndroidContentSdk.setControlsRegion(
            DramaPlayerRegion(
                view = DramaPlayerRegionView { context, _ -> FrameLayout(context) },
                autoHide = false,
            ),
        )
        // Later, for example from your own button or timer:
        controller.setRegionVisible(PlayerPageRegion.Controls, showControls)
        // Follow the SDK controls in part if you like: controller.state.controlsVisible
    }

    // ---------------------------------------------------------------------------------------------
    // 12. [Player] Your own player (no SDK player page)
    // ---------------------------------------------------------------------------------------------

    /**
     * 12.1 View version. The SDK still selects, authorizes, and preloads episodes.
     * Playback pauses when the view is removed, recycled, or hidden, and resumes when it is shown.
     *
     * @return the player; call `release()` when your screen goes away.
     */
    fun showOwnPlayer(container: ViewGroup, dramaId: String): ContentPlayer {
        val player = AndroidContentSdk.createPlayer()
        player.open(Drama.PlayerParams(id = dramaId, episodeOrder = 1))
        // Default draws the SDK loading, play/pause, progress, and error overlays; None draws only video and subtitles.
        container.addView(AndroidContentSdk.createPlayerView(container.context, player, ContentPlayerOverlays.Default))
        return player
    }

    /** 12.2 Compose version. Playback pauses when the view leaves the composition. */
    @Composable
    fun OwnPlayerInCompose(dramaId: String) {
        val player = remember(dramaId) {
            AndroidContentSdk.createPlayer().also { it.open(Drama.PlayerParams(id = dramaId)) }
        }
        DisposableEffect(player) { onDispose { player.release() } }
        AndroidContentSdk.ContentPlayerView(player, Modifier.fillMaxSize(), ContentPlayerOverlays.None)
        // `player` is also a ContentPlayerController: draw your own controls and call play(), seekTo(), ...
    }

    // ---------------------------------------------------------------------------------------------
    // 13. [Reporting] Revenue your app earns itself (every field is required; amounts in US dollars)
    // ---------------------------------------------------------------------------------------------

    /**
     * 13.1 One impression of an ad YOUR app showed, from the ad platform's paid callback.
     * Ads the SDK shows itself (commercial edition) are already reported; do not report them.
     *
     * `AdRevenueReport` fields (all required):
     * - `aboutUnlock`: `true` when the ad was shown to unlock this episode.
     * - `resource` (or `dramaId`, `episodeNumber`, `episodeId`): the episode the ad belongs to;
     *   `controller.state.resource` gives it.
     * - `valueUsd`: this impression's value in US dollars (not eCPM), e.g. 0.0015; 0 when the ad
     *   platform reports none. The currency is always sent as `USD`.
     * - `precision`: how the platform determined the value: `Unknown`, `Estimated`,
     *   `PublisherProvided`, or `Precise` (AdMob's precision type).
     * - `adNetworkName`: the ad network that served it, as your mediation reports it; "unknown" if none.
     * - `adType`: `Rewarded`, `Interstitial`, `AppOpen`, `Native`, `Banner`, `RewardedInterstitial`, or `Other`.
     * - `dedupeId`: the same value every time THIS impression is reported, e.g. AdMob's
     *   `responseInfo.responseId`. A report with an id already reported is dropped.
     */
    fun reportAdImpression(controller: ContentPlayerController, valueUsd: Double, adSourceName: String, responseId: String) {
        val resource = controller.state.resource ?: return
        AndroidContentSdk.reportAdRevenue(
            AdRevenueReport(
                aboutUnlock = true,
                resource = resource,
                valueUsd = valueUsd,
                precision = AdRevenuePrecision.Precise,
                adNetworkName = adSourceName.ifBlank { "unknown" },
                adType = RevenueAdType.Rewarded,
                dedupeId = responseId,
            ),
        )
    }

    /**
     * 13.2 One successful purchase or subscription payment.
     * `PurchaseRevenueReport` fields (all required): `aboutUnlock` and the episode as in 13.1;
     * `revenueUsd`: the amount the user actually paid, in US dollars; `dedupeId`: the same value
     * every time this payment is reported, e.g. the Google Play `orderId`.
     */
    fun reportPurchase(controller: ContentPlayerController, paidUsd: Double, orderId: String) {
        val resource = controller.state.resource ?: return
        AndroidContentSdk.reportPurchaseRevenue(
            PurchaseRevenueReport(aboutUnlock = true, resource = resource, revenueUsd = paidUsd, dedupeId = orderId),
        )
    }
}

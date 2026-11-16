package com.lc.content.sdk.demo.lab

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lc.content.sdk.AndroidContentSdk
import com.lc.content.sdk.api.Drama
import com.lc.content.sdk.api.SdkException
import com.lc.content.sdk.api.event.PlayerEvent
import com.lc.content.sdk.api.event.PlayerQualityMode
import com.lc.content.sdk.api.event.PlayerQualitySelection
import com.lc.content.sdk.api.event.PlayerResolvedQuality
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.api.player.PlayerPageRegion
import com.lc.content.sdk.demo.customization.formatTime

/**
 * Content SDK - the player lab's SDK calls (demo only)
 *
 * Every SDK call the lab makes, and nothing else: no UI, so each function is a few lines you can
 * copy. Each call writes what it did to [LabLog], which the log overlay shows. The same calls with
 * explanations are in catalog/SdkApiCatalog.kt (sections 6 to 11).
 *
 * Run everything on the main thread, as the SDK requires for callbacks and listeners.
 */
object LabActions {

    // -------------------------------------------------------------------------------------------
    // Events and controller (SdkApiCatalog sections 6 and 8)
    // -------------------------------------------------------------------------------------------

    /**
     * Starts listening to the SDK. `setPlayerEventListener` and `setPlayerControllerListener` each keep
     * ONE process-wide listener: setting again replaces the previous, `null` stops. [onController] gets
     * the controller of the SDK player page that is shown now, and `null` when none is.
     */
    fun startListening(onController: (ContentPlayerController?) -> Unit) {
        AndroidContentSdk.setPlayerEventListener(::onEvent)
        AndroidContentSdk.setPlayerControllerListener { controller ->
            LabLog.add(LogKind.Api, if (controller == null) "no SDK player page" else "page controller ready")
            resetForPage(controller)
            onController(controller)
        }
        LabLog.add(LogKind.Api, "setPlayerEventListener + setPlayerControllerListener")
    }

    fun stopListening() {
        AndroidContentSdk.setPlayerEventListener(null)
        AndroidContentSdk.setPlayerControllerListener(null)
        resetForPage(null)
        LabLog.add(LogKind.Api, "listeners removed")
    }

    /** A new page starts with both regions shown and no stream resolved yet; `null` drops the old page. */
    private fun resetForPage(controller: ContentPlayerController?) {
        if (controller === regionPage) return
        regionPage = controller
        hiddenRegions = emptySet()
        resolvedQuality = null
        pendingQuality = null
    }

    /** One readable log line per event; the high-frequency progress ticks are left out. */
    private fun onEvent(event: PlayerEvent) {
        val episode = event.resource?.let { "#${it.episodeNumber}" }.orEmpty()
        val line = when (event) {
            is PlayerEvent.FirstFrameRendered -> "$episode first frame in ${event.startupDurationMs} ms"
            is PlayerEvent.PlaybackStateChanged -> "$episode ${event.previousState} -> ${event.state}"
            is PlayerEvent.PlaybackEnded -> "$episode ended"
            is PlayerEvent.PlaybackError -> {
                LabLog.add(LogKind.Error, "$episode error ${event.error.code} recoverable=${event.recoverable}")
                return
            }
            is PlayerEvent.PositionDiscontinuity -> "$episode jump to ${event.progress.positionMs} ms (${event.reason})"
            is PlayerEvent.PlaybackSpeedChanged -> "$episode speed ${event.speed}x"
            is PlayerEvent.QualityChanged -> "$episode quality requested: ${describe(event.selection)}"
            is PlayerEvent.QualityResolved -> {
                onQualityResolved(episode, event.quality)
                return
            }
            is PlayerEvent.ControlDispatched -> "$episode command ${event.control}"
            is PlayerEvent.PlaybackProgressUpdated -> return
        }
        LabLog.add(LogKind.Event, line)
    }

    // -------------------------------------------------------------------------------------------
    // Playback commands (SdkApiCatalog section 8.2)
    // -------------------------------------------------------------------------------------------

    /** A `pause()` holds until the next play from your app or the user. */
    fun play(controller: ContentPlayerController) = command("play") { controller.play() }

    fun pause(controller: ContentPlayerController) = command("pause") { controller.pause() }

    /**
     * Play or pause by the play INTENTION (`playWhenReady`), not by the playback state: the intention
     * stays true while the player waits for data, so a tap while buffering pauses instead of "playing again".
     */
    fun togglePlay(controller: ContentPlayerController) {
        if (controller.state.playWhenReady) pause(controller) else play(controller)
    }

    fun seekTo(controller: ContentPlayerController, positionMs: Long) =
        command("seekTo ${formatTime(positionMs)}") { controller.seekTo(positionMs) }

    fun seekBy(controller: ContentPlayerController, deltaMs: Long) =
        command("seekTo ${if (deltaMs > 0) "+" else ""}${deltaMs / 1000}s") {
            controller.seekTo((controller.state.progress.positionMs + deltaMs).coerceAtLeast(0))
        }

    fun selectEpisode(controller: ContentPlayerController, episodeNumber: Int) =
        command("selectEpisode $episodeNumber") { controller.selectEpisode(episodeNumber) }

    /** Steps [delta] episodes from the one on screen (-1 previous, 1 next). */
    fun episode(controller: ContentPlayerController, delta: Int) {
        val resource = controller.state.resource ?: return LabLog.add(LogKind.Api, "selectEpisode ignored: no episode yet")
        selectEpisode(controller, resource.episodeNumber + delta)
    }

    fun speed(controller: ContentPlayerController, speed: Float) =
        command("setPlaybackSpeed $speed") { controller.setPlaybackSpeed(speed) }

    /**
     * Requests a quality, but only when the stream offers something to switch to. `setQuality` itself
     * cannot fail: a height the stream lacks quietly resolves to the closest lower one. So this
     * checks `state.availableQualityHeights` first and reports a failure in the log when there is
     * nothing to switch to, and [onQualityResolved] reports what the player really ended up with.
     */
    fun quality(controller: ContentPlayerController, selection: PlayerQualitySelection) {
        val heights = controller.state.availableQualityHeights
        val wanted = describe(selection)
        // A stream that lists no other rendition plays one fixed resolution: the one it resolved to.
        val only = heights.singleOrNull() ?: resolvedQuality?.shortEdge.takeIf { heights.isEmpty() }
        when {
            only != null ->
                LabLog.add(LogKind.Error, "Switch to $wanted failed: this stream has only one resolution (${only}p), nothing to switch to")

            heights.isEmpty() ->
                LabLog.add(LogKind.Error, "Switch to $wanted failed: the stream has not reported its resolutions yet")

            selection.mode == PlayerQualityMode.Manual && selection.requestedHeight !in heights ->
                LabLog.add(LogKind.Error, "Switch to $wanted failed: the stream offers ${heights.joinToString { "${it}p" }}")

            else -> {
                pendingQuality = selection
                command("setQuality $wanted") { controller.setQuality(selection) }
            }
        }
    }

    /** The stream the player resolved last; the controls show it as the real resolution. */
    var resolvedQuality by mutableStateOf<PlayerResolvedQuality?>(null)
        private set

    /** The manual or automatic request waiting for its [PlayerEvent.QualityResolved]. */
    private var pendingQuality: PlayerQualitySelection? = null

    private fun onQualityResolved(episode: String, quality: PlayerResolvedQuality) {
        resolvedQuality = quality
        val line = "$episode playing ${quality.describeStream()}"
        val asked = pendingQuality
        pendingQuality = null
        if (asked != null && asked.mode == PlayerQualityMode.Manual && asked.requestedHeight != quality.shortEdge) {
            LabLog.add(LogKind.Error, "$line, but ${asked.requestedHeight}p was requested: switch failed")
        } else {
            LabLog.add(LogKind.Event, line)
        }
    }

    private fun describe(selection: PlayerQualitySelection) =
        if (selection.mode == PlayerQualityMode.Auto) "Auto" else "${selection.requestedHeight}p"

    /** Selection of the Auto button: unconstrained, so the height is 0. */
    val AutoQuality = PlayerQualitySelection(PlayerQualityMode.Auto, requestedHeight = 0)

    fun manualQuality(height: Int) = PlayerQualitySelection(PlayerQualityMode.Manual, requestedHeight = height)

    // -------------------------------------------------------------------------------------------
    // Page controls (SdkApiCatalog sections 8 and 11)
    // -------------------------------------------------------------------------------------------

    /**
     * Keeps the page controls shown while your own panel covers the page, and lets them hide again
     * when it closes. The lab calls it when its control panel opens and closes.
     */
    fun holdControls(controller: ContentPlayerController, held: Boolean) =
        command("setControlsHeld($held)") { controller.setControlsHeld(held) }

    /**
     * Shows or hides your custom version of [region]. Only a region set with `autoHide = false`
     * follows this at once; one that hides with the SDK controls keeps the value for later.
     */
    fun showRegion(controller: ContentPlayerController, region: PlayerPageRegion, visible: Boolean) {
        if (controller === regionPage) hiddenRegions = if (visible) hiddenRegions - region else hiddenRegions + region
        command("setRegionVisible($region, $visible)") { controller.setRegionVisible(region, visible) }
    }

    /**
     * What the lab last set for [region] on the page on screen. The SDK keeps it per page, across
     * style and `autoHide` changes, and has no getter, so the lab remembers it for its switches.
     */
    fun isRegionShown(region: PlayerPageRegion): Boolean = region !in hiddenRegions

    /** The page [hiddenRegions] belongs to. */
    private var regionPage: ContentPlayerController? = null

    private var hiddenRegions by mutableStateOf(emptySet<PlayerPageRegion>())

    private inline fun command(name: String, action: () -> Unit) {
        action()
        LabLog.add(LogKind.Api, name)
    }

    // -------------------------------------------------------------------------------------------
    // Playback options (SdkApiCatalog section 9). Each applies from the next episode of each player.
    // -------------------------------------------------------------------------------------------

    fun setAutoPlay(enabled: Boolean) {
        AndroidContentSdk.setAutoPlay(enabled)
        LabLog.add(LogKind.Api, "setAutoPlay($enabled)")
    }

    fun setAutoAdvance(enabled: Boolean) {
        AndroidContentSdk.setAutoAdvance(enabled)
        LabLog.add(LogKind.Api, "setAutoAdvance($enabled)")
    }

    fun setResumeFromHistory(enabled: Boolean) {
        AndroidContentSdk.setResumeFromHistory(enabled)
        LabLog.add(LogKind.Api, "setResumeFromHistory($enabled)")
    }

    // -------------------------------------------------------------------------------------------
    // Content and history (SdkApiCatalog sections 7 and 10)
    // -------------------------------------------------------------------------------------------

    /** `getDramaList` (suspend). An empty list means no more data; errors are [SdkException]. */
    suspend fun loadDramas(): List<Drama> = try {
        AndroidContentSdk.getDramaList(page = 1, pageSize = 10)
            .also { LabLog.add(LogKind.Api, "getDramaList -> ${it.size} dramas") }
    } catch (error: SdkException) {
        LabLog.add(LogKind.Error, "getDramaList failed: ${error.code} ${error.msg}")
        emptyList()
    }

    /** `getPlaybackHistory`: newest first, per signed-in user on this device. */
    suspend fun loadHistory() {
        val records = AndroidContentSdk.getPlaybackHistory(limit = 10)
        LabLog.add(LogKind.Api, "getPlaybackHistory -> ${records.size} records")
        records.forEach {
            LabLog.add(LogKind.Api, "${it.dramaName}: episode ${it.episodeNumber}/${it.episodeCount} at ${it.positionMs / 1000}s")
        }
    }
}

/** The P label of a stream: its shorter side, as in 1080 for a 1080x1920 portrait video. */
internal val PlayerResolvedQuality.shortEdge: Int get() = minOf(width, height)

/** The stream as `<P label> (<width>x<height>, <bitrate> Mbps)`, read from the player event. */
internal fun PlayerResolvedQuality.describeStream(): String {
    val mbps = bitrate / 1_000_000.0
    val rate = if (bitrate > 0) ", ${"%.1f".format(mbps)} Mbps" else ""
    return "${shortEdge}p (${width}x$height$rate)"
}

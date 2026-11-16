package com.lc.content.sdk.demo.customization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lc.content.sdk.api.event.PlaybackProgress
import com.lc.content.sdk.api.event.PlayerPlaybackState
import com.lc.content.sdk.api.player.ContentPlayerContent
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.api.player.ContentPlayerState

/**
 * Listens to one player controller, once, for every custom region to read.
 *
 * **Why one place.** A controller keeps ONE listener of each kind: `setListener`,
 * `setProgressListener`, and `setContentListener` each replace the previous one. The info region,
 * the controls region, the episode list, and the player lab all need playback state, so if each
 * registered its own, the next would silence the one before. Register here and let them all read.
 *
 * **How to read it.**
 * - Compose reads [state], [progress], and [content]. They are snapshot state, so a read recomposes.
 * - Views call [observe] and render again in the callback.
 *
 * The SDK hands the same controller to every episode's region, so one feed lives for the whole page
 * and survives episode switches. It forgets the controller when the page is released.
 *
 * Call everything on the main thread, as the SDK does.
 */
class PlayerFeed private constructor(private val controller: ContentPlayerController) {

    /** Status, play intention, episode, lock, speed, and quality. Changes rarely. */
    var state: ContentPlayerState by mutableStateOf(controller.state)
        private set

    /** Position, duration, and buffered position. Changes about every 300 ms while playing. */
    var progress: PlaybackProgress by mutableStateOf(controller.state.progress)
        private set

    /** The open drama and its episodes, with the lock state of each. */
    var content: ContentPlayerContent by mutableStateOf(controller.content)
        private set

    private val observers = mutableListOf<() -> Unit>()

    init {
        // Every setter calls back once at once with the current value, then on every change.
        controller.setListener { next ->
            state = next
            if (next.playbackState == PlayerPlaybackState.Released) feeds.remove(controller)
            notifyObservers()
        }
        controller.setProgressListener { next ->
            progress = next
            notifyObservers()
        }
        controller.setContentListener { next ->
            content = next
            notifyObservers()
        }
    }

    /** Calls [onChange] on the main thread after every update. The returned function stops it. */
    fun observe(onChange: () -> Unit): () -> Unit {
        observers += onChange
        return { observers -= onChange }
    }

    private fun notifyObservers() = observers.toList().forEach { it() }

    // -- From SDK types to what the screens draw ------------------------------------------------

    /** @return What the info region draws now. */
    internal fun infoUi(): InfoUi {
        val resource = state.resource
        return InfoUi(
            dramaName = resource?.dramaName.orEmpty(),
            episodeNumber = resource?.episodeNumber ?: 0,
            episodeTitle = resource?.episodeName.orEmpty(),
        )
    }

    /** @return What the controls region draws now. */
    internal fun controlsUi(): ControlsUi {
        val resource = state.resource
        return ControlsUi(
            episodeNumber = resource?.episodeNumber ?: 0,
            episodeCount = resource?.episodeCount ?: 0,
            positionMs = progress.positionMs,
            durationMs = progress.durationMs,
            speed = state.playbackSpeed,
            // The SDK's own timeline shows the same moving light while it waits for data.
            loading = state.playbackState == PlayerPlaybackState.Preparing ||
                state.playbackState == PlayerPlaybackState.Buffering,
        )
    }

    /** @return What the episode list draws now. */
    internal fun episodeListUi() = EpisodeListUi(
        dramaName = content.drama?.name.orEmpty(),
        currentNumber = state.resource?.episodeNumber ?: 0,
        episodes = content.episodes.map { EpisodeCellUi(number = it.episodeNumber, locked = it.locked) },
    )

    companion object {
        private val feeds = HashMap<ContentPlayerController, PlayerFeed>()

        /** @return The feed of [controller], created on first use. Call on the main thread. */
        fun of(controller: ContentPlayerController): PlayerFeed = feeds.getOrPut(controller) { PlayerFeed(controller) }
    }
}

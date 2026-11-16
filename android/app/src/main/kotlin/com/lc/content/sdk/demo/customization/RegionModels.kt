package com.lc.content.sdk.demo.customization

/*
 * What the custom regions draw, as plain data.
 *
 * The UI files in compose/ and view/ take these classes and know nothing about the SDK. The mapping
 * from the SDK's controller state to these classes is in PlayerFeed.kt, so "what the SDK gives you"
 * and "what the screen shows" are two separate steps you can read one at a time.
 */

/** What the info region draws: the back button is static, the intro changes with the episode. */
internal data class InfoUi(
    val dramaName: String,
    val episodeNumber: Int,
    val episodeTitle: String,
) {
    /** false until the SDK has bound an episode; the intro is then left out. */
    val hasEpisode: Boolean get() = episodeNumber > 0
}

/** What the controls region draws. */
internal data class ControlsUi(
    val episodeNumber: Int,
    val episodeCount: Int,
    val positionMs: Long,
    val durationMs: Long,
    val speed: Float,
    /** true while the player waits for data; the timeline shows a moving light. */
    val loading: Boolean,
) {
    val hasPrevious: Boolean get() = episodeNumber > 1
    val hasNext: Boolean get() = episodeNumber in 1 until episodeCount

    /** Played part of the episode, 0..1; 0 while the duration is not known yet. */
    val fraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    /** Seeking needs a known duration; before that the timeline is display only. */
    val seekable: Boolean get() = durationMs > 0
}

/** One cell of the episode list. */
internal data class EpisodeCellUi(
    val number: Int,
    val locked: Boolean,
)

/** What the episode list draws. */
internal data class EpisodeListUi(
    val dramaName: String,
    val currentNumber: Int,
    val episodes: List<EpisodeCellUi>,
)

/** Playback speeds the speed button cycles through, in order. */
private val SpeedSteps = listOf(1f, 1.5f, 2f)

/** @return The speed after [current] in 1x, 1.5x, 2x, back to 1x. */
internal fun nextSpeed(current: Float): Float = SpeedSteps[(SpeedSteps.indexOf(current) + 1) % SpeedSteps.size]

/** @return [ms] as `m:ss`. */
internal fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

/** @return [speed] as the button label, `1x` / `1.5x` / `2x`. */
internal fun speedLabel(speed: Float): String = if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

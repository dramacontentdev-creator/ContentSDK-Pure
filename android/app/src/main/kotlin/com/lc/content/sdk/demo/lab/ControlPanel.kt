package com.lc.content.sdk.demo.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lc.content.sdk.api.event.PlayerQualitySelection
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.customization.PlayerFeed
import com.lc.content.sdk.demo.customization.formatTime
import com.lc.content.sdk.demo.customization.speedLabel
import com.lc.content.sdk.demo.ui.DemoColors

/** The two pages of the control panel. */
internal enum class PanelSection(val label: String) {
    Controls("Controls"),
    Settings("Settings"),
}

/**
 * The lab's control panel: a sheet over the bottom of the SDK player page, drawn entirely by the
 * demo and driven through `ContentPlayerController`. It is what a host app's own player UI looks
 * like when the SDK page keeps the video and the app keeps the controls.
 *
 * [controller] is the controller of the page shown now, or null while the page is loading; the
 * controls then say so and the settings still work.
 */
@Composable
internal fun ControlPanel(
    controller: ContentPlayerController?,
    section: PanelSection,
    onSection: (PanelSection) -> Unit,
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * LabLayout.PANEL_MAX_HEIGHT_FRACTION).dp
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(LabLayout.EDGE_MARGIN.dp)
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .clip(shape)
            .background(DemoColors.Surface.copy(alpha = 0.96f))
            .border(1.dp, DemoColors.Outline, shape)
            // Taps on the panel stay on the panel and do not reach the player behind it.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        // Section switch
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PanelSection.entries.forEach { entry -> Chip(entry.label, selected = entry == section) { onSection(entry) } }
        }
        // Section body
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (section) {
                PanelSection.Controls -> if (controller != null) ControlsSection(controller) else NoController()
                PanelSection.Settings -> SettingsSection(controller)
            }
        }
    }
}

/** Shown in place of the controls while the page has no controller yet. */
@Composable
private fun NoController() {
    Note("The player page is not ready yet. The controls use the controller of the page that is shown.")
}

/**
 * Playback controls of the page on screen: status, timeline, transport, speed, quality, and episodes.
 * Every control is one `ContentPlayerController` call in [LabActions]; the state it shows comes from
 * [PlayerFeed], so it needs no listener of its own.
 */
@Composable
private fun ControlsSection(controller: ContentPlayerController) {
    val feed = PlayerFeed.of(controller)
    val state = feed.state
    val progress = feed.progress
    val resource = state.resource
    // A locked episode shows the SDK's unlock or unavailable layer; playback commands would be ignored.
    val playable = !state.episodeLocked

    // Status line: what the controller reports right now.
    Text(
        text = buildString {
            append(state.playbackState)
            append(if (state.playWhenReady) "  ·  play intent on" else "  ·  play intent off")
            if (state.episodeLocked) append("  ·  locked")
            append("\n").append(resource?.let { "${it.dramaName}  EP ${it.episodeNumber}/${it.episodeCount}" } ?: "no episode yet")
        },
        color = DemoColors.TextPrimary,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 18.sp,
    )

    // Timeline: follows playback, seeks once when the finger lifts. Off until the duration is known.
    var dragging by remember { mutableStateOf<Float?>(null) }
    Column {
        Slider(
            value = dragging ?: progress.fraction(),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { LabActions.seekTo(controller, (it * progress.durationMs).toLong()) }
                dragging = null
            },
            enabled = progress.durationMs > 0 && playable,
            colors = SliderDefaults.colors(
                thumbColor = DemoColors.Accent,
                activeTrackColor = DemoColors.Accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragging?.let { (it * progress.durationMs).toLong() } ?: progress.positionMs
            TimeText(formatTime(shown))
            TimeText(formatTime(progress.durationMs))
        }
    }

    // Transport: previous, back 10 s, play / pause (by the play intention), forward 10 s, next.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(R.drawable.ic_chevron_left, "Previous episode", enabled = (resource?.episodeNumber ?: 0) > 1) {
            LabActions.episode(controller, -1)
        }
        RoundIconButton(R.drawable.ic_replay_10, "Back 10 seconds", enabled = playable) { LabActions.seekBy(controller, -10_000) }
        RoundIconButton(
            icon = if (state.playWhenReady) R.drawable.ic_pause else R.drawable.ic_play,
            description = if (state.playWhenReady) "Pause" else "Play",
            size = LabLayout.PLAY_BUTTON.dp,
            filled = true,
            enabled = playable,
        ) { LabActions.togglePlay(controller) }
        RoundIconButton(R.drawable.ic_forward_10, "Forward 10 seconds", enabled = playable) { LabActions.seekBy(controller, 10_000) }
        RoundIconButton(
            icon = R.drawable.ic_chevron_right,
            description = "Next episode",
            enabled = resource != null && resource.episodeNumber < resource.episodeCount,
        ) { LabActions.episode(controller, 1) }
    }

    // Speed and quality: one row each, the current choice is highlighted.
    Segmented("Speed", listOf(1f, 1.5f, 2f).map { it to speedLabel(it) }, selected = state.playbackSpeed, enabled = playable) {
        LabActions.speed(controller, it)
    }
    QualityRow(state.availableQualityHeights, state.quality, enabled = playable) { LabActions.quality(controller, it) }

    // Episodes: jump straight to one; a locked one shows a lock.
    val episodes = feed.content.episodes
    if (episodes.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Episodes")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(episodes, key = { it.episodeId }) { episode ->
                    val label = if (episode.locked) "🔒 ${episode.episodeNumber}" else "${episode.episodeNumber}"
                    Chip(label, selected = episode.episodeNumber == resource?.episodeNumber) {
                        LabActions.selectEpisode(controller, episode.episodeNumber)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeText(text: String) {
    Text(text, color = DemoColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
}

/**
 * The resolutions the stream really offers, and the one playing. When there is nothing to switch to
 * it says so; a tap then still reports the failure in the log, which is what `LabActions.quality` does.
 */
@Composable
private fun QualityRow(
    heights: List<Int>,
    selected: PlayerQualitySelection,
    enabled: Boolean,
    onSelect: (PlayerQualitySelection) -> Unit,
) {
    val resolved = LabActions.resolvedQuality
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val options = listOf(LabActions.AutoQuality to "Auto") + heights.map { LabActions.manualQuality(it) to "${it}p" }
        Segmented("Quality", options, selected = selected, enabled = enabled, onSelect = onSelect)
        val only = heights.singleOrNull() ?: resolved?.shortEdge.takeIf { heights.isEmpty() }
        when {
            only != null -> Note("Playing ${resolved?.describeStream() ?: "${only}p"}. This stream has one resolution, so it cannot be switched.")
            heights.isEmpty() -> Note("The resolutions of this stream are not known yet.")
            else -> Note("Playing ${resolved?.describeStream() ?: "…"}. Offered: ${heights.joinToString { "${it}p" }}.")
        }
    }
}

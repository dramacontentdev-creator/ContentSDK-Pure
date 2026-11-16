package com.lc.content.sdk.demo.customization.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.customization.ControlsUi
import com.lc.content.sdk.demo.customization.PlayerFeed
import com.lc.content.sdk.demo.customization.RegionColors
import com.lc.content.sdk.demo.customization.RegionSlots
import com.lc.content.sdk.demo.customization.formatTime
import com.lc.content.sdk.demo.customization.nextSpeed
import com.lc.content.sdk.demo.customization.speedLabel
import kotlin.math.roundToInt

private val AccentGradient = Brush.horizontalGradient(listOf(Color(RegionColors.ACCENT), Color(RegionColors.ACCENT_END)))
private val Pill = Color(RegionColors.PILL)

/**
 * Compose version of the custom CONTROLS region (see CustomRegions.kt): a thin timeline with its
 * 44 dp touch band, then one row of 40 dp controls, where the SDK draws its own.
 *
 * This is the whole connection to the SDK. Each tap is one controller call:
 * `seekTo`, `selectEpisode`, `setPlaybackSpeed`; the episode pill opens [ComposeEpisodeList].
 */
@Composable
internal fun ComposeControlsRegion(controller: ContentPlayerController) {
    val feed = PlayerFeed.of(controller)
    var showEpisodes by remember { mutableStateOf(false) }

    // A locked episode shows the SDK's unlock or unavailable layer; seeking and switching would be ignored.
    if (feed.state.episodeLocked) return

    val ui = feed.controlsUi()
    if (showEpisodes) ComposeEpisodeList(controller, feed, onDismiss = { showEpisodes = false })

    ControlsRegionContent(
        ui = ui,
        onSeek = { fraction -> controller.seekTo((fraction * ui.durationMs).toLong()) },
        onPrevious = { controller.selectEpisode(ui.episodeNumber - 1) },
        onNext = { controller.selectEpisode(ui.episodeNumber + 1) },
        onEpisodes = { showEpisodes = true },
        onSpeed = { controller.setPlaybackSpeed(nextSpeed(ui.speed)) },
    )
}

/** The controls region as pure UI: no SDK types, so it can be drawn from fixed data. */
@Composable
internal fun ControlsRegionContent(
    ui: ControlsUi,
    onSeek: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEpisodes: () -> Unit,
    onSpeed: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(RegionColors.SCRIM_BOTTOM))))
                .navigationBarsPadding(),
        ) {
            Timeline(ui, onSeek)
            // The SDK's gap between the timeline and the row; the times sit in the timeline band above it.
            Spacer(Modifier.height(RegionSlots.ROW_TOP_GAP.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RegionSlots.ROW_PADDING_H.dp)
                    .height(RegionSlots.ROW_HEIGHT.dp),
                horizontalArrangement = Arrangement.spacedBy(RegionSlots.ROW_GAP.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(R.drawable.ic_chevron_left, "Previous episode", enabled = ui.hasPrevious, onClick = onPrevious)
                EpisodePill(ui, onClick = onEpisodes, modifier = Modifier.weight(1f))
                IconButton(R.drawable.ic_chevron_right, "Next episode", enabled = ui.hasNext, onClick = onNext)
                SpeedPill(ui, onClick = onSpeed)
            }
        }
    }
}

/**
 * Elapsed and total time, at the two ends of the timeline band, just above the line. They live in
 * the band (44 dp tall, mostly empty) and not in the 14 dp gap below it, which is too low for text.
 */
@Composable
private fun Times(ui: ControlsUi, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RegionSlots.ROW_PADDING_H.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TimeText(formatTime(ui.positionMs))
        TimeText(formatTime(ui.durationMs))
    }
}

@Composable
private fun TimeText(text: String) {
    Text(
        text = text,
        color = Color(RegionColors.TEXT_SECONDARY),
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
    )
}

/** Opens the episode list. Shows where you are, `3 / 24`, with a list icon to say it opens something. */
@Composable
private fun EpisodePill(ui: ControlsUi, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .height(RegionSlots.ROW_HEIGHT.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Pill)
            .clickable(enabled = ui.episodeCount > 0, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(R.drawable.ic_list, size = 18)
        Text(
            text = if (ui.episodeCount > 0) "EP ${ui.episodeNumber} / ${ui.episodeCount}" else "Episodes",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Cycles 1x, 1.5x, 2x. The label is the current speed. */
@Composable
private fun SpeedPill(ui: ControlsUi, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(RegionSlots.ROW_HEIGHT.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(speedLabel(ui.speed), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IconButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(RegionSlots.BUTTON_SIZE.dp)
            .clip(CircleShape)
            .background(Pill)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, size = 22, description = description, alpha = if (enabled) 1f else 0.35f)
    }
}

@Composable
private fun Icon(icon: Int, size: Int, description: String? = null, alpha: Float = 1f) {
    Image(
        painter = painterResource(icon),
        contentDescription = description,
        colorFilter = ColorFilter.tint(Color.White.copy(alpha = alpha)),
        modifier = Modifier.size(size.dp),
    )
}

/**
 * Timeline with the SDK's geometry: a 44 dp touch band whose line sits at its bottom edge, 2 dp
 * thick and 6 dp while dragging. While the player waits for data a light runs along the line.
 *
 * It follows the finger only horizontally, so a vertical swipe on it still changes the episode.
 */
@Composable
private fun Timeline(ui: ControlsUi, onSeek: (Float) -> Unit) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    val latestOnSeek by rememberUpdatedState(onSeek)
    val dragging = dragFraction != null
    val lineHeight by animateDpAsState(
        targetValue = (if (dragging) RegionSlots.TIMELINE_LINE_HEIGHT_DRAGGING else RegionSlots.TIMELINE_LINE_HEIGHT).dp,
        label = "timelineHeight",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RegionSlots.TIMELINE_TOUCH_HEIGHT.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            // Horizontal drags only, and only once the duration is known.
            .pointerInput(ui.seekable) {
                if (!ui.seekable) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { dragFraction = (it.x / widthPx).coerceIn(0f, 1f) },
                    onDragEnd = {
                        dragFraction?.let(latestOnSeek)
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                    },
                )
            },
        contentAlignment = Alignment.BottomStart,
    ) {
        val shown = dragFraction ?: ui.fraction
        Box(
            Modifier
                .fillMaxWidth()
                .height(lineHeight)
                .background(Color(RegionColors.TRACK)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown)
                    .background(AccentGradient),
            )
            if (ui.loading) LoadingLight()
        }
        // While dragging, the bubble shows the time under the finger instead.
        if (dragging) {
            DragBubble(fraction = shown, ui = ui, widthPx = widthPx)
        } else {
            Times(ui, Modifier.padding(bottom = (RegionSlots.TIMELINE_LINE_HEIGHT + 6).dp))
        }
    }
}

/** The time under the finger while dragging, above the line. */
@Composable
private fun DragBubble(fraction: Float, ui: ControlsUi, widthPx: Float) {
    val bubbleWidth = 56.dp
    val bubbleWidthPx = with(LocalDensity.current) { bubbleWidth.toPx() }
    val x = (fraction * widthPx - bubbleWidthPx / 2).coerceIn(0f, (widthPx - bubbleWidthPx).coerceAtLeast(0f))
    Text(
        text = formatTime((fraction * ui.durationMs).toLong()),
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(bottom = 14.dp)
            .offset { IntOffset(x.roundToInt(), 0) }
            .width(bubbleWidth)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color(RegionColors.GLASS))
            .padding(vertical = 4.dp),
    )
}

/** A light that runs along the timeline while the player waits for data, like the SDK's own. */
@Composable
private fun LoadingLight() {
    val phase by rememberInfiniteTransition(label = "loading").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "loadingPhase",
    )
    Canvas(Modifier.fillMaxSize()) {
        val width = size.width * 0.3f
        val x = (size.width + width) * phase - width
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent),
                startX = x,
                endX = x + width,
            ),
            topLeft = Offset(x, 0f),
            size = Size(width, size.height),
        )
    }
}

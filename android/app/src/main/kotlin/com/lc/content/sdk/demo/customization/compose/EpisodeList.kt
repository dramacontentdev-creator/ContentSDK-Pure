package com.lc.content.sdk.demo.customization.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.customization.EpisodeCellUi
import com.lc.content.sdk.demo.customization.EpisodeListUi
import com.lc.content.sdk.demo.customization.PlayerFeed
import com.lc.content.sdk.demo.customization.RegionColors
import com.lc.content.sdk.demo.customization.RegionSlots

private val Panel = Color(RegionColors.PANEL)
private val CellFill = Color(RegionColors.CELL)
private val AccentGradient = Brush.horizontalGradient(listOf(Color(RegionColors.ACCENT), Color(RegionColors.ACCENT_END)))

/**
 * Compose version of a custom EPISODE LIST (see CustomRegions.kt). It opens from the episode pill of
 * the custom controls row, where the SDK opens its own list, and uses the SDK's geometry: five
 * columns, rounded top corners, at most 70 % of the screen.
 *
 * **Why a Dialog.** It is a window of its own on purpose. The custom controls hide with the SDK's
 * controls a few seconds into playback; a list drawn inside them would be taken away while the user
 * is still choosing. A dialog is not affected, the video keeps playing behind it, and a tap outside
 * closes it. While it is open, [ContentPlayerController.setControlsHeld] keeps the controls behind it
 * shown, so they are still there when it closes.
 *
 * Switching episodes is [ContentPlayerController.selectEpisode].
 */
@Composable
internal fun ComposeEpisodeList(controller: ContentPlayerController, feed: PlayerFeed, onDismiss: () -> Unit) {
    DisposableEffect(controller) {
        controller.setControlsHeld(true)
        onDispose { controller.setControlsHeld(false) }
    }
    EpisodeListContent(
        ui = feed.episodeListUi(),
        onSelect = { number ->
            controller.selectEpisode(number)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

/** The episode list as pure UI: no SDK types, so it can be drawn from fixed data. */
@Composable
internal fun EpisodeListContent(ui: EpisodeListUi, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Scrim: a tap anywhere outside the panel closes the list.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(RegionSlots.EPISODE_PANEL_MAX_HEIGHT_FRACTION)
                    .clip(RoundedCornerShape(topStart = RegionSlots.EPISODE_PANEL_CORNER_RADIUS.dp, topEnd = RegionSlots.EPISODE_PANEL_CORNER_RADIUS.dp))
                    .background(Panel)
                    // Taps on the panel itself must not reach the scrim.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                Handle()
                Header(ui, onDismiss)
                Grid(ui, onSelect)
            }
        }
    }
}

/** The small bar at the top that says "this is a sheet". */
@Composable
private fun Handle() {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = 0.25f)),
        )
    }
}

@Composable
private fun Header(ui: EpisodeListUi, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = ui.dramaName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${ui.episodes.size} episodes",
                color = Color(RegionColors.TEXT_SECONDARY),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(CellFill).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close",
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Five columns of square cells. Opens scrolled to the row of the current episode. */
@Composable
private fun Grid(ui: EpisodeListUi, onSelect: (Int) -> Unit) {
    val currentRow = ((ui.currentNumber - 1).coerceAtLeast(0)) / RegionSlots.EPISODE_COLUMNS
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = currentRow * RegionSlots.EPISODE_COLUMNS)
    LazyVerticalGrid(
        columns = GridCells.Fixed(RegionSlots.EPISODE_COLUMNS),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(ui.episodes, key = { it.number }) { cell ->
            Cell(cell, isCurrent = cell.number == ui.currentNumber, onClick = { onSelect(cell.number) })
        }
    }
}

/** The current episode is filled with the accent; a locked one carries a lock. */
@Composable
private fun Cell(cell: EpisodeCellUi, isCurrent: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .then(if (isCurrent) Modifier.background(AccentGradient) else Modifier.background(CellFill))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${cell.number}",
            color = Color.White.copy(alpha = if (cell.locked && !isCurrent) 0.55f else 1f),
            fontSize = 16.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
        )
        if (cell.locked) {
            Image(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = "Locked",
                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.7f)),
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(12.dp),
            )
        }
    }
}

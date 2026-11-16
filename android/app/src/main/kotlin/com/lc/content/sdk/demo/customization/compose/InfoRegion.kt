package com.lc.content.sdk.demo.customization.compose

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.customization.InfoUi
import com.lc.content.sdk.demo.customization.PlayerFeed
import com.lc.content.sdk.demo.customization.RegionColors
import com.lc.content.sdk.demo.customization.RegionSlots

/**
 * Compose version of the custom INFO region (see CustomRegions.kt).
 *
 * It draws what the SDK draws in this region, where the SDK draws it: the back button in the top
 * bar, the episode intro at the bottom left. The SDK's back button belongs to this region, so
 * replacing the region means offering your own, as here.
 *
 * Connected to the SDK in two lines: the controller gives the [PlayerFeed], the feed gives the [InfoUi].
 */
@Composable
internal fun ComposeInfoRegion(controller: ContentPlayerController) {
    val ui = PlayerFeed.of(controller).infoUi()
    // Closing the player page is the host's decision; the back dispatcher is how any Activity closes.
    val onBack = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    InfoRegionContent(ui = ui, onBack = { onBack?.onBackPressed() })
}

/** The info region as pure UI: no SDK types, so it can be drawn from fixed data. */
@Composable
internal fun InfoRegionContent(ui: InfoUi, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        BackButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart))
        if (ui.hasEpisode) Intro(ui, Modifier.align(Alignment.BottomStart))
    }
}

/** Top bar: same height, padding, and button size as the SDK's. Only the button takes touches. */
@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .height(RegionSlots.TOP_BAR_HEIGHT.dp)
            .padding(horizontal = RegionSlots.TOP_BAR_PADDING_H.dp, vertical = RegionSlots.TOP_BAR_PADDING_V.dp),
    ) {
        Box(
            modifier = Modifier
                .size(RegionSlots.BUTTON_SIZE.dp)
                .clip(CircleShape)
                .background(Color(RegionColors.GLASS))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Bottom left, above the controls row. Draws no touch handler, so taps reach the page. */
@Composable
private fun Intro(ui: InfoUi, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = RegionSlots.INTRO_PADDING_H.dp,
                end = RegionSlots.INTRO_PADDING_H.dp,
                bottom = (RegionSlots.CONTROLS_HEIGHT + RegionSlots.INTRO_GAP_ABOVE_CONTROLS).dp,
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = ui.dramaName,
                style = OnVideo.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = "CUSTOM",
                color = Color(RegionColors.ACCENT),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(RegionColors.ACCENT).copy(alpha = 0.18f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "EP ${ui.episodeNumber}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(RegionColors.ACCENT))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Text(
                text = ui.episodeTitle,
                style = OnVideo.copy(fontSize = 13.sp, color = Color(RegionColors.TEXT_SECONDARY)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Text on the video: white with a soft shadow, readable on any frame. */
internal val OnVideo = TextStyle(
    color = Color.White,
    shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 6f),
)

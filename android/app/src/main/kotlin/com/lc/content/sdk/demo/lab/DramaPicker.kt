package com.lc.content.sdk.demo.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lc.content.sdk.api.Drama
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.ui.DemoColors

/** Cover size of a drama row, in dp (a 3:4 poster). */
private const val COVER_WIDTH = 72
private const val COVER_HEIGHT = 96

/** Log lines shown under the list: the answers of `getDramaList` and `getPlaybackHistory`. */
private const val PICKER_LOG_LINES = 12

/**
 * The first page of the lab: the dramas of `getDramaList`, to pick the one the player opens.
 *
 * `getPlaybackHistory` answers in the log below the list, as does the drama list itself.
 */
@Composable
internal fun DramaPicker(
    dramas: List<Drama>,
    onPick: (Drama) -> Unit,
    onReload: () -> Unit,
    onHistory: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundIconButton(R.drawable.ic_arrow_back, "Back", onClick = onBack)
            Text("Player lab", color = DemoColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "Pick a drama. The SDK player page opens full screen; the button at the top right opens " +
                "the lab's own controls and settings, all driven through the player controller.",
            color = DemoColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        ButtonRow {
            Chip("Reload dramas", onClick = onReload)
            Chip("Playback history", onClick = onHistory)
        }
        SectionLabel("Dramas")
        if (dramas.isEmpty()) {
            Text("No dramas yet.", color = DemoColors.TextSecondary, fontSize = 13.sp)
        } else {
            dramas.forEach { drama -> DramaRow(drama) { onPick(drama) } }
        }
        SectionLabel("Log")
        LogLines(max = PICKER_LOG_LINES)
    }
}

/**
 * One drama of `getDramaList`: the cover, the name, and the summary fields the list already carries
 * (`episodeSize`, `freeCount`, `tags`, `description`). Reading them takes no extra call.
 */
@Composable
private fun DramaRow(drama: Drama, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DemoColors.Surface)
            .border(1.dp, DemoColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AsyncImage(
            model = drama.coverImageUrl,
            contentDescription = drama.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(COVER_WIDTH.dp)
                .height(COVER_HEIGHT.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DemoColors.Outline),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = drama.name,
                color = DemoColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${drama.episodeSize} episodes · first ${drama.freeCount} free",
                color = DemoColors.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            if (drama.tags.isNotEmpty()) {
                Text(
                    text = drama.tags.joinToString("  ·  "),
                    color = DemoColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (drama.description.isNotBlank()) {
                Text(
                    text = drama.description,
                    color = DemoColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

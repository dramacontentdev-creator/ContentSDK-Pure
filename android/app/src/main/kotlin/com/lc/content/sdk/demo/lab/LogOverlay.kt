package com.lc.content.sdk.demo.lab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.ui.DemoColors

/** Geometry of the log overlay: where it sits over the SDK page and how much it may cover. */
private object OverlayLayout {
    /** Below the SDK top bar (56 dp), which holds the back button of the page. */
    const val TOP_OFFSET = 56

    /** Lines the collapsed strip shows, and its width cap so it leaves most of the video visible. */
    const val COLLAPSED_LINES = 3
    const val COLLAPSED_MAX_WIDTH = 260

    /** The expanded panel stops short of the right edge, where the panel button sits. */
    const val EXPANDED_END_CLEARANCE = LabLayout.EDGE_MARGIN * 2 + LabLayout.ROUND_BUTTON
    const val EXPANDED_MAX_HEIGHT_FRACTION = 0.45f
}

/**
 * The lab's log, in the top-left corner of the SDK player page.
 *
 * Collapsed, it is a small strip with the newest [OverlayLayout.COLLAPSED_LINES] lines; only its
 * header takes touches, so taps and swipes on the lines still reach the page. Expanded, it is a
 * scrollable panel with every line and a clear button, and it keeps its touches to itself.
 */
@Composable
internal fun LogOverlay(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val lines = LabLog.lines

    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(start = LabLayout.EDGE_MARGIN.dp, top = OverlayLayout.TOP_OFFSET.dp),
    ) {
        if (expanded) {
            ExpandedLog(lines = lines, onCollapse = { expanded = false })
        } else {
            CollapsedLog(lines = lines, onExpand = { expanded = true })
        }
    }
}

/** The strip: a header that opens the panel, then the newest lines, which take no touches. */
@Composable
private fun CollapsedLog(lines: List<LogLine>, onExpand: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = OverlayLayout.COLLAPSED_MAX_WIDTH.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        OverlayHeader(count = lines.size, expanded = false, onToggle = onExpand)
        lines.take(OverlayLayout.COLLAPSED_LINES).forEach { LiveLine(it) }
    }
}

/** The panel: header with clear and collapse, then every line in a scrollable list. */
@Composable
private fun ExpandedLog(lines: List<LogLine>, onCollapse: () -> Unit) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * OverlayLayout.EXPANDED_MAX_HEIGHT_FRACTION).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = OverlayLayout.EXPANDED_END_CLEARANCE.dp)
            .heightIn(max = maxHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(DemoColors.Surface.copy(alpha = 0.96f))
            .border(1.dp, DemoColors.Outline, RoundedCornerShape(16.dp))
            // Taps on the panel stay on the panel and do not reach the player behind it.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OverlayHeader(count = lines.size, expanded = true, onToggle = onCollapse, modifier = Modifier.weight(1f))
            Chip("Clear", onClick = LabLog::clear)
        }
        if (lines.isEmpty()) {
            Note("Nothing yet. SDK calls and playback events appear here.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(lines, key = { it.id }) { line -> LogLineRow(line) }
            }
        }
    }
}

/** `LOG · n` and a chevron; tapping it toggles between strip and panel. */
@Composable
private fun OverlayHeader(count: Int, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clickable(onClick = onToggle).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("LOG", color = DemoColors.TextSecondary, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
        Text("· $count", color = DemoColors.TextSecondary, fontSize = 10.sp)
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = if (expanded) "Collapse log" else "Expand log",
            colorFilter = ColorFilter.tint(DemoColors.TextSecondary),
            modifier = Modifier.size(14.dp).rotate(if (expanded) -90f else 90f),
        )
    }
}

/** One line of the strip: a colored dot and the message, two lines at most. */
@Composable
private fun LiveLine(line: LogLine) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(line.kind.color()))
        Text(
            text = line.message,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

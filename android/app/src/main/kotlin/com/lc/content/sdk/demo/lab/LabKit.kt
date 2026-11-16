package com.lc.content.sdk.demo.lab

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lc.content.sdk.api.event.PlaybackProgress
import com.lc.content.sdk.demo.ui.DemoColors

/*
 * Small building blocks the lab screens share: buttons, option rows, switches, and the log lines.
 * No SDK calls here.
 */

/** Sizes the lab's chrome shares; the panel and the overlay use them to stay out of each other's way. */
internal object LabLayout {
    /** Round buttons over the video, and their distance from the screen edge. */
    const val ROUND_BUTTON = 44
    const val EDGE_MARGIN = 12

    /** The control panel never takes more than this share of the screen height, so the video stays visible. */
    const val PANEL_MAX_HEIGHT_FRACTION = 0.6f

    /** The play / pause button of the transport row; the other transport buttons use [ROUND_BUTTON]. */
    const val PLAY_BUTTON = 60
}

/** @return Played part of the episode, 0..1; 0 while the duration is not known yet. */
internal fun PlaybackProgress.fraction(): Float =
    if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

/** A round button with an icon; [filled] paints it with the accent, otherwise it is dark glass over the video. */
@Composable
internal fun RoundIconButton(
    @DrawableRes icon: Int,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = LabLayout.ROUND_BUTTON.dp,
    filled: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) DemoColors.Accent else Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(size * 0.5f),
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
}

/** A pill-shaped button; [selected] paints it with the accent. */
@Composable
internal fun Chip(label: String, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) Color.White else DemoColors.TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) DemoColors.Accent else Color.White.copy(alpha = 0.1f))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Buttons that wrap onto the next line when the row is full. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ButtonRow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

/** A small grey title above a group of controls. */
@Composable
internal fun SectionLabel(text: String) {
    Text(text.uppercase(), color = DemoColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.2.sp)
}

/** A one-line explanation under a control. */
@Composable
internal fun Note(text: String) {
    Text(text, color = DemoColors.TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
}

/** A titled group of options where the current one is highlighted; one tap switches. Wraps when it is full. */
@Composable
internal fun <T> Segmented(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = DemoColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(60.dp))
        ButtonRow {
            options.forEach { (value, label) -> Chip(label, selected = value == selected, enabled = enabled) { onSelect(value) } }
        }
    }
}

/**
 * One setting: a title, a one-line explanation, and a switch at the end. [detail] says what the switch
 * does in the SDK's words, so the panel reads like the API it drives.
 */
@Composable
internal fun SwitchRow(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = DemoColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = DemoColors.TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DemoColors.Accent,
                uncheckedThumbColor = DemoColors.TextSecondary,
                uncheckedTrackColor = DemoColors.Outline,
                uncheckedBorderColor = DemoColors.Outline,
            ),
        )
    }
}

/** The color a log line of [kind] is drawn in. */
internal fun LogKind.color(): Color = when (this) {
    LogKind.Api -> DemoColors.Info
    LogKind.Event -> DemoColors.Success
    LogKind.Error -> DemoColors.Error
}

/** The tag in front of a log line of [kind]. */
internal fun LogKind.tag(): String = name.uppercase()

/** The newest [max] log lines, each with a colored tag. */
@Composable
internal fun LogLines(max: Int) {
    val lines = LabLog.lines
    if (lines.isEmpty()) {
        Text("Nothing yet. Events and calls appear here.", color = DemoColors.TextSecondary, fontSize = 12.sp)
        return
    }
    lines.take(max).forEach { LogLineRow(it) }
}

/** One log line: time, colored tag, message. */
@Composable
internal fun LogLineRow(line: LogLine) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(line.time, color = DemoColors.TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(line.kind.tag(), color = line.kind.color(), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(line.message, color = DemoColors.TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
    }
}

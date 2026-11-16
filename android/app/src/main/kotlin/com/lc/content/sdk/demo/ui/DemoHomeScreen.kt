package com.lc.content.sdk.demo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lc.content.sdk.demo.DemoEdition
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.customization.PlayerPageStyle

/**
 * The demo's home screen: SDK status, the integration checklist, the player page style, and the two
 * entry points (the SDK drama home and the player lab).
 *
 * Demo UI only. Nothing in this file is required to integrate the SDK; the SDK calls are made by
 * MainActivity.kt and DemoEdition.kt.
 */
@Composable
fun DemoHomeScreen(
    status: SdkStatus,
    pageStyle: PlayerPageStyle,
    onPageStyleChange: (PlayerPageStyle) -> Unit,
    onOpenDramaHome: () -> Unit,
    onOpenLab: () -> Unit,
) {
    DemoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DemoColors.Background)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(16.dp))
                Header()
                StatusCard(status)
                StepsCard()
                PageStyleCard(pageStyle, onPageStyleChange)
                LabCard(status, onOpenLab)
                Spacer(Modifier.height(8.dp))
            }
            OpenButton(
                status = status,
                onClick = onOpenDramaHome,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DemoColors.AccentGradient),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "CONTENT SDK · ${DemoEdition.NAME.uppercase()}",
            color = DemoColors.Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
        )
        Text("Integration Demo", color = DemoColors.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "The minimum code needed to bring short-drama content into your Android app. ${DemoEdition.SUMMARY}",
            color = DemoColors.TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(text, color = DemoColors.TextSecondary, fontSize = 11.sp, letterSpacing = 1.5.sp)
}

@Composable
private fun StatusCard(status: SdkStatus) {
    val (label, detail, color) = when (status) {
        SdkStatus.NotStarted -> Triple("Initialized", "Tap the button below to start the SDK.", DemoColors.TextSecondary)
        SdkStatus.Starting -> Triple("Starting…", DemoEdition.STARTING_DETAIL, DemoColors.Warning)
        SdkStatus.Ready -> Triple("Ready", "The SDK is running. SDK pages can be opened.", DemoColors.Success)
        is SdkStatus.Failed -> Triple("Start failed", "${status.code} · ${status.message}", DemoColors.Error)
    }
    val dotColor by animateColorAsState(color, label = "statusDot")

    DemoCard {
        CardTitle("SDK STATUS")
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(10.dp))
            Text(label, color = DemoColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Text(detail, color = DemoColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun StepsCard() {
    DemoCard {
        CardTitle("INTEGRATION STEPS")
        Spacer(Modifier.height(6.dp))
        DemoEdition.integrationSteps.forEach { step ->
            Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(DemoColors.Accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(step.label, color = DemoColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(step.title, color = DemoColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(step.file, color = DemoColors.TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

/** The player page styles as tiles; the chosen one has an accent border. */
@Composable
private fun PageStyleCard(selected: PlayerPageStyle, onSelect: (PlayerPageStyle) -> Unit) {
    DemoCard {
        CardTitle("PLAYER PAGE (OPTIONAL)")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Replace the SDK's intro and controls with your own UI. Pick a style, then open the player.",
            color = DemoColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayerPageStyle.entries.forEach { style -> StyleTile(style, style == selected) { onSelect(style) } }
        }
    }
}

@Composable
private fun StyleTile(style: PlayerPageStyle, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor by animateColorAsState(if (selected) DemoColors.Accent else DemoColors.Outline, label = "tileBorder")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) DemoColors.Accent.copy(alpha = 0.08f) else Color.Transparent)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).border(2.dp, borderColor, CircleShape).padding(4.dp)
                .background(if (selected) DemoColors.Accent else Color.Transparent, CircleShape),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(style.label, color = DemoColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(style.detail, color = DemoColors.TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun LabCard(status: SdkStatus, onOpen: () -> Unit) {
    DemoCard {
        CardTitle("PLAYER LAB (OPTIONAL)")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "The SDK player page with the demo's own controls over it: play, seek, speed, quality, " +
                "episodes, playback options, and the page UI style, all through the player controller. " +
                "The code is in the lab folder.",
            color = DemoColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DemoColors.Accent, RoundedCornerShape(12.dp))
                .clickable(enabled = status != SdkStatus.Starting, onClick = onOpen),
            contentAlignment = Alignment.Center,
        ) {
            Text("Open player lab", color = DemoColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OpenButton(status: SdkStatus, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val starting = status == SdkStatus.Starting
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DemoColors.AccentGradient, alpha = if (starting) 0.5f else 1f)
            .clickable(enabled = !starting, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (starting) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(
                text = if (status is SdkStatus.Failed) "Retry" else "Open Drama Home",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun DemoHomeScreenPreview() {
    DemoHomeScreen(
        status = SdkStatus.Ready,
        pageStyle = PlayerPageStyle.SdkDefault,
        onPageStyleChange = {},
        onOpenDramaHome = {},
        onOpenLab = {},
    )
}

package com.lc.content.sdk.demo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// -------------------------------------------------------------------------------------------------
// Demo UI only. Nothing in the ui/ and lab/ folders is needed to integrate the SDK.
// -------------------------------------------------------------------------------------------------

/** The demo's palette: a dark page, raised surfaces, and one accent. */
internal object DemoColors {
    val Background = Color(0xFF0E0F13)
    val Surface = Color(0xFF17191F)
    val Outline = Color(0xFF262932)
    val TextPrimary = Color(0xFFF4F5F7)
    val TextSecondary = Color(0xFF9AA0AC)

    /** The accent and the end of its gradient; the custom player regions use the same pair. */
    val Accent = Color(0xFFFF4D6D)
    val AccentEnd = Color(0xFFFF8A4C)

    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFF87171)
    val Info = Color(0xFF60A5FA)

    val AccentGradient = Brush.linearGradient(listOf(Accent, AccentEnd))
}

/** Dark Material theme with the demo accent; wrap every demo screen in it. */
@Composable
internal fun DemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = DemoColors.Accent), content = content)
}

/** A raised, outlined block on the home screen. */
@Composable
internal fun DemoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DemoColors.Surface)
            .border(1.dp, DemoColors.Outline, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        content()
    }
}

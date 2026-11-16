package com.lc.content.sdk.demo.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.api.player.PlayerPageRegion
import com.lc.content.sdk.demo.customization.CustomRegions
import com.lc.content.sdk.demo.customization.PlayerPageStyle

/**
 * The settings page of the control panel: every switch is one SDK call, named in its detail line.
 *
 * - Playback options are SDK-wide and apply from the next episode ([LabSettings]).
 * - The player page style and `autoHide` re-install the page regions ([CustomRegions]); pages that
 *   are open change at once.
 * - Region visibility is a call on the page's [controller], so it needs one and starts over on each
 *   new page.
 */
@Composable
internal fun SettingsSection(controller: ContentPlayerController?) {
    PlaybackOptions()
    PageStyle()
    CustomRegionSwitches(controller)
    DemoSwitches()
}

/** `setAutoPlay`, `setAutoAdvance`, `setResumeFromHistory`. */
@Composable
private fun PlaybackOptions() {
    Group("Playback options · from the next episode") {
        SwitchRow(
            title = "Auto play",
            detail = "setAutoPlay: start as soon as an episode is ready, on entry and after each switch.",
            checked = LabSettings.autoPlay,
            onCheckedChange = LabSettings::changeAutoPlay,
        )
        SwitchRow(
            title = "Auto advance",
            detail = "setAutoAdvance: move to the next episode when one ends.",
            checked = LabSettings.autoAdvance,
            onCheckedChange = LabSettings::changeAutoAdvance,
        )
        SwitchRow(
            title = "Resume from history",
            detail = "setResumeFromHistory: open a drama where it was left when no episode is named.",
            checked = LabSettings.resumeFromHistory,
            onCheckedChange = LabSettings::changeResumeFromHistory,
        )
    }
}

/** `setInfoRegion` / `setControlsRegion` with the SDK version, Compose content, or Android Views. */
@Composable
private fun PageStyle() {
    Group("Player page UI · applies to the open page at once") {
        Segmented(
            title = "Regions",
            options = PlayerPageStyle.entries.map { it to it.shortLabel },
            selected = CustomRegions.style,
            onSelect = { CustomRegions.apply(it) },
        )
        Note(CustomRegions.style.detail)
    }
}

/** `DramaPlayerRegion.autoHide` and `controller.setRegionVisible` for the custom info and controls regions. */
@Composable
private fun CustomRegionSwitches(controller: ContentPlayerController?) {
    val custom = CustomRegions.style != PlayerPageStyle.SdkDefault
    val manual = custom && !CustomRegions.autoHide

    Group("Custom regions · Compose or View style only") {
        SwitchRow(
            title = "Hide with SDK controls",
            detail = "DramaPlayerRegion(autoHide = true): your content shows and hides with a tap, like the SDK's. Off: it stays until you hide it below.",
            checked = CustomRegions.autoHide,
            enabled = custom,
            onCheckedChange = { CustomRegions.apply(CustomRegions.style, newAutoHide = it) },
        )
        SwitchRow(
            title = "Info region shown",
            detail = "setRegionVisible(Info, …): back button and episode intro.",
            checked = LabActions.isRegionShown(PlayerPageRegion.Info),
            enabled = manual && controller != null,
            onCheckedChange = { shown -> controller?.let { LabActions.showRegion(it, PlayerPageRegion.Info, shown) } },
        )
        SwitchRow(
            title = "Controls region shown",
            detail = "setRegionVisible(Controls, …): timeline and the episode / speed row.",
            checked = LabActions.isRegionShown(PlayerPageRegion.Controls),
            enabled = manual && controller != null,
            onCheckedChange = { shown -> controller?.let { LabActions.showRegion(it, PlayerPageRegion.Controls, shown) } },
        )
        Note("While this panel is open the page controls are held shown: setControlsHeld(true) on open, false on close.")
    }
}

/** Switches that change only the lab, not the SDK. */
@Composable
private fun DemoSwitches() {
    Group("Lab") {
        SwitchRow(
            title = "Log overlay",
            detail = "Show SDK calls and playback events in the top-left corner.",
            checked = LabSettings.logOverlay,
            onCheckedChange = { LabSettings.logOverlay = it },
        )
    }
}

/** A titled group of settings. */
@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(title)
        content()
    }
}

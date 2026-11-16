package com.lc.content.sdk.demo.customization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lc.content.sdk.AndroidContentSdk
import com.lc.content.sdk.DramaPlayerRegion
import com.lc.content.sdk.DramaPlayerRegionView
import com.lc.content.sdk.demo.customization.compose.ComposeControlsRegion
import com.lc.content.sdk.demo.customization.compose.ComposeInfoRegion
import com.lc.content.sdk.demo.customization.view.createControlsRegionView
import com.lc.content.sdk.demo.customization.view.createInfoRegionView

/** The player page looks the demo can switch between. Demo UI only. */
enum class PlayerPageStyle(val label: String, val shortLabel: String, val detail: String) {
    SdkDefault("SDK default", "SDK", "The SDK's own back button, intro, timeline, episode / speed buttons, and unlock screen."),
    Compose("Custom · Compose", "Compose", "The same slots drawn by your Compose content (customization/compose)."),
    View("Custom · View", "View", "The same slots drawn by your Android Views (customization/view)."),
}

/**
 * Content SDK - optional: replace parts of the SDK player page with your own UI
 *
 * Start here. This file is the whole SDK-facing part of the customization; everything else in this
 * folder is the UI it installs.
 *
 * ## The three regions
 *
 * | Region          | SDK version draws                                  | Set with                              |
 * |-----------------|----------------------------------------------------|---------------------------------------|
 * | video region    | cover, center play/pause, playback error           | `AndroidContentSdk.setVideoRegion`    |
 * | info region     | back button (top) and episode intro (bottom left)  | `AndroidContentSdk.setInfoRegion`     |
 * | controls region | timeline and the episode / speed buttons (bottom)  | `AndroidContentSdk.setControlsRegion` |
 *
 * For each region you choose one of:
 * - keep the SDK version:   `DramaPlayerRegion()`
 * - hide it:                `DramaPlayerRegion(useDefault = false)`
 * - replace it:             `DramaPlayerRegion(content = { controller -> ... })` (Compose) or
 *                           `DramaPlayerRegion(view = DramaPlayerRegionView { context, controller -> ... })` (View)
 *
 * The demo replaces the info and controls regions and keeps the video region, so the cover, the center
 * play button, and the playback error message stay.
 *
 * ## The unlock screen (commercial edition)
 *
 * A locked episode shows an unlock screen that is not one of the three regions and cannot be hidden:
 * the SDK's, or yours with `setUnlockView` / `setUnlockContent`. The demo sets it with the style, in
 * EditionCustomization.kt; the commercial demo's unlock/ folder explains how it unlocks.
 *
 * ## What your content gets, and what it must do
 *
 * - **Same slots.** Your content is drawn over the whole page. Put each part where the SDK version
 *   sits ([RegionSlots]) and it replaces it in place. The SDK back button is part of the info region,
 *   so your info content must offer its own.
 * - **Shows and hides with the SDK controls.** A tap on the page toggles the controls and they hide
 *   while playing. Your info and controls content follows that on its own and takes no touches while
 *   hidden. It stays created, so your state and listeners survive. To show and hide a region
 *   yourself, pass `autoHide = false` for it and call `controller.setRegionVisible`; the lab's
 *   settings panel (lab/) switches both at run time.
 * - **Also while the page loads.** The content is drawn before the SDK has bound an episode, so the
 *   SDK's back button and controls never show first and get replaced. The controller's `state.resource`
 *   is null until then; draw nothing that needs an episode before it is set.
 * - **Subtitles stay clear.** The SDK keeps subtitles above the area its own versions use: 124 dp from
 *   the page's bottom edge, system bars included. If your content is taller, say so with
 *   `bottomInsetDp`, as the info region does below, and subtitles move up above it.
 * - **Touches.** Only give real controls a click listener. Empty areas must stay without one, so taps
 *   (show / hide, double-tap play / pause) and vertical swipes (episodes) still reach the page. A View
 *   that takes a touch down also takes the swipe that starts on it, so keep touch areas small.
 * - **One listener each.** A controller keeps one `setListener`, one `setProgressListener`, and one
 *   `setContentListener`; setting one replaces the previous. [PlayerFeed] registers them once for
 *   every region.
 * - **Loading.** The SDK's loading light is part of its controls region. Draw your own while
 *   `playbackState` is `Preparing` or `Buffering`, as the timeline here does.
 * - **When to set it.** Once for the whole process, for example in `Application.onCreate`. Pages that
 *   are already open apply a change at once. The demo sets it when you pick a style on its home screen.
 *
 * ## Where things are
 *
 * - `PlayerFeed.kt`        listens to the controller once; turns SDK state into the data below
 * - `RegionModels.kt`      what each region draws, as plain data (no SDK types)
 * - `RegionSlots.kt`       where the SDK's parts sit, so yours replace them in place
 * - `compose/` and `view/` the same three parts written for Compose and for Android Views
 */
object CustomRegions {

    /** The style in use. Kept here, not in an Activity, because the SDK keeps regions for the whole process. */
    var style by mutableStateOf(PlayerPageStyle.SdkDefault)
        private set

    /**
     * Whether the custom info and controls content shows and hides with the SDK controls (true, the
     * SDK default) or stays until your app calls `controller.setRegionVisible` (false). Ignored by
     * the SDK default style, which has no custom content.
     */
    var autoHide by mutableStateOf(true)
        private set

    /** Installs [newStyle] for every SDK player page, including pages that are open. */
    fun apply(newStyle: PlayerPageStyle, newAutoHide: Boolean = autoHide) {
        style = newStyle
        autoHide = newAutoHide
        // The edition's own part of the style: the unlock screen in the commercial edition.
        EditionCustomization.apply(newStyle)
        when (newStyle) {
            PlayerPageStyle.SdkDefault -> {
                AndroidContentSdk.setInfoRegion(DramaPlayerRegion())
                AndroidContentSdk.setControlsRegion(DramaPlayerRegion())
            }

            PlayerPageStyle.Compose -> {
                AndroidContentSdk.setInfoRegion(
                    DramaPlayerRegion(
                        content = { controller -> ComposeInfoRegion(controller) },
                        // The intro is taller than the SDK's own clearance for subtitles: say how tall.
                        bottomInsetDp = RegionSlots.INTRO_BOTTOM_INSET,
                        autoHide = newAutoHide,
                    ),
                )
                AndroidContentSdk.setControlsRegion(
                    DramaPlayerRegion(
                        content = { controller -> ComposeControlsRegion(controller) },
                        autoHide = newAutoHide,
                    ),
                )
            }

            PlayerPageStyle.View -> {
                AndroidContentSdk.setInfoRegion(
                    DramaPlayerRegion(
                        view = DramaPlayerRegionView { context, controller -> createInfoRegionView(context, controller) },
                        bottomInsetDp = RegionSlots.INTRO_BOTTOM_INSET,
                        autoHide = newAutoHide,
                    ),
                )
                AndroidContentSdk.setControlsRegion(
                    DramaPlayerRegion(
                        view = DramaPlayerRegionView { context, controller -> createControlsRegionView(context, controller) },
                        autoHide = newAutoHide,
                    ),
                )
            }
        }
    }
}

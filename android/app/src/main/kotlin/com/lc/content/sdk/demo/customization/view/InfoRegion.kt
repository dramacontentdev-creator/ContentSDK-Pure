package com.lc.content.sdk.demo.customization.view

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.customization.InfoUi
import com.lc.content.sdk.demo.customization.PlayerFeed
import com.lc.content.sdk.demo.customization.RegionColors
import com.lc.content.sdk.demo.customization.RegionSlots

/**
 * Android View version of the custom INFO region (see CustomRegions.kt): the back button in the top
 * bar and the episode intro at the bottom left, where the SDK draws its own. It looks and behaves
 * like the Compose version in compose/InfoRegion.kt, so you can compare the two.
 *
 * It is built in code to keep the demo to one file per region; in your app you would inflate an XML
 * layout. Only the back button takes touches, so taps and vertical swipes reach the SDK page.
 */
internal class InfoRegionView(context: Context, private val onBack: () -> Unit) : FrameLayout(context) {

    private val topBar = FrameLayout(context)
    private val dramaName = label(context, "", sp = 18f, bold = true).apply { singleLine() }
    private val episodeBadge = label(context, "", sp = 11f, bold = true)
    private val episodeTitle = label(context, "", sp = 13f, color = RegionColors.TEXT_SECONDARY).apply { singleLine() }
    private val intro = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    init {
        // Top bar: same height, padding, and button size as the SDK's.
        val back = iconButton(context, R.drawable.ic_arrow_back, RegionSlots.BUTTON_SIZE, 22, RegionColors.GLASS, "Back")
        back.setOnClickListener { onBack() }
        topBar.addView(back, FrameLayout.LayoutParams(context.dp(RegionSlots.BUTTON_SIZE), context.dp(RegionSlots.BUTTON_SIZE), Gravity.CENTER_VERTICAL))
        addView(topBar, LayoutParams(MATCH_PARENT, context.dp(RegionSlots.TOP_BAR_HEIGHT), Gravity.TOP))

        // Bottom left: drama name with a small CUSTOM tag, then the episode badge and title.
        val customTag = label(context, "CUSTOM", sp = 9f, bold = true, color = RegionColors.ACCENT).apply {
            letterSpacing = 0.08f
            setPadding(context.dp(5), context.dp(2), context.dp(5), context.dp(2))
            background = roundedBackground(context, 0x2EFF4D6D, 4)
            setShadowLayer(0f, 0f, 0f, 0)
        }
        episodeBadge.apply {
            setPadding(context.dp(8), context.dp(2), context.dp(8), context.dp(2))
            background = pillBackground(RegionColors.ACCENT)
            setShadowLayer(0f, 0f, 0f, 0)
        }
        intro.addView(
            LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(dramaName, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 0f))
                addView(customTag, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = context.dp(8) })
            },
        )
        intro.addView(
            LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(episodeBadge)
                addView(episodeTitle, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = context.dp(8) })
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = context.dp(6) },
        )
        addView(intro, LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))

        applySystemBars(top = 0, bottom = 0)
        // The page draws edge to edge. Keep clear of the status bar and the navigation bar, as the SDK versions do.
        trackSystemBars(this, ::applySystemBars)
    }

    /** Renders [ui]. Safe to call as often as the SDK reports changes. */
    fun bind(ui: InfoUi) {
        intro.visibility = if (ui.hasEpisode) VISIBLE else GONE
        dramaName.text = ui.dramaName
        episodeBadge.text = "EP ${ui.episodeNumber}"
        episodeTitle.text = ui.episodeTitle
    }

    private fun applySystemBars(top: Int, bottom: Int) {
        topBar.setPadding(
            context.dp(RegionSlots.TOP_BAR_PADDING_H),
            top + context.dp(RegionSlots.TOP_BAR_PADDING_V),
            context.dp(RegionSlots.TOP_BAR_PADDING_H),
            context.dp(RegionSlots.TOP_BAR_PADDING_V),
        )
        (topBar.layoutParams as? LayoutParams)?.height = top + context.dp(RegionSlots.TOP_BAR_HEIGHT)
        intro.setPadding(
            context.dp(RegionSlots.INTRO_PADDING_H),
            0,
            context.dp(RegionSlots.INTRO_PADDING_H),
            bottom + context.dp(RegionSlots.CONTROLS_HEIGHT + RegionSlots.INTRO_GAP_ABOVE_CONTROLS),
        )
    }

    private fun TextView.singleLine() {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    private companion object {
        const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}

/**
 * Creates the info region view and connects it to the SDK: the controller gives the [PlayerFeed], and
 * the view renders the feed's [InfoUi] on every change. The SDK calls this each time the playback
 * episode changes, with a controller that is valid while the returned view is attached.
 */
internal fun createInfoRegionView(context: Context, controller: ContentPlayerController): View {
    val feed = PlayerFeed.of(controller)
    val view = InfoRegionView(context) { context.findComponentActivity()?.onBackPressedDispatcher?.onBackPressed() }
    observeWhileAttached(view, feed) { view.bind(feed.infoUi()) }
    return view
}

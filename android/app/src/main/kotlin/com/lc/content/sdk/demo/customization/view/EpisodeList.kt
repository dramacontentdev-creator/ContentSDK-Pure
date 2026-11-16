package com.lc.content.sdk.demo.customization.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.customization.EpisodeCellUi
import com.lc.content.sdk.demo.customization.EpisodeListUi
import com.lc.content.sdk.demo.customization.PlayerFeed
import com.lc.content.sdk.demo.customization.RegionColors
import com.lc.content.sdk.demo.customization.RegionSlots

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/**
 * Opens the custom EPISODE LIST, Android View version (see CustomRegions.kt). It looks and behaves like
 * the Compose version in compose/EpisodeList.kt and uses the SDK's geometry: five columns, rounded top
 * corners, at most 70 % of the screen.
 *
 * **Why a Dialog.** It is a window of its own on purpose. The custom controls hide with the SDK's
 * controls a few seconds into playback; a list drawn inside them would be taken away while the user
 * is still choosing. A dialog is not affected, the video keeps playing behind it, and a tap outside
 * closes it. While it is open, [ContentPlayerController.setControlsHeld] keeps the controls behind it
 * shown, so they are still there when it closes.
 *
 * Switching episodes is [ContentPlayerController.selectEpisode]. The list is a snapshot of the
 * episodes when it opens; the Compose version follows lock changes live.
 *
 * @param context The Activity context the SDK passes to your region view; a dialog needs a window token.
 */
internal fun showEpisodeList(context: Context, controller: ContentPlayerController, feed: PlayerFeed) {
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(
        EpisodeListView(context, feed.episodeListUi()) { number ->
            controller.selectEpisode(number)
            dialog.dismiss()
        }.also { it.onDismiss = dialog::dismiss },
    )
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setLayout(MATCH, MATCH)
        setDimAmount(0.6f)
    }
    dialog.setOnDismissListener { controller.setControlsHeld(false) }
    controller.setControlsHeld(true)
    dialog.show()
}

/** The episode list as a pure view: no SDK types, so it can be created from fixed data. */
internal class EpisodeListView(
    context: Context,
    ui: EpisodeListUi,
    private val onSelect: (Int) -> Unit,
) : FrameLayout(context) {

    /** Called when the user closes the list without choosing. */
    var onDismiss: () -> Unit = {}

    private val panel = LinearLayout(context)

    init {
        // Scrim: a tap anywhere outside the panel closes the list.
        setOnClickListener { onDismiss() }

        val radius = context.dp(RegionSlots.EPISODE_PANEL_CORNER_RADIUS).toFloat()
        panel.apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                setColor(RegionColors.PANEL)
            }
            // A tap on the panel itself must not reach the scrim behind it.
            isClickable = true
            setPadding(context.dp(16), 0, context.dp(16), 0)
            addView(handle(), LinearLayout.LayoutParams(MATCH, WRAP))
            addView(header(ui), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = context.dp(14); bottomMargin = context.dp(14) })
            val scroll = ScrollView(context).apply { addView(grid(ui)) }
            addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
            // Open on the row of the current episode.
            scroll.post { scroll.scrollTo(0, currentRowTop(ui)) }
        }
        addView(
            panel,
            LayoutParams(MATCH, (context.resources.displayMetrics.heightPixels * RegionSlots.EPISODE_PANEL_MAX_HEIGHT_FRACTION).toInt(), Gravity.BOTTOM),
        )
        // Keep the last row above the navigation bar, as the SDK's list does.
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            panel.setPadding(context.dp(16), 0, context.dp(16), insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
    }

    /** The small bar at the top that says "this is a sheet". */
    private fun handle(): FrameLayout = FrameLayout(context).apply {
        addView(
            View(context).apply { background = pillBackground(0x40FFFFFF) },
            LayoutParams(context.dp(36), context.dp(4), Gravity.CENTER_HORIZONTAL).apply { topMargin = context.dp(10) },
        )
        minimumHeight = context.dp(14)
    }

    private fun header(ui: EpisodeListUi): LinearLayout = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        val title = label(context, ui.dramaName, sp = 18f, bold = true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(0f, 0f, 0f, 0)
        }
        val subtitle = label(context, "${ui.episodes.size} episodes", sp = 12f, color = RegionColors.TEXT_SECONDARY).apply {
            setShadowLayer(0f, 0f, 0f, 0)
        }
        addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(title)
                addView(subtitle, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = context.dp(2) })
            },
            LinearLayout.LayoutParams(0, WRAP, 1f),
        )
        val close = iconButton(context, R.drawable.ic_close, 36, 18, RegionColors.CELL, "Close")
        close.setOnClickListener { onDismiss() }
        addView(close, LinearLayout.LayoutParams(context.dp(36), context.dp(36)))
    }

    private val gap get() = context.dp(8)

    /** Five columns of square cells. */
    private fun grid(ui: EpisodeListUi): GridLayout = GridLayout(context).apply {
        columnCount = RegionSlots.EPISODE_COLUMNS
        ui.episodes.forEach { cell ->
            addView(
                cell(cell, isCurrent = cell.number == ui.currentNumber),
                GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    setMargins(0, 0, gap, gap)
                },
            )
        }
    }

    private val cellSize: Int
        get() = (context.resources.displayMetrics.widthPixels - context.dp(32) - gap * (RegionSlots.EPISODE_COLUMNS - 1)) / RegionSlots.EPISODE_COLUMNS

    /** Top of the row that holds the current episode, in pixels. */
    private fun currentRowTop(ui: EpisodeListUi): Int =
        ((ui.currentNumber - 1).coerceAtLeast(0) / RegionSlots.EPISODE_COLUMNS) * (cellSize + gap)

    /** The current episode is filled with the accent; a locked one carries a lock. */
    private fun cell(cell: EpisodeCellUi, isCurrent: Boolean): FrameLayout = FrameLayout(context).apply {
        background = if (isCurrent) {
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(RegionColors.ACCENT, RegionColors.ACCENT_END))
                .apply { cornerRadius = context.dp(12).toFloat() }
        } else {
            roundedBackground(context, RegionColors.CELL, 12)
        }
        val number = label(context, "${cell.number}", sp = 16f, bold = isCurrent).apply {
            setShadowLayer(0f, 0f, 0f, 0)
            alpha = if (cell.locked && !isCurrent) 0.55f else 1f
        }
        addView(number, LayoutParams(WRAP, WRAP, Gravity.CENTER))
        if (cell.locked) {
            addView(
                icon(context, R.drawable.ic_lock, 12).apply { alpha = 0.7f },
                LayoutParams(context.dp(12), context.dp(12), Gravity.TOP or Gravity.END).apply { setMargins(0, context.dp(5), context.dp(5), 0) },
            )
        }
        setOnClickListener { onSelect(cell.number) }
    }
}

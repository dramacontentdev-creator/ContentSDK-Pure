package com.lc.content.sdk.demo.customization.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.demo.R
import com.lc.content.sdk.demo.customization.ControlsUi
import com.lc.content.sdk.demo.customization.PlayerFeed
import com.lc.content.sdk.demo.customization.RegionColors
import com.lc.content.sdk.demo.customization.RegionSlots
import com.lc.content.sdk.demo.customization.formatTime
import com.lc.content.sdk.demo.customization.nextSpeed
import com.lc.content.sdk.demo.customization.speedLabel
import kotlin.math.abs

/** What the controls view asks for. Plain callbacks, so the view needs no SDK. */
internal class ControlsActions(
    val onSeek: (fraction: Float) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onEpisodes: () -> Unit,
    val onSpeed: () -> Unit,
)

/**
 * Android View version of the custom CONTROLS region (see CustomRegions.kt): a thin timeline with its
 * 44 dp touch band, then one row of 40 dp controls, where the SDK draws its own. It looks and behaves
 * like the Compose version in compose/ControlsRegion.kt, so you can compare the two.
 *
 * Only the timeline band and the buttons take touches. **A View that takes a touch down also takes the
 * swipe that starts on it**, so a vertical swipe that begins on the timeline band does not change the
 * episode (the Compose version does not have this limit). Keep touch areas as small as these.
 */
internal class ControlsRegionView(context: Context, private val actions: ControlsActions) : FrameLayout(context) {

    private val timeline = TimelineView(context) { actions.onSeek(it) }
    private val elapsed = timeText(context)
    private val total = timeText(context)
    private val previous = iconButton(context, R.drawable.ic_chevron_left, RegionSlots.BUTTON_SIZE, 22, RegionColors.PILL, "Previous episode")
    private val next = iconButton(context, R.drawable.ic_chevron_right, RegionSlots.BUTTON_SIZE, 22, RegionColors.PILL, "Next episode")
    private val episodeText = label(context, "", sp = 14f, bold = true).apply { setShadowLayer(0f, 0f, 0f, 0) }
    private val episodePill = LinearLayout(context)
    private val speedText = label(context, "", sp = 14f, bold = true).apply { setShadowLayer(0f, 0f, 0f, 0) }
    private val bottom = LinearLayout(context)

    init {
        previous.setOnClickListener { actions.onPrevious() }
        next.setOnClickListener { actions.onNext() }

        // Episode pill: a list icon and `EP 3 / 24`. It opens the episode list.
        episodePill.apply {
            gravity = Gravity.CENTER
            background = pillBackground(RegionColors.PILL)
            addView(icon(context, R.drawable.ic_list, 18))
            addView(episodeText, LinearLayout.LayoutParams(WRAP, WRAP).apply { leftMargin = context.dp(8) })
            setOnClickListener { actions.onEpisodes() }
        }

        // Speed pill: shows the current speed; each tap goes to the next one.
        val speedPill = FrameLayout(context).apply {
            background = pillBackground(RegionColors.PILL)
            setPadding(context.dp(16), 0, context.dp(16), 0)
            addView(speedText, LayoutParams(WRAP, WRAP, Gravity.CENTER))
            setOnClickListener { actions.onSpeed() }
        }

        // Elapsed and total time sit in the timeline band, just above the line: the 14 dp gap below the
        // band is too low for text. The labels take no touches, so the timeline underneath still does.
        val times = LinearLayout(context).apply {
            gravity = Gravity.BOTTOM
            setPadding(
                context.dp(RegionSlots.ROW_PADDING_H),
                0,
                context.dp(RegionSlots.ROW_PADDING_H),
                context.dp(RegionSlots.TIMELINE_LINE_HEIGHT + 6),
            )
            addView(elapsed, LinearLayout.LayoutParams(0, WRAP, 1f))
            addView(total)
        }
        val timelineBand = FrameLayout(context).apply {
            addView(timeline, LayoutParams(MATCH, MATCH))
            addView(times, LayoutParams(MATCH, MATCH))
        }
        val gap = context.dp(RegionSlots.ROW_GAP)
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(RegionSlots.ROW_PADDING_H), 0, context.dp(RegionSlots.ROW_PADDING_H), 0)
            addView(previous, LinearLayout.LayoutParams(context.dp(RegionSlots.BUTTON_SIZE), context.dp(RegionSlots.BUTTON_SIZE)))
            addView(episodePill, LinearLayout.LayoutParams(0, context.dp(RegionSlots.ROW_HEIGHT), 1f).apply { setMargins(gap, 0, gap, 0) })
            addView(next, LinearLayout.LayoutParams(context.dp(RegionSlots.BUTTON_SIZE), context.dp(RegionSlots.BUTTON_SIZE)))
            addView(speedPill, LinearLayout.LayoutParams(WRAP, context.dp(RegionSlots.ROW_HEIGHT)).apply { leftMargin = gap })
        }

        bottom.apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.TRANSPARENT, RegionColors.SCRIM_BOTTOM))
            addView(timelineBand, LinearLayout.LayoutParams(MATCH, context.dp(RegionSlots.TIMELINE_TOUCH_HEIGHT)))
            // The SDK's gap between the timeline and the row.
            addView(row, LinearLayout.LayoutParams(MATCH, context.dp(RegionSlots.ROW_HEIGHT)).apply { topMargin = context.dp(RegionSlots.ROW_TOP_GAP) })
        }
        addView(bottom, LayoutParams(MATCH, WRAP, Gravity.BOTTOM))

        // The page draws edge to edge: keep the row above the navigation bar, as the SDK's own does.
        trackSystemBars(this) { _, bottomBar -> bottom.setPadding(0, 0, 0, bottomBar) }
    }

    /** Renders [ui]. Safe to call as often as the SDK reports changes. */
    fun bind(ui: ControlsUi) {
        timeline.bind(ui)
        elapsed.text = formatTime(ui.positionMs)
        total.text = formatTime(ui.durationMs)
        episodeText.text = if (ui.episodeCount > 0) "EP ${ui.episodeNumber} / ${ui.episodeCount}" else "Episodes"
        episodePill.isEnabled = ui.episodeCount > 0
        speedText.text = speedLabel(ui.speed)
        previous.setEnabledLook(ui.hasPrevious)
        next.setEnabledLook(ui.hasNext)
    }

    private fun timeText(context: Context): TextView =
        label(context, "", sp = 10f, color = RegionColors.TEXT_SECONDARY).apply {
            typeface = Typeface.MONOSPACE
            setShadowLayer(0f, 0f, 0f, 0)
        }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}

/**
 * Creates the controls region view and connects it to the SDK. Each action is one controller call:
 * `seekTo`, `selectEpisode`, `setPlaybackSpeed`; the episode pill opens the episode list.
 *
 * The SDK calls this each time the playback episode changes, with a controller that is valid while
 * the returned view is attached.
 */
internal fun createControlsRegionView(context: Context, controller: ContentPlayerController): View {
    val feed = PlayerFeed.of(controller)
    val view = ControlsRegionView(
        context,
        ControlsActions(
            onSeek = { fraction -> controller.seekTo((fraction * feed.progress.durationMs).toLong()) },
            onPrevious = { controller.selectEpisode(feed.controlsUi().episodeNumber - 1) },
            onNext = { controller.selectEpisode(feed.controlsUi().episodeNumber + 1) },
            onEpisodes = { showEpisodeList(context, controller, feed) },
            onSpeed = { controller.setPlaybackSpeed(nextSpeed(feed.state.playbackSpeed)) },
        ),
    )
    observeWhileAttached(view, feed) {
        // A locked episode shows the SDK's unlock or unavailable layer; seeking and switching would be ignored.
        view.visibility = if (feed.state.episodeLocked) View.GONE else View.VISIBLE
        view.bind(feed.controlsUi())
    }
    return view
}

/**
 * Timeline with the SDK's geometry: a 44 dp touch band whose line sits at its bottom edge, 2 dp thick
 * and 6 dp while dragging. While the player waits for data a light runs along the line. A drag only
 * counts once it is mostly horizontal.
 */
private class TimelineView(context: Context, private val onSeek: (Float) -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var ui: ControlsUi? = null
    private var dragFraction: Float? = null
    private var downX = 0f
    private var downY = 0f
    private var dragStarted = false

    fun bind(ui: ControlsUi) {
        this.ui = ui
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val state = ui ?: return
        val w = width.toFloat()
        val lineHeight = context.dp(if (dragFraction != null) RegionSlots.TIMELINE_LINE_HEIGHT_DRAGGING else RegionSlots.TIMELINE_LINE_HEIGHT).toFloat()
        val top = height - lineHeight
        val bottom = height.toFloat()

        // The unplayed track, then the played part with the accent gradient.
        paint.shader = null
        paint.color = RegionColors.TRACK
        canvas.drawRect(0f, top, w, bottom, paint)
        val played = w * (dragFraction ?: state.fraction)
        paint.shader = LinearGradient(0f, 0f, w, 0f, RegionColors.ACCENT, RegionColors.ACCENT_END, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, top, played, bottom, paint)

        // The thumb while dragging.
        dragFraction?.let {
            paint.shader = null
            paint.color = Color.WHITE
            canvas.drawCircle(w * it, top + lineHeight / 2, context.dp(7).toFloat(), paint)
        }

        // The light that runs while the player waits for data.
        if (state.loading) {
            val light = w * 0.3f
            val phase = (System.currentTimeMillis() % LOADING_CYCLE_MS) / LOADING_CYCLE_MS.toFloat()
            val x = (w + light) * phase - light
            paint.shader = LinearGradient(x, 0f, x + light, 0f, intArrayOf(Color.TRANSPARENT, 0xE6FFFFFF.toInt(), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            canvas.drawRect(x, top, x + light, bottom, paint)
            postInvalidateOnAnimation()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (ui?.seekable != true) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                dragStarted = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragStarted && abs(event.x - downX) > slop && abs(event.x - downX) > abs(event.y - downY)) {
                    dragStarted = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (dragStarted) {
                    dragFraction = (event.x / width).coerceIn(0f, 1f)
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                if (dragStarted) dragFraction?.let(onSeek)
                reset()
            }

            MotionEvent.ACTION_CANCEL -> reset()
        }
        return true
    }

    private fun reset() {
        dragStarted = false
        dragFraction = null
        invalidate()
    }

    private companion object {
        const val LOADING_CYCLE_MS = 1100L
    }
}

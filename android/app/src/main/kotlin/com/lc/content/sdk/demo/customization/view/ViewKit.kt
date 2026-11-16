package com.lc.content.sdk.demo.customization.view

import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import com.lc.content.sdk.demo.customization.PlayerFeed
import com.lc.content.sdk.demo.customization.RegionColors

/*
 * Small helpers the View version of the custom regions share. They keep the region files about the
 * regions, not about Android View boilerplate.
 */

internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

/** The Activity that hosts the player page. The context the SDK passes may be wrapped. */
internal tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

/** A rounded rectangle. */
internal fun roundedBackground(context: Context, color: Int, radiusDp: Int): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = context.dp(radiusDp).toFloat()
    }

/** A capsule: the radius is far larger than any height, so the ends round off fully. */
internal fun pillBackground(color: Int): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = 1000f
    }

internal fun circleBackground(color: Int): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

/** Text on the video: white with a soft shadow, readable on any frame. */
internal fun label(context: Context, text: String, sp: Float, bold: Boolean = false, color: Int = RegionColors.TEXT): TextView =
    TextView(context).apply {
        this.text = text
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(context.dp(3).toFloat(), 0f, 0f, 0x99000000.toInt())
    }

/** A white vector icon of [sizeDp]. */
internal fun icon(context: Context, drawable: Int, sizeDp: Int): ImageView =
    ImageView(context).apply {
        setImageResource(drawable)
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(Color.WHITE))
        layoutParams = FrameLayout.LayoutParams(context.dp(sizeDp), context.dp(sizeDp), Gravity.CENTER)
    }

/** A round button of [sizeDp] holding an icon of [iconDp]. */
internal fun iconButton(context: Context, drawable: Int, sizeDp: Int, iconDp: Int, fill: Int, description: String): FrameLayout =
    FrameLayout(context).apply {
        background = circleBackground(fill)
        contentDescription = description
        addView(icon(context, drawable, iconDp))
    }

/** Makes [view] a clickable that only reacts when [enabled]; a disabled one is dimmed. */
internal fun View.setEnabledLook(enabled: Boolean) {
    isEnabled = enabled
    alpha = if (enabled) 1f else 0.35f
}

/**
 * Renders on every update of [feed] while [view] is attached, and once at attach. The observer is
 * dropped on detach, so the view of a replaced episode stops reacting.
 */
internal fun observeWhileAttached(view: View, feed: PlayerFeed, render: () -> Unit) {
    var stop: (() -> Unit)? = null
    view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            stop = feed.observe(render)
            render()
        }

        override fun onViewDetachedFromWindow(v: View) {
            stop?.invoke()
            stop = null
        }
    })
    render()
}

/**
 * Reports the height of the status bar ([onChange] `top`) and the navigation bar (`bottom`) in pixels,
 * at attach and whenever they change.
 *
 * The SDK draws the page edge to edge and hands your view the whole page, system bars included, so a
 * view must keep clear of the bars itself. Two sources are used on purpose: the insets listener, and
 * the window's insets read at attach, because a view hosted inside Compose is not always sent insets
 * by its parent; without the second one the view would draw under the bars.
 */
internal fun trackSystemBars(view: View, onChange: (top: Int, bottom: Int) -> Unit) {
    val types = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
    fun read(insets: WindowInsetsCompat?) {
        val bars = insets?.getInsets(types) ?: return
        onChange(bars.top, bars.bottom)
    }
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
        read(insets)
        insets
    }
    view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            read(ViewCompat.getRootWindowInsets(v))
            ViewCompat.requestApplyInsets(v)
        }

        override fun onViewDetachedFromWindow(v: View) = Unit
    })
    if (view.isAttachedToWindow) read(ViewCompat.getRootWindowInsets(view))
}

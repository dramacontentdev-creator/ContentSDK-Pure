package com.lc.content.sdk.demo.customization

/**
 * Colors of the custom regions, as ARGB ints so the Compose and the View version draw the same.
 * Compose wraps one with `Color(value)`; a View uses the int directly.
 */
internal object RegionColors {
    /** Accent, and the end of its gradient. The same pair as the demo home screen. */
    const val ACCENT = 0xFFFF4D6D.toInt()
    const val ACCENT_END = 0xFFFF8A4C.toInt()

    /** Text on the video. */
    const val TEXT = 0xFFFFFFFF.toInt()
    const val TEXT_SECONDARY = 0xB3FFFFFF.toInt()

    /** Round buttons over the video: frosted dark glass. */
    const val GLASS = 0x66000000

    /** Buttons over the video that carry a value, such as the episode and speed pills. */
    const val PILL = 0x33FFFFFF

    /** The unplayed part of the timeline. */
    const val TRACK = 0x40FFFFFF

    /** The shade behind the bottom controls; fades from nothing at the top. */
    const val SCRIM_BOTTOM = 0x99000000.toInt()

    /** The episode list panel and its cells. */
    const val PANEL = 0xFF17191F.toInt()
    const val CELL = 0x14FFFFFF
}

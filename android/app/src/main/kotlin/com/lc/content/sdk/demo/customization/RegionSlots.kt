package com.lc.content.sdk.demo.customization

/**
 * Where the SDK's own parts sit, in dp. The custom regions use the same numbers, which is what makes
 * them replace the SDK parts in place: the same top bar, the same bottom row, the same distance from
 * the screen edge. Bottom values are measured above the system navigation bar.
 */
internal object RegionSlots {
    // Info region, top: the back button in a 56 dp bar.
    const val TOP_BAR_HEIGHT = 56
    const val TOP_BAR_PADDING_H = 16
    const val TOP_BAR_PADDING_V = 8
    const val BUTTON_SIZE = 40

    // Controls region: a 44 dp touch band whose line sits at its bottom, a gap, then a 40 dp row.
    const val TIMELINE_TOUCH_HEIGHT = 44
    const val TIMELINE_LINE_HEIGHT = 2
    const val TIMELINE_LINE_HEIGHT_DRAGGING = 6
    const val ROW_TOP_GAP = 14
    const val ROW_HEIGHT = 40
    const val ROW_PADDING_H = 16
    const val ROW_GAP = 8

    /** Height of the whole bottom row area: the gap plus the row. */
    const val CONTROLS_HEIGHT = ROW_TOP_GAP + ROW_HEIGHT

    /**
     * Room for the elapsed / total time labels, which sit in the timeline band just above the line.
     * The SDK's own controls draw no time, so the intro rises by this much to stay clear of them.
     */
    const val TIMES_CLEARANCE = 16

    // Info region, bottom: the intro sits left, a little above the controls and their time labels.
    const val INTRO_PADDING_H = 16
    const val INTRO_GAP_ABOVE_CONTROLS = 12 + TIMES_CLEARANCE

    /**
     * What the intro occupies at the bottom of the page, system bars included. It is declared to the SDK
     * (`bottomInsetDp`) so subtitles stay above it: controls (54) + gap (28) + intro (about 44) + the
     * navigation bar.
     */
    const val INTRO_BOTTOM_INSET = 192

    // Episode list, as the SDK's own list.
    const val EPISODE_COLUMNS = 5
    const val EPISODE_PANEL_MAX_HEIGHT_FRACTION = 0.7f
    const val EPISODE_PANEL_CORNER_RADIUS = 24
}

import CoreGraphics

/// Where the SDK's own parts sit, in points. The custom regions use the same numbers, which is what
/// makes them replace the SDK parts in place: the same top bar, the same bottom row, the same
/// distance from the screen edge. Bottom values are measured above the home indicator.
enum RegionSlots {
    // Info region, top: the back button in a 56 pt bar.
    static let topBarHeight: CGFloat = 56
    static let topBarPaddingH: CGFloat = 16
    static let topBarPaddingV: CGFloat = 8
    static let buttonSize: CGFloat = 40

    // Controls region: a 44 pt touch band whose line sits at its bottom, a gap, then a 40 pt row.
    static let timelineTouchHeight: CGFloat = 44
    static let timelineLineHeight: CGFloat = 2
    static let timelineLineHeightDragging: CGFloat = 6
    static let rowTopGap: CGFloat = 14
    static let rowHeight: CGFloat = 40
    static let rowPaddingH: CGFloat = 16
    static let rowGap: CGFloat = 8

    /// Height of the whole bottom row area: the gap plus the row.
    static let controlsHeight: CGFloat = rowTopGap + rowHeight

    /// Room for the elapsed / total time labels, which sit in the timeline band just above the line.
    /// The SDK's own controls draw no time, so the intro rises by this much to stay clear of them.
    static let timesClearance: CGFloat = 16

    // Info region, bottom: the intro sits left, a little above the controls and their time labels.
    static let introPaddingH: CGFloat = 16
    static let introGapAboveControls: CGFloat = 12 + timesClearance

    /// What the intro occupies at the bottom of the page, safe area included. It is declared to the
    /// SDK (`bottomInsetDp`) so subtitles stay above it: controls (54) + gap (28) + intro (about 44)
    /// + the home indicator.
    static let introBottomInset: Int32 = 192

    // Episode list, as the SDK's own list.
    static let episodeColumns = 5
    static let episodePanelCornerRadius: CGFloat = 24
}

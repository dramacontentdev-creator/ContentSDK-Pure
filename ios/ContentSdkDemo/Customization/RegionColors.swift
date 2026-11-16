import SwiftUI
import UIKit

/// Colors of the custom regions as ARGB values, so the SwiftUI and the UIKit version draw the same.
/// The same numbers as the Android demo.
enum RegionColors {
    /// Accent, and the end of its gradient. The same pair as the demo home screen.
    static let accent: UInt32 = 0xFFFF4D6D
    static let accentEnd: UInt32 = 0xFFFF8A4C

    /// Text on the video.
    static let text: UInt32 = 0xFFFFFFFF
    static let textSecondary: UInt32 = 0xB3FFFFFF

    /// Round buttons over the video: frosted dark glass.
    static let glass: UInt32 = 0x66000000

    /// Buttons over the video that carry a value, such as the episode and speed pills.
    static let pill: UInt32 = 0x33FFFFFF

    /// The unplayed part of the timeline.
    static let track: UInt32 = 0x40FFFFFF

    /// The shade behind the bottom controls; fades from nothing at the top.
    static let scrimBottom: UInt32 = 0x99000000

    /// The episode list panel and its cells.
    static let panel: UInt32 = 0xFF17191F
    static let cell: UInt32 = 0x14FFFFFF
}

extension Color {
    /// `Color(argb: 0xFFFF4D6D)`.
    init(argb: UInt32) {
        self.init(
            .sRGB,
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255,
            opacity: Double((argb >> 24) & 0xFF) / 255
        )
    }
}

extension UIColor {
    /// `UIColor(argb: 0xFFFF4D6D)`.
    convenience init(argb: UInt32) {
        self.init(
            red: CGFloat((argb >> 16) & 0xFF) / 255,
            green: CGFloat((argb >> 8) & 0xFF) / 255,
            blue: CGFloat(argb & 0xFF) / 255,
            alpha: CGFloat((argb >> 24) & 0xFF) / 255
        )
    }
}

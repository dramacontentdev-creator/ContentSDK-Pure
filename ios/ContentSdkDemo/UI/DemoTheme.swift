import SwiftUI

// -----------------------------------------------------------------------------
// Demo UI only. Nothing in the UI/ and Lab/ folders is needed to integrate the SDK.
// -----------------------------------------------------------------------------

/// The demo's palette: a dark page, raised surfaces, and one accent. The same colors as the Android demo.
enum DemoColors {
    static let background = Color(red: 0.055, green: 0.059, blue: 0.075)
    static let surface = Color(red: 0.090, green: 0.098, blue: 0.122)
    static let outline = Color(red: 0.149, green: 0.161, blue: 0.196)
    static let textPrimary = Color(red: 0.957, green: 0.961, blue: 0.969)
    static let textSecondary = Color(red: 0.604, green: 0.627, blue: 0.675)

    /// The accent and the end of its gradient; the custom player regions use the same pair.
    static let accent = Color(red: 1.0, green: 0.302, blue: 0.427)
    static let accentEnd = Color(red: 1.0, green: 0.541, blue: 0.298)

    static let success = Color(red: 0.204, green: 0.827, blue: 0.600)
    static let warning = Color(red: 0.984, green: 0.749, blue: 0.141)
    static let error = Color(red: 0.973, green: 0.443, blue: 0.443)
    static let info = Color(red: 0.376, green: 0.647, blue: 0.980)

    static let accentGradient = LinearGradient(
        colors: [accent, accentEnd], startPoint: .leading, endPoint: .trailing
    )
}

/// A raised, outlined block on the home screen.
struct DemoCard<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) { content }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(18)
            .background(DemoColors.surface, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(DemoColors.outline, lineWidth: 1))
    }
}

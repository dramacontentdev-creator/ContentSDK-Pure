// =================================================================================================
//  PURE edition. This file and its commercial twin are the only files of Customization/ that differ
//  between the editions, together with the Unlock folder, SwiftUI/SwiftUIUnlockLayer.swift and
//  UIKit/UIKitUnlockLayer.swift that only exist in the commercial demo.
// =================================================================================================

/// What the edition adds to the player page styles. The pure edition adds nothing: it has no SDK ads
/// and no SDK unlock screen (`setUnlockView` and `unlockWithRewardedAd` are not in this package), so
/// there is nothing to replace. Monetizing locked episodes is up to your app; see
/// Catalog/EditionApiCatalog.swift.
enum EditionCustomization {

    static func apply(_ style: PlayerPageStyle) {}
}

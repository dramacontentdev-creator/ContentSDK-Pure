package com.lc.content.sdk.demo.customization

// =================================================================================================
//  PURE edition. This file and its commercial twin are the only files of customization/ that differ
//  between the editions, together with the unlock/ folder and the UnlockLayer files that only exist
//  in the commercial demo.
// =================================================================================================

/**
 * What the edition adds to the player page styles. The pure edition adds nothing: it has no SDK ads
 * and no SDK unlock screen (`setUnlockView` and `unlockWithRewardedAd` are not in this package), so
 * there is nothing to replace. Monetizing locked episodes is up to your app; see
 * catalog/EditionApiCatalog.kt.
 */
internal object EditionCustomization {

    @Suppress("UNUSED_PARAMETER")
    fun apply(style: PlayerPageStyle) = Unit
}

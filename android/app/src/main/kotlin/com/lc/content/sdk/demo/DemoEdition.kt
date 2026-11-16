package com.lc.content.sdk.demo

import android.app.Activity
import android.util.Log
import com.lc.content.sdk.AndroidContentSdk
import com.lc.content.sdk.demo.ui.IntegrationStep
import com.lc.content.sdk.demo.ui.SdkStatus

// =================================================================================================
//  PURE edition. Kotlin files that differ from the commercial demo: this file,
//  catalog/EditionApiCatalog.kt, and customization/EditionCustomization.kt; the commercial demo also
//  has its own unlock UI. Every other Kotlin file is identical in the commercial and pure demos.
// =================================================================================================

/** What the demo UI shows about this edition. Demo only. */
object DemoEdition {
    const val NAME = "Pure"
    const val SUMMARY = "No SDK ads: your app monetizes on its own and reports the revenue."
    const val STARTING_DETAIL = "Booting the SDK."

    /** The SDK's `setAutoPlay` default in this edition: an episode is prepared and waits for a play. */
    const val AUTO_PLAY_DEFAULT = false

    /** The integration checklist on the demo home screen; labels match the `Step` comments in code. */
    val integrationSteps = listOf(
        IntegrationStep("1", "Add SDK repositories", "settings.gradle.kts"),
        IntegrationStep("2", "Add the AAR and its dependencies", "app/build.gradle.kts"),
        IntegrationStep("3", "Initialize the SDK", "DemoApplication.kt"),
        IntegrationStep("4", "Start the SDK", "DemoEdition.kt"),
        IntegrationStep("5", "Open the SDK page", "MainActivity.kt"),
    )
}

/**
 * Your SDK credentials for the PURE edition.
 *
 * The values below are the DEMO credentials. The commercial and the pure demo, on Android and on
 * iOS, all use this one set, and it is for the demos only. Replace it with YOUR App ID / App
 * Secret, which you get at https://open.lucky-chick.com.
 * Do not hard-code your production secret in a public repository.
 */
object SdkConfig {
    /** YOUR Content App ID. */
    const val APP_ID = "1d724208-378c-489a-8462-f9038fa1ffd8"

    /** YOUR Content App Secret. */
    const val APP_SECRET = "427908ce2f21dc75bab6202bd7a3390f29e27bfe0b3e686b4570af7e4ef972fd"

    /**
     * YOUR logged-in user's id, so watch history follows the user across devices.
     * Pass "" if your app has no login; the SDK then creates an anonymous user.
     */
    const val USER_ID = ""
}

/**
 * Content SDK - integration Step 4 (start)
 *
 * `start` boots the SDK (network, player). Call it once; do NOT show SDK pages before it succeeds.
 * The pure edition shows no SDK ads, so there is no ad consent step.
 *
 * The callback runs on the main thread.
 */
fun startContentSdk(activity: Activity, onResult: (SdkStatus) -> Unit) {
    AndroidContentSdk.start { error ->
        if (error == null) {
            onResult(SdkStatus.Ready)
        } else {
            // Show a failure message or retry entry.
            Log.w("ContentSdkDemo", "Content SDK start failed: ${error.code} ${error.msg}")
            onResult(SdkStatus.Failed(error.code, error.msg))
        }
    }
}

package com.lc.content.sdk.demo.catalog

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.lc.content.sdk.AndroidContentSdk
import com.lc.content.sdk.api.SdkException
import com.lc.content.sdk.api.player.ContentPlayerController
import com.lc.content.sdk.api.revenue.PurchaseRevenueReport
import java.lang.ref.WeakReference

/*
 * =================================================================================================
 *  Content SDK - PURE edition API catalog (Android)
 * =================================================================================================
 *
 *  This is the Ads block of SdkApiCatalog.kt. The pure edition has no SDK ads, ad consent, ad events,
 *  or unlock UI. Your app decides where to charge, pauses there, and reports the revenue; see P2.
 *
 *  | API                                        | When to call                            | Section |
 *  |--------------------------------------------|-----------------------------------------|---------|
 *  | start (suspend version)                    | coroutine version of Step 4             | P1      |
 *  | pause + your paywall or ad + play          | an episode your own rules charge for    | P2      |
 *  | reportPurchaseRevenue / reportAdRevenue    | after your purchase or ad               | P2, 13  |
 * =================================================================================================
 */
@Suppress("unused")
object EditionApiCatalog {

    private const val TAG = "ContentSdkDemo"

    // ---------------------------------------------------------------------------------------------
    // P1. [Setup] Step 4 with coroutines
    // ---------------------------------------------------------------------------------------------

    /**
     * Same as `startContentSdk` (DemoEdition.kt in the demo root), with a suspend function.
     * e.g. `lifecycleScope.launch { EditionApiCatalog.startWithCoroutines() }`.
     *
     * @return true when SDK pages can be opened.
     */
    suspend fun startWithCoroutines(): Boolean = try {
        AndroidContentSdk.start()
        true
    } catch (error: SdkException) {
        Log.w(TAG, "start failed: ${error.code} ${error.msg}")
        false
    }

    // ---------------------------------------------------------------------------------------------
    // P2. [Ads] Your own monetization
    // ---------------------------------------------------------------------------------------------

    /** Episodes 1..FREE_EPISODES play freely; this is YOUR rule, the SDK knows nothing about it. */
    private const val FREE_EPISODES = 5

    /** Episode the paywall was last shown for, so state changes on the same episode do not show it again. */
    private var promptedEpisodeId: String? = null

    /**
     * When an SDK player page reaches an episode your own rules charge for: pause, show YOUR paywall
     * or ad, report the revenue, then call `play()`.
     *
     * This works for episodes that can play. An episode the content service reports as locked
     * (`state.episodeLocked`) has no playable source: `play()` does not start it and the SDK shows
     * "unavailable" on it, so do not sell it here.
     *
     * Call it once in Application.onCreate. Each listener here is the only one of its kind:
     * - It replaces the `setPlayerControllerListener` of SdkApiCatalog 8.1; keep one in your app.
     * - `controller.setListener` replaces the controller's state listener. If your app already has
     *   one (in this demo, PlayerFeed under customization/), run this check there instead.
     */
    fun monetizeEpisodes(application: Application) {
        // The SDK player page is an Activity of its own: show the paywall on whichever is in front.
        application.registerActivityLifecycleCallbacks(ResumedActivity)
        AndroidContentSdk.setPlayerControllerListener { controller ->
            controller?.setListener { state ->
                val resource = state.resource ?: return@setListener
                if (state.episodeLocked || resource.episodeNumber <= FREE_EPISODES) return@setListener
                if (resource.episodeId == promptedEpisodeId) return@setListener
                promptedEpisodeId = resource.episodeId
                controller.pause()
                showYourPaywall { orderId, paidUsd ->
                    reportPurchaseThenPlay(controller, orderId, paidUsd)
                }
            }
        }
    }

    private fun reportPurchaseThenPlay(controller: ContentPlayerController, orderId: String, paidUsd: Double) {
        val resource = controller.state.resource ?: return
        AndroidContentSdk.reportPurchaseRevenue(
            PurchaseRevenueReport(aboutUnlock = true, resource = resource, revenueUsd = paidUsd, dedupeId = orderId),
        )
        // With your own rewarded ad instead of a purchase: report it with reportAdRevenue (SdkApiCatalog 13.1).
        controller.play()
    }

    /** Stand-in for YOUR purchase or ad flow. Replace it with Google Play Billing or your ad SDK. */
    private fun showYourPaywall(onPaid: (orderId: String, paidUsd: Double) -> Unit) {
        val activity = ResumedActivity.current ?: return
        AlertDialog.Builder(activity)
            .setTitle("Keep watching")
            .setMessage("Demo paywall. Replace it with your own purchase or ad flow.")
            .setPositiveButton("Pay \$0.99") { _, _ -> onPaid("DEMO-ORDER-${System.currentTimeMillis()}", 0.99) }
            .setNegativeButton("Not now", null)
            .show()
    }

    /** The Activity in front, held weakly so a closed one is not kept. */
    private object ResumedActivity : Application.ActivityLifecycleCallbacks {
        private var resumed: WeakReference<Activity>? = null
        val current: Activity? get() = resumed?.get()

        override fun onActivityResumed(activity: Activity) {
            resumed = WeakReference(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            if (current === activity) resumed = null
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}

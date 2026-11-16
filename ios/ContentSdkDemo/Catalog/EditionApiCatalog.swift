import UIKit
import ContentSdk

/*
 =================================================================================================
  Content SDK - PURE edition API catalog (iOS)
 =================================================================================================

  The pure edition has no SDK ads, ad consent, ad events, or unlock UI. Your app decides where to
  charge, pauses there, and reports the revenue; see P1.

  | API                                          | When to call                           | Section |
  |----------------------------------------------|----------------------------------------|---------|
  | pause + your paywall or ad + play            | an episode your own rules charge for   | P1      |
  | reportPurchaseRevenue / reportAdRevenue      | after your purchase or ad              | P1, 13  |
 =================================================================================================
 */
enum EditionApiCatalog {

    // MARK: - P1. [Ads] Your own monetization

    /// When an SDK player page reaches an episode your own rules charge for: pause, show YOUR
    /// paywall or ad, report the revenue, then call `play()`.
    ///
    /// This works for episodes that can play. An episode the content service reports as locked
    /// (`state.episodeLocked`) has no playable source: `play()` does not start it and the SDK shows
    /// "unavailable" on it, so do not sell it here.
    ///
    /// Register once at launch. Each listener here is the only one of its kind:
    /// - It replaces the `setPlayerControllerListener` of SdkApiCatalog 8.1; keep one in your app.
    /// - `controller.setListener` replaces the controller's state listener. If your app already has
    ///   one (in this demo, PlayerFeed under Customization/), run this check there instead.
    static func monetizeEpisodes() {
        IosContentSdk.shared.setPlayerControllerListener(listener: EpisodeMonetizer.shared)
    }
}

/// Watches the SDK player page on screen and shows your paywall past the free episodes.
private final class EpisodeMonetizer: NSObject, PlayerControllerListener, ContentPlayerListener {
    static let shared = EpisodeMonetizer()

    /// Episodes 1...freeEpisodes play freely; this is YOUR rule, the SDK knows nothing about it.
    private let freeEpisodes: Int32 = 5

    private var controller: ContentPlayerController?
    /// Episode the paywall was last shown for, so state changes on the same episode do not show it again.
    private var promptedEpisodeId: String?

    func onPlayerControllerChanged(controller: ContentPlayerController?) {
        self.controller = controller
        controller?.setListener(listener: self)
    }

    func onStateChanged(state: ContentPlayerState) {
        guard let controller, let resource = state.resource, !state.episodeLocked,
              resource.episodeNumber > freeEpisodes, resource.episodeId != promptedEpisodeId else { return }
        promptedEpisodeId = resource.episodeId
        controller.pause()
        showYourPaywall { [weak self] transactionId, paidUsd in
            self?.reportPurchaseThenPlay(controller, transactionId: transactionId, paidUsd: paidUsd)
        }
    }

    private func reportPurchaseThenPlay(_ controller: ContentPlayerController, transactionId: String, paidUsd: Double) {
        guard let resource = controller.state.resource else { return }
        IosContentSdk.shared.reportPurchaseRevenue(report: PurchaseRevenueReport(
            aboutUnlock: true,
            resource: resource,
            revenueUsd: paidUsd,
            dedupeId: transactionId
        ))
        // With your own rewarded ad instead of a purchase: report it with reportAdRevenue (SdkApiCatalog 13.1).
        controller.play()
    }

    /// Stand-in for YOUR purchase or ad flow. Replace it with StoreKit or your ad SDK.
    private func showYourPaywall(onPaid: @escaping (_ transactionId: String, _ paidUsd: Double) -> Void) {
        guard let presenter = UIApplication.shared.topViewController else { return }
        let alert = UIAlertController(
            title: "Keep watching",
            message: "Demo paywall. Replace it with your own purchase or ad flow.",
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "Pay $0.99", style: .default) { _ in
            onPaid("DEMO-TRANSACTION-\(Int(Date().timeIntervalSince1970))", 0.99)
        })
        alert.addAction(UIAlertAction(title: "Not now", style: .cancel))
        presenter.present(alert, animated: true)
    }
}

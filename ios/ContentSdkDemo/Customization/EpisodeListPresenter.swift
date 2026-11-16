import UIKit

/// Shows the custom episode list as a sheet over the player page. SwiftUI/SwiftUIEpisodeList.swift and
/// UIKit/UIKitEpisodeList.swift each add their own `show...` function; this owns the presenting.
///
/// A sheet and not a view inside the controls region: that region hides together with the SDK
/// controls, and the list must stay while you choose.
enum EpisodeListPresenter {
    static func present(_ controller: UIViewController, backgroundColor: UIColor) {
        guard let top = UIApplication.shared.topViewController else { return }
        controller.view.backgroundColor = backgroundColor
        controller.modalPresentationStyle = .pageSheet
        if let sheet = controller.sheetPresentationController {
            sheet.detents = [.medium(), .large()]
            sheet.prefersGrabberVisible = true
            sheet.preferredCornerRadius = RegionSlots.episodePanelCornerRadius
        }
        top.present(controller, animated: true)
    }
}

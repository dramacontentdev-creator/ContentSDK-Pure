import SwiftUI
import UIKit

/// Puts a SwiftUI view into the `UIView` the SDK asks a region for.
///
/// The hosting controller is kept alive by the returned container; without that, SwiftUI stops
/// updating the view. The SDK hands a touch to the view only where it draws something, so taps
/// and vertical swipes on empty areas still reach the page.
enum SwiftUIHost {
    static func view<Content: View>(_ root: Content) -> UIView {
        let host = UIHostingController(rootView: root)
        host.view.backgroundColor = .clear
        return HostContainer(host: host)
    }
}

private final class HostContainer: UIView {
    private let host: UIViewController

    init(host: UIViewController) {
        self.host = host
        super.init(frame: .zero)
        backgroundColor = .clear
        host.view.translatesAutoresizingMaskIntoConstraints = false
        addSubview(host.view)
        NSLayoutConstraint.activate([
            host.view.leadingAnchor.constraint(equalTo: leadingAnchor),
            host.view.trailingAnchor.constraint(equalTo: trailingAnchor),
            host.view.topAnchor.constraint(equalTo: topAnchor),
            host.view.bottomAnchor.constraint(equalTo: bottomAnchor),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    /// Makes the hosting controller a child of the view controller that shows the page.
    ///
    /// Without that, SwiftUI keeps the safe area it had when the view was created. The SDK builds
    /// the next episode's page below the screen, so after a swipe or "next episode" the top bar
    /// would sit under the status bar, where the back button cannot be tapped.
    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window == nil {
            guard host.parent != nil else { return }
            host.willMove(toParent: nil)
            host.removeFromParent()
        } else if host.parent == nil, let parent = owningViewController() {
            parent.addChild(host)
            host.didMove(toParent: parent)
        }
    }

    private func owningViewController() -> UIViewController? {
        var responder: UIResponder? = superview
        while let current = responder {
            if let controller = current as? UIViewController { return controller }
            responder = current.next
        }
        return nil
    }
}

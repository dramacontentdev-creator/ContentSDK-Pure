import UIKit

/*
 * Small helpers the UIKit version of the custom regions share. They keep the region files about the
 * regions, not about UIKit boilerplate.
 */

/// A full-page container that lets touches outside its subviews reach the SDK player page.
class PassThroughRegionView: UIView {
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        let hit = super.hitTest(point, with: event)
        return hit === self ? nil : hit
    }
}

/// A page container that tells when it is on screen. The SDK creates a new region view for every
/// episode, so a view must stop listening when it leaves the window.
class AttachObservingView: PassThroughRegionView {
    var onAttach: (() -> Void)?
    var onDetach: (() -> Void)?

    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window != nil { onAttach?() } else { onDetach?() }
    }
}

/// Renders on every update of `feed` while `view` is on screen, and once at attach. The observer is
/// dropped on detach, so the view of a replaced episode stops reacting.
///
/// `render` gets the view as its argument: use that one, not the outer variable. The view keeps
/// these closures, so a closure that captured the view itself would keep it alive for good.
func observeWhileAttached<Region: AttachObservingView>(_ view: Region, feed: PlayerFeed, render: @escaping (Region) -> Void) {
    var stop: (() -> Void)?
    view.onAttach = { [weak view] in
        stop?()
        stop = feed.observe { [weak view] in if let view { render(view) } }
        if let view { render(view) }
    }
    view.onDetach = {
        stop?()
        stop = nil
    }
    render(view)
}

/// A label with the demo's text style. Text on the video gets a soft shadow so any frame stays readable.
func makeLabel(_ text: String = "", size: CGFloat, weight: UIFont.Weight = .regular, argb: UInt32 = RegionColors.text,
               monospaced: Bool = false, shadow: Bool = false) -> UILabel {
    let label = UILabel()
    label.text = text
    label.textColor = UIColor(argb: argb)
    label.font = monospaced
        ? UIFont.monospacedSystemFont(ofSize: size, weight: weight)
        : UIFont.systemFont(ofSize: size, weight: weight)
    if shadow {
        label.layer.shadowColor = UIColor.black.cgColor
        label.layer.shadowOpacity = 0.6
        label.layer.shadowRadius = 3
        label.layer.shadowOffset = .zero
    }
    return label
}

/// A round button of `size` points holding an SF Symbol.
func makeCircleButton(symbol: String, size: CGFloat, fill: UInt32, label: String) -> UIButton {
    var config = UIButton.Configuration.plain()
    config.image = UIImage(systemName: symbol, withConfiguration: UIImage.SymbolConfiguration(pointSize: 16, weight: .semibold))
    config.baseForegroundColor = .white
    config.background.backgroundColor = UIColor(argb: fill)
    config.background.cornerRadius = size / 2
    let button = UIButton(configuration: config)
    button.accessibilityLabel = label
    button.translatesAutoresizingMaskIntoConstraints = false
    NSLayoutConstraint.activate([
        button.widthAnchor.constraint(equalToConstant: size),
        button.heightAnchor.constraint(equalToConstant: size),
    ])
    return button
}

/// A capsule button with an optional SF Symbol and a title.
func makePillButton(symbol: String?, title: String, height: CGFloat) -> UIButton {
    var config = UIButton.Configuration.plain()
    config.baseForegroundColor = .white
    config.imagePadding = 8
    config.contentInsets = NSDirectionalEdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16)
    config.background.backgroundColor = UIColor(argb: RegionColors.pill)
    config.background.cornerRadius = height / 2
    if let symbol {
        config.image = UIImage(systemName: symbol, withConfiguration: UIImage.SymbolConfiguration(pointSize: 15, weight: .semibold))
    }
    var attributed = AttributedString(title)
    attributed.font = UIFont.systemFont(ofSize: 14, weight: .semibold)
    config.attributedTitle = attributed
    let button = UIButton(configuration: config)
    button.translatesAutoresizingMaskIntoConstraints = false
    button.heightAnchor.constraint(equalToConstant: height).isActive = true
    return button
}

extension UIButton {
    /// Changes the title of a button made by `makePillButton`.
    func setPillTitle(_ title: String) {
        guard var config = configuration else { return }
        var attributed = AttributedString(title)
        attributed.font = UIFont.systemFont(ofSize: 14, weight: .semibold)
        config.attributedTitle = attributed
        configuration = config
    }

    /// A button that only reacts when `enabled`; a disabled one is dimmed.
    func setEnabledLook(_ enabled: Bool) {
        isEnabled = enabled
        alpha = enabled ? 1 : 0.35
    }
}

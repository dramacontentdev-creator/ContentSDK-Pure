import ContentSdk
import UIKit

/// UIKit version of the custom INFO region (see CustomRegions.swift): the back button in the top
/// bar and the episode intro at the bottom left, where the SDK draws its own. It looks and behaves
/// like the SwiftUI version in SwiftUI/SwiftUIInfoRegion.swift, so you can compare the two.
///
/// It is built in code to keep the demo to one file per region; in your app you would use a XIB or a
/// storyboard. Only the back button takes touches, so taps and vertical swipes reach the SDK page.
final class UIKitInfoRegionView: AttachObservingView {

    private let dramaName = makeLabel(size: 18, weight: .bold, shadow: true)
    private let episodeTitle = makeLabel(size: 13, argb: RegionColors.textSecondary, shadow: true)
    private let intro = UIStackView()

    init(onBack: @escaping () -> Void) {
        super.init(frame: .zero)
        backgroundColor = .clear

        // Top bar: same height, padding, and button size as the SDK's.
        let back = makeCircleButton(symbol: "arrow.left", size: RegionSlots.buttonSize, fill: RegionColors.glass, label: "Back")
        back.addAction(UIAction { _ in onBack() }, for: .touchUpInside)
        back.translatesAutoresizingMaskIntoConstraints = false
        addSubview(back)

        // Bottom left: drama name with a small CUSTOM tag, then the episode badge and title.
        let customTag = PaddedLabel(horizontal: 5, vertical: 2)
        customTag.text = "CUSTOM"
        customTag.font = UIFont.systemFont(ofSize: 9, weight: .bold)
        customTag.textColor = UIColor(argb: RegionColors.accent)
        customTag.backgroundColor = UIColor(argb: RegionColors.accent).withAlphaComponent(0.18)
        customTag.layer.cornerRadius = 4
        customTag.layer.masksToBounds = true

        let badge = PaddedLabel(horizontal: 8, vertical: 2)
        badge.font = UIFont.systemFont(ofSize: 11, weight: .bold)
        badge.textColor = .white
        badge.backgroundColor = UIColor(argb: RegionColors.accent)
        badge.layer.masksToBounds = true
        badgeLabel = badge

        let nameRow = UIStackView(arrangedSubviews: [dramaName, customTag, UIView()])
        nameRow.spacing = 8
        nameRow.alignment = .center
        let episodeRow = UIStackView(arrangedSubviews: [badge, episodeTitle, UIView()])
        episodeRow.spacing = 8
        episodeRow.alignment = .center
        intro.axis = .vertical
        intro.spacing = 6
        intro.addArrangedSubview(nameRow)
        intro.addArrangedSubview(episodeRow)
        intro.translatesAutoresizingMaskIntoConstraints = false
        addSubview(intro)

        NSLayoutConstraint.activate([
            back.leadingAnchor.constraint(equalTo: leadingAnchor, constant: RegionSlots.topBarPaddingH),
            back.topAnchor.constraint(equalTo: safeAreaLayoutGuide.topAnchor, constant: RegionSlots.topBarPaddingV),
            intro.leadingAnchor.constraint(equalTo: leadingAnchor, constant: RegionSlots.introPaddingH),
            intro.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -RegionSlots.introPaddingH),
            intro.bottomAnchor.constraint(
                equalTo: safeAreaLayoutGuide.bottomAnchor,
                constant: -(RegionSlots.controlsHeight + RegionSlots.introGapAboveControls)
            ),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    private var badgeLabel: PaddedLabel?

    override func layoutSubviews() {
        super.layoutSubviews()
        badgeLabel?.layer.cornerRadius = (badgeLabel?.bounds.height ?? 0) / 2
    }

    /// Renders `ui`. Safe to call as often as the SDK reports changes.
    func bind(_ ui: InfoUi) {
        intro.isHidden = !ui.hasEpisode
        dramaName.text = ui.dramaName
        badgeLabel?.text = "EP \(ui.episodeNumber)"
        episodeTitle.text = ui.episodeTitle
    }
}

/// A label with room around its text.
final class PaddedLabel: UILabel {
    private let horizontal: CGFloat
    private let vertical: CGFloat

    init(horizontal: CGFloat, vertical: CGFloat) {
        self.horizontal = horizontal
        self.vertical = vertical
        super.init(frame: .zero)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    override func drawText(in rect: CGRect) {
        super.drawText(in: rect.inset(by: UIEdgeInsets(top: vertical, left: horizontal, bottom: vertical, right: horizontal)))
    }

    override var intrinsicContentSize: CGSize {
        let size = super.intrinsicContentSize
        return CGSize(width: size.width + horizontal * 2, height: size.height + vertical * 2)
    }
}

/// Creates the info region view and connects it to the SDK: the controller gives the `PlayerFeed`,
/// and the view renders the feed's `InfoUi` on every change. The SDK calls this each time the
/// playback episode changes, with a controller that is valid while the returned view is on screen.
func createInfoRegionView(controller: ContentPlayerController) -> UIView {
    let feed = PlayerFeed.of(controller)
    let view = UIKitInfoRegionView(onBack: PlayerPageCloser.close)
    observeWhileAttached(view, feed: feed) { view in view.bind(feed.infoUi()) }
    return view
}

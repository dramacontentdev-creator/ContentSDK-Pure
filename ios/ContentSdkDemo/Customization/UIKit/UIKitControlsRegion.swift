import ContentSdk
import UIKit

/// What the controls view asks for. Plain callbacks, so the view needs no SDK.
struct ControlsActions {
    let onSeek: (Double) -> Void
    let onPrevious: () -> Void
    let onNext: () -> Void
    let onEpisodes: () -> Void
    let onSpeed: () -> Void
}

/// UIKit version of the custom CONTROLS region (see CustomRegions.swift): a thin timeline with its
/// 44 pt touch band, then one row of 40 pt controls, where the SDK draws its own. It looks and
/// behaves like the SwiftUI version in SwiftUI/SwiftUIControlsRegion.swift, so you can compare the two.
///
/// Only the timeline band and the buttons take touches. The timeline's pan recognizer starts only for
/// a mostly horizontal drag, so a vertical swipe that begins on it still changes the episode.
final class UIKitControlsRegionView: AttachObservingView {

    private let timeline: TimelineView
    private let elapsed = makeLabel(size: 10, argb: RegionColors.textSecondary, monospaced: true)
    private let total = makeLabel(size: 10, argb: RegionColors.textSecondary, monospaced: true)
    private let previous = makeCircleButton(symbol: "chevron.left", size: RegionSlots.buttonSize, fill: RegionColors.pill, label: "Previous episode")
    private let nextButton = makeCircleButton(symbol: "chevron.right", size: RegionSlots.buttonSize, fill: RegionColors.pill, label: "Next episode")
    private let episodePill = makePillButton(symbol: "list.bullet", title: "", height: RegionSlots.rowHeight)
    private let speedPill = makePillButton(symbol: nil, title: "", height: RegionSlots.rowHeight)
    private let bottom = UIStackView()
    private let scrim = CAGradientLayer()

    init(actions: ControlsActions) {
        timeline = TimelineView(onSeek: actions.onSeek)
        super.init(frame: .zero)
        backgroundColor = .clear

        previous.addAction(UIAction { _ in actions.onPrevious() }, for: .touchUpInside)
        nextButton.addAction(UIAction { _ in actions.onNext() }, for: .touchUpInside)
        episodePill.addAction(UIAction { _ in actions.onEpisodes() }, for: .touchUpInside)
        speedPill.addAction(UIAction { _ in actions.onSpeed() }, for: .touchUpInside)

        // The shade behind the controls fades from nothing at the top and reaches under the home indicator.
        scrim.colors = [UIColor.clear.cgColor, UIColor(argb: RegionColors.scrimBottom).cgColor]
        let shade = UIView()
        shade.isUserInteractionEnabled = false
        shade.layer.addSublayer(scrim)
        shade.translatesAutoresizingMaskIntoConstraints = false
        addSubview(shade)
        shadeView = shade

        // Elapsed and total time sit in the timeline band, just above the line: the 14 pt gap below the
        // band is too low for text. The labels take no touches, so the timeline underneath still does.
        let times = UIStackView(arrangedSubviews: [elapsed, UIView(), total])
        times.isUserInteractionEnabled = false
        times.translatesAutoresizingMaskIntoConstraints = false
        let band = UIView()
        timeline.translatesAutoresizingMaskIntoConstraints = false
        band.addSubview(timeline)
        band.addSubview(times)
        NSLayoutConstraint.activate([
            timeline.leadingAnchor.constraint(equalTo: band.leadingAnchor),
            timeline.trailingAnchor.constraint(equalTo: band.trailingAnchor),
            timeline.topAnchor.constraint(equalTo: band.topAnchor),
            timeline.bottomAnchor.constraint(equalTo: band.bottomAnchor),
            times.leadingAnchor.constraint(equalTo: band.leadingAnchor, constant: RegionSlots.rowPaddingH),
            times.trailingAnchor.constraint(equalTo: band.trailingAnchor, constant: -RegionSlots.rowPaddingH),
            times.bottomAnchor.constraint(equalTo: band.bottomAnchor, constant: -(RegionSlots.timelineLineHeight + 6)),
            band.heightAnchor.constraint(equalToConstant: RegionSlots.timelineTouchHeight),
        ])

        let row = UIStackView(arrangedSubviews: [previous, episodePill, nextButton, speedPill])
        row.spacing = RegionSlots.rowGap
        row.alignment = .center
        row.isLayoutMarginsRelativeArrangement = true
        row.layoutMargins = UIEdgeInsets(top: 0, left: RegionSlots.rowPaddingH, bottom: 0, right: RegionSlots.rowPaddingH)
        episodePill.setContentHuggingPriority(.defaultLow, for: .horizontal)
        speedPill.setContentHuggingPriority(.required, for: .horizontal)

        bottom.axis = .vertical
        bottom.addArrangedSubview(band)
        bottom.setCustomSpacing(RegionSlots.rowTopGap, after: band)
        bottom.addArrangedSubview(row)
        bottom.translatesAutoresizingMaskIntoConstraints = false
        addSubview(bottom)

        NSLayoutConstraint.activate([
            bottom.leadingAnchor.constraint(equalTo: leadingAnchor),
            bottom.trailingAnchor.constraint(equalTo: trailingAnchor),
            bottom.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor),
            shade.leadingAnchor.constraint(equalTo: leadingAnchor),
            shade.trailingAnchor.constraint(equalTo: trailingAnchor),
            shade.bottomAnchor.constraint(equalTo: bottomAnchor),
            shade.topAnchor.constraint(equalTo: bottom.topAnchor),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    private var shadeView: UIView?

    override func layoutSubviews() {
        super.layoutSubviews()
        if let shade = shadeView { scrim.frame = shade.bounds }
    }

    /// Renders `ui`. Safe to call as often as the SDK reports changes.
    func bind(_ ui: ControlsUi) {
        timeline.bind(ui)
        elapsed.text = formatTime(ui.positionMs)
        total.text = formatTime(ui.durationMs)
        episodePill.setPillTitle(ui.episodeCount > 0 ? "EP \(ui.episodeNumber) / \(ui.episodeCount)" : "Episodes")
        episodePill.isEnabled = ui.episodeCount > 0
        speedPill.setPillTitle(speedLabel(ui.speed))
        previous.setEnabledLook(ui.hasPrevious)
        nextButton.setEnabledLook(ui.hasNext)
    }
}

/// Creates the controls region view and connects it to the SDK. Each action is one controller call:
/// `seekTo`, `selectEpisode`, `setPlaybackSpeed`; the episode pill opens the episode list.
///
/// The SDK calls this each time the playback episode changes, with a controller that is valid while
/// the returned view is on screen.
func createControlsRegionView(controller: ContentPlayerController) -> UIView {
    let feed = PlayerFeed.of(controller)
    let view = UIKitControlsRegionView(actions: ControlsActions(
        onSeek: { fraction in controller.seekTo(positionMs: Int64(fraction * Double(feed.progress.durationMs))) },
        onPrevious: { controller.selectEpisode(episodeNumber: Int32(feed.controlsUi().episodeNumber - 1)) },
        onNext: { controller.selectEpisode(episodeNumber: Int32(feed.controlsUi().episodeNumber + 1)) },
        onEpisodes: { EpisodeListPresenter.showUIKit(controller: controller, feed: feed) },
        onSpeed: { controller.setPlaybackSpeed(speed: nextSpeed(feed.state.playbackSpeed)) }
    ))
    observeWhileAttached(view, feed: feed) { view in
        // A locked episode shows the SDK's unlock or unavailable layer; seeking and switching would be ignored.
        view.isHidden = feed.state.episodeLocked
        view.bind(feed.controlsUi())
    }
    return view
}

/// Timeline with the SDK's geometry: a 44 pt touch band whose line sits at its bottom edge, 2 pt
/// thick and 6 pt while dragging. While the player waits for data a light runs along the line. A
/// drag only counts once it is mostly horizontal.
private final class TimelineView: UIView, UIGestureRecognizerDelegate {
    private let onSeek: (Double) -> Void
    private let track = CALayer()
    private let played = CAGradientLayer()
    private let light = CAGradientLayer()
    private let thumb = CALayer()
    private var ui: ControlsUi?
    private var dragFraction: Double?

    init(onSeek: @escaping (Double) -> Void) {
        self.onSeek = onSeek
        super.init(frame: .zero)
        backgroundColor = .clear

        track.backgroundColor = UIColor(argb: RegionColors.track).cgColor
        played.colors = [UIColor(argb: RegionColors.accent).cgColor, UIColor(argb: RegionColors.accentEnd).cgColor]
        played.startPoint = CGPoint(x: 0, y: 0.5)
        played.endPoint = CGPoint(x: 1, y: 0.5)
        light.colors = [UIColor.clear.cgColor, UIColor.white.withAlphaComponent(0.9).cgColor, UIColor.clear.cgColor]
        light.startPoint = CGPoint(x: 0, y: 0.5)
        light.endPoint = CGPoint(x: 1, y: 0.5)
        thumb.backgroundColor = UIColor.white.cgColor
        thumb.cornerRadius = 7
        [track, played, light, thumb].forEach { layer.addSublayer($0) }

        let pan = UIPanGestureRecognizer(target: self, action: #selector(handlePan(_:)))
        pan.delegate = self
        addGestureRecognizer(pan)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    func bind(_ ui: ControlsUi) {
        self.ui = ui
        setNeedsLayout()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let width = bounds.width
        let dragging = dragFraction != nil
        let lineHeight = dragging ? RegionSlots.timelineLineHeightDragging : RegionSlots.timelineLineHeight
        let top = bounds.height - lineHeight
        let shown = CGFloat(dragFraction ?? ui?.fraction ?? 0)

        CATransaction.begin()
        CATransaction.setDisableActions(true)
        track.frame = CGRect(x: 0, y: top, width: width, height: lineHeight)
        played.frame = CGRect(x: 0, y: top, width: width * shown, height: lineHeight)
        thumb.isHidden = !dragging
        thumb.frame = CGRect(x: width * shown - 7, y: top + lineHeight / 2 - 7, width: 14, height: 14)
        light.frame = CGRect(x: 0, y: top, width: width * 0.3, height: lineHeight)
        CATransaction.commit()
        updateLoadingLight(width: width)
    }

    /// The light runs while the player waits for data; its animation stops when the data arrives.
    private func updateLoadingLight(width: CGFloat) {
        let loading = ui?.loading == true && width > 0
        light.isHidden = !loading
        guard loading else {
            light.removeAnimation(forKey: "run")
            return
        }
        guard light.animation(forKey: "run") == nil else { return }
        let run = CABasicAnimation(keyPath: "position.x")
        run.fromValue = -width * 0.15
        run.toValue = width * 1.15
        run.duration = 1.1
        run.repeatCount = .infinity
        light.add(run, forKey: "run")
    }

    // MARK: - Touch: horizontal drags only, and only once the duration is known

    override func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        guard ui?.seekable == true, let pan = gestureRecognizer as? UIPanGestureRecognizer else { return false }
        let velocity = pan.velocity(in: self)
        return abs(velocity.x) > abs(velocity.y)
    }

    @objc private func handlePan(_ pan: UIPanGestureRecognizer) {
        let fraction = Double(min(max(pan.location(in: self).x / max(bounds.width, 1), 0), 1))
        switch pan.state {
        case .began, .changed:
            dragFraction = fraction
            setNeedsLayout()
        case .ended:
            dragFraction = nil
            onSeek(fraction)
            setNeedsLayout()
        default:
            dragFraction = nil
            setNeedsLayout()
        }
    }
}

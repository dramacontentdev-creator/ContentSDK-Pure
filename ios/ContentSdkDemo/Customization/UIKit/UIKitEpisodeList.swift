import ContentSdk
import UIKit

/// UIKit version of the custom EPISODE LIST, opened from the episode pill of the controls region.
/// It looks and behaves like the SwiftUI version in SwiftUI/SwiftUIEpisodeList.swift.
///
/// It is shown in its own presented sheet, not drawn inside the controls region: that region hides
/// together with the SDK controls, and the list must stay while you choose.
extension EpisodeListPresenter {
    /// Presents the list on top of the player page. The list follows the player through `feed`.
    static func showUIKit(controller: ContentPlayerController, feed: PlayerFeed) {
        present(UIKitEpisodeListController(controller: controller, feed: feed), backgroundColor: UIColor(argb: RegionColors.panel))
    }
}

private final class UIKitEpisodeListController: UIViewController, UICollectionViewDataSource, UICollectionViewDelegate {
    private let controller: ContentPlayerController
    private let feed: PlayerFeed
    private var ui: EpisodeListUi
    private let titleLabel = makeLabel(size: 18, weight: .bold)
    private let collection = UICollectionView(frame: .zero, collectionViewLayout: UIKitEpisodeListController.makeLayout())
    private var stopObserving: (() -> Void)?

    init(controller: ContentPlayerController, feed: PlayerFeed) {
        self.controller = controller
        self.feed = feed
        ui = feed.episodeListUi()
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    override func viewDidLoad() {
        super.viewDidLoad()
        titleLabel.text = ui.dramaName

        let close = makeCircleButton(symbol: "xmark", size: 32, fill: RegionColors.pill, label: "Close")
        close.addAction(UIAction { [weak self] _ in self?.dismiss(animated: true) }, for: .touchUpInside)
        let header = UIStackView(arrangedSubviews: [titleLabel, UIView(), close])
        header.alignment = .center
        header.translatesAutoresizingMaskIntoConstraints = false

        collection.backgroundColor = .clear
        collection.dataSource = self
        collection.delegate = self
        collection.register(EpisodeCell.self, forCellWithReuseIdentifier: EpisodeCell.reuseId)
        collection.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(header)
        view.addSubview(collection)
        NSLayoutConstraint.activate([
            header.topAnchor.constraint(equalTo: view.topAnchor, constant: 20),
            header.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            header.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            collection.topAnchor.constraint(equalTo: header.bottomAnchor, constant: 12),
            collection.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 15),
            collection.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -15),
            collection.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }

    private static func makeLayout() -> UICollectionViewLayout {
        let item = NSCollectionLayoutItem(layoutSize: .init(
            widthDimension: .fractionalWidth(1.0 / CGFloat(RegionSlots.episodeColumns)), heightDimension: .absolute(54)
        ))
        item.contentInsets = NSDirectionalEdgeInsets(top: 5, leading: 5, bottom: 5, trailing: 5)
        let group = NSCollectionLayoutGroup.horizontal(
            layoutSize: .init(widthDimension: .fractionalWidth(1), heightDimension: .absolute(54)),
            repeatingSubitem: item,
            count: RegionSlots.episodeColumns
        )
        return UICollectionViewCompositionalLayout(section: NSCollectionLayoutSection(group: group))
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        // Keeps the controls behind the sheet shown, so they are still there when it closes.
        controller.setControlsHeld(held: true)
        // A swipe-to-dismiss that is cancelled calls this again without viewDidDisappear.
        stopObserving?()
        stopObserving = feed.observe { [weak self] in
            guard let self else { return }
            // The feed also calls back on every progress tick; reload only when the list itself changed.
            let next = feed.episodeListUi()
            guard next != ui else { return }
            ui = next
            titleLabel.text = ui.dramaName
            collection.reloadData()
        }
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        controller.setControlsHeld(held: false)
        stopObserving?()
        stopObserving = nil
    }

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int { ui.episodes.count }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard let cell = collectionView.dequeueReusableCell(withReuseIdentifier: EpisodeCell.reuseId, for: indexPath) as? EpisodeCell else {
            return UICollectionViewCell()
        }
        let episode = ui.episodes[indexPath.item]
        cell.bind(episode, current: episode.number == ui.currentNumber)
        return cell
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        // The SDK stops at the first locked episode when you select past it.
        controller.selectEpisode(episodeNumber: Int32(ui.episodes[indexPath.item].number))
        dismiss(animated: true)
    }
}

private final class EpisodeCell: UICollectionViewCell {
    static let reuseId = "EpisodeCell"
    private let number = makeLabel(size: 15, weight: .semibold)
    private let lock = UIImageView(image: UIImage(systemName: "lock.fill"))
    private let accent = CAGradientLayer()

    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.layer.cornerRadius = 10
        contentView.layer.masksToBounds = true
        accent.colors = [UIColor(argb: RegionColors.accent).cgColor, UIColor(argb: RegionColors.accentEnd).cgColor]
        accent.startPoint = CGPoint(x: 0, y: 0.5)
        accent.endPoint = CGPoint(x: 1, y: 0.5)
        contentView.layer.insertSublayer(accent, at: 0)
        number.textAlignment = .center
        number.translatesAutoresizingMaskIntoConstraints = false
        lock.tintColor = UIColor.white.withAlphaComponent(0.7)
        lock.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(number)
        contentView.addSubview(lock)
        NSLayoutConstraint.activate([
            number.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            number.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            lock.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 4),
            lock.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -4),
            lock.widthAnchor.constraint(equalToConstant: 10),
            lock.heightAnchor.constraint(equalToConstant: 10),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    override func layoutSubviews() {
        super.layoutSubviews()
        accent.frame = contentView.bounds
    }

    func bind(_ episode: EpisodeCellUi, current: Bool) {
        number.text = "\(episode.number)"
        number.alpha = episode.locked ? 0.5 : 1
        lock.isHidden = !episode.locked
        // A standalone layer fades its changes in by default; a reused cell must switch at once.
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        accent.isHidden = !current
        CATransaction.commit()
        contentView.backgroundColor = current ? .clear : UIColor(argb: RegionColors.cell)
    }
}

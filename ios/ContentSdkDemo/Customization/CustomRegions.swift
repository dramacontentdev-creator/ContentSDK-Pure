import ContentSdk
import SwiftUI
import UIKit

/// The player page looks the demo can switch between. Demo UI only.
enum PlayerPageStyle: CaseIterable {
    case sdkDefault
    case swiftUI
    case uiKit

    var label: String {
        switch self {
        case .sdkDefault: return "SDK default"
        case .swiftUI: return "Custom · SwiftUI"
        case .uiKit: return "Custom · UIKit"
        }
    }

    /// The label in the player lab's segmented row, where space is short.
    var shortLabel: String {
        switch self {
        case .sdkDefault: return "SDK"
        case .swiftUI: return "SwiftUI"
        case .uiKit: return "UIKit"
        }
    }

    var detail: String {
        switch self {
        case .sdkDefault: return "The SDK's own back button, intro, timeline, episode / speed buttons, and unlock screen."
        case .swiftUI: return "The same slots drawn by your SwiftUI views (Customization/SwiftUI)."
        case .uiKit: return "The same slots drawn by your UIKit views (Customization/UIKit)."
        }
    }
}

/*
 Content SDK - optional: replace parts of the SDK player page with your own UI

 Start here. This file is the whole SDK-facing part of the customization; everything else in this
 folder is the UI it installs.

 ## The three regions

 | Region          | SDK version draws                                  | Set with                        |
 |-----------------|----------------------------------------------------|---------------------------------|
 | video region    | cover, center play/pause, playback error           | `IosContentSdk.shared.setVideoRegion`    |
 | info region     | back button (top) and episode intro (bottom left)  | `IosContentSdk.shared.setInfoRegion`     |
 | controls region | timeline and the episode / speed buttons (bottom)  | `IosContentSdk.shared.setControlsRegion` |

 For each region you choose one of:
 - keep the SDK version:   `DramaPlayerRegion(useDefault: true, view: nil)`
 - hide it:                `DramaPlayerRegion(useDefault: false, view: nil)`
 - replace it:             `DramaPlayerRegion(useDefault: true, view: yourFactory)`, where
                           `yourFactory.createView(controller:)` returns a `UIView`

 The demo replaces the info and controls regions and keeps the video region, so the cover, the center
 play button, and the playback error message stay.

 ## The unlock screen (commercial edition)

 A locked episode shows an unlock screen that is not one of the three regions and cannot be hidden:
 the SDK's, or yours with `setUnlockView`. The demo sets it with the style, in
 EditionCustomization.swift; the commercial demo's Unlock folder explains how it unlocks.

 ## What your view gets, and what it must do

 - **Same slots.** Your view is drawn over the whole page. Put each part where the SDK version sits
   (RegionSlots) and it replaces it in place. The SDK back button is part of the info region, so your
   info view must offer its own. It closes the page with `PlayerPageCloser`.
 - **Shows and hides with the SDK controls.** A tap on the page toggles the controls and they hide
   while playing. Your info and controls view follows that on its own and takes no touches while
   hidden. It stays created, so your state and listeners survive. To show and hide a region
   yourself, pass `autoHide: false` for it and call `controller.setRegionVisible`; the lab's
   settings panel (Lab/) switches both at run time.
 - **Also while the page loads.** The view is drawn before the SDK has bound an episode. The
   controller's `state.resource` is nil until then; draw nothing that needs an episode before it is set.
 - **Subtitles stay clear.** The SDK keeps subtitles 124 points above the page's bottom edge (112
   points of clearance plus a 12 points gap). If your view is taller, say so with `bottomInsetDp`, as
   the info region does below, and subtitles move up above it.
 - **Touches.** The SDK gives your view a touch only where it draws something. Elsewhere taps
   (show / hide, double-tap play / pause) and vertical swipes (episodes) reach the page. Draw
   backgrounds only behind your controls: a tap on a drawn background goes to your view. A vertical
   swipe that starts on a button still switches episodes.
 - **One listener each.** A controller keeps one `setListener`, one `setProgressListener`, and one
   `setContentListener`; setting one replaces the previous. `PlayerFeed` registers them once for
   every region.
 - **Loading.** The SDK's loading light is part of its controls region. Draw your own while
   `playbackState` is `.preparing` or `.buffering`, as the timeline here does.
 - **When to set it.** Once for the whole process, for example at launch. Pages that are already
   open apply a change at once. The demo sets it when you pick a style on its home screen.

 ## Where things are

 - `PlayerFeed.swift`      listens to the controller once; turns SDK state into the data below
 - `RegionModels.swift`    what each region draws, as plain data (no SDK types)
 - `RegionSlots.swift`     where the SDK's parts sit, so yours replace them in place
 - `SwiftUI/` and `UIKit/` the same three parts written for SwiftUI and for UIKit
 */
final class CustomRegions: ObservableObject {
    static let shared = CustomRegions()

    /// The style in use. Kept here, not in a view, because the SDK keeps regions for the whole process.
    @Published private(set) var style: PlayerPageStyle = .sdkDefault

    /// Whether the custom info and controls views show and hide with the SDK controls (true, the SDK
    /// default) or stay until your app calls `controller.setRegionVisible` (false). Ignored by the SDK
    /// default style, which has no custom views.
    @Published private(set) var autoHide = true

    /// Installs `newStyle` for every SDK player page, including pages that are open.
    func apply(_ newStyle: PlayerPageStyle, autoHide newAutoHide: Bool? = nil) {
        let hides = newAutoHide ?? autoHide
        style = newStyle
        autoHide = hides
        // The edition's own part of the style: the unlock screen in the commercial edition.
        EditionCustomization.apply(newStyle)
        let sdk = IosContentSdk.shared
        switch newStyle {
        case .sdkDefault:
            sdk.setInfoRegion(region: DramaPlayerRegion(useDefault: true, view: nil))
            sdk.setControlsRegion(region: DramaPlayerRegion(useDefault: true, view: nil))

        case .swiftUI:
            sdk.setInfoRegion(region: DramaPlayerRegion(
                useDefault: true,
                view: RegionViewFactory { SwiftUIHost.view(SwiftUIInfoRegion(controller: $0)) },
                // The intro is taller than the SDK's own clearance for subtitles: say how tall.
                bottomInsetDp: RegionSlots.introBottomInset,
                autoHide: hides
            ))
            sdk.setControlsRegion(region: DramaPlayerRegion(
                useDefault: true,
                view: RegionViewFactory { SwiftUIHost.view(SwiftUIControlsRegion(controller: $0)) },
                bottomInsetDp: 0,
                autoHide: hides
            ))

        case .uiKit:
            sdk.setInfoRegion(region: DramaPlayerRegion(
                useDefault: true,
                view: RegionViewFactory { createInfoRegionView(controller: $0) },
                bottomInsetDp: RegionSlots.introBottomInset,
                autoHide: hides
            ))
            sdk.setControlsRegion(region: DramaPlayerRegion(
                useDefault: true,
                view: RegionViewFactory { createControlsRegionView(controller: $0) },
                bottomInsetDp: 0,
                autoHide: hides
            ))
        }
    }
}

/// Turns a closure into the factory the SDK asks for. The SDK calls it each time the playback
/// episode changes, because the view lives inside that episode's page.
final class RegionViewFactory: NSObject, DramaPlayerRegionView {
    private let make: (ContentPlayerController) -> UIView

    init(make: @escaping (ContentPlayerController) -> UIView) {
        self.make = make
    }

    func createView(controller: ContentPlayerController) -> UIView {
        make(controller)
    }
}

/// How a custom region closes the player page. The back button of the SDK version is part of the
/// info region, so a custom info region needs its own way out. The screen that shows the player
/// sets `handler`; on iOS your app owns navigation, so only it knows how to leave.
enum PlayerPageCloser {
    static var handler: (() -> Void)?

    static func close() {
        handler?()
    }
}

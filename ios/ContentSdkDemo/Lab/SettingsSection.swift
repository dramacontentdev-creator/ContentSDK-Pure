import ContentSdk
import SwiftUI

/// The settings page of the control panel: every switch is one SDK call, named in its detail line.
///
/// - Playback options are SDK-wide and apply from the next episode (`LabSettings`).
/// - The player page style and `autoHide` re-install the page regions (`CustomRegions`); pages that
///   are open change at once.
/// - Region visibility is a call on the page's `controller`, so it needs one and starts over on each
///   new page.
struct SettingsSection: View {
    let controller: ContentPlayerController?
    @ObservedObject private var settings = LabSettings.shared
    @ObservedObject private var regions = CustomRegions.shared

    var body: some View {
        group("Playback options · from the next episode") {
            SwitchRow(
                title: "Auto play",
                detail: "setAutoPlay: start as soon as an episode is ready, on entry and after each switch.",
                isOn: settings.autoPlay,
                onChange: settings.changeAutoPlay
            )
            SwitchRow(
                title: "Auto advance",
                detail: "setAutoAdvance: move to the next episode when one ends.",
                isOn: settings.autoAdvance,
                onChange: settings.changeAutoAdvance
            )
            SwitchRow(
                title: "Resume from history",
                detail: "setResumeFromHistory: open a drama where it was left when no episode is named.",
                isOn: settings.resumeFromHistory,
                onChange: settings.changeResumeFromHistory
            )
        }

        group("Player page UI · applies to the open page at once") {
            Segmented(
                title: "Regions",
                options: PlayerPageStyle.allCases.map { (value: $0, label: $0.shortLabel) },
                isSelected: { $0 == regions.style },
                onSelect: { regions.apply($0) }
            )
            Note(regions.style.detail)
        }

        CustomRegionSwitches(controller: controller)

        group("Lab") {
            SwitchRow(
                title: "Log overlay",
                detail: "Show SDK calls and playback events in the top-left corner.",
                isOn: settings.logOverlay,
                onChange: { settings.logOverlay = $0 }
            )
        }
    }
}

/// `DramaPlayerRegion.autoHide` and `controller.setRegionVisible` for the custom info and controls regions.
private struct CustomRegionSwitches: View {
    let controller: ContentPlayerController?
    @ObservedObject private var regions = CustomRegions.shared
    @ObservedObject private var actions = LabActions.shared

    var body: some View {
        let custom = regions.style != .sdkDefault
        let manual = custom && !regions.autoHide
        group("Custom regions · SwiftUI or UIKit style only") {
            SwitchRow(
                title: "Hide with SDK controls",
                detail: "DramaPlayerRegion(autoHide: true): your view shows and hides with a tap, like the SDK's. Off: it stays until you hide it below.",
                isOn: regions.autoHide,
                enabled: custom,
                onChange: { regions.apply(regions.style, autoHide: $0) }
            )
            SwitchRow(
                title: "Info region shown",
                detail: "setRegionVisible(.info, …): back button and episode intro.",
                isOn: actions.isRegionShown(.info),
                enabled: manual && controller != nil,
                onChange: { shown in
                    if let controller { actions.showRegion(controller, .info, visible: shown) }
                }
            )
            SwitchRow(
                title: "Controls region shown",
                detail: "setRegionVisible(.controls, …): timeline and the episode / speed row.",
                isOn: actions.isRegionShown(.controls),
                enabled: manual && controller != nil,
                onChange: { shown in
                    if let controller { actions.showRegion(controller, .controls, visible: shown) }
                }
            )
            Note("While this panel is open the page controls are held shown: setControlsHeld(true) on open, false on close.")
        }
    }
}

/// A titled group of settings.
private func group<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
    VStack(alignment: .leading, spacing: 12) {
        SectionLabel(title)
        content()
    }
}

import SwiftUI

/// The demo's home screen: SDK status, the integration checklist, the player page style, and the
/// two entry points (the SDK drama home and the player lab).
///
/// Demo UI only. Nothing in this file is required to integrate the SDK; the SDK calls are made by
/// SdkIntegration.swift and DemoEdition.swift.
struct DemoHomeView: View {
    @StateObject private var demo = ContentSdkDemo()
    @ObservedObject private var regions = CustomRegions.shared

    var body: some View {
        NavigationStack(path: $demo.path) {
            VStack(spacing: 0) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        header.padding(.top, 16)
                        statusCard
                        stepsCard
                        pageStyleCard
                        labCard
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 24)
                }
                openButton
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
            }
            .background(DemoColors.background.ignoresSafeArea())
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: SdkRoute.self) { route in
                SdkDestination(route: route, demo: demo)
            }
        }
        .tint(DemoColors.accent)
        .preferredColorScheme(.dark)
    }

    // MARK: - Header and status

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(LinearGradient(colors: [DemoColors.accent, DemoColors.accentEnd],
                                     startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(width: 52, height: 52)
                .overlay(Image(systemName: "play.fill").font(.system(size: 22)).foregroundStyle(.white))
            Text("CONTENT SDK · \(DemoEdition.name.uppercased())")
                .font(.system(size: 12, weight: .semibold))
                .kerning(2)
                .foregroundStyle(DemoColors.accent)
                .padding(.top, 12)
            Text("Integration Demo")
                .font(.system(size: 30, weight: .bold))
                .foregroundStyle(DemoColors.textPrimary)
            Text("The minimum code needed to bring short-drama content into your iOS app. \(DemoEdition.summary)")
                .font(.system(size: 15))
                .foregroundStyle(DemoColors.textSecondary)
                .lineSpacing(4)
        }
    }

    private var statusCard: some View {
        let (label, detail, color): (String, String, Color) = {
            switch demo.status {
            case .notStarted: return ("Initialized", "Tap the button below to start the SDK.", DemoColors.textSecondary)
            case .starting: return ("Starting…", DemoEdition.startingDetail, DemoColors.warning)
            case .ready: return ("Ready", "The SDK is running. SDK pages can be opened.", DemoColors.success)
            case let .failed(code, message): return ("Start failed", "\(code) · \(message)", DemoColors.error)
            }
        }()

        return DemoCard {
            sectionTitle("SDK STATUS")
            HStack(spacing: 10) {
                Circle().fill(color).frame(width: 10, height: 10)
                Text(label)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(DemoColors.textPrimary)
            }
            .padding(.top, 10)
            .animation(.easeInOut, value: demo.status)
            Text(detail)
                .font(.system(size: 13))
                .foregroundStyle(DemoColors.textSecondary)
                .padding(.top, 6)
        }
    }

    private var stepsCard: some View {
        DemoCard {
            sectionTitle("INTEGRATION STEPS")
            ForEach(DemoEdition.integrationSteps, id: \.label) { step in
                HStack(spacing: 14) {
                    Text(step.label)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(DemoColors.accent)
                        .frame(width: 28, height: 28)
                        .background(DemoColors.accent.opacity(0.14), in: Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(step.title)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(DemoColors.textPrimary)
                        Text(step.file)
                            .font(.system(size: 12, design: .monospaced))
                            .foregroundStyle(DemoColors.textSecondary)
                    }
                }
                .padding(.vertical, 10)
            }
        }
    }

    // MARK: - Optional: player page style and player lab

    /// The player page styles as tiles; the chosen one has an accent border.
    private var pageStyleCard: some View {
        DemoCard {
            sectionTitle("PLAYER PAGE (OPTIONAL)")
            Text("Replace the SDK's intro and controls with your own UI. Pick a style, then open the player.")
                .font(.system(size: 13))
                .foregroundStyle(DemoColors.textSecondary)
                .padding(.top, 6)
            VStack(spacing: 10) {
                ForEach(PlayerPageStyle.allCases, id: \.label) { style in
                    styleTile(style, selected: style == regions.style)
                }
            }
            .padding(.top, 12)
        }
    }

    private func styleTile(_ style: PlayerPageStyle, selected: Bool) -> some View {
        Button { regions.apply(style) } label: {
            HStack(spacing: 14) {
                Circle()
                    .stroke(selected ? DemoColors.accent : DemoColors.outline, lineWidth: 2)
                    .frame(width: 20, height: 20)
                    .overlay(Circle().fill(selected ? DemoColors.accent : .clear).padding(4))
                VStack(alignment: .leading, spacing: 2) {
                    Text(style.label)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(DemoColors.textPrimary)
                    Text(style.detail)
                        .font(.system(size: 12))
                        .foregroundStyle(DemoColors.textSecondary)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 0)
            }
            .padding(14)
            .background(selected ? DemoColors.accent.opacity(0.08) : .clear, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(selected ? DemoColors.accent : DemoColors.outline, lineWidth: selected ? 1.5 : 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var labCard: some View {
        DemoCard {
            sectionTitle("PLAYER LAB (OPTIONAL)")
            Text("The SDK player page with the demo's own controls over it: play, seek, speed, quality, episodes, playback options, and the page UI style, all through the player controller. The code is in the Lab folder.")
                .font(.system(size: 13))
                .foregroundStyle(DemoColors.textSecondary)
                .padding(.top, 6)
            Button(action: demo.onOpenLabTapped) {
                Text("Open player lab")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(DemoColors.accent)
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(DemoColors.accent, lineWidth: 1))
            }
            .disabled(demo.status == .starting)
            .padding(.top, 12)
        }
    }

    private var openButton: some View {
        Button(action: demo.onOpenDramaHomeTapped) {
            ZStack {
                if demo.status == .starting {
                    ProgressView().tint(.white)
                } else {
                    Text(isFailed ? "Retry" : "Open Drama Home")
                        .font(.system(size: 16, weight: .semibold))
                }
            }
            .frame(maxWidth: .infinity, minHeight: 56)
            .foregroundStyle(.white)
            .background(DemoColors.accentGradient.opacity(demo.status == .starting ? 0.5 : 1),
                        in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .disabled(demo.status == .starting)
    }

    private var isFailed: Bool {
        if case .failed = demo.status { return true }
        return false
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11))
            .kerning(1.5)
            .foregroundStyle(DemoColors.textSecondary)
    }
}

#Preview {
    DemoHomeView()
}

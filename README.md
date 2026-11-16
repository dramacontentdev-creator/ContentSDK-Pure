# Content SDK Demo

English | [中文](README.zh-CN.md)

Shows how to integrate Content SDK on Android and iOS: drama home, player, playback control, player page customization, and revenue reports. Integration steps are marked in the code so you can copy them into your app.

This repository does not include the SDK. Get the SDK package from your SDK provider and put the files where the quick start says, then run.

## Repository layout

```text
android/                         Android project (open this folder in Android Studio)
  settings.gradle.kts            Repositories
  app/
    build.gradle.kts             AAR and dependency script
    libs/                        SDK AAR goes here (not committed)
    content-sdk-dependencies.gradle   Dependency script from the SDK package (not committed)
    src/main/kotlin/com/lc/content/sdk/demo/
      DemoApplication.kt         Initialize
      DemoEdition.kt             App ID / App Secret, start
      MainActivity.kt            Open SDK pages
      catalog/                   API reference with examples to copy
      customization/             Player page customization (Compose and View)
      lab/                       Player lab
      ui/                        Demo UI only, not needed in your app
ios/                             iOS project
  Podfile                        SDK Pod
  ContentSdkPure/                    iOS SDK package goes here (not committed)
  ContentSdkDemo.xcworkspace     Created by pod install; open the project with it
  ContentSdkDemo/
    ContentSdkDemoApp.swift      Initialize
    DemoEdition.swift            App ID / App Secret, start
    SdkIntegration.swift         Open SDK pages
    Catalog/                     API reference with examples to copy
    Customization/               Player page customization (SwiftUI and UIKit)
    Lab/                         Player lab
    UI/                          Demo UI only, not needed in your app
```

## Quick start

### Android

1. From `ContentSdk-pure-Android-YOUR_SDK_VERSION-release`, copy `ContentSdk-pure-YOUR_SDK_VERSION.aar` into `android/app/libs/`.
2. Copy `content-sdk-dependencies.gradle` from the same package into `android/app/`.
3. Open `android/` in Android Studio and run `app`.

### iOS

1. Copy everything in `ContentSdk-pure-iOS-YOUR_SDK_VERSION-release` into `ios/ContentSdkPure/`.
2. Run `pod install` in `ios/`.
3. Open `ios/ContentSdkDemo.xcworkspace` and run.

To run on a device, create `ios/Signing.local.xcconfig` with `DEVELOPMENT_TEAM = YOUR_TEAM_ID` (see `ios/Signing.xcconfig`). The file is git-ignored; setting the team in Xcode's Signing tab would write it into the project file instead.

> The AAR, the dependency script, and the iOS SDK package must come from the same SDK version.

## Integrate into your app

Each step is marked `Content SDK - integration Step N` in the code. Copy them in order. Android and iOS are numbered separately:

| Step | Android | iOS |
|---|---|---|
| 1 | `settings.gradle.kts`: `google()`, `mavenCentral()`, and the AAR folder | `Podfile`: `pod 'ContentSdkPure'` |
| 2 | `app/build.gradle.kts`: AAR and dependency script | `ContentSdkDemoApp.swift`: initialize |
| 3 | `DemoApplication.kt`: initialize | `DemoEdition.swift`: start |
| 4 | `DemoEdition.kt`: start | `SdkIntegration.swift`: open SDK pages |
| 5 | `MainActivity.kt`: open SDK pages | |

In iOS code, write `import ContentSdk`.

Then fill in your own values:

- App ID / App Secret: get them at [https://open.lucky-chick.com](https://open.lucky-chick.com) and put them in `SdkConfig` in `DemoEdition.kt` / `DemoEdition.swift`.

## Features

From the demo home screen you can open:

- **SDK pages**: the drama home and the player, with the full UI from the SDK.
- **Player page styles**: pick SDK default, Compose / SwiftUI, or View / UIKit on the home screen, then open the player to see your own UI replace the info region, controls region, and episode list (`customization/`).
- **Player lab**: the SDK player page fills the screen and the demo draws its own control panel over it: play / pause, seek, speed, quality, episodes, playback options, region visibility, and a log of every call and playback event (`lab/` / `Lab/`; the SDK calls are in `LabActions.kt` / `LabActions.swift`).
- **Your own monetization**: the SDK shows no ads. `EditionApiCatalog` has an example: when an episode your own rules charge for starts, pause, show your paywall or ad, report the revenue, then continue. The demo does not switch it on.

## API reference

The § numbers are sections of `SdkApiCatalog` (`catalog/SdkApiCatalog.kt` on Android, `Catalog/SdkApiCatalog.swift` on iOS). Both platforms use the same numbers.

| API | Where | Use |
|---|---|---|
| `initialize` / `start` | Integration steps | Initialize and start the SDK. Show SDK pages only after `start` succeeds |
| `setLoggingEnabled` | §1 | SDK diagnostic logs |
| `setStrategy` | §2 | Resource strategy (cache and preload level) |
| `setColorPalette` / `resetColorPalette`, `setSubtitleStyle` / `resetSubtitleStyle` | §3 | Theme colors and subtitle style |
| Android: `openDramaHomePage`, `openDramaPlayerPage`, `createDramaHomeFragment`, `createDramaPlayerFragment`, `DramaHomeScreen`, `DramaPlayerScreen`; iOS: `dramaHome`, `dramaPlayer` | §4 | Show the drama home and player with Activity / Fragment / Compose or UIKit / SwiftUI |
| `login` / `setLanguage` | §5 | User or app language changes |
| `setPlayerEventListener` | §6 | Playback events: first frame, state, progress, errors |
| `getDramaList` | §7 | Fetch the drama list and show it in your own UI |
| `setPlayerControllerListener`; controller `play`, `pause`, `seekTo`, `setPlaybackSpeed`, `setQuality`, `selectEpisode`, `setListener` | §8.1 - 8.2, lab | Control the SDK player page from your own UI |
| `setContentListener` / `setProgressListener` | §8.3, `customization/PlayerFeed` | Drama info, episode list, and progress for your own controls |
| `setControlsHeld` | §8.4, lab | Keep the page controls visible while your own dialog covers the page |
| `setAutoPlay` / `setAutoAdvance` / `setResumeFromHistory` | §9, lab settings | Auto play, auto advance, resume from history |
| `getPlaybackHistory` / `removePlaybackHistory` / `clearPlaybackHistory` | §10 | Continue watching |
| `setVideoRegion` / `setInfoRegion` / `setControlsRegion`, `DramaPlayerRegion` | §11, `customization/` | Replace the video, info, or controls region of the player page |
| `DramaPlayerRegion(autoHide)`, `setRegionVisible`, `state.controlsVisible` | §11, lab settings | Decide yourself when a region shows or hides |
| `createPlayer`; Android `ContentPlayerView` / `createPlayerView`, iOS `playerViewController` | §12 | Put a player in your own UI instead of the SDK player page |
| `reportAdRevenue` / `reportPurchaseRevenue` | §13 | Report revenue from your app's own ads and purchases |
| `start` (coroutine version) | `EditionApiCatalog` P1 (Android) | Start the SDK with coroutines |
| Locked episode: pause, your paywall or ad, report, play | `EditionApiCatalog` P2 (Android) / P1 (iOS) | Your own monetization flow |

## Using the APIs

1. **Get the required steps running first**: copy steps 1 to the last one from "Integrate into your app", then open the drama home after `start` succeeds. That alone covers the basic features.
2. **Look up `SdkApiCatalog` as needed**: each section says when to call the API, what the parameters mean, and has an example to copy. Section numbers are in the table above. Every other API is optional; without a call the SDK uses its defaults.
3. **Monetization is in `EditionApiCatalog`**: the `monetizeEpisodes` example pauses on an episode you charge for, shows your paywall or ad, reports it with `reportPurchaseRevenue` / `reportAdRevenue`, then calls `play()`. A controller has one state listener, so if you already listen (like `PlayerFeed` in `customization/`), put the check there. An episode the content service reports as locked (`state.episodeLocked`) cannot play; `play()` does not start it.
4. **To change the player page UI**: start with `customization/CustomRegions.kt` (iOS `CustomRegions.swift`), which holds the SDK calls and explanations. Then copy `compose/` or `view/` (iOS `SwiftUI/` or `UIKit/`) for your UI toolkit. `PlayerFeed` turns the player state into what your UI draws.
5. **To check playback control**: tap through the matching buttons in the lab, read the calls and events in its log, then find the SDK call in `LabActions`.

Rules for every call:

- Call every API on the main thread; callbacks also run on the main thread.
- Call `initialize` once per process. Use the other APIs after `start` succeeds.
- Each kind of listener keeps one instance. Setting it again replaces the old one; pass `null` / `nil` to stop.

## Common API usage

Expand an item for when to call it, what to watch for, and Android / iOS examples. The full examples are in `SdkApiCatalog`.

<details>
<summary><b>Theme colors: setColorPalette / resetColorPalette</b></summary>

- Call it any time; open SDK pages update at once. Usually set it once before `start`.
- The SDK has `Colors.light` (default) and `Colors.dark`. To follow your app theme, call it again when your app switches light / dark.
- Custom palette: copy a built-in palette and replace only the fields you need. Create colors with `Colors.argb(0xAARRGGBB)` (iOS `Colors.shared.argb(argb:)`).
- Common fields: page background `background.background_1`, card background `background.background_2`, main text `textIcon.default_1`, brand color `textIcon.brand` + `fills.brand_default` + `line.brand` (change all three).

Android:

```kotlin
// Built-in palettes: Colors.light (default) / Colors.dark
AndroidContentSdk.setColorPalette(Colors.dark)

// Custom: start from a built-in palette and replace only what you need.
// The brand color lives in three groups; change them together.
val brand = Colors.argb(0xFFFF6A00)
val base = Colors.light
AndroidContentSdk.setColorPalette(
    base.copy(
        textIcon = base.textIcon.copy(brand = brand),
        fills = base.fills.copy(brand_default = brand),
        line = base.line.copy(brand = brand),
        background = base.background.copy(background_1 = Colors.argb(0xFFF7F7F7)),
    ),
)

// Back to the SDK default (light)
AndroidContentSdk.resetColorPalette()
```

iOS:

```swift
// Built-in palettes: Colors.shared.light (default) / Colors.shared.dark
IosContentSdk.shared.setColorPalette(colors: Colors.shared.dark)

// Custom: doCopy takes every parameter; pass the original value for the ones you keep.
let brand = Colors.shared.argb(argb: 0xFFFF6A00)
let base = Colors.shared.light
let t = base.textIcon, f = base.fills, l = base.line
let colors = base.doCopy(
    mask: base.mask,
    textIcon: t.doCopy(
        brand: brand,
        white_1: t.white_1, white_2: t.white_2, white_3: t.white_3, white_4: t.white_4, white_5: t.white_5,
        default_1: t.default_1, default_2: t.default_2, default_3: t.default_3, default_4: t.default_4
    ),
    background: base.background,
    fills: f.doCopy(brand_default: brand, white: f.white, defalt: f.defalt),
    line: l.doCopy(
        brand: brand,
        line_1: l.line_1, line_2: l.line_2, line_3: l.line_3, line_4: l.line_4, line_5: l.line_5
    ),
    error: base.error,
    player: base.player,
    feed: base.feed,
    placeholder: base.placeholder
)
IosContentSdk.shared.setColorPalette(colors: colors)

// Back to the SDK default (light)
IosContentSdk.shared.resetColorPalette()
```

</details>

<details>
<summary><b>Subtitle style: setSubtitleStyle / resetSubtitleStyle</b></summary>

- Call it any time; subtitles already on screen update too.
- Only the fields you set override the subtitle file or SDK defaults.
- Fields: `fontScale` (0.5 to 2.0, base 18sp), `textColor`, `backgroundColor`, `outlineColor`, `bold`, `bottomMargin` (share of the height from the bottom, 0 to 0.4).

Android:

```kotlin
AndroidContentSdk.setSubtitleStyle(SubtitleStyle(fontScale = 1.2f, textColor = Colors.argb(0xFFFFFF00)))
// Back to the subtitle file styles and SDK defaults
AndroidContentSdk.resetSubtitleStyle()
```

iOS:

```swift
let style = SubtitleStyle(
    fontScale: 1.2,
    textColor: Colors.shared.argb(argb: 0xFFFFFF00),
    backgroundColor: Colors.shared.unspecified, // unspecified = keep the file / SDK default
    outlineColor: Colors.shared.unspecified,
    bold: nil,
    bottomMargin: nil
)
IosContentSdk.shared.setSubtitleStyle(style: style)
// Back to the subtitle file styles and SDK defaults
IosContentSdk.shared.resetSubtitleStyle()
```

</details>

<details>
<summary><b>Resource strategy: setStrategy</b></summary>

- The default is `Adaptive`, which adjusts to the device's performance and free storage; usually you do not call it.
- For a fixed level, call it before `start`: `Standard` (mainstream), `LowPerformance` (entry-level), `HighPerformance` (high-end).

Android:

```kotlin
AndroidContentSdk.setStrategy(ContentSdkStrategy.Standard)
```

iOS:

```swift
IosContentSdk.shared.setStrategy(strategy: ContentSdkStrategy.standard)
```

</details>

<details>
<summary><b>User and language: login / setLanguage</b></summary>

- If `initialize` already got the user ID, you do not need `login` at launch. Call it when the user signs in, switches account, or signs out (empty string); watch history follows the user.
- Call `setLanguage` after your app changes language; later content requests use it. `effectiveLanguage` in the callback is what the SDK uses; unsupported languages fall back to `en-US`.

Android:

```kotlin
// Login, account switch, or logout ("" = anonymous user)
AndroidContentSdk.login(userId) { error ->
    if (error != null) Log.w(TAG, "login failed: ${error.code} ${error.msg}")
}
// App language changed, e.g. "en-US", "zh-CN"
AndroidContentSdk.setLanguage("zh-CN") { effectiveLanguage, error -> }
```

iOS:

```swift
IosContentSdk.shared.login(userId: userId) { error in
    if let error { print("login failed: \(error.code) \(error.msg)") }
}
IosContentSdk.shared.setLanguage(language: "zh-CN") { effectiveLanguage, error in
    print("SDK language = \(effectiveLanguage ?? "-"), error = \(error?.code ?? "none")")
}
```

</details>

<details>
<summary><b>Drama list: getDramaList</b></summary>

- For your own drama UI; call after `start` succeeds. `page` starts at 1; an empty list means no more data.
- Optional filters: `dramaIds` (only these dramas), `recentDays` (released in the last N days).
- Each `Drama` has `id`, `name`, `coverImageUrl`, `episodeSize`, `freeCount`, `description`, `tags`. On tap, open the player with `drama.asPlayerParams()`.

Android:

```kotlin
lifecycleScope.launch {
    try {
        val dramas = AndroidContentSdk.getDramaList(page = 1, pageSize = 20)
        // Filtered: getDramaList(page = 1, pageSize = 20, dramaIds = listOf("YOUR_DRAMA_ID"), recentDays = 7)
        // On tap:
        // AndroidContentSdk.openDramaPlayerPage(context, drama.asPlayerParams())
    } catch (error: SdkException) {
        // Show an error or a retry entry
    }
}
```

iOS:

```swift
IosContentSdk.shared.getDramaList(page: 1, pageSize: 20) { dramas, error in
    if let error { print("getDramaList failed: \(error.code) \(error.msg)") }
    // On tap:
    // let player = IosContentSdk.shared.dramaPlayer(params: drama.asPlayerParams()) { /* close */ }
}
```

</details>

<details>
<summary><b>Playback control: setPlayerControllerListener</b></summary>

- Set it once at app launch. Each time an SDK player page shows, you get its controller; `null` / `nil` when the page leaves.
- After `pause()` playback stays paused: returning to the foreground or switching episodes does not resume until `play()` or the user taps play.
- Offer only the qualities in `state.availableQualityHeights`; with one entry there is nothing to switch.
- The state listener does not report position; use `setProgressListener` (about every 300 ms) for a progress bar.

Android:

```kotlin
// Once, e.g. in Application.onCreate
AndroidContentSdk.setPlayerControllerListener { controller ->
    playerController = controller            // null when the page leaves
    controller?.setListener { state ->
        playButton.isSelected = state.playWhenReady
    }
}

playerController?.pause()
playerController?.play()
playerController?.seekTo(30_000)
playerController?.setPlaybackSpeed(1.5f)
playerController?.selectEpisode(3)
// Quality: only offer what state.availableQualityHeights lists
playerController?.setQuality(PlayerQualitySelection(PlayerQualityMode.Manual, requestedHeight = 720))
```

iOS:

```swift
final class SdkPlayerPages: NSObject, PlayerControllerListener, ContentPlayerListener {
    private(set) var controller: ContentPlayerController?
    func onPlayerControllerChanged(controller: ContentPlayerController?) {
        self.controller = controller         // nil when the page leaves
        controller?.setListener(listener: self)
    }
    func onStateChanged(state: ContentPlayerState) { /* update your UI */ }
}
IosContentSdk.shared.setPlayerControllerListener(listener: SdkPlayerPages())

controller.pause()
controller.play()
controller.seekTo(positionMs: 30_000)
controller.setPlaybackSpeed(speed: 1.5)
controller.selectEpisode(episodeNumber: 3)
controller.setQuality(selection: PlayerQualitySelection(mode: .manual, requestedHeight: 720))
```

</details>

<details>
<summary><b>Playback options: setAutoPlay / setAutoAdvance / setResumeFromHistory</b></summary>

- `setAutoPlay`: start playing once an episode is ready. Default `false`. When off, wait for the user or `play()`.
- `setAutoAdvance`: move to the next episode when one finishes. Default `true`.
- `setResumeFromHistory`: open a drama without an episode at the recorded episode and position. Default `true`.
- Changes apply from each player's next episode and do not interrupt what is playing.

Android:

```kotlin
AndroidContentSdk.setAutoPlay(true)
AndroidContentSdk.setAutoAdvance(true)
AndroidContentSdk.setResumeFromHistory(true)
```

iOS:

```swift
IosContentSdk.shared.setAutoPlay(enabled: true)
IosContentSdk.shared.setAutoAdvance(enabled: true)
IosContentSdk.shared.setResumeFromHistory(enabled: true)
```

</details>

<details>
<summary><b>Playback history: getPlaybackHistory</b></summary>

- Stored on this device per signed-in user, newest first; up to 50 dramas, and a drama not watched for 30 days is dropped. Empty before login.
- Use it for "continue watching"; opening `Drama.PlayerParams(id = record.dramaId)` resumes at the recorded point.

Android:

```kotlin
lifecycleScope.launch {
    val records = AndroidContentSdk.getPlaybackHistory(limit = 20)
    // record.dramaId, dramaName, coverImageUrl, episodeNumber, positionMs, durationMs, updatedAtMs
}
AndroidContentSdk.removePlaybackHistory("YOUR_DRAMA_ID")
AndroidContentSdk.clearPlaybackHistory()
```

iOS:

```swift
IosContentSdk.shared.getPlaybackHistory(limit: 20) { records in
    for record in records { print("\(record.dramaName) #\(record.episodeNumber) at \(record.positionMs) ms") }
}
IosContentSdk.shared.removePlaybackHistory(dramaId: "YOUR_DRAMA_ID")
IosContentSdk.shared.clearPlaybackHistory()
```

</details>

<details>
<summary><b>Playback events: setPlayerEventListener</b></summary>

- One listener only; setting it again replaces the old one. Callbacks run in order on the main thread; exceptions in your listener do not affect playback.
- Every event has `resource` (the current drama and episode, empty before one loads); most also have `progress`.
- Other events: `PlaybackStateChanged`, `PlaybackProgressUpdated`, `PositionDiscontinuity`, `PlaybackSpeedChanged`, `QualityChanged`, `QualityResolved`, `ControlDispatched`.

Android:

```kotlin
AndroidContentSdk.setPlayerEventListener { event ->
    when (event) {
        is PlayerEvent.FirstFrameRendered -> { /* event.startupDurationMs */ }
        is PlayerEvent.PlaybackEnded -> { }
        is PlayerEvent.PlaybackError -> { /* event.error.code, event.recoverable */ }
        else -> Unit
    }
}
AndroidContentSdk.setPlayerEventListener(null) // stop, e.g. on logout
```

iOS:

```swift
final class PlayerEventHandler: NSObject, PlayerEventListener {
    func onPlayerEvent(event: PlayerEvent) {
        if let e = event as? PlayerEvent.FirstFrameRendered { print(e.startupDurationMs) }
        else if event is PlayerEvent.PlaybackEnded { }
        else if let e = event as? PlayerEvent.PlaybackError { print(e.error.code, e.recoverable) }
    }
}
IosContentSdk.shared.setPlayerEventListener(listener: PlayerEventHandler())
IosContentSdk.shared.setPlayerEventListener(listener: nil) // stop
```

</details>

<details>
<summary><b>Player page regions: setVideoRegion / setInfoRegion / setControlsRegion</b></summary>

- The page has three regions: `video` (cover, play button, error), `info` (back button and intro), `controls` (timeline, episodes, settings). Each can keep the SDK version, use your UI, or be hidden.
- Set them once at app launch. They apply to every SDK player page, including open ones. `DramaPlayerRegion()` restores the SDK version.
- Your UI covers the whole page and is created again for each episode. Leave empty areas without click listeners so swipes and taps reach the page.
- By default it shows and hides with the SDK controls; with `autoHide = false` you control it with `controller.setRegionVisible`. If your content is taller than the SDK controls, set `bottomInsetDp` so subtitles move up.
- A runnable version is in `customization/`.

Android:

```kotlin
// Controls region: your own view (gets the page's controller)
AndroidContentSdk.setControlsRegion(
    DramaPlayerRegion(view = DramaPlayerRegionView { context, controller ->
        MyControls(context).apply { bind(controller) }
    }),
)
// Info region: hide the SDK intro and back button
AndroidContentSdk.setInfoRegion(DramaPlayerRegion(useDefault = false))
// Video region: back to the SDK version
AndroidContentSdk.setVideoRegion(DramaPlayerRegion())
// Compose: DramaPlayerRegion(content = { controller -> MyControls(controller) })
```

iOS:

```swift
final class MyControlsRegion: NSObject, DramaPlayerRegionView {
    func createView(controller: ContentPlayerController) -> UIView { MyControlsView(controller: controller) }
}
IosContentSdk.shared.setControlsRegion(region: DramaPlayerRegion(useDefault: true, view: MyControlsRegion()))
IosContentSdk.shared.setInfoRegion(region: DramaPlayerRegion(useDefault: false, view: nil))
IosContentSdk.shared.setVideoRegion(region: DramaPlayerRegion(useDefault: true, view: nil))
```

</details>

<details>
<summary><b>Your own player: createPlayer</b></summary>

- Put a player in your own UI (list item, tab, sheet) instead of the SDK player page. The SDK still handles episodes, authorization, preloading, and playback events.
- `ContentPlayer` is also a controller with all the playback control calls above. One player shows in one view at a time.
- It pauses when the view is removed, hidden, or the page goes to the background, and resumes when shown again. Call `release()` when done.

Android:

```kotlin
val player = AndroidContentSdk.createPlayer()
player.open(Drama.PlayerParams(id = "YOUR_DRAMA_ID", episodeOrder = 1))
// View; ContentPlayerOverlays.None draws only video and subtitles
container.addView(AndroidContentSdk.createPlayerView(context, player, ContentPlayerOverlays.Default))
// Compose: AndroidContentSdk.ContentPlayerView(player, Modifier.fillMaxSize(), ContentPlayerOverlays.None)
player.release() // when your screen goes away
```

iOS:

```swift
let player = IosContentSdk.shared.createPlayer()
player.open(params: Drama.PlayerParams(id: "YOUR_DRAMA_ID", episodeOrder: 1, drama: nil))
let playerVC = IosContentSdk.shared.playerViewController(player: player, overlays: .default_)
parent.addChild(playerVC)
playerVC.view.frame = container.bounds
container.addSubview(playerVC.view)
playerVC.didMove(toParent: parent)
```

</details>

<details>
<summary><b>Revenue reports: reportAdRevenue / reportPurchaseRevenue</b></summary>

- Ads your app shows: call `reportAdRevenue` once per impression, usually in the ad platform's paid callback. Your own purchases or subscriptions: call `reportPurchaseRevenue` once per successful payment.
- Every field is required; amounts are in US dollars. `valueUsd` is this impression's value (not eCPM).
- `dedupeId` deduplicates: the ad platform's response id for ads, the store order id for purchases (Google Play `orderId`, App Store `transactionId`). Different impressions or payments need different ids.

Android:

```kotlin
val resource = controller.state.resource ?: return
AndroidContentSdk.reportAdRevenue(
    AdRevenueReport(
        aboutUnlock = true,
        resource = resource,
        valueUsd = adValue.valueMicros / 1_000_000.0,
        precision = AdRevenuePrecision.Precise,
        adNetworkName = adSourceName.ifBlank { "unknown" },
        adType = RevenueAdType.Rewarded,
        dedupeId = responseId,
    ),
)
AndroidContentSdk.reportPurchaseRevenue(
    PurchaseRevenueReport(aboutUnlock = true, resource = resource, revenueUsd = 4.99, dedupeId = orderId),
)
```

iOS:

```swift
guard let resource = controller.state.resource else { return }
IosContentSdk.shared.reportAdRevenue(report: AdRevenueReport(
    aboutUnlock: true,
    resource: resource,
    valueUsd: valueUsd,
    precision: .precise,
    adNetworkName: adSourceName.isEmpty ? "unknown" : adSourceName,
    adType: .rewarded,
    dedupeId: responseId
))
IosContentSdk.shared.reportPurchaseRevenue(report: PurchaseRevenueReport(
    aboutUnlock: true, resource: resource, revenueUsd: 4.99, dedupeId: transactionId
))
```

</details>

## Environment

| Android | |
|---|---|
| Android Gradle Plugin / Gradle | 9.3.0 / 9.5.0 |
| Kotlin | 2.3.10 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 |
| Gradle JDK | 25 (downloaded by Gradle if missing) |

| iOS | |
|---|---|
| Xcode | 26.6 |
| CocoaPods | 1.17.0 |
| Deployment target | iOS 16.0, iPhone |
| Run on | Device, or an Apple-silicon Mac simulator |

## Logs

Debug builds turn on SDK diagnostic logging with `setLoggingEnabled`, so you can follow initialization and page loading. Keep it off in release builds.

- Android: filter Logcat by `ContentSdk`.
- iOS: filter the Xcode console by `ContentSdk`.

## Notes

- The App ID / App Secret in this demo are for the demo only. Use your own values in your app.
- The Android icons in `app/src/main/res/drawable/ic_*.xml` are Material icons (Apache License 2.0). iOS uses SF Symbols, so there are no icon assets to copy.

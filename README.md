# Content SDK Demo: Pure Edition

English | [中文](README.zh-CN.md) · [Commercial edition demo](https://github.com/dramacontentdev-creator/ContentSDK-Commercial)

This repository does not include the SDK. Get the SDK package for this edition and platform from your SDK provider, then put the files where the quick start below says.

The SDK shows no ads. Your app monetizes locked episodes on its own and reports the revenue to the
SDK. Use the pure SDK packages here.

## Quick start: Android

1. From `ContentSdk-pure-Android-YOUR_SDK_VERSION-release`, copy `ContentSdk-pure-YOUR_SDK_VERSION.aar` into `android/app/libs/`.
2. Copy `content-sdk-dependencies.gradle` from the same package into `android/app/`. The pure script has no ad dependencies; do not reuse the commercial one.
3. Open `android/` in Android Studio and run `app`.

## Quick start: iOS

1. Copy everything in `ContentSdk-pure-iOS-YOUR_SDK_VERSION-release` into `ios/ContentSdkPure/`. The package contains `ContentSdkPure.podspec` and no `SKAdNetworkItems.plist`.
2. Run `pod install` in `ios/`.
3. Open `ios/ContentSdkDemo.xcworkspace` and run.

To run on a device, create `ios/Signing.local.xcconfig` with `DEVELOPMENT_TEAM = YOUR_TEAM_ID` (see `ios/Signing.xcconfig`). The file is git-ignored; setting the team in Xcode's Signing tab would write it into the project file instead.

The Swift module is still `ContentSdk`, so the code keeps `import ContentSdk`.

## Integrate into your app

Each step is marked `Content SDK - integration Step N` in the code. Copy them in order. Android and iOS count their steps separately, so read each column from top to bottom (the iOS column has no dependency step):

| Step | Android | iOS |
|---|---|---|
| 1 | `settings.gradle.kts`: `google()`, `mavenCentral()`, and the AAR folder | `Podfile`: `pod 'ContentSdkPure'` |
| 2 | `app/build.gradle.kts`: AAR and dependencies | `ContentSdkDemoApp.swift`: initialize |
| 3 | `DemoApplication.kt`: initialize | `DemoEdition.swift`: start |
| 4 | `DemoEdition.kt`: start | `SdkIntegration.swift`: open the SDK page |
| 5 | `MainActivity.kt`: open the SDK page | |

No AdMob Application ID, SKAdNetworkItems, or ad consent step is needed.

Then fill in your own values: get the App ID / App Secret at
[https://open.lucky-chick.com](https://open.lucky-chick.com) and put them in `SdkConfig` in
`DemoEdition.kt` / `DemoEdition.swift`. The demo values are for the demos only.

Everything else is optional and works with SDK defaults:

- `customization/`: replace parts of the SDK player page with your own UI. On Android start with `CustomRegions.kt`, which holds the SDK calls and a long explanation; `compose/` and `view/` hold the same info region, controls region, and episode list written for Compose and for Android Views, and `PlayerFeed.kt` turns the controller's state into what they draw. On iOS the same pieces are `CustomRegions.swift`, `SwiftUI/`, `UIKit/`, and `PlayerFeed.swift`; there the app owns navigation, so a custom info region closes the page through `PlayerPageCloser`. Pick a style on the demo home screen (SDK default, Compose / SwiftUI, or View / UIKit), then open the player. Copy whichever matches your UI toolkit.
- `lab/` (Android) / `Lab/` (iOS): the player lab. The SDK player page fills the screen and the demo draws its own control panel over it: play / pause, seek, speed, quality, and episodes; playback options; the page style; `autoHide`, `setRegionVisible`, and `setControlsHeld`; and a log of every call and playback event. Every SDK call it makes is in `LabActions.kt` / `LabActions.swift`. Open it from the demo home screen.
- `catalog/SdkApiCatalog.kt` / `SdkApiCatalog.swift`: APIs both editions have, with examples to copy (playback control, playback options, history, player page regions, your own player, revenue reports, ...).
- `catalog/EditionApiCatalog.kt` / `EditionApiCatalog.swift`: your own monetization, as an example to copy (`monetizeEpisodes`; the demo app does not switch it on). When an episode your own rules charge for starts (in the example: past episode 5): pause, show your paywall or ad, report it with `reportPurchaseRevenue` / `reportAdRevenue`, then call `play()`. A controller has one state listener, so if your app already listens (like `PlayerFeed` in `customization/`), put the check there. An episode the content service reports as locked (`state.episodeLocked`) cannot play: `play()` does not start it and the SDK shows "unavailable".
- `ui/` (Android and iOS): demo UI only.

## API index

Sections are those of `SdkApiCatalog` (`catalog/SdkApiCatalog.kt` on Android, `Catalog/SdkApiCatalog.swift` on iOS); both platforms use the same numbers.

| API | Where | Use |
|---|---|---|
| `initialize` / `start` | Integration steps above; Android with coroutines: `EditionApiCatalog` P1 | Required setup |
| `setLoggingEnabled` | §1 | SDK logs |
| `setStrategy` | §2 | Resource strategy |
| `setColorPalette` / `resetColorPalette`, `setSubtitleStyle` / `resetSubtitleStyle` | §3 | Theme colors and subtitles |
| Android: `openDramaPlayerPage`, `createDramaHomeFragment`, `createDramaPlayerFragment`, `DramaHomeScreen`, `DramaPlayerScreen`; iOS: `dramaHome` variants, `dramaPlayer` | §4 | Other ways to show SDK pages |
| `login` / `setLanguage` | §5 | User or app language changed |
| `setPlayerEventListener` | §6 | Playback events |
| `getDramaList` | §7 | Your own drama list |
| `setPlayerControllerListener`; controller `play`, `pause`, `seekTo`, `setPlaybackSpeed`, `setQuality`, `selectEpisode`, `setListener` | §8.1 - 8.2; runnable: lab | Drive the SDK player page from your UI |
| `setContentListener` / `setProgressListener` | §8.3; runnable: `customization/PlayerFeed` | Content and progress for your own controls |
| `setControlsHeld` | §8.4; runnable: lab | Keep the page controls shown under your dialog |
| `setAutoPlay` / `setAutoAdvance` / `setResumeFromHistory` | §9; runnable: lab settings | Playback options |
| `getPlaybackHistory` / `removePlaybackHistory` / `clearPlaybackHistory` | §10 | Continue watching |
| `setVideoRegion` / `setInfoRegion` / `setControlsRegion`, `DramaPlayerRegion` | §11; runnable: `customization/` | Replace parts of the player page |
| `DramaPlayerRegion(autoHide)`, `setRegionVisible`, `state.controlsVisible` | §11; runnable: lab settings | Show and hide your regions yourself |
| `createPlayer`; Android `ContentPlayerView` / `createPlayerView`, iOS `playerViewController` | §12 | The player inside your own UI |
| `reportAdRevenue` / `reportPurchaseRevenue` | §13 | Revenue your app earns itself |
| Locked episodes: pause, your paywall or ad, report, play | `EditionApiCatalog` P2 (Android) / P1 (iOS) | Your own monetization |

Android source layout (`app/src/main/kotlin/com/lc/content/sdk/demo/`):

```
DemoApplication.kt   Step 3: initialize
DemoEdition.kt       credentials, Step 4: start (differs per edition)
MainActivity.kt      Step 5: open the SDK page
catalog/             every API with an example to copy
customization/       replace parts of the player page (Compose and View)
lab/                 player lab: your own controls over the SDK player page
ui/                  the demo's own screens; not needed to integrate
```

iOS source layout (`ios/ContentSdkDemo/`):

```
ContentSdkDemoApp.swift   Step 2: initialize
DemoEdition.swift         credentials, Step 3: start (differs per edition)
SdkIntegration.swift      Step 4: open the SDK page
Catalog/                  every API with an example to copy
Customization/            replace parts of the player page (SwiftUI and UIKit)
Lab/                      player lab: your own controls over the SDK player page
UI/                       the demo's own screens; not needed to integrate
```

iOS uses SF Symbols, so there are no icon assets to copy.

The icons in `app/src/main/res/drawable/ic_*.xml` are Material icons (Apache License 2.0).

App ids: Android `com.lc.content.sdk.demo.pure`, iOS `com.lc.content.sdk.demo.ios.pure`. All four demos (commercial and pure, Android and iOS) use one set of demo credentials, for the demos only; use your own App ID / App Secret in your app.

## Demo environment

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

Debug builds turn on SDK diagnostic logging with `setLoggingEnabled`, so you can follow
initialization and page loading. Keep it off in release builds.

- Android: filter Logcat by `ContentSdk`.
- iOS: filter the Xcode console by `ContentSdk`.

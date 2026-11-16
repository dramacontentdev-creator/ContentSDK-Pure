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

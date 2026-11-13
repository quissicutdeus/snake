# Sniddle on iPhone and iPad

How Sniddle gets built for iOS without a Mac, and how it gets onto the kids' iPhones and iPads with a free
Apple ID from a Linux computer. It covers the install path, the weekly chore, how CI builds the app, and
what the iOS port (phase 2) needs from the platform.

**Status checked 2026-11-12.** Free-Apple-ID sideloading is a cat-and-mouse game with Apple's servers.
The tools below broke and were fixed several times in the two months before this was written. Check the
links before relying on any step, and expect to come back to this page after an iOS update.

The fixed decisions: iPhone and iPad, a **free Apple ID** (no paid developer program, no TestFlight, no App
Store), **no Mac** (every iOS build runs on GitHub's macOS runners), and Compose Multiplatform for the port.

## In short

- **Install with [Impactor](https://github.com/khcrysalis/PlumeImpactor) on the Linux computer, over USB.**
  It signs CI's unsigned `sniddle.ipa` with the family's free Apple ID and installs it.
- **Once a week, per device:** plug the iPhone or iPad into the computer and install the newest
  `sniddle.ipa` again with the same Apple ID. It takes about five minutes, and the high scores and looks
  stay.
- If the week is missed, Sniddle **stops opening**. Nothing is lost: install again and it's back as it was.
- CI builds an **unsigned** IPA on a `macos-26` runner (Xcode 26.4.1). Today that happens only when the
  workflow is run by hand. When iOS ships, the IPA goes onto the same GitHub release as the APK. On a
  public repo the macOS minutes cost nothing.

## What a free Apple ID allows

From Apple's [membership comparison](https://developer.apple.com/support/compare-memberships/), under
"Enable a personal team in Xcode":

- "You can install up to **3 apps per device**. Provisioning profiles that enable apps to be installed on a
  device will **expire 7 days from issuance**. You'll need to rebuild and reinstall your app to your device
  after expiration."
- "You can register up to **10 App IDs**, which expire after 7 days."
- "You can register up to **3 devices**, which expire after 7 days."

What that means here:

- Sniddle uses one of the three app slots on each device. LocalDevVPN comes from the App Store and doesn't
  count; SideStore, if used, takes a second slot.
- One Apple ID covers at most three iPhones or iPads at a time. A fourth device needs a second Apple ID.
- Sniddle needs no capabilities (no iCloud, no push, no app groups), so nothing in it hits the free tier's
  restrictions.

On iOS and iPadOS 16 and later, every device has to have **Developer Mode** turned on before it will run an
app signed this way. Apple's
[Enabling Developer Mode on a device](https://developer.apple.com/documentation/xcode/enabling-developer-mode-on-a-device)
covers it ("installing an .ipa file … the device explicitly asks the person using it to confirm that
they're a developer"). It is a one-time switch, and it survives restarts.

## The install tools, compared

Every Linux tool works the same way. It signs in to Apple's developer services as the free Apple ID, as
Xcode would, creates a development certificate and a 7-day provisioning profile, signs the app, and
installs it over USB through `usbmuxd`. The [Impactor README](https://github.com/khcrysalis/PlumeImpactor#how-it-works)
explains this well.

| Tool | Linux today? | Weekly refresh | Verdict |
| --- | --- | --- | --- |
| [Impactor](https://github.com/khcrysalis/PlumeImpactor) (formerly PlumeImpactor), GUI + `plumesign` CLI | Yes: AppImage, [Flathub](https://flathub.org/en/apps/dev.khcrysalis.PlumeImpactor), v2.7.2 (2026-10-24) | Plug in at the computer. Its auto-refresh on Linux only works "when a device is plugged in" (no Wi-Fi, because `usbmuxd` has none) | **Recommended** |
| [iloader](https://github.com/nab138/iloader) | Yes: AppImage, .deb, .rpm, v2.4.1 (2026-10-28) | Plug in at the computer and "Import IPA" again | Fallback. Built mainly to install SideStore; on iOS 27, imported apps reported to close at launch ([#1082](https://github.com/nab138/iloader/issues/1082)) |
| [SideStore](https://github.com/SideStore/SideStore), installed once with iloader | First install only | On the device: LocalDevVPN on, Wi-Fi, tap refresh in SideStore (or a Shortcuts automation) | Too fragile for kids right now (see below) |
| [AltServer-Linux](https://github.com/NyaMisty/AltServer-Linux) | Last release v0.0.5, April 2022 | Computer on the same Wi-Fi | Unmaintained. Open issue: AltStore crashes on iOS 26.4 |
| [Sideloadly](https://sideloadly.io/) | **No**: "macOS 10.12+ · Windows 7+" | Its own daemon | Not an option without Windows or a Mac |
| [zsign](https://github.com/zhlynn/zsign) | Yes, but only signs | — | Needs a `.p12` certificate and a `.mobileprovision`, which a free Apple ID can't download. It doesn't install anything |
| AltStore Classic / [AltStore PAL](https://github.com/altstoreio/AltStore) | No | AltServer runs on macOS or Windows; PAL is an EU marketplace for notarised apps | Not an option |

### Why not SideStore

SideStore is the only way to refresh **without** a computer each week. Its
[FAQ](https://docs.sidestore.io/docs/faq) says "You only need a computer once during installation", and that
it "will periodically 'refresh' your apps in the background". It is the right answer if the weekly cable
becomes the problem. Right now it isn't kid-proof:

- **More to keep working on each device:** Developer Mode, the trusted developer profile,
  [LocalDevVPN](https://docs.sidestore.io/docs/installation/prerequisites) ("keep it on whenever you install,
  update, or refresh apps"), Wi-Fi ("mobile network is not suitable"), a SideStore sign-in, and a pairing
  file. The [install guide](https://docs.sidestore.io/docs/installation/install) warns that the pairing file
  "may expire after an update or reset, or at random times", and replacing it needs the computer again.
- **Recent breakage:** an Apple server change broke sign-in in September 2026. The current recommended build
  is [0.7.0-alpha](https://github.com/SideStore/SideStore/releases/tag/0.7.0-alpha), and 0.6.4 is marked
  "DO NOT USE".
- **Open reports from the last two weeks:**
  - Installing SideStore fails on iOS 27.2 with "The provisioning profile is banned"
    ([iloader #1102](https://github.com/nab138/iloader/issues/1102),
    [#1144](https://github.com/nab138/iloader/issues/1144)).
  - Shortcuts refresh fails while the device is locked on iOS 27.2
    ([SideStore #1723](https://github.com/SideStore/SideStore/issues/1723)).
  - Device registration fails on iOS 26.4+
    ([#1698](https://github.com/SideStore/SideStore/issues/1698)).
- **Uses a second app slot,** and refreshing SideStore itself uses App IDs.

Retry it once the family is comfortable with the basic path. "SideStore + Sniddle" fits in the 3-app limit.

### Rules that apply whichever tool you use

1. **One Apple ID, forever, for Sniddle.** Both Impactor and iloader install the app as
   `com.quissicutdeus.sniddle.<TEAMID>`, where the team ID belongs to the Apple ID. Impactor's
   [signer.rs](https://github.com/khcrysalis/PlumeImpactor/blob/main/crates/plume_utils/src/signer.rs) does
   `format!("{identifier}.{team_id}")`, and isideload's
   [sideloader.rs](https://github.com/nab138/isideload/blob/main/isideload/src/sideload/sideloader.rs) does
   `format!("{}.{}", main_bundle_id, team.team_id)`.
   - Same Apple ID: an install lands on top of the existing app, and iOS keeps its data (scores, kids,
     looks).
   - A different Apple ID: you get a *second* Sniddle with empty data.
   - Because both tools use the same form, switching between Impactor and iloader with the same Apple ID
     keeps the data.
2. **Use a separate Apple ID for sideloading, not a parent's main one.** These tools talk to Apple's private
   developer endpoints and get Mac-style device headers ("anisette") from a third-party server by default:
   Impactor uses `ani.f1sh.me` / `mac-ani.jkcoxson.com`
   ([omnisette](https://github.com/khcrysalis/PlumeImpactor/blob/main/3rdparty/omnisette/src/lib.rs)), and
   iloader uses `ani.iloader.app`
   ([isideload](https://github.com/nab138/isideload/blob/main/isideload/src/anisette/remote_v3/mod.rs)).
   - The password goes to Apple, not to those servers.
   - SideStore's FAQ still warns that "Older Anisette servers that are used by many users are known to cause
     locking of Apple ID's".
   - Give the separate Apple ID two-factor authentication on a phone number you control: a "burner" with no
     phone can't receive the code ([Impactor #275](https://github.com/khcrysalis/PlumeImpactor/issues/275)).
3. **One computer, one tool, one certificate.** A free account can hold only a few development certificates.
   A tool that creates a new one (on another computer, after a reinstall, or SideStore's "revoke or create a
   new signing certificate" prompt) may revoke the old one, and every app signed with that one stops
   opening. Impactor keeps its key locally and reuses the certificate, which "last 365 days".
4. **Prefer a full install to a "refresh".** A full install re-signs the IPA and installs it over the old
   one. Impactor's profile-only refresh fails on iOS 27.2
   ([#311](https://github.com/khcrysalis/PlumeImpactor/issues/311); its author: "auto-refresh has always been
   wonky"). A full install also picks up the newest release.

## Install it: step by step (Linux, free Apple ID)

### Once, on the computer

1. Install `usbmuxd`, which talks to iPhones over USB: `paru -S usbmuxd`.
2. Install Impactor from [Flathub](https://flathub.org/en/apps/dev.khcrysalis.PlumeImpactor)
   (`flatpak install flathub dev.khcrysalis.PlumeImpactor`), or download `Impactor-linux-x86_64.appimage`
   from its [releases](https://github.com/khcrysalis/PlumeImpactor/releases). Use only those two sources.
3. Make (or pick) the separate Apple ID for sideloading, with two-factor authentication on your phone
   number.

### Once per iPhone or iPad

1. Give the device a passcode if it has none, and connect it to Wi-Fi.
2. Download `sniddle.ipa` on the computer. Until iOS ships, it comes from the **Artifacts** of a run of the
   [iOS workflow](../.github/workflows/ios.yml). After that, it comes from the
   [latest release](../../../releases/latest).
3. Plug the device in with a cable and unlock it. Tap **Trust** on the device and enter its passcode.
4. *Then* open Impactor. On Linux, `usbmuxd` can stop while no device is connected, so plug in first; if the
   device isn't listed, quit and reopen Impactor (its README's Linux note). Sign in with the sideloading
   Apple ID, entering the two-factor code when asked.
5. Pick the device, drop `sniddle.ipa` onto Impactor, and install.
6. On the device, trust the developer
   ([as in SideStore's guide](https://docs.sidestore.io/docs/installation/install)): **Settings → General →
   VPN & Device Management →** the Apple ID under "Developer App" **→ Trust**.
7. Turn on Developer Mode: **Settings → Privacy & Security → Developer Mode → on**, then **Restart**. After
   the restart, swipe up, tap **Turn On** and enter the passcode.
8. Open Sniddle.

**Kid-proofing tip:** [Guided Access](https://support.apple.com/en-us/111795) (Settings → Accessibility)
locks the device to Sniddle until a grown-up triple-clicks and enters a code.

### Every week: the refresh chore

Do it on a fixed day, for example every Sunday, for each device, well inside the 7 days:

1. Download the newest `sniddle.ipa`. Same file name, so it simply replaces the old one.
2. Plug the device in and unlock it, *then* open Impactor (quit and reopen it if the device isn't listed).
3. Install `sniddle.ipa` again (the same steps 5 and 8 as above, with the same Apple ID). No need to trust
   or enable anything again.

About five minutes per device. A calendar reminder helps; the iOS port should also say when it is due (see
[Phase 2](#phase-2-ios-platform-services)).

**If a week is missed:** Sniddle stops opening. Tapping it does nothing, or iOS says the app is no longer
available. Do the weekly steps; the kids' snails, names and scores are all still there, because iOS keeps
an app's data while the app stays installed. **Don't delete the app**, because that deletes its data.

**After an iOS update:** expect to tap **Trust** on the device again. If Impactor fails, check its
[issues](https://github.com/khcrysalis/PlumeImpactor/issues) first. iloader with the same Apple ID is the
fallback, but it is a real switch, not a one-off:

- iloader may create its own certificate and revoke Impactor's (rule 3). If it does, reinstall with iloader
  on **every** device that same day, or the others stop opening.
- On iOS 27, apps installed with iloader's "Import IPA" have been reported to close at launch while the same
  signed app installed another way works ([iloader #1082](https://github.com/nab138/iloader/issues/1082)).
  Check that it is fixed first.

### For the command line

`plumesign` (on the same release page) does the install without the GUI. These flags are from its
[source](https://github.com/khcrysalis/PlumeImpactor/blob/main/apps/plumesign/src/commands/sign.rs); this is
untested here:

```sh
plumesign account login            # once; prompts for the Apple ID, password and 2FA code
plumesign sign --package sniddle.ipa --apple-id --register-and-install
```

## How CI builds the iPhone app

[`.github/workflows/ios.yml`](../.github/workflows/ios.yml) holds the design. It runs only from **Run
workflow** until phases 1 and 2 land, so it can't block a push or ship anything.

### No Mac: the Xcode project is generated

- [`iosApp/project.yml`](../iosApp/project.yml) is the whole Xcode project, as text.
- CI turns it into `Sniddle.xcodeproj` with [XcodeGen](https://github.com/yonaskolb/XcodeGen) **2.46.0**:
  downloaded from its GitHub release and checked against the release's sha256, not installed with Homebrew,
  which can't pin a version.
- The `.xcodeproj` is never committed (`iosApp/.gitignore`), so nobody has to hand-merge Xcode's project
  file.
- XcodeGen also builds and runs on Linux. With `swift build` in the `swift:6.3.3-noble` Docker image, the
  spec generated the expected project, script phase and shared scheme. Run that way, it needs a real user
  (it fails with "Couldn't find current username" as root) and the `SettingPresets` from the release zip
  next to the binary.
- Why not the alternatives:
  - The Compose Multiplatform template's checked-in `.xcodeproj` can only sensibly be edited in Xcode.
  - [Tuist](https://github.com/tuist/tuist) does more than this one-target app needs: a Swift manifest, its
    own release train and cloud services.

### Runner and Xcode

- `runs-on: macos-26`, the Apple-silicon image. It is pinned to the OS version, not `macos-latest`, and not
  the `xcode-27` image, which is a
  [public preview](https://github.com/actions/runner-images/issues/14404).
- Xcode 26.4.1, picked with `DEVELOPER_DIR`. Kotlin 2.4.20–2.4.21 is listed as compatible with Xcode 26.4 in
  the [Kotlin Multiplatform compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html).
- The [image](https://github.com/actions/runner-images/blob/main/images/macos/macos-26-arm64-Readme.md) also
  has 26.0.1–26.6, iOS 26.2–26.5 simulators (iPhone 17, iPad Air, iPad Pro…), JDK 21 and Android platform
  37.2.
- It keeps only the newest patch of each Xcode, so when 26.4.1 is replaced the job fails at "Xcode version"
  instead of quietly building with another one. Raise the Xcode version together with Kotlin.
- The same table lists Kotlin 2.4.20–2.4.21 with Gradle up to 9.7.0 and the Android Gradle plugin up to
  9.3.1. This repository is on Gradle 9.9 and AGP 9.5.1. That works for Android today, but it is outside
  what JetBrains lists for Kotlin Multiplatform, so it is the first suspect if the iOS build misbehaves.

### Jobs

- **`game-tests`:** `./gradlew :game:iosSimulatorArm64Test :game:linkDebugTestIosArm64`. It compiles the game
  rules with Kotlin/Native, runs their tests in an iOS simulator, and links the tests for a real arm64
  device too. These task names are confirmed by the KMP conversion. On Linux,
  `:game:compileTestKotlinIosArm64` compiles them, but linking and running need a Mac.
  - Kotlin picks Xcode's default test simulator itself
    ([KotlinNativeTestRunFactories.kt](https://github.com/JetBrains/kotlin/blob/v2.4.21/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/KotlinNativeTestRunFactories.kt)),
    so no device name is hard-coded.
  - This catches Kotlin/Native-only differences (memory model, number formatting, missing JVM APIs) that the
    JVM tests in `build.yml` can't.
  - The 10 000-step soak tests run here too. If they make the job too slow, give them their own source set.
- **`ipa`:**
  1. Generate the project with XcodeGen.
  2. `xcodebuild build -sdk iphoneos -configuration Release` with `CODE_SIGNING_ALLOWED=NO`. The project's
     "Compile Kotlin framework" phase runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, which
     is Kotlin's
     [direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html).
  3. Copy `Sniddle.app` into `Payload/` and zip it as `sniddle.ipa`.
  4. Check it, failing the job on anything wrong: the binary is arm64, the bundle ID and version are right,
     the asset catalog was compiled, and the IPA has the expected layout.
- **Caches:** `~/.konan`, the Kotlin/Native toolchain (about 1 GB), keyed on `gradle/libs.versions.toml`.
  The Gradle cache is shared with `build.yml` through `setup-gradle`.
- **Versions:** the IPA gets `MARKETING_VERSION` (`CFBundleShortVersionString`) = the release version and
  `CURRENT_PROJECT_VERSION` (`CFBundleVersion`) = the run number, the same scheme as the APK.
- **Action pins:** `checkout@v7.0.1`, `setup-java@v6.2.0`, `setup-gradle@v6.4.1`, `cache@v6.1.0` and
  `upload-artifact@v7.0.2`, all exact release tags.

**Unsigned on purpose.** A free Apple ID has no certificate that CI could hold: the sideload tool creates
one on the family's computer, and it's tied to that computer's key. So CI ships an app with signing switched
off, and the tool signs the whole bundle at install time.

**Static framework.** The shared UI should be a *static* Kotlin framework (`isStatic = true`). It is linked
into the app's own binary and nothing is embedded under `Frameworks/`, so the sideload tool has one
signature to redo, not two.

**Before phase 2 there is no Kotlin UI to link,** so the build phase skips Gradle with an Xcode warning, and
[`SniddleApp.swift`](../iosApp/Sniddle/SniddleApp.swift) shows a placeholder screen with the version. That
placeholder is useful now: it lets the owner try the whole install path on a real device before the port is
done. Once a `composeApp/` directory exists, the job fails if the app binary doesn't contain the Kotlin UI,
so a placeholder can't be released by mistake.

### Shipping it with the APK (phase 3)

When the port is ready, move both jobs into `build.yml`:

- Run `ipa` beside `build`, and have the `release` job `need` it.
- Attach `sniddle.ipa` to the **same release** as `sniddle.apk`, so `releases/latest/download/sniddle.ipa`
  always works and both platforms share one version number.
- Move the version step into its own small job that `build` and `ipa` both `need`, so neither waits for the
  other.
- Count `iosApp/` as an app change in "Check whether the app changed".

**Until then, `build.yml` should list `iosApp/` among the paths that don't release.** That is not done
yet, and `build.yml` is outside this change. Without it, merging this scaffold publishes an Android release
with nothing new in it.

### macOS minutes

Standard GitHub-hosted runners are
[free in public repositories](https://docs.github.com/en/billing/concepts/product-billing/github-actions),
and macOS is included. Rough estimates, to be replaced with real timings after the first runs:

| Job | Cold cache | Warm cache |
| --- | --- | --- |
| `game-tests` | ~10–15 min | ~5–8 min |
| `ipa` (Kotlin/Native release framework + Swift) | ~15–25 min | ~8–15 min |

- At ~20 releases a month, that is ~300–600 macOS minutes a month, at no cost while the repo is public.
- If the repo ever went private, macOS minutes cost $0.062 each (same page): about $1–2.50 per release.
- macOS runners queue longer than Linux ones, so once the IPA is part of every release, a release waits for
  the slower macOS job.

## App shell (`iosApp/`)

- **`project.yml`:** one iOS application target for iPhone and iPad (`TARGETED_DEVICE_FAMILY 1,2`), not Mac
  Catalyst or Vision Pro.
  - iOS 16.0 minimum: `persistentSystemOverlays` and `defersSystemGestures` need it, and Developer Mode
    exists from 16 on.
  - Swift 5 language mode, because Kotlin's Objective-C headers carry no `Sendable` annotations.
  - The phase-2 names are in one place: `KOTLIN_UI_MODULE` = `composeApp`, `KOTLIN_UI_FRAMEWORK` =
    `ComposeApp`.
- **`Sniddle/SniddleApp.swift`:** a SwiftUI `App` with one window that hosts
  `MainViewControllerKt.MainViewController()` from the Kotlin framework. It mirrors `MainActivity`:
  - It keeps the screen awake (`isIdleTimerDisabled`).
  - It hides the status bar.
  - It hides the home indicator (`persistentSystemOverlays(.hidden)`).
  - It defers the system edge gestures (`defersSystemGestures(on: .all)`): a thumb sliding off the edge
    mid-game only shows the indicator, and a second swipe leaves. That is the iOS version of Android's
    "bars come back with a swipe".
- **`Sniddle/Info.plist`:** no `NS…UsageDescription` keys, so no permissions.
  - Orientations: portrait and both landscapes on iPhone, all four on iPad. The Android activity has no
    `screenOrientation`, so it follows the device.
  - One scene (no second Sniddle window).
  - `CADisableMinimumFrameDurationOnPhone`, for 120 Hz on ProMotion iPhones.
  - A launch screen of arena green (`#9ED96B`, Android's `windowBackground`).
  - `UIRequiredDeviceCapabilities` = arm64.
- **App icon:** [`iosApp/scripts/make-icon.sh`](../iosApp/scripts/make-icon.sh) makes it from the Android
  launcher icon, so there is one source of truth.
  - [`vector-to-svg.xsl`](../iosApp/icon/vector-to-svg.xsl) turns `ic_launcher_foreground.xml` into SVG. It
    keeps the middle 72 of the 108 units that an Android launcher shows, so the snail is the same size on
    both platforms.
  - `rsvg-convert` draws it at 1024×1024 on the launcher background `#1F2547`, and ImageMagick removes the
    alpha channel, because iOS shows transparent icon pixels as black.
  - The result is a single-size `AppIcon.appiconset` (Xcode makes every other size).
  - The PNG is committed, because the macOS runners have no librsvg. Run the script again when the Android
    icon changes.
  - The XSLT stops on any element or attribute it doesn't understand (each element has a list of the
    attributes it handles), rather than leaving part of the icon out.

## Phase 2: iOS platform services

Everything the shared Compose Multiplatform UI needs from iOS that `app/` gets from Android today. Each of
these can be a small `expect`/`actual` in Kotlin: Kotlin/Native calls UIKit and AVFAudio directly, so none
of them needs Swift.

### Audio (synthesized PCM)

`game/audio` makes every effect and the music loop as PCM, and Android plays them through one preloaded
`AudioTrack` per effect plus a looping one. On iOS, use
[AVAudioEngine](https://developer.apple.com/documentation/avfaudio/avaudioengine):

- At start-up, copy each effect into an `AVAudioPCMBuffer`.
- Keep a small pool of `AVAudioPlayerNode`s attached to the main mixer, so effects can overlap, and
  `scheduleBuffer` on a free one per cue.
- Play the music on its own player node with the `.loops` option.
- Keep Kotlin code off the real-time audio thread. Don't use an
  [AVAudioSourceNode](https://developer.apple.com/documentation/avfaudio/avaudiosourcenode) render callback
  written in Kotlin, because Kotlin/Native's allocator and GC can stall it. Prebuilt buffers need no
  callback at all.
- **Audio session category** (open question for the owner):
  - [`.ambient`](https://developer.apple.com/documentation/avfaudio/avaudiosession/category-swift.struct/ambient)
    obeys the Silent switch and mixes with other apps' audio. Apple: "Screen locking and the Silent switch …
    silence your audio".
  - [`.playback`](https://developer.apple.com/documentation/avfaudio/avaudiosession/category-swift.struct/playback)
    plays even in silent mode.
  - The default,
    [`.soloAmbient`](https://developer.apple.com/documentation/avfaudio/avaudiosession/category-swift.struct/soloambient),
    obeys the switch but stops other audio.
  - The suggestion is `.ambient`. A kid who flips the switch gets silence, which matches how Android follows
    the media volume.
- Handle `AVAudioSession.interruptionNotification` (a phone call or a timer) and the engine's configuration
  change (headphones in or out): stop, then restart the engine. Stop the music when the app leaves the
  foreground, as `MainActivity.onPause` does.

### Haptics

- [`UIImpactFeedbackGenerator`](https://developer.apple.com/documentation/uikit/uiimpactfeedbackgenerator)
  (and `UINotificationFeedbackGenerator`) for the `render/Haptics` cues. Call `prepare()` a moment before a
  likely cue.
- **iPads have no Taptic Engine.** There these calls do nothing. Check
  [`CHHapticEngine.capabilitiesForHardware().supportsHaptics`](https://developer.apple.com/documentation/corehaptics/chhapticdevicecapability/supportshaptics)
  and hide the buzz button where it is false, so it doesn't look broken.
- The phone's own Sounds & Haptics setting can switch haptics off, as Android's does.

### Storage

- `NSUserDefaults` is the counterpart of SharedPreferences: same key/value model, same small data (top-10
  scores, up to four kids, settings, the saved run). Keep Android's key names, so one shared `Storage`
  implementation works for both.
- The data lives in the app's container. It survives the weekly reinstall (same bundle ID), and it is lost
  if the app is deleted or installed under a different Apple ID (see the [rules](#rules-that-apply-whichever-tool-you-use)).
- There is no iCloud sync, and none is wanted (no network, no accounts).
- Android and iPhone scores stay separate.
- iOS also kills apps in the background, so the saved-run code (`SavedRun`) matters as much as on Android.
  Save on the Compose lifecycle's `ON_PAUSE` / `willResignActive`.

### Safe areas, notch and Dynamic Island

- The arena draws edge to edge (`.ignoresSafeArea()` in Swift).
- The HUD, the pause button, the thumbstick ring's clamp, warning arrows and dialogs must stay inside
  `WindowInsets.safeDrawing`:
  - On iPhones in landscape, the Dynamic Island or notch is on **one side**, and it swaps with rotation.
  - The home indicator is at the bottom.
  - iPads have rounded corners.
- Each two-player half needs its own insets: only the half next to the notch loses space.

### iPad multitasking and window sizes

- In iPadOS 26, apps run in freely resizable windows.
  [`UIRequiresFullScreen` is deprecated](https://developer.apple.com/documentation/bundleresources/information-property-list/uirequiresfullscreen)
  (deprecated in 26.0; Apple: "support multitasking and dynamic resizing"), so the app doesn't set it.
- The Compose layout must handle any size and aspect ratio, including narrow Slide Over–like windows and a
  window resized mid-game.
- The "Play together" button already depends on screen size. It should depend on the *window* size, so it
  disappears when the window gets too narrow.
- Pause when the window loses focus, as on Android.
- One scene only (`UIApplicationSupportsMultipleScenes` = false).

### Home indicator and system gestures during play

- `persistentSystemOverlays(.hidden)` fades the home indicator, and `defersSystemGestures(on: .all)` makes
  an edge swipe need a second swipe. That stops a kid steering near an edge from leaving the game.
- Because the Compose view controller sits inside SwiftUI, **verify on a device** that iOS asks the hosting
  controller (not the Compose one) for these preferences. If it doesn't, set them on the Compose controller
  from Kotlin (`prefersHomeIndicatorAutoHidden`, `preferredScreenEdgesDeferringSystemGestures`).

### Other things the port needs

- **The expiry warning.** The app can read its own `embedded.mobileprovision` (in the app bundle;
  `ExpirationDate` is in the signed plist inside it). Two days before expiry, the title screen can say
  "Grown-ups: Sniddle needs plugging in by Sunday". It needs no network and no permission. This turns
  "the app stopped working" into a reminder.
- **Frame rate:** a `CADisplayLink` drives Compose, and the fixed 60 Hz simulation interpolates as on
  Android. Check 120 Hz on a ProMotion iPhone.
- **Back gesture:** iOS has none, so every screen needs a visible way back. Android has the system back.
- **Fonts and emoji:** names count whole emoji (see the last commits). Check the 🐣 and holiday emoji with
  Apple's emoji font.
- **The date:** `Profile` is the only place the date is read. On iOS that is `NSDate` and the current
  calendar or time zone, through kotlinx-datetime or an `actual`.

## Open questions for the owner

1. **Which iPhones and iPads, on which iOS versions?** This decides the most:
   - Whether iOS 16 is a low enough minimum. A device stuck on iOS 15 (iPhone 6s/7, older iPads) would need
     iOS 15 and some Swift `if #available` work.
   - Whether the iOS 27.2 breakage above applies.
   - Whether one Apple ID is enough (at most 3 devices).
2. **Silent switch:** should Sniddle's sounds obey the iPhone's Silent switch (`.ambient`) or play anyway
   (`.playback`)?
3. **Which Apple ID** will be the sideloading one? It should be a separate one, with two-factor
   authentication on your phone.
4. **Is the weekly cable chore acceptable?** If it isn't, retry SideStore once its iOS 27.2 problems are
   fixed. That means accepting LocalDevVPN and more moving parts on each device.
5. **Release cadence:** once iOS is in the pipeline, should every app change also build the IPA (slower
   releases, free macOS minutes), or only some?
6. **Try the placeholder now?** Running the iOS workflow by hand gives a placeholder IPA. Installing it on
   one device would prove the whole install path before any porting work. The placeholder counts its
   launches and keeps the count in `UserDefaults`. If the count keeps going up after a weekly reinstall,
   and after a missed week followed by a reinstall, then the claim this page depends on holds on a real
   device: the kids' data survives.

# iOS port: what the Compose UI needs from Android

Phase 1 of the iOS port. This covers the app module's UI
(`app/src/main/kotlin/com/quissicutdeus/sniddle/`, about 6,300 lines). The
`game` module is covered separately, because it is being converted to Kotlin
Multiplatform on its own.

In this document, **Android-only** means imports of `android.*`, `androidx.activity`,
`androidx.core` and `java.*`. The `androidx.compose.*` packages (runtime,
runtime-saveable, ui, foundation, animation, material3) and
`androidx.lifecycle.compose` are not Android-only. Compose Multiplatform
publishes them under the same package names, as JetBrains artifacts
(`org.jetbrains.compose.*`, `org.jetbrains.androidx.lifecycle:*`), so code that
uses them moves without changes.

## Platform services and their seams

Phase 1 put each platform service behind a small Kotlin interface. Each
interface has an Android implementation, and those are passed in at the top:
`MainActivity` calls `SniddleApp(storage, calendar, sound, buzzer)`. No screen
reads `LocalContext` or `LocalView`, and none calls an Android API.

| Service | Seam (common) | Android implementation | iOS replacement |
| --- | --- | --- | --- |
| Saved data (SharedPreferences) | `KeyValueStore`, used by `Storage` and `SavedPlayer` | `SharedPreferencesStore`; `phoneStorage(context)` opens the `"sniddle"` file | A `KeyValueStore` backed by `NSUserDefaults` (string, integer, bool, and an array for the string set). iPhones have no older saves to stay compatible with, but keep the key names anyway. |
| Sound effects and music (AudioTrack) | `SoundOutput` (`play`, `music`) | `SoundPlayer` | `AVAudioEngine` with one `AVAudioPlayerNode` per sound. The buffers hold the same 16-bit mono PCM from `Synth.render` / `Music.render`, and the music loops. Use the `.ambient` audio session, so the mute switch is respected. |
| Haptics (`View.performHapticFeedback`) | `Buzzer` (`buzz(Buzz)`); the game's buzz switch is checked before it | `ViewBuzzer`, built from the root `LocalView` in `MainActivity` | `UIImpactFeedbackGenerator` / `UINotificationFeedbackGenerator`. Compose's `LocalHapticFeedback` has only coarse types, so verify it in phase 2 before relying on it. |
| Today's date (`java.time.LocalDate`) | `PhoneCalendar` returns a `Day`; `Day.epochDay` is plain Kotlin and is tested against java.time from 1900 to 2200 | `SystemCalendar` | `NSCalendar.currentCalendar` components of `NSDate()`, or kotlinx-datetime. kotlinx-datetime would be a new dependency. |
| Back button (`androidx.activity.compose.BackHandler`) | `ui/SystemBack.kt`, the only file that calls it | `BackHandler` | iPhones have no back button. Make it a no-op, or use Compose Multiplatform's back handler for the edge swipe (its artifact and API are experimental, so verify in phase 2). |
| Wall clock, random seed, `Math.PI` | Now `kotlin.time.Clock.System`, `kotlin.random.Random` and `kotlin.math.PI` | n/a | Already common. |

## Everything else the UI uses

| Dependency | Where | Compose Multiplatform / iOS |
| --- | --- | --- |
| `ComposeCanvas2D` (draws the game's `Canvas2D` through `android.graphics` Paint, Path and Typeface, via `nativeCanvas`) | `ComposeCanvas2D.kt`, created in `GameLoop`, `TitleScreen`, `Widgets` and `WhoIsPlaying` | **The main phase-2 job.** Its public surface (`draw(DrawScope, block)`) is already common, so make it an `expect class`. Keep the Android `actual` unchanged so Android's pixels stay the same. The iOS `actual` draws on Skia: `nativeCanvas` is `org.jetbrains.skia.Canvas`, with `Paint`, `Path` and `Font`/`TextLine` for text. |
| `LifecycleEventEffect` (ON_PAUSE pauses the game and saves bedtime time; ON_RESUME re-checks bedtime) | `GameLoop`, `Bedtime`, `RestScreen` | `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose`. On iOS these events follow the app resigning and becoming active. Verify that switching apps pauses the game. |
| `rememberSaveable` / `Saver` (the current screen, a game in progress, dialog state) | `App`, `GameScreen`, `FirstGameTips`, `GrownUps`, `RestScreen`, `BirthdaySetting`, `Removal`, `TogetherScreen` | Common (runtime-saveable). **Risk:** iOS doesn't restore state after it kills an app in the background, so the README's promise that "the game is still there when you come back" holds only on Android. Phase 2 would have to save `SavedRun` itself. |
| Window insets (`WindowInsets.safeDrawing`, `safeDrawingPadding`) | `GameScreen`, `Bedtime`, `TitleScreen`, others | Common. On iOS they come from the safe area (notch, home indicator). |
| `LocalWindowInfo.containerSize`, `LocalDensity`, `LocalLayoutDirection`, `BoxWithConstraints`, `FlowRow` | `roomForTwo()` in `TogetherScreen` and the layouts | Common. "Play together" shows on iPads and on large iPhones held sideways, by the same dp rule. |
| `Dialog` (`onDismissRequest` only), `KeyboardOptions` (Words capitalization, Number keyboard, Done action) | `BirthdaySetting`, `Removal`, `GrownUps`, `GameOverScreen`, `MySnailScreen` | Common. No Android-only options are used. |
| Material3 (`MaterialTheme`, `Typography`, `lightColorScheme`, `Button`, `TextButton`, `Surface`, `Switch`, `OutlinedTextField`, `Text`) | Everywhere | `org.jetbrains.compose.material3`. Everything used exists. |
| Fonts and emoji (default font family; text such as 🐣, 😴, ❤️) | `Theme`, the text | iOS uses the system font and Apple Color Emoji. The text metrics differ from Roboto, so iOS screenshots won't match Android's pixel for pixel. |
| Keep screen on, edge-to-edge, immersive system bars | `MainActivity` | `UIApplication.idleTimerDisabled = true`, plus `prefersStatusBarHidden` and `prefersHomeIndicatorAutoHidden` on the view controller hosting `ComposeUIViewController`. |
| Screen orientation | Manifest: no lock; `configChanges` keep the activity alive across rotation | `Info.plist`: allow every orientation on iPad, plus both landscapes on iPhone. |
| Resources (`R.string.app_name`, launcher icon, theme) | Manifest and `res/` only; the UI uses no resources | Name and icon go in the Xcode project. The icon is an Android vector drawable, so iOS needs an asset catalog of PNGs. |
| `android.util.Log`, `Handler`/`Looper`, threads | `SoundPlayer` only | Part of the iOS sound implementation. |

## Files

**Move to the shared module as they are.** These have common imports only:

- `KeyValueStore.kt`, `Storage.kt`, `SavedPlayer.kt`, `PlayClock.kt`,
  `PhoneCalendar.kt`, `SoundOutput.kt`
- `ui/`: `App`, `Bedtime`, `BirthdayCheer`, `BirthdaySetting`, `DeviceNotes`,
  `FirstGameTips`, `GameLoop`, `GameOverScreen`, `GameScreen`, `GrownUps`,
  `HighScoresScreen`, `KidColors`, `LittleKidMode`, `MySnailScreen`, `News`,
  `Profile`, `Removal`, `RestScreen`, `Settings`, `Theme`, `TitleScreen`,
  `TogetherOverScreen`, `TogetherScreen`, `TwoPlayerGame`, `TwoPlayerScreen`,
  `WhoIsPlaying`, `Widgets`

`GameLoop`, `GameScreen`, `TitleScreen`, `Widgets` and `WhoIsPlaying` compile
in common code only once `ComposeCanvas2D` is an `expect class`.

**Stay Android-only.** In phase 2 these go to `androidMain` or stay in `app`.
They are the only files with Android imports:

- `MainActivity.kt`: window flags and system bars; builds the services
- `SharedPreferencesStore.kt`: `KeyValueStore` on SharedPreferences, and `phoneStorage`
- `SystemCalendar.kt`: `java.time`
- `SoundPlayer.kt`: AudioTrack
- `ViewBuzzer.kt`: `HapticFeedbackConstants`
- `ui/SystemBack.kt`: `androidx.activity.compose.BackHandler`
- `ComposeCanvas2D.kt`: `android.graphics` (becomes the Android `actual`)
- `AndroidManifest.xml`, `res/`, `baseline-prof.txt`

**Tests.** The Robolectric and screenshot tests in `app/src/test` stay
Android-only. `SavedDataTest` pins the saved-data format: every key, read the
way earlier versions wrote it, and written back with the same types. `DayTest`
pins the bedtime timer's day count. The test fakes `NoSound` and `NoBuzz` are
in `ScreenTest.kt`.

## Dependencies phase 2 will need

- The Compose Multiplatform Gradle plugin, plus `org.jetbrains.compose.{runtime,foundation,ui,material3}`
- `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose`, for `LifecycleEventEffect`
- Optional: `kotlinx-datetime`, if the iOS `PhoneCalendar` uses it instead of `NSCalendar`
- Optional: a Compose Multiplatform back-handler artifact (verify the name), if iOS gets the edge-swipe back gesture

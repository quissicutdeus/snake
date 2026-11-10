# Snake Arena

A cartoony snake game for kids, made for our family's Pixel phones and tablets.
**No ads, no purchases, no accounts, no internet** — the app asks for no
permissions at all and works fully offline.

Grow by eating food dots, gobble up smaller snakes, and steer clear of bigger
ones. Your snake follows your finger.

## How to play

- **Steer**: touch and hold anywhere — your snake heads towards your finger.
  Let go and it keeps going straight.
- **Grow**: eat the food dots. Your **length** is your score.
- **Bigger = slower.** Small snakes are quick; big snakes are strong.
- **Faces tell you who's who**: a snake that looks *scared* is smaller than you
  (go get it!). A snake with an *angry face and a red outline* is bigger (run!).
  Smiling snakes are about your size — you just bounce off each other.
- **Bite a smaller snake's head** → you eat it whole and grow a lot.
- **Bite a smaller snake's body** → you snip its tail off. The tail falls apart
  into pieces anyone can eat.
- **Bump into a bigger snake** → you bounce back and lose a heart ❤️, then
  blink for a moment (you can't be hurt while blinking).
- **A bigger snake bites your body** → your tail falls off. Go back and eat it!
- Lose all 3 hearts and you get gobbled up. Game over — put your name on the
  high-score list!

**Power-ups** glow on the map (arrows at the screen edge point to nearby ones).
Only you can pick them up, and each lasts a few seconds — watch the bar under
your score:

| Power-up | Looks like | What it does |
|---|---|---|
| **Super Strength** | throbbing red ball with a barbell | You win every fight: eat or snip snakes *bigger* than you. Everyone gets scared and runs. |
| **Super Speed** | flickering yellow ball with a lightning bolt | Much faster and turns quicker, with a sparkle trail. |
| **Super Armor** | blue ball with a shield and ripples | Nothing can hurt, snip or eat you. Bigger snakes bounce off — *CLANG!* |

**My Snake** lets you pick colors, a pattern (stripes, polka dots, zigzag,
rainbow, checker), eyes and a hat. Everything is unlocked from the start.

## Install it on a Pixel

Needs **Android 16 or newer** (Pixel 6 and later).

1. On the phone, open the **[latest release](../../releases/latest)** and tap
   **`snake-arena.apk`** (or use the direct link:
   [`releases/latest/download/snake-arena.apk`](../../releases/latest/download/snake-arena.apk)).
2. Open the download. Android asks to allow installing apps from that source —
   allow it once.

Or with a cable and `adb`: `adb install -r snake-arena.apk`.

Snake Arena is still a **beta**: every push to `main` that passes
[CI](.github/workflows/build.yml) publishes a new [release](../../releases)
`v0.0.<build number>`; **Run workflow** on the
[Build workflow page](../../actions/workflows/build.yml) makes one on demand
from any branch. The build number is also the app's version code, and every
build is signed with the same key ([`app/debug.keystore`](app/debug.keystore),
committed on purpose; it is not secret and not for the Play Store), so a new
release **installs over the old one and keeps the high scores**. (A local
`0.0-dev` build can't replace a newer release; uninstall first.)

## Build it yourself

You need JDK 21 and the Android SDK with **platform 37.2** (Android 17).

```sh
./gradlew lintAll                    # every linter (see below)
./gradlew :game:test                 # game rules unit tests (incl. 10 000-step soak tests)
./gradlew :app:testDebugUnitTest     # renders every screen (phone portrait + landscape, tablet)
                                     #   to app/build/screenshots/
./gradlew :app:assembleRelease       # -> app/build/outputs/apk/release/app-release.apk
./gradlew :app:assembleDebug         # -> app/build/outputs/apk/debug/app-debug.apk
```

[CI](.github/workflows/build.yml) runs all of these on every push, keeps the
screenshots as a downloadable artifact and publishes the APK as a release.

Handy extras that need no phone:

```sh
./gradlew :game:renderPreviews   # PNGs of the real game art -> game/build/previews/
./gradlew :game:balanceReport    # bot-played games: survival time, score, size mix
```

Toolchain: Gradle 9.9, Android Gradle Plugin 9.5 (built-in Kotlin), Kotlin 2.4,
Jetpack Compose (BOM 2026.09), compileSdk 37.2 / targetSdk 37 / minSdk 36.
Versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml). The
app ID / namespace is `com.quissicutdeus.snake` (from snake.quissicutdeus.com).

## Linters

`./gradlew lintAll` runs all of them; any finding fails the build (and CI).

| Tool | Checks | Config |
|---|---|---|
| **ktlint** (via Spotless) | formatting and Kotlin style; `./gradlew spotlessApply` fixes most of it | [`.editorconfig`](.editorconfig) |
| **detekt** | code smells: complexity, long functions, magic numbers in game logic, unused private code, likely bugs | [`config/detekt/detekt.yml`](config/detekt/detekt.yml) |
| **Android Lint** | every check switched on, for the app *and* the pure-Kotlin game module, plus Slack's Compose rules | [`app/lint.xml`](app/lint.xml) |
| **Kotlin compiler** | all warnings are errors, with the extra K2 checks on (unused variables, redundant code…) | [`app/build.gradle.kts`](app/build.gradle.kts), [`game/build.gradle.kts`](game/build.gradle.kts) |
| **Dead code** (`./gradlew deadCode`) | R8 shrinks a special build without optimizing; anything it can drop is code the app never uses | [`config/deadcode-allowlist.txt`](config/deadcode-allowlist.txt) |

Every rule that is switched off or loosened is explained next to the setting.

## Tune the game

All gameplay numbers live in one file (detekt makes sure they stay there):
[`game/src/main/kotlin/com/quissicutdeus/snake/game/GameConfig.kt`](game/src/main/kotlin/com/quissicutdeus/snake/game/GameConfig.kt).
After a change, run `./gradlew :game:test :game:balanceReport` to see how games go.

| Want… | Change |
|---|---|
| Easier | more `PLAYER_HEARTS`, longer `HURT_INVULNERABLE_SECONDS`, lower `AI_AGGRESSION` or `AI_SPEED_FACTOR`, higher `SMALLER_FRACTION` |
| Harder | the opposite, or more `AI_TARGET_COUNT` |
| Faster growth | `PELLET_VALUE`, `PELLET_COUNT`, `EAT_WHOLE_GAIN` |
| Faster / slower snakes | `MAX_SPEED`, `SPEED_FLOOR_FRACTION`, `TURN_RATE` |
| Bigger arena | `ARENA_WIDTH`, `ARENA_HEIGHT` (and more `PELLET_COUNT`) |
| See more / less of the arena | `BASE_VIEW_SPAN` |
| Fatter / thinner snakes | `START_RADIUS`, `RADIUS_GROWTH_EXPONENT` |
| More / fewer power-ups | `POWER_UP_MAX`, `POWER_UP_INTERVAL`, `POWER_UP_LIFETIME` |
| Longer / shorter powers | `STRENGTH_SECONDS`, `SPEED_SECONDS`, `ARMOR_SECONDS`, `SPEED_POWER_FACTOR` |

## How it's built

```
game/   pure Kotlin, no Android: all rules, AI and drawing code
  model/   Snake (body follows the head's trail), food, SnakeStyle, scores
  sim/     GameSim (fixed 60 Hz steps), Movement, Collisions (spatial grid),
           Rules, PowerUps, Ai, Spawner, Scoreboard
  render/  Canvas2D (tiny drawing interface), SnakeArt / FaceArt / HatArt /
           FoodArt / PowerUpArt, SceneRenderer (arena) + HudRenderer, Camera,
           Fx (particles, comic words, shake), PosedSnake
  test/    unit + soak tests; Java2D Canvas2D for PNG previews
app/    Android app (Jetpack Compose)
  ComposeCanvas2D   draws the game art on the Compose canvas
  ui/               Title, Game (frame loop + finger steering + pause),
                    Game over ("Who played?"), High scores, My Snake
  Storage           snake look, top-10 scores and names (SharedPreferences)
```

The simulation runs at a fixed 60 steps per second; drawing blends between
the last two steps so motion stays smooth on 90/120 Hz Pixel screens. Because
the game logic and art don't depend on Android, an iOS version could reuse them
one day.

The original design notes are in [`PLAN.md`](PLAN.md). Ideas for later: sound
effects, a joystick option, levels, iOS, Play Store.

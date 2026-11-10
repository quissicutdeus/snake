# Snake Arena — kid-friendly snake game for Android

## Context

Mobile games for kids are full of microtransactions and paywalls. This is a
family-owned alternative: a cartoony snake game with **no ads, no purchases, no
accounts, no network permission, fully offline**. Android is the target; iOS is
only "maybe someday".

Decisions made with the owner:
- **Kotlin + Jetpack Compose** (plain Android, not Multiplatform). Game rules and
  drawing live in a pure-Kotlin module so an iOS port later stays possible.
- **Smooth slither movement** (free 360°, curvy bodies, open arena).
- **Follow-the-finger steering** (snake heads toward where the finger is).
- **Endless mode with hearts.**
- Snake **customization** and a **scoreboard** (details below).

## Game rules

- **Arena**: bounded world larger than the screen; camera follows the player.
  Walls gently bounce you (no damage).
- **Size = length.** Thickness grows with length so bigger snakes *look* bigger.
  Kid cue: snakes smaller than you have a scared face; bigger ones an angry face
  and a red outline.
- **Food pellets** are scattered around; eating them grows you.
- **Bigger = slower.** Speed falls smoothly as length grows (fast at minimum
  size, floor of roughly half that so big snakes never feel stuck). Applies to
  every snake: small ones can outrun big ones, so you can escape what hunts you
  and must corner what you hunt. Losing your tail makes you faster again.
- **Your head touches a smaller snake's head** → you eat it whole and grow.
- **Your head touches a smaller snake's body** → it is cut there. The front part
  survives (shorter) and flees; the tail falls off as loose pieces that stay on
  the ground and can be eaten by anyone.
- **Your head touches a bigger snake** (anywhere) → you bounce back, lose one
  heart, and blink invulnerable for ~1.5 s.
- **A bigger snake's head hits you** — mirrored: hits your body → your tail falls
  off (go back and re-eat it); hits your head → lose a heart and bounce.
  At 0 hearts you are eaten: game over.
- **Roughly equal size (within ~10%)** → both bounce, no damage.
- AI snakes follow the same rules against each other.
- **AI behaviour**: wander → seek food → chase smaller snakes nearby → flee
  bigger ones. Spawner keeps a mix around the player's size (~60% smaller,
  ~40% bigger), always spawning off-screen.
- **Session**: 3 hearts, score = longest length reached. Screens: Title → Play →
  Game Over (Play again), plus My Snake and High Scores from the title. Pause.

## Snake customization

"My Snake" screen with a live wiggling preview:
- **Color**: ~12 bright swatches for the body, plus a second color for patterns.
- **Pattern**: solid, stripes, polka dots, zigzag, rainbow, checker.
- **Face / extras**: eye style (googly, sleepy, sunglasses); hat (none, crown,
  party hat, bow).
- **Surprise me** button that randomizes everything.
- **Everything unlocked from the start** — no coins, unlocks or shop.
- Saved on device. AI snakes get random looks from the same options.

`SnakeStyle` is plain data in the `game` module; everything is drawn in code
(no art asset files).

## Scoreboard

Fully offline (no accounts, no online leaderboard):
- **High scores on the device**: top 10 runs with player name, score, snakes
  eaten, and a small picture of the snake used. At game over a qualifying score
  asks "Who played?" — tap a previously used name (siblings sharing a tablet) or
  type a new one. Reachable from the title; new entry highlighted. Reset sits
  behind a "hold for 3 seconds" button.
- **Live "Biggest snakes" list during play**: top 5 in the corner by length, your
  row highlighted. AI snakes get silly names (Noodle, Sir Hiss, Wiggles, …).
- Ranking/insert/trim logic lives in `game` and is unit-tested.

## Architecture

```
settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradlew
game/   pure Kotlin/JVM library — no Android or UI dependencies
  model/   Vec2, Snake (head, heading, path points, length, style), Pellet,
           TailPiece, World, SnakeStyle, ScoreEntry
  sim/     GameSim.step(dt, input) – fixed 60 Hz timestep, seeded RNG
           Movement (turn-rate-limited steering, body follows path, speed curve)
           Collisions (head-vs-head / head-vs-body, spatial grid)
           Rules (eat, sever tail, bounce + damage, invulnerability)
           Ai, Spawner, Scoreboard (ranking)
  render/  SnakeArt – draws snakes/pellets/HUD bits against a tiny `Canvas2D`
           interface (circle, path, fill, stroke, text). Shared by the app and
           by the headless preview tool, so the real art can be checked as PNGs.
  GameConfig – all tunables in one place
  test/    unit tests + soak test
  preview  (JVM main / test helper) – Java2D implementation of Canvas2D that
           writes PNG frames for visual checks without a phone
app/    Android application, Jetpack Compose
  ComposeCanvas2D (adapter: Canvas2D → DrawScope), GameScreen (Canvas +
  withFrameNanos loop + pointer input), Camera, Hud, Title / MySnake /
  HighScores / GameOver screens, storage via DataStore or SharedPreferences.
  MainActivity: immersive, keep screen on. Manifest with NO permissions.
.github/workflows/build.yml – run game tests, build debug APK, upload artifact
README.md – how to build, install on a phone, tune the game
```

## Toolchain (checked 2026-11-10)

- **AGP 9.2.x** (Compose 1.12 needs AGP ≥ 9.2.0 and compileSdk 37). AGP 9 has
  built-in Kotlin: do not apply `org.jetbrains.kotlin.android`; still apply
  `org.jetbrains.kotlin.plugin.compose`. To pin a newer Kotlin, declare
  `org.jetbrains.kotlin.jvm` with `apply false` in the root build.
- **Compose BOM 2026.08.00**, compileSdk/targetSdk 37, minSdk 26.
- **Kotlin 2.4.21** (latest on Maven Central).
- **Gradle 9.x** wrapper (9.8.2 / 9.9.0 are out; pick one AGP 9.2 supports).
- JDK 21 is installed in the cloud container.
- Re-check these versions when the session starts and the hosts below are reachable.

## Environment requirement

The Android build needs **`dl.google.com`** and **`maven.google.com`** (Google
Maven: AGP, AndroidX; plus the Android SDK). The Default cloud environment
blocks them. To build in the cloud session, allow both hosts (environment menu
→ Edit → Network access → Allowed domains, keep "Allow package managers"
ticked). Without them only the `game` module builds locally, and the APK is
built by GitHub Actions.

If the hosts are allowed, a session should install the Android command-line
tools + `platforms;android-37` + build-tools, set `ANDROID_HOME`, and build the
APK locally.

## Milestones

1. **Scaffold** — Gradle project, `game` + `app` modules, wrapper,
   `.gitignore`, CI workflow.
2. **Core sim + tests** — movement, speed curve, pellets, growth, walls.
3. **Snake-vs-snake rules + tests** — eat, sever tail into pieces,
   bounce/damage, invulnerability, equal-size bounce, game over.
4. **AI + spawner + tests**.
5. **Rendering + input** — `SnakeArt`, Compose adapter, camera,
   follow-finger steering, HUD. PNG previews via the Java2D tool.
6. **Customization** — `SnakeStyle`, patterns/eyes/hats, My Snake screen.
7. **Scoreboard** — saved top 10, "Who played?" picker, high-score screen,
   live top-5 HUD list.
8. **Screens + polish** — title, pause, game over; juice (squash on eat, blink
   when hurt, pop particles, screen shake on bounce).
9. **Android packaging** — icon, manifest, debug APK via CI; README.

Later: sound effects, joystick option, levels, iOS, Play Store signing.

## Verification

- `./gradlew :game:test` — unit tests for every rule (e.g. "head hits smaller
  body at segment k → victim keeps k, the rest become tail pieces"; "hit bigger →
  heart −1, bounce, invulnerable"; "speed strictly decreases with length, never
  below the floor"; scoreboard keeps top 10 sorted), plus a 10 000-step AI-only
  soak test asserting no crashes or NaNs.
- PNG previews from the Java2D renderer (mid-game, severed tail, each pattern
  and hat) inspected by eye.
- `./gradlew :app:assembleDebug` locally if Google hosts are allowed; otherwise
  on GitHub Actions (check the run result and logs).
- Real-device check (install the APK on a phone and play) is done by the owner.
- Commit per milestone; push to the working branch. No PR unless asked.

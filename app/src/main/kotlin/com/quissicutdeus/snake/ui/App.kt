package com.quissicutdeus.snake.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.quissicutdeus.snake.Storage
import com.quissicutdeus.snake.game.audio.Sound
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.Unlock
import com.quissicutdeus.snake.game.sim.Eater
import com.quissicutdeus.snake.game.sim.Scoreboard

/** Result of one game, handed from the play screen to the game-over screen. */
data class GameResult(
    val score: Int,
    val snakesEaten: Int,
    val style: SnakeStyle,
    val timestamp: Long,
    /** The snake that ate the player (null if unknown, e.g. a game saved by an older version). */
    val eater: Eater? = null,
    /** Whole seconds the player was the biggest snake in the arena. */
    val biggestSeconds: Int = 0,
)

sealed interface Screen {
    data object Title : Screen

    data class Play(
        val round: Int,
    ) : Screen

    data class GameOver(
        val result: GameResult,
        /** Looks this game unlocked (not saved with the screen: the cheer is shown once). */
        val unlocked: List<Unlock> = emptyList(),
    ) : Screen

    data object MySnake : Screen

    data class HighScores(
        val highlight: Long? = null,
    ) : Screen
}

/** Saves the current [Screen] as a short text (see [SniddleApp]). */
private val ScreenSaver: Saver<Screen, String> =
    Saver(
        save = { screen ->
            when (screen) {
                Screen.Title -> {
                    "title"
                }

                is Screen.Play -> {
                    "play|${screen.round}"
                }

                is Screen.GameOver -> {
                    with(screen.result) {
                        val by = eater?.let { "|${it.style.encode()}|${it.name}" } ?: ""
                        "over|$score|$snakesEaten|$timestamp|$biggestSeconds|${style.encode()}$by"
                    }
                }

                Screen.MySnake -> {
                    "mysnake"
                }

                is Screen.HighScores -> {
                    "scores|${screen.highlight ?: ""}"
                }
            }
        },
        restore = { text -> decodeScreen(text) },
    )

private fun decodeScreen(text: String): Screen? {
    // The eater's name is last and may contain '|', so split at most 8 ways.
    val p = text.split('|', limit = 8)
    return when (p[0]) {
        "title" -> {
            Screen.Title
        }

        "play" -> {
            p.getOrNull(1)?.toIntOrNull()?.let { Screen.Play(it) }
        }

        "over" -> {
            gameOverAt(p)
        }

        "mysnake" -> {
            Screen.MySnake
        }

        "scores" -> {
            Screen.HighScores(p.getOrNull(1)?.toLongOrNull())
        }

        else -> {
            null
        }
    }
}

/** "over|score|eaten|time|biggest|style[|eaterStyle|eaterName]", already split; null if unreadable. */
private fun gameOverAt(p: List<String>): Screen? {
    val score = p.getOrNull(1)?.toIntOrNull() ?: return null
    val eaten = p.getOrNull(2)?.toIntOrNull() ?: return null
    val time = p.getOrNull(3)?.toLongOrNull() ?: return null
    val biggest = p.getOrNull(4)?.toIntOrNull() ?: return null
    return Screen.GameOver(GameResult(score, eaten, SnakeStyle.decode(p.getOrNull(5)), time, eaterAt(p), biggest))
}

/** The eater saved after the result (fields 6 and 7), if any. */
private fun eaterAt(p: List<String>): Eater? {
    val style = p.getOrNull(6) ?: return null
    val name = p.getOrNull(7) ?: return null
    return Eater(name, SnakeStyle.decode(style))
}

/**
 * Top level: a tiny screen switcher (no navigation library needed). [playSound]
 * plays an effect. The screen is saved with the activity, so after Android
 * closes the app in the background the kid comes back to it (a game, paused).
 */
@Composable
fun SniddleApp(
    storage: Storage,
    playSound: (Sound) -> Unit,
    playMusic: (Boolean) -> Unit = {},
) {
    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Title) }
    val me = remember { Profile(storage) }
    var round by rememberSaveable { mutableIntStateOf(0) }

    fun play() {
        me.dismissAlreadyEarned()
        round++
        screen = Screen.Play(round)
    }

    SniddleTheme {
        when (val s = screen) {
            Screen.Title -> {
                TitleRoute(
                    me,
                    onPlay = ::play,
                    onMySnake = { screen = Screen.MySnake },
                    onAddPlayer = { if (me.addPlayer()) screen = Screen.MySnake },
                    onHighScores = { screen = Screen.HighScores() },
                )
            }

            is Screen.Play -> {
                GameScreen(
                    key = s.round,
                    style = me.style,
                    playerName = Scoreboard.snakeName(me.snakeName),
                    onGameOver = { screen = Screen.GameOver(it, me.recordGame(it)) },
                    onQuit = { screen = Screen.Title },
                    settings = me.settings,
                    onSettingsChange = me::changeSettings,
                    onSound = { if (me.settings.soundOn) playSound(it) },
                    onMusic = { playMusic(it && me.settings.soundOn && me.settings.musicOn) },
                )
            }

            is Screen.GameOver -> {
                GameOverScreen(
                    result = s.result,
                    unlocked = s.unlocked,
                    storage = storage,
                    playerName = me.snakeName,
                    onSaved = { screen = Screen.HighScores(highlight = it) },
                    onPlayAgain = ::play,
                    onHome = { screen = Screen.Title },
                )
            }

            Screen.MySnake -> {
                MySnakeRoute(me, storage, onDone = { screen = Screen.Title })
            }

            is Screen.HighScores -> {
                HighScoresScreen(
                    storage = storage,
                    highlight = s.highlight,
                    onPlay = ::play,
                    onBack = { screen = Screen.Title },
                )
            }
        }
        // Back always leads home (the play screen handles back itself to pause).
        BackHandler(enabled = screen !is Screen.Title && screen !is Screen.Play) { screen = Screen.Title }
    }
}

/**
 * The title screen, fed from the [Profile]: look, settings, who's playing
 * and any looks earned without a cheer.
 */
@Composable
private fun TitleRoute(
    me: Profile,
    onPlay: () -> Unit,
    onMySnake: () -> Unit,
    onAddPlayer: () -> Unit,
    onHighScores: () -> Unit,
) {
    TitleScreen(
        style = me.style,
        onPlay = onPlay,
        onMySnake = onMySnake,
        onHighScores = onHighScores,
        settings = me.settings,
        onSettingsChange = me::changeSettings,
        content = {
            WhoIsPlaying(me.players, me.playerId, me::switchTo, onAddPlayer)
            AlreadyEarnedBanner(me.style, me.alreadyEarned, me::alreadyEarnedShown, me::dismissAlreadyEarned)
        },
    )
}

/** My Snake, fed from the [Profile]: look, name (with names from the high scores) and unlocks. */
@Composable
private fun MySnakeRoute(
    me: Profile,
    storage: Storage,
    onDone: () -> Unit,
) {
    MySnakeScreen(
        style = me.style,
        onChange = me::changeStyle,
        onDone = onDone,
        name = me.snakeName,
        onNameChange = me::changeSnakeName,
        knownNames = remember { storage.loadNames() },
        stats = me.stats,
    )
}

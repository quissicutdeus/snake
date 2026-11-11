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
import com.quissicutdeus.snake.game.sim.Eater

/** Result of one game, handed from the play screen to the game-over screen. */
data class GameResult(
    val score: Int,
    val snakesEaten: Int,
    val style: SnakeStyle,
    val timestamp: Long,
    /** The snake that ate the player (null if unknown, e.g. a game saved by an older version). */
    val eater: Eater? = null,
)

sealed interface Screen {
    data object Title : Screen

    data class Play(
        val round: Int,
    ) : Screen

    data class GameOver(
        val result: GameResult,
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
                        "over|$score|$snakesEaten|$timestamp|${style.encode()}$by"
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
    // The eater's name is last and may contain '|', so split at most 7 ways.
    val p = text.split('|', limit = 7)
    return when (p[0]) {
        "title" -> {
            Screen.Title
        }

        "play" -> {
            p.getOrNull(1)?.toIntOrNull()?.let { Screen.Play(it) }
        }

        "over" -> {
            val score = p.getOrNull(1)?.toIntOrNull()
            val eaten = p.getOrNull(2)?.toIntOrNull()
            val time = p.getOrNull(3)?.toLongOrNull()
            if (score == null || eaten == null || time == null) {
                null
            } else {
                Screen.GameOver(GameResult(score, eaten, SnakeStyle.decode(p.getOrNull(4)), time, eaterAt(p)))
            }
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

/** The eater saved after the result (fields 5 and 6), if any: older saves have none. */
private fun eaterAt(p: List<String>): Eater? {
    val style = p.getOrNull(5) ?: return null
    val name = p.getOrNull(6) ?: return null
    return Eater(name, SnakeStyle.decode(style))
}

/** Top level: a tiny screen switcher (no navigation library needed). [playSound] plays an effect. */
@Composable
fun SniddleApp(
    storage: Storage,
    playSound: (Sound) -> Unit,
) {
    // Saved with the activity, so after Android closes the app in the background
    // the kid comes back to the same screen (a game comes back paused).
    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Title) }
    var settings by remember { mutableStateOf(storage.loadSettings()) }
    var style by remember { mutableStateOf(storage.loadStyle()) }
    var round by rememberSaveable { mutableIntStateOf(0) }

    fun changeSettings(new: Settings) {
        settings = new
        storage.saveSettings(new)
    }

    fun play() {
        round++
        screen = Screen.Play(round)
    }

    SniddleTheme {
        when (val s = screen) {
            Screen.Title -> {
                TitleScreen(
                    style = style,
                    onPlay = ::play,
                    onMySnake = { screen = Screen.MySnake },
                    onHighScores = { screen = Screen.HighScores() },
                    settings = settings,
                    onSettingsChange = ::changeSettings,
                )
            }

            is Screen.Play -> {
                GameScreen(
                    key = s.round,
                    style = style,
                    onGameOver = { screen = Screen.GameOver(it) },
                    onQuit = { screen = Screen.Title },
                    settings = settings,
                    onSettingsChange = ::changeSettings,
                    onSound = { if (settings.soundOn) playSound(it) },
                )
            }

            is Screen.GameOver -> {
                GameOverScreen(
                    result = s.result,
                    storage = storage,
                    onSaved = { screen = Screen.HighScores(highlight = it) },
                    onPlayAgain = ::play,
                    onHome = { screen = Screen.Title },
                )
            }

            Screen.MySnake -> {
                MySnakeScreen(
                    style = style,
                    onChange = {
                        style = it
                        storage.saveStyle(it)
                    },
                    onDone = { screen = Screen.Title },
                )
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

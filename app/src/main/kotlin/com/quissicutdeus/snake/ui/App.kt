package com.quissicutdeus.snake.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.quissicutdeus.snake.Storage
import com.quissicutdeus.snake.game.audio.Sound
import com.quissicutdeus.snake.game.model.SnakeStyle

/** Result of one game, handed from the play screen to the game-over screen. */
data class GameResult(
    val score: Int,
    val snakesEaten: Int,
    val style: SnakeStyle,
    val timestamp: Long,
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

/** Top level: a tiny screen switcher (no navigation library needed). [playSound] plays an effect. */
@Composable
fun SniddleApp(
    storage: Storage,
    playSound: (Sound) -> Unit,
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Title) }
    var settings by remember { mutableStateOf(storage.loadSettings()) }
    var style by remember { mutableStateOf(storage.loadStyle()) }
    var round by remember { mutableIntStateOf(0) }

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

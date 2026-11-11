package com.quissicutdeus.sniddle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.quissicutdeus.sniddle.Storage
import com.quissicutdeus.sniddle.game.audio.Sound
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.Unlock
import com.quissicutdeus.sniddle.game.model.Unlocks
import com.quissicutdeus.sniddle.game.sim.Eater
import com.quissicutdeus.sniddle.game.sim.Scoreboard
import java.time.LocalDate

/** Result of one game, handed from the play screen to the game-over screen. */
data class GameResult(
    val score: Int,
    val snailsEaten: Int,
    val style: SnailStyle,
    val timestamp: Long,
    /** The snail that ate the player (null if unknown, e.g. a game saved by an older version). */
    val eater: Eater? = null,
    /** Whole seconds the player was the biggest snail in the arena. */
    val biggestSeconds: Int = 0,
    /** Played in little kid mode (its high score gets a 🐣). */
    val littleKid: Boolean = false,
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

    data object MySnail : Screen

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
                        "over|$score|$snailsEaten|$timestamp|$biggestSeconds|${if (littleKid) 1 else 0}|${style.encode()}$by"
                    }
                }

                Screen.MySnail -> {
                    "mysnail"
                }

                is Screen.HighScores -> {
                    "scores|${screen.highlight ?: ""}"
                }

                // Deliberately not carried on: a two-player game comes back as the title screen.
                is TogetherScreen -> {
                    "title"
                }
            }
        },
        restore = { text -> decodeScreen(text) },
    )

private fun decodeScreen(text: String): Screen? {
    // The eater's name is last and may contain '|', so split at most 9 ways.
    val p = text.split('|', limit = 9)
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

        "mysnail" -> {
            Screen.MySnail
        }

        "scores" -> {
            Screen.HighScores(p.getOrNull(1)?.toLongOrNull())
        }

        else -> {
            null
        }
    }
}

/** "over|score|eaten|time|biggest|little|style[|eaterStyle|eaterName]", already split; null if unreadable. */
private fun gameOverAt(p: List<String>): Screen? {
    val score = p.getOrNull(1)?.toIntOrNull() ?: return null
    val eaten = p.getOrNull(2)?.toIntOrNull() ?: return null
    val time = p.getOrNull(3)?.toLongOrNull() ?: return null
    val biggest = p.getOrNull(4)?.toIntOrNull() ?: return null
    val little = p.getOrNull(5) == "1"
    return Screen.GameOver(GameResult(score, eaten, SnailStyle.decode(p.getOrNull(6)), time, eaterAt(p), biggest, little))
}

/** The eater saved after the result (fields 7 and 8), if any. */
private fun eaterAt(p: List<String>): Eater? {
    val style = p.getOrNull(7) ?: return null
    val name = p.getOrNull(8) ?: return null
    return Eater(name, SnailStyle.decode(style))
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
    /** A fixed date for tests to play on; null for today's. */
    today: (() -> LocalDate)? = null,
) {
    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Title) }
    val me = remember { if (today == null) Profile(storage) else Profile(storage, today) }
    val notes = remember { DeviceNotes(storage) }
    val bedtime = remember { Bedtime(storage, me) }
    var round by rememberSaveable { mutableIntStateOf(0) }

    fun play() {
        me.dismissAlreadyEarned()
        notes.dismissNews()
        round++
        screen = if (bedtime.refresh()) Screen.Title else Screen.Play(round)
    }

    SniddleTheme {
        when (val s = screen) {
            Screen.Title -> {
                TitleRoute(
                    me,
                    notes,
                    bedtime,
                    onPlay = ::play,
                    onMySnail = { screen = Screen.MySnail },
                    onAddPlayer = { if (me.addPlayer()) screen = Screen.MySnail },
                    go = { screen = it },
                )
            }

            is TogetherScreen -> {
                TogetherRoute(s, Household(me, storage, notes, bedtime), Speakers(playSound, playMusic), { ++round }) { screen = it }
            }

            is Screen.Play -> {
                PlayRoute(
                    s.round,
                    me,
                    bedtime,
                    Speakers(playSound, playMusic),
                    onGameOver = { screen = Screen.GameOver(it, me.recordGame(it)) },
                    onHome = { screen = Screen.Title },
                )
            }

            is Screen.GameOver -> {
                GameOverScreen(
                    result = s.result,
                    unlocked = s.unlocked,
                    storage = storage,
                    playerName = me.snailName,
                    onSaved = { screen = Screen.HighScores(highlight = it) },
                    onPlayAgain = ::play,
                    onHome = { screen = Screen.Title },
                )
            }

            Screen.MySnail -> {
                MySnailRoute(me, storage, onDone = { screen = Screen.Title })
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

/** Where the game's sound effects and music go. */
internal class Speakers(
    val playSound: (Sound) -> Unit,
    val playMusic: (Boolean) -> Unit,
)

/** A one-player game for the kid playing, in their look and mode, with their tips and the bedtime timer. */
@Composable
private fun PlayRoute(
    round: Int,
    me: Profile,
    bedtime: Bedtime,
    speakers: Speakers,
    onGameOver: (GameResult) -> Unit,
    onHome: () -> Unit,
) {
    GameScreen(
        key = round,
        style = me.style,
        playerName = Scoreboard.snailName(me.snailName),
        littleKid = me.littleKid,
        onGameOver = onGameOver,
        onQuit = onHome,
        settings = me.settings,
        onSettingsChange = me::changeSettings,
        onSound = { if (me.settings.soundOn) speakers.playSound(it) },
        onMusic = { speakers.playMusic(it && me.settings.soundOn && me.settings.musicOn) },
        showTips = remember(round) { tipsDue(me.stats) },
        bedtime = bedtime.forGame(onHome),
        options = remember(round) { me.todaysOptions() },
    )
}

/**
 * The title screen, fed from the [Profile]: look, settings, who's playing
 * and any looks earned without a cheer. One card at a time: "What's new"
 * first, then (once that's tapped away, or on the next visit) the
 * already-earned looks. Past [bedtime], the rest screen instead.
 */
@Composable
private fun TitleRoute(
    me: Profile,
    notes: DeviceNotes,
    bedtime: Bedtime,
    onPlay: () -> Unit,
    onMySnail: () -> Unit,
    onAddPlayer: () -> Unit,
    /** Opens another screen (High scores, Play together). */
    go: (Screen) -> Unit,
) = BedtimeHome(bedtime, me.style, onMySnail, onHighScores = { go(Screen.HighScores()) }) {
    TitleScreen(
        style = me.style,
        onPlay = onPlay,
        onPlayTogether = { go(if (bedtime.refresh()) Screen.Title else TogetherScreen.Pick) }.takeIf { roomForTwo() },
        onMySnail = onMySnail,
        onHighScores = { go(Screen.HighScores()) },
        settings = me.settings,
        onSettingsChange = me::changeSettings,
        arena = remember { me.todaysOptions().arena },
        content = {
            WhoIsPlaying(me.players, me.playerId, me::switchTo, onAddPlayer)
            if (me.isBirthday) HappyBirthday(me.snailName, Unlocks.isUnlocked(Hat.BIRTHDAY_CAKE, me.stats))
            if (notes.news.isNotEmpty()) {
                WhatsNewCard(notes.news, notes::newsShown, notes::dismissNews)
            } else {
                AlreadyEarnedBanner(me.style, me.alreadyEarned, me::alreadyEarnedShown, me::dismissAlreadyEarned)
            }
        },
    )
    // Over everything, each time the birthday kid is picked.
    if (me.isBirthday) BirthdayConfetti(me.playerId, Modifier.fillMaxSize())
}

/**
 * My Snail, fed from the [Profile]: look, name (with names from the high
 * scores), unlocks, and removing this kid (back to the title after).
 */
@Composable
internal fun MySnailRoute(
    me: Profile,
    storage: Storage,
    onDone: () -> Unit,
) {
    MySnailScreen(
        style = me.style,
        onChange = me::changeStyle,
        onDone = onDone,
        name = me.snailName,
        onNameChange = me::changeSnailName,
        knownNames = remember { storage.loadNames() },
        stats = me.stats,
        littleKid = LittleKidMode(me.littleKid, me::changeLittleKid),
        birthday = BirthdaySetting(me.birthday, me::changeBirthday),
        removal =
            me.players.takeIf { it.size > 1 }?.firstOrNull { it.id == me.playerId }?.let { p ->
                Removal(p.label) {
                    me.removePlayer(p.id)
                    onDone()
                }
            },
    )
}

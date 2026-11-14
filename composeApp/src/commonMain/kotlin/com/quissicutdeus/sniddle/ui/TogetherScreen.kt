package com.quissicutdeus.sniddle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quissicutdeus.sniddle.Storage
import com.quissicutdeus.sniddle.game.audio.Sound
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.game.model.ScoreEntry
import com.quissicutdeus.sniddle.game.model.Stats
import com.quissicutdeus.sniddle.game.model.Unlock
import com.quissicutdeus.sniddle.game.model.Unlocks
import com.quissicutdeus.sniddle.game.sim.Scoreboard

/**
 * The two-player screens, a branch of [Screen]. They aren't saved with the
 * activity: if Android closes the app in the background, the kids come back
 * to the title screen (one-player games carry on; see SavedRun).
 */
sealed interface TogetherScreen : Screen {
    /** "Who's playing with you?" */
    data object Pick : TogetherScreen

    /** A new kid, added from [Pick], names and dresses their snail, then back to [Pick]. */
    data object AddKid : TogetherScreen

    data class Play(
        val round: Int,
        /** Left kid first. */
        val kids: List<PlayerCard>,
    ) : TogetherScreen

    data class Over(
        val result: TogetherResult,
        /** Looks each kid unlocked, in the same order. */
        val unlocked: List<List<Unlock>>,
    ) : TogetherScreen
}

/** Each half needs room to play: tablets, and big phones held sideways (Material's "expanded" width). */
private val TwoPlayerMinWidth = 840.dp
private val TwoPlayerMinHeight = 400.dp

/** The id of a "Player 2" who isn't one of the kids on this phone. */
const val GUEST_ID = -1

/** True when the window is wide enough for two halves to be playable. */
@Composable
internal fun roomForTwo(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) { size.width.toDp() >= TwoPlayerMinWidth && size.height.toDp() >= TwoPlayerMinHeight }
}

/** What the two-player screens need from the app: the kid picked, saved data, the news card and the bedtime timer. */
internal class Household(
    val me: Profile,
    val storage: Storage,
    val notes: DeviceNotes,
    val bedtime: Bedtime,
)

/**
 * The two-player screens, fed from the [Household]; [go] moves to another
 * screen and [nextRound] numbers a new game. A game starts like a
 * one-player one: the title's cards are put away, and past bedtime it goes
 * home (to the rest screen) instead.
 */
@Composable
internal fun TogetherRoute(
    screen: TogetherScreen,
    home: Household,
    speakers: Speakers,
    nextRound: () -> Int,
    go: (Screen) -> Unit,
) {
    val me = home.me

    fun start(kids: List<PlayerCard>) {
        me.dismissAlreadyEarned()
        home.notes.dismissNews()
        go(if (home.bedtime.refresh()) Screen.Title else TogetherScreen.Play(nextRound(), kids))
    }
    when (screen) {
        TogetherScreen.Pick -> {
            PickPartnerRoute(me, onGo = ::start, onAdd = { if (me.addPlayer()) go(TogetherScreen.AddKid) }, onBack = { go(Screen.Title) })
        }

        TogetherScreen.AddKid -> {
            MySnailRoute(me, home.storage, onDone = { go(TogetherScreen.Pick) })
        }

        is TogetherScreen.Play -> {
            TogetherPlayRoute(screen, home, speakers, onGameOver = { go(TogetherScreen.Over(it, recordTogether(me, home.storage, it))) }) {
                go(Screen.Title)
            }
        }

        is TogetherScreen.Over -> {
            TogetherOverScreen(
                result = screen.result,
                unlocked = screen.unlocked,
                onPlayAgain = { start(screen.result.kids.map { PlayerCard(it.playerId, it.name, it.style) }) },
                onHome = { go(Screen.Title) },
            )
        }
    }
}

/** A two-player game: today's options for both kids, the bedtime timer, and the shared sound switches. */
@Composable
private fun TogetherPlayRoute(
    screen: TogetherScreen.Play,
    home: Household,
    speakers: Speakers,
    onGameOver: (TogetherResult) -> Unit,
    onHome: () -> Unit,
) {
    val me = home.me
    TwoPlayerScreen(
        key = screen.round,
        kids = screen.kids,
        onGameOver = onGameOver,
        onQuit = onHome,
        settings = me.settings,
        onSettingsChange = me::changeSettings,
        onSound = { if (me.settings.soundOn) speakers.sound.play(it) },
        onMusic = { speakers.sound.music(it && me.settings.soundOn && me.settings.musicOn) },
        onBuzz = speakers.buzzer::buzz,
        options = remember(screen.round) { me.togetherOptions(screen.kids[1].id) },
        bedtime = home.bedtime.forTogether(record = { recordTogether(me, home.storage, it) }, onStopped = onHome),
    )
}

/**
 * Counts the game for each kid on this phone (unlocks), and puts each kid on
 * the shared high-score list if they made it, under their name: there's no
 * "Who played?" to ask, the game knows. Returns each kid's new looks.
 */
fun recordTogether(
    me: Profile,
    storage: Storage,
    result: TogetherResult,
    /** The holidays on; null for today's, with each kid's own birthday. */
    season: Set<Holiday>? = null,
): List<List<Unlock>> {
    val unlocked = result.kids.map { me.recordGameFor(it.playerId, it.asGameResult(result), season) }
    result.kids.forEachIndexed { i, kid ->
        if (Scoreboard.qualifies(storage.loadScores(), kid.score)) {
            // Each entry needs its own timestamp (it identifies the entry).
            val name = Scoreboard.cleanName(kid.name)
            storage.addScore(ScoreEntry(name, kid.score, kid.snailsEaten, kid.style, result.timestamp + i, result.littleKid))
        }
    }
    return unlocked
}

/**
 * The kid picked on the title screen plays on the left; this picks who plays
 * on the right, starting with the next kid. With only one kid on the phone,
 * a "Player 2" with a random look can play, or a new kid can be added.
 */
@Composable
private fun PickPartnerRoute(
    me: Profile,
    onGo: (List<PlayerCard>) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
) {
    val first = me.players.firstOrNull { it.id == me.playerId } ?: PlayerCard(me.playerId, "Player 1", me.style)
    val guest = remember { PlayerCard(GUEST_ID, "Player 2", Unlocks.randomStyle(kotlin.random.Random.Default, Stats())) }
    val others = me.players.filter { it.id != me.playerId }.ifEmpty { listOf(guest) }
    val next = me.players.indexOfFirst { it.id == me.playerId }.let { i -> me.players.drop(i + 1).firstOrNull() ?: me.players.first() }
    var picked by rememberSaveable { mutableIntStateOf(next.takeIf { it.id != me.playerId }?.id ?: GUEST_ID) }
    PickPartnerScreen(
        first = first,
        others = others,
        picked = picked,
        onPick = { picked = it },
        canAdd = me.players.size < Storage.MAX_PLAYERS,
        onAdd = onAdd,
        onGo = { onGo(listOf(first, others.firstOrNull { it.id == picked } ?: others.first())) },
        onBack = onBack,
    )
}

@Composable
private fun PickPartnerScreen(
    first: PlayerCard,
    others: List<PlayerCard>,
    picked: Int,
    onPick: (Int) -> Unit,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onGo: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(KidColors.MenuBackground)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShadowText("Play together!", MaterialTheme.typography.displayLarge)
            ShadowText("Who's playing with ${first.label}?", MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                others.forEach { kid -> KidCard(kid, kid.id == picked, onClick = { onPick(kid.id) }) }
                if (canAdd) AddKidCard(onAdd)
            }
            Spacer(Modifier.height(16.dp))
            val partner = others.firstOrNull { it.id == picked } ?: others.first()
            ShadowText("${first.label} on the left, ${partner.label} on the right", MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                KidButton("Go!", onGo)
                KidButton("Back", onBack, color = KidColors.Grape, big = false)
            }
        }
    }
}

private val CardSize = 132.dp

/** A kid's snail and name to tap; the one picked has a yellow ring. */
@Composable
private fun KidCard(
    kid: PlayerCard,
    isPicked: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(CardSize)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isPicked) KidColors.Card else KidColors.CardFaint)
            .border(if (isPicked) 5.dp else 2.dp, if (isPicked) KidColors.Sunny else KidColors.OutlineFaint, RoundedCornerShape(24.dp))
            .semantics {
                selected = isPicked
                contentDescription = "Play with ${kid.label}"
            }.clickable(role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SnailPicture(kid.style, Modifier.size(CardSize - 30.dp))
        Text(
            kid.label,
            style = MaterialTheme.typography.titleMedium,
            color = KidColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "+ Add player": a new kid names and dresses their snail, then comes back here. */
@Composable
private fun AddKidCard(onClick: () -> Unit) {
    Column(
        Modifier
            .width(CardSize)
            .clip(RoundedCornerShape(24.dp))
            .background(KidColors.Sky)
            .semantics { contentDescription = "Add a player" }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(CardSize - 30.dp), contentAlignment = Alignment.Center) {
            Text("+", style = MaterialTheme.typography.displayLarge, color = Color.White)
        }
        Text("Add player", style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
    }
}

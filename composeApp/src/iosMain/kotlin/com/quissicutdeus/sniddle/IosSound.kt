package com.quissicutdeus.sniddle

import com.quissicutdeus.sniddle.game.audio.Music
import com.quissicutdeus.sniddle.game.audio.Sound
import com.quissicutdeus.sniddle.game.audio.Synth
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEngineConfigurationChangeNotification
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatFloat32
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeBufferInterrupts
import platform.AVFAudio.AVAudioPlayerNodeBufferLoops
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.setActive
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

/**
 * iOS's [SoundOutput], the counterpart of Android's SoundPlayer: the same
 * synthesized samples, played through an AVAudioEngine. Each sound has its
 * own player node with its buffer made in advance, so starting one is instant,
 * different sounds overlap, and a sound asked for again starts over. The music
 * loops on a node of its own. No Kotlin runs on the audio thread: the engine
 * only plays the prepared buffers.
 *
 * The audio session is "ambient": the Silent switch and screen lock silence the
 * game, and it mixes with other apps' audio (docs/ios.md). It needs no
 * permission.
 *
 * The samples are made on a background queue at startup; a sound asked for
 * before then is just skipped, as on Android. Sound is never worth ending a
 * game over: if the engine can't start, the game plays silently, and it tries
 * again after an interruption (a call, a timer) or a change of output
 * (headphones in or out). Everything else runs on the main thread.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSound : SoundOutput {
    private val engine = AVAudioEngine()
    private val format =
        AVAudioFormat(
            commonFormat = AVAudioPCMFormatFloat32,
            sampleRate = Synth.SAMPLE_RATE.toDouble(),
            channels = 1u,
            interleaved = false,
        )
    private var players: Map<Sound, Pair<AVAudioPlayerNode, AVAudioPCMBuffer>>? = null
    private var music: Pair<AVAudioPlayerNode, AVAudioPCMBuffer>? = null

    /** Whether music was asked for; remembered so a request before loading finishes still counts. */
    private var musicWanted = false

    init {
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryAmbient, error = null)
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            val effects = Sound.entries.associateWith { Synth.render(it) }
            val tune = Music.render()
            dispatch_async(dispatch_get_main_queue()) { install(effects, tune) }
        }
        val center = NSNotificationCenter.defaultCenter
        val main = NSOperationQueue.mainQueue
        // A call or a timer stops the engine; so does a change of output. Start it again when allowed.
        center.addObserverForName(AVAudioSessionInterruptionNotification, `object` = null, queue = main) { _ -> restart() }
        center.addObserverForName(AVAudioEngineConfigurationChangeNotification, `object` = engine, queue = main) { _ -> restart() }
        center.addObserverForName(UIApplicationDidBecomeActiveNotification, `object` = null, queue = main) { _ -> restart() }
        // The game's frame loop stops in the background, so it can't stop the music itself (Android's onPause).
        center.addObserverForName(UIApplicationWillResignActiveNotification, `object` = null, queue = main) { _ -> music(false) }
    }

    private fun install(
        effects: Map<Sound, ShortArray>,
        tune: ShortArray,
    ) {
        players = effects.mapValues { (_, pcm) -> attach(pcm) }
        music = attach(tune)
        restart()
    }

    /** A player node for [pcm], attached to the engine's mixer, with its buffer. */
    private fun attach(pcm: ShortArray): Pair<AVAudioPlayerNode, AVAudioPCMBuffer> {
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = pcm.size.toUInt())
        buffer.frameLength = pcm.size.toUInt()
        val samples = buffer.floatChannelData!![0]!!
        for (i in pcm.indices) samples[i] = pcm[i] / PCM_FULL_SCALE
        val node = AVAudioPlayerNode()
        engine.attachNode(node)
        engine.connect(node, to = engine.mainMixerNode, format = format)
        return node to buffer
    }

    /**
     * Starts the engine (and the session) if it isn't running, and with it the
     * music loop if that is wanted. A running engine is left alone, so coming
     * back to the app carries the tune on from where it was.
     */
    private fun restart() {
        if (players == null || engine.running || !startEngine()) return
        val (node, buffer) = music ?: return
        // A stopped engine drops what its nodes had scheduled, so the loop is scheduled afresh.
        node.stop()
        node.scheduleBuffer(buffer, atTime = null, options = AVAudioPlayerNodeBufferLoops, completionHandler = null)
        if (musicWanted) node.play()
    }

    /** Activates the session and starts the engine; false if iOS won't allow it right now. */
    private fun startEngine(): Boolean {
        AVAudioSession.sharedInstance().setActive(true, error = null)
        engine.prepare()
        return engine.startAndReturnError(null)
    }

    /** Plays [sound] from its start, cutting off the same sound if it is still going. */
    override fun play(sound: Sound) {
        val (node, buffer) = players?.get(sound) ?: return
        if (!engine.running) return
        node.scheduleBuffer(buffer, atTime = null, options = AVAudioPlayerNodeBufferInterrupts, completionHandler = null)
        if (!node.playing) node.play()
    }

    /** Starts (from where it left off) or pauses the background music loop. */
    override fun music(on: Boolean) {
        musicWanted = on
        val (node, _) = music ?: return
        if (!engine.running) return
        if (on && !node.playing) node.play()
        if (!on && node.playing) node.pause()
    }

    private companion object {
        /** 16-bit samples become floats in -1..1. */
        const val PCM_FULL_SCALE = 32_768f
    }
}

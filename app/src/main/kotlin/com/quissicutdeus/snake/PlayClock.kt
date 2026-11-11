package com.quissicutdeus.snake

import java.time.LocalDate

/**
 * One day's play on this phone: which day ([day], as days since 1970 on the
 * phone's calendar), how long games really ran, and extra time a grown-up
 * gave on top of the limit.
 */
data class PlayDay(
    val day: Long,
    val playedMs: Long = 0,
    val extraMs: Long = 0,
) {
    fun encode(): String = "$day,$playedMs,$extraMs"

    /**
     * This record on [today]: a later day starts afresh. An earlier day means
     * the clock was turned back, so the count carries on as it is (otherwise
     * turning the clock back a day would give a whole new day's play).
     */
    fun on(today: Long): PlayDay = if (today > day) PlayDay(today) else this

    companion object {
        /** Null for nothing saved or anything unreadable. */
        fun decode(text: String?): PlayDay? {
            val p = text?.split(',')?.takeIf { it.size == FIELDS }?.map { it.trim().toLongOrNull() ?: return null } ?: return null
            return PlayDay(p[0], p[1].coerceAtLeast(0), p[2].coerceAtLeast(0))
        }

        private const val FIELDS = 3
    }
}

/**
 * The bedtime timer's count: time actually playing (a game running, not
 * paused, not over), added up per calendar day for the whole phone, and how
 * much of [limitMinutes] (0 = no limit) is left.
 *
 * Played time comes from the game's own frame times, never from the wall
 * clock, so changing the clock mid-game does nothing; the wall clock ([today])
 * only says which day it is. The record is read with [load] and written with
 * [save] every few seconds of play and on [flush], so closing the app (or
 * Android closing it) loses at most a few seconds.
 */
class PlayClock(
    private val limitMinutes: () -> Int,
    private val load: () -> String?,
    private val save: (String) -> Unit,
    private val today: () -> LocalDate,
) {
    private var record = PlayDay.decode(load()) ?: PlayDay(todayNumber())

    /** Played time not written yet, and the part of a millisecond not counted yet. */
    private var unsavedMs = 0L
    private var fractionMs = 0f

    private fun todayNumber() = today().toEpochDay()

    /** Today's record (the first look on a new day starts it afresh). */
    private fun current(): PlayDay {
        val now = record.on(todayNumber())
        if (now != record) {
            record = now
            save(record.encode())
        }
        return record
    }

    /** Time played today, in milliseconds. */
    val playedMs: Long get() = current().playedMs

    /** Play left today in milliseconds (never below 0), or null when there's no limit. */
    fun leftMs(): Long? {
        val minutes = limitMinutes()
        if (minutes <= 0) return null
        val d = current()
        return (minutes * MS_PER_MINUTE + d.extraMs - d.playedMs).coerceAtLeast(0)
    }

    /** There's a limit and today's play has used it all. */
    val timeUp: Boolean get() = leftMs() == 0L

    /** One frame of [dtSeconds]; it counts only while [running] (a game going, not paused or over). */
    fun onFrame(
        dtSeconds: Float,
        running: Boolean,
    ) {
        if (!running || dtSeconds <= 0f) return
        val exact = dtSeconds * MS_PER_SECOND + fractionMs
        val ms = exact.toLong()
        fractionMs = exact - ms
        val d = current()
        record = d.copy(playedMs = d.playedMs + ms)
        unsavedMs += ms
        if (unsavedMs >= SAVE_EVERY_MS) flush()
    }

    /** Writes down what's been played (when the app goes to the background, or a game ends). */
    fun flush() {
        if (unsavedMs == 0L) return
        unsavedMs = 0
        save(record.encode())
    }

    /** A grown-up gives [minutes] more play today. */
    fun grant(minutes: Int) {
        val d = current()
        record = d.copy(extraMs = d.extraMs + minutes * MS_PER_MINUTE)
        unsavedMs = 0
        save(record.encode())
    }

    /**
     * A grown-up has just set the timer, so the phone's date is presumably
     * right again: a record from a "future" day (the clock was set ahead, then
     * back) moves to today, keeping its count, so a new day can start at the
     * next midnight instead of on that future date.
     */
    fun settle() {
        val now = todayNumber()
        if (record.day > now) {
            record = record.copy(day = now)
            save(record.encode())
        }
    }

    companion object {
        const val MS_PER_MINUTE = 60_000L
        private const val MS_PER_SECOND = 1000f

        /** Played time is written down this often, so Android closing the app loses at most this much. */
        private const val SAVE_EVERY_MS = 5_000L
    }
}

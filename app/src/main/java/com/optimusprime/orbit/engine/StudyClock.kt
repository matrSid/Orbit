package com.optimusprime.orbit.engine

enum class SessionPhase { IDLE, RUNNING, PAUSED_MANUAL, PAUSED_AWAY }

/**
 * Plain Kotlin, no Android imports — a small state machine for "how long has
 * the student actually been studying." Time only accumulates while
 * [SessionPhase.RUNNING]; every pause (manual or camera-triggered) freezes
 * the clock until [resume] is called. Driven by wall-clock timestamps rather
 * than a tick counter so it can't drift if the app is backgrounded.
 */
class StudyClock {
    var phase: SessionPhase = SessionPhase.IDLE
        private set

    var distractionCount: Int = 0
        private set

    private var accumulatedMillis: Long = 0L
    private var runningSinceMillis: Long? = null

    fun start(nowMillis: Long) {
        phase = SessionPhase.RUNNING
        accumulatedMillis = 0L
        runningSinceMillis = nowMillis
        distractionCount = 0
    }

    fun pauseManual(nowMillis: Long) = freeze(nowMillis, SessionPhase.PAUSED_MANUAL)

    fun pauseForAbsence(nowMillis: Long) {
        if (phase == SessionPhase.RUNNING) distractionCount++
        freeze(nowMillis, SessionPhase.PAUSED_AWAY)
    }

    private fun freeze(nowMillis: Long, newPhase: SessionPhase) {
        if (phase == SessionPhase.RUNNING) {
            accumulatedMillis += nowMillis - (runningSinceMillis ?: nowMillis)
            runningSinceMillis = null
        }
        phase = newPhase
    }

    fun resume(nowMillis: Long) {
        if (phase == SessionPhase.PAUSED_MANUAL || phase == SessionPhase.PAUSED_AWAY) {
            phase = SessionPhase.RUNNING
            runningSinceMillis = nowMillis
        }
    }

    fun elapsedMillis(nowMillis: Long): Long =
        accumulatedMillis + if (phase == SessionPhase.RUNNING) {
            nowMillis - (runningSinceMillis ?: nowMillis)
        } else 0L

    /** Freezes the clock and returns the final active duration. Does not reset. */
    fun finish(nowMillis: Long): Long {
        if (phase == SessionPhase.RUNNING) {
            accumulatedMillis += nowMillis - (runningSinceMillis ?: nowMillis)
            runningSinceMillis = null
        }
        return accumulatedMillis
    }

    fun reset() {
        phase = SessionPhase.IDLE
        accumulatedMillis = 0L
        runningSinceMillis = null
        distractionCount = 0
    }
}

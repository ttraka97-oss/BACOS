package com.bacos.app.domain

import com.bacos.app.data.db.CardStateEntity
import kotlin.math.roundToInt

/**
 * SRSEngine — adaptive spaced repetition (SuperMemo SM-2, adapted).
 *
 * Every card carries: ease (difficulty factor), interval, repetitions, lapses.
 * The scheduler decides when the card must come back — intervals grow
 * multiplicatively with ease, and reset on a lapse. NOT fixed 1/3/7 days.
 */
object SRSEngine {

    /** UI grades mapped to SM-2 quality (0..5) */
    const val GRADE_AGAIN = 1
    const val GRADE_HARD = 3
    const val GRADE_GOOD = 4
    const val GRADE_EASY = 5

    data class ReviewResult(
        val ease: Double,
        val intervalDays: Int,
        val repetitions: Int,
        val dueAt: Long,
        val lapses: Int,
        /** estimated retention strength 0..1 right after review */
        val strength: Double,
    )

    fun review(state: CardStateEntity, grade: Int, now: Long): ReviewResult {
        val q = grade.coerceIn(0, 5)
        var ease = state.ease
        var repetitions = state.repetitions
        var interval = state.intervalDays
        var lapses = state.lapses

        if (q >= 3) {
            // Pass
            repetitions += 1
            ease = (ease + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceIn(1.3, 2.8)
            interval = when (repetitions) {
                1 -> 1
                2 -> (if (q == GRADE_HARD) 4 else 6)
                else -> (interval * ease).roundToInt().coerceAtLeast(1)
            }
        } else {
            // Lapse — memory reset
            repetitions = 0
            interval = 1
            lapses += 1
            ease = (ease - 0.2).coerceIn(1.3, 2.8)
        }

        val dayMs = 24 * 60 * 60 * 1000L
        val dueAt = now + interval * dayMs
        val strength = (1.0 / (1.0 + interval * 0.12)) * if (q >= 3) 1.0 else 0.35

        return ReviewResult(ease, interval, repetitions, dueAt, lapses, strength)
    }

    /** First-time card initialization with a small head start. */
    fun initial(now: Long): CardStateEntity = CardStateEntity(
        cardId = 0,
        ease = 2.5,
        intervalDays = 0,
        repetitions = 0,
        dueAt = now,
        lastReviewedAt = 0L,
        lapses = 0,
    )
}

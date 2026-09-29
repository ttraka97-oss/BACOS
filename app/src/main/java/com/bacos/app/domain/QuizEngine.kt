package com.bacos.app.domain

import com.bacos.app.data.db.ProgressEntity
import com.bacos.app.data.db.QuestionEntity
import kotlin.random.Random

/**
 * QuizEngine — adaptive question selection.
 *
 * Weight of each question = weakness of its lesson (1 - mastery)
 * adjusted for difficulty vs the student's recent accuracy so the
 * quiz targets ~70% success rate (optimal learning zone).
 */
object QuizEngine {

    fun selectQuestions(
        questions: List<QuestionEntity>,
        progressByLesson: Map<Long, ProgressEntity>,
        count: Int,
        recentAccuracy: Double?,   // null → no history; 0..1
        seed: Int = Random.nextInt(),
    ): List<QuestionEntity> {
        if (questions.isEmpty() || count <= 0) return emptyList()
        val rng = Random(seed)

        val accuracy = recentAccuracy
        val scored = questions.map { q ->
            val mastery = progressByLesson[q.lessonId]?.mastery ?: 0
            val weakness = 1.0 - (mastery / 100.0)

            // difficulty preference: push harder when strong, easier when weak
            val difficultyBonus = when {
                accuracy == null || accuracy in 0.55..0.80 -> 0.0
                accuracy > 0.80 -> (q.difficulty - 2) * 0.15   // prefer hard
                else -> (2 - q.difficulty) * 0.15               // prefer easy
            }

            // never seen lessons get a strong boost
            val unseen = if (progressByLesson[q.lessonId] == null) 0.35 else 0.0

            val weight = (weakness + unseen + difficultyBonus).coerceAtLeast(0.05)
            q to weight
        }

        val picked = mutableListOf<QuestionEntity>()
        val pool = scored.toMutableList()
        val target = count.coerceAtMost(pool.size)
        repeat(target) {
            val total = pool.sumOf { it.second }
            var r = rng.nextDouble() * total
            var idx = 0
            while (idx < pool.size - 1 && r > pool[idx].second) {
                r -= pool[idx].second
                idx++
            }
            picked.add(pool[idx].first)
            pool.removeAt(idx)
        }
        return picked
    }

    /** Mastery update after a quiz — exponential moving average per lesson. */
    fun updateMastery(currentMastery: Int, correct: Int, total: Int): Int {
        if (total == 0) return currentMastery
        val ratio = correct.toDouble() / total
        val target = ratio * 100.0
        // 40% of the way to the target each quiz — fast early, stabilizing later
        val updated = currentMastery + (target - currentMastery) * 0.4
        return updated.roundToInt().coerceIn(0, 100)
    }

    private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
}

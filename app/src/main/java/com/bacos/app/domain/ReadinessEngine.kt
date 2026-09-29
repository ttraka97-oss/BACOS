package com.bacos.app.domain

import com.bacos.app.data.db.LessonEntity
import com.bacos.app.data.db.ProgressEntity
import kotlin.math.roundToInt

/**
 * ReadinessEngine — BAC READINESS™ score (0..100).
 *
 * Transparent, auditable formula — never a random number:
 *
 *   subjectScore = 0.60 × mastery      (quiz-driven mastery average)
 *                + 0.25 × coverage     (fraction of curriculum touched)
 *                + 0.15 × retention   (recent review accuracy)
 *
 *   overall      = mean(subjectScores) × 0.9 + consistencyBonus × 0.1
 *   consistency  = min(streak / 14, 1) — 2 weeks of daily work is a full bonus
 */
object ReadinessEngine {

    data class SubjectInput(
        val masteryAvg: Double,   // 0..100 average mastery of studied lessons
        val coverage: Double,     // 0..1 fraction of lessons with progress > 0
        val retention: Double,    // 0..1 recent review accuracy
    )

    fun subjectScore(input: SubjectInput): Double =
        0.60 * input.masteryAvg + 0.25 * (input.coverage * 100.0) + 0.15 * (input.retention * 100.0)

    fun overall(subjectScores: List<Double>, streak: Int): Int {
        if (subjectScores.isEmpty()) return 0
        val base = subjectScores.average()
        val consistency = (streak.coerceAtLeast(0).toDouble() / 14.0).coerceAtMost(1.0)
        return ((base * 0.9) + (consistency * 100.0 * 0.1)).roundToInt().coerceIn(0, 100)
    }

    /** Compute per-subject scores from raw DB rows. */
    fun computeSubjectInputs(
        lessonsBySubject: Map<Long, List<LessonEntity>>,
        progressByLesson: Map<Long, ProgressEntity>,
        recentAccuracy: Double,
    ): Map<Long, SubjectInput> {
        return lessonsBySubject.mapValues { (_, lessons) ->
            val progresses = lessons.mapNotNull { progressByLesson[it.id] }
            val studied = progresses.filter { it.mastery > 0 || it.status != "new" }
            val masteryAvg = if (progresses.isEmpty()) 0.0 else progresses.map { it.mastery.toDouble() }.average()
            val coverage = if (lessons.isEmpty()) 0.0 else studied.size.toDouble() / lessons.size
            SubjectInput(masteryAvg, coverage, recentAccuracy)
        }
    }

    /** Human label for a readiness score. */
    fun label(score: Int): String = when {
        score >= 85 -> "جاهز"
        score >= 65 -> "على الطريق الصحيح"
        score >= 40 -> "يحتاج عمل جاد"
        else -> "بداية الرحلة"
    }
}

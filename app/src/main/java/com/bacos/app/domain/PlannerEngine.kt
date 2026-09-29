package com.bacos.app.domain

import com.bacos.app.data.db.LessonEntity
import com.bacos.app.data.db.MistakeEntity
import com.bacos.app.data.db.ProgressEntity

/**
 * PlannerEngine — the "BAC BRAIN" daily mission builder.
 *
 * Decides what the student should do RIGHT NOW from real signals:
 *   1. Due reviews (spaced repetition queue)
 *   2. The next unfinished lesson in curriculum order
 *   3. Unresolved mistakes (weakness queue)
 *   4. The weakest subject (neglected or low mastery)
 */
object PlannerEngine {

    data class TodayMission(
        val dueReviews: Int,
        val reviewSample: List<Long>,            // card ids to review
        val nextLesson: LessonEntity?,
        val practiceTarget: String?,             // subject name to practice
        val fixConcepts: List<MistakeEntity>,
        val weakestSubject: String?,
        val greetingLine: String,
    )

    fun build(
        dueCardIds: List<Long>,
        lessons: List<LessonEntity>,              // curriculum order
        progressByLesson: Map<Long, ProgressEntity>,
        openMistakes: List<MistakeEntity>,
        subjectMastery: Map<String, Double>,      // subject name → avg mastery
        daysSinceActive: Int,
    ): TodayMission {
        // 1 ── Reviews
        val reviews = dueCardIds.take(15)

        // 2 ── Next lesson: first one not done, in order
        val next = lessons.firstOrNull { l ->
            val p = progressByLesson[l.id]
            p == null || p.status != "done"
        }

        // 3 ── Mistakes needing fixing
        val fixes = openMistakes.filter { !it.resolved }.take(5)

        // 4 ── Weakest subject
        val weakest = subjectMastery.minByOrNull { it.value }?.key

        val greeting = when {
            daysSinceActive >= 5 -> "أهلاً بعودتك 👋 أعدت بناء خطتك"
            daysSinceActive in 2..4 -> "غبت شوية — خلينا نلحقو اللي فات"
            else -> when (java.time.LocalTime.now().hour) {
                in 5..11 -> "صباح الخير 👋"
                in 12..17 -> "مساء الخير 👋"
                else -> "مساء الخير 👋 وقت المراجعة"
            }
        }

        return TodayMission(
            dueReviews = dueCardIds.size,
            reviewSample = reviews,
            nextLesson = next,
            practiceTarget = weakest,
            fixConcepts = fixes,
            weakestSubject = weakest,
            greetingLine = greeting,
        )
    }
}

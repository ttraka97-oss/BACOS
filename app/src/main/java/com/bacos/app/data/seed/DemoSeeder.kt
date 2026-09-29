package com.bacos.app.data.seed

import com.bacos.app.data.db.AppDb
import com.bacos.app.data.db.CardStateEntity
import com.bacos.app.data.db.MistakeEntity
import com.bacos.app.data.db.ProgressEntity
import com.bacos.app.data.db.SessionEntity
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * DemoSeeder — populates a rich, realistic student state
 * (mastery history, due reviews, past mistakes, study sessions, streak)
 * so the very first launch shows a living BAC BRAIN, not empty tables.
 *
 * Used when the student picks "تجربة ببيانات تجريبية" in onboarding.
 */
object DemoSeeder {

    private const val DAY = 24 * 60 * 60 * 1000L

    suspend fun seed(db: AppDb) {
        val now = System.currentTimeMillis()

        val lessons = db.lessonDao().all()
        if (lessons.isEmpty()) return

        // ── Curriculum progress: math solid, physics weak, SVT strong (matches demo story)
        data class Profile(val keyword: String, val mastery: Int, val status: String)
        val rules = listOf(
            Profile("متتالي", 72, "done"),
            Profile("نهايات", 58, "learning"),
            Profile("اشتقاق", 61, "learning"),
            Profile("لوغاريتم", 40, "learning"),
            Profile("احتمال", 55, "learning"),
            Profile("RC", 48, "learning"),
            Profile("RLC", 25, "new"),
            Profile("تحلل", 35, "learning"),
            Profile("حموض", 30, "new"),
            Profile("بروتين", 79, "done"),
            Profile("مشبك", 82, "done"),
            Profile("مناعة", 74, "done"),
        )

        val progressRows = lessons.mapIndexedNotNull { idx, l ->
            val rule = rules.firstOrNull { l.titleAr.contains(it.keyword) || l.summary.contains(it.keyword) }
                ?: Profile("", 20 + (idx * 7) % 45, if (idx % 2 == 0) "learning" else "new")
            ProgressEntity(
                lessonId = l.id,
                mastery = rule.mastery,
                status = rule.status,
                lastStudiedAt = now - (idx % 7 + 1) * DAY,
                attempts = 2 + idx % 4,
                correct = 1 + idx % 3,
            )
        }
        db.progressDao().upsertAll(progressRows)

        // ── Card states: about a third due now, rest scheduled in the future
        val allCards = mutableListOf<CardStateEntity>()
        progressRows.forEachIndexed { i, p ->
            val cards = db.flashcardDao().byLessons(listOf(p.lessonId))
            cards.forEachIndexed { j, c ->
                val due = (i + j) % 3 == 0
                allCards.add(
                    CardStateEntity(
                        cardId = c.id,
                        ease = 2.1 + (i % 4) * 0.15,
                        intervalDays = if (due) 1 else 3 + (i + j) % 5,
                        repetitions = 1 + (i + j) % 3,
                        dueAt = if (due) now - DAY else now + (2 + (i + j) % 8) * DAY,
                        lastReviewedAt = now - (if (due) 4 else 1) * DAY,
                        lapses = (i + j) % 2,
                    )
                )
            }
        }
        db.cardStateDao().upsertAll(allCards)

        // ── Mistake book: real concepts from seeded questions
        val questions = db.questionDao().byLessons(lessons.map { it.id })
        val mistakeTargets = questions.filter {
            it.concept.contains("اشتقاق") || it.concept.contains("ln") || it.concept.contains("RC") ||
                it.concept.contains("نصف العمر") || it.concept.contains("معايرة") || it.concept.contains("الشفرة")
        }.take(4)
        mistakeTargets.forEachIndexed { i, q ->
            db.mistakeDao().insert(
                MistakeEntity(
                    questionId = q.id,
                    lessonId = q.lessonId,
                    concept = q.concept,
                    chosenIndex = (q.correctIndex + 1) % 4,
                    createdAt = now - (i + 1) * 2 * DAY,
                    resolved = false,
                )
            )
        }

        // ── Session history: last 10 days, realistic XP + minutes
        val today = LocalDate.now()
        val types = listOf("review", "quiz", "lesson", "focus")
        val minutesArr = intArrayOf(25, 40, 15, 50, 30, 20, 45, 35, 60, 30)
        for (d in 10 downTo 1) {
            if (d == 4 || d == 7) continue // missed days — realistic
            val day = today.minusDays(d.toLong())
            db.sessionDao().insert(
                SessionEntity(
                    day = day.toString(),
                    minutes = minutesArr[d % minutesArr.size],
                    type = types[d % types.size],
                    xp = 40 + (d * 17) % 60,
                    questions = 5 + (d * 3) % 12,
                    correct = 3 + (d * 2) % 9,
                    startedAt = day.atTime(19, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                )
            )
        }
    }
}

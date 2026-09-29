package com.bacos.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// ═══════════════════════════════════════════════════════════════
// BACOS database schema — local-first, mirrors backend/schema.sql
// ═══════════════════════════════════════════════════════════════

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val branch: String = "sciences",          // sciences | math | techmath | gestion | letters
    val examDate: Long = 0L,                  // BAC exam date (millis)
    val dailyMinutes: Int = 60,
    val streak: Int = 0,
    val lastActiveDay: String = "",           // yyyy-MM-dd
    val totalXp: Long = 0L,
    val onboarded: Boolean = false,
    val demoMode: Boolean = false,
    val darkMode: Boolean = true,
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val nameAr: String,
    val nameFr: String,
    val colorHex: String,                      // accent for the subject
    val orderIdx: Int,
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val nameAr: String,
    val nameFr: String,
    val orderIdx: Int,
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val titleAr: String,
    val summary: String,
    val difficulty: Int,                       // 1..3
    val minutes: Int,
    val sectionsJson: String,                  // [{t:title, b:body}]
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: Long,
    val text: String,
    val choicesJson: String,                   // ["a","b","c","d"]
    val correctIndex: Int,
    val difficulty: Int,                       // 1..3
    val explanation: String,
    val concept: String,                       // e.g. "إشارة المشتق"
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: Long,
    val front: String,
    val back: String,
)

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val lessonId: Long,
    val mastery: Int = 0,                      // 0..100
    val status: String = "new",               // new | learning | done
    val lastStudiedAt: Long = 0L,
    val attempts: Int = 0,
    val correct: Int = 0,
)

@Entity(tableName = "card_states")
data class CardStateEntity(
    @PrimaryKey val cardId: Long,
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueAt: Long = 0L,
    val lastReviewedAt: Long = 0L,
    val lapses: Int = 0,
)

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val lessonId: Long,
    val concept: String,
    val chosenIndex: Int,
    val createdAt: Long,
    val resolved: Boolean = false,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: String,                           // yyyy-MM-dd
    val minutes: Int,
    val type: String,                          // focus | review | quiz | lesson
    val xp: Int,
    val questions: Int = 0,
    val correct: Int = 0,
    val startedAt: Long,
)

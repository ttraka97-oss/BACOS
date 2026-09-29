package com.bacos.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ProfileEntity)
}

@Dao
interface SubjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SubjectEntity>): List<Long>

    @Query("SELECT * FROM subjects ORDER BY orderIdx")
    suspend fun all(): List<SubjectEntity>

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int
}

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ChapterEntity>): List<Long>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIdx")
    suspend fun bySubject(subjectId: Long): List<ChapterEntity>

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun count(): Int
}

@Dao
interface LessonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<LessonEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lesson: LessonEntity): Long

    @Query("SELECT * FROM lessons WHERE chapterId = :chapterId")
    suspend fun byChapter(chapterId: Long): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun byId(id: Long): LessonEntity?

    @Query("SELECT * FROM lessons")
    suspend fun all(): List<LessonEntity>

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun count(): Int
}

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<QuestionEntity>): List<Long>

    @Query("SELECT * FROM questions WHERE lessonId IN (:lessonIds)")
    suspend fun byLessons(lessonIds: List<Long>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun byId(id: Long): QuestionEntity?

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int
}

@Dao
interface FlashcardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<FlashcardEntity>): List<Long>

    @Query("SELECT * FROM flashcards WHERE lessonId IN (:lessonIds)")
    suspend fun byLessons(lessonIds: List<Long>): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun byId(id: Long): FlashcardEntity?
}

@Dao
interface ProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<ProgressEntity>)

    @Query("SELECT * FROM progress WHERE lessonId = :lessonId")
    suspend fun byLesson(lessonId: Long): ProgressEntity?

    @Query("SELECT * FROM progress")
    suspend fun all(): List<ProgressEntity>
}

@Dao
interface CardStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: CardStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<CardStateEntity>)

    @Query("SELECT * FROM card_states WHERE dueAt <= :now LIMIT :limit")
    suspend fun due(now: Long, limit: Int): List<CardStateEntity>

    @Query("SELECT card_states.* FROM card_states JOIN flashcards ON flashcards.id = card_states.cardId WHERE flashcards.lessonId IN (:lessonIds)")
    suspend fun byLessons(lessonIds: List<Long>): List<CardStateEntity>

    @Query("SELECT COUNT(*) FROM card_states WHERE dueAt <= :now")
    suspend fun dueCount(now: Long): Int
}

@Dao
interface MistakeDao {
    @Insert
    suspend fun insert(m: MistakeEntity)

    @Query("SELECT * FROM mistakes WHERE resolved = 0 ORDER BY createdAt DESC")
    suspend fun open(): List<MistakeEntity>

    @Query("UPDATE mistakes SET resolved = 1 WHERE id = :id")
    suspend fun resolve(id: Long)

    @Query("SELECT * FROM mistakes ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<MistakeEntity>
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(s: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    suspend fun all(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE day = :day")
    suspend fun byDay(day: String): List<SessionEntity>
}

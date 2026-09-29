package com.bacos.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        SubjectEntity::class,
        ChapterEntity::class,
        LessonEntity::class,
        QuestionEntity::class,
        FlashcardEntity::class,
        ProgressEntity::class,
        CardStateEntity::class,
        MistakeEntity::class,
        SessionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDb : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun lessonDao(): LessonDao
    abstract fun questionDao(): QuestionDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun progressDao(): ProgressDao
    abstract fun cardStateDao(): CardStateDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(context: Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "bacos.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}

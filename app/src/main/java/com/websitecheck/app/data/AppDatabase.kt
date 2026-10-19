package com.websitecheck.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SiteEntity::class, CheckLogEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun checkLogDao(): CheckLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "website_health_check.db",
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE sites ADD COLUMN keywordCheckEnabled INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE sites ADD COLUMN expectedKeyword TEXT NOT NULL DEFAULT ''",
                )
            }
        }
    }
}

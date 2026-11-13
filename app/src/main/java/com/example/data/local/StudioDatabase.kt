package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        ProjectFileEntity::class,
        ChangeHistoryEntity::class,
        CompilationHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS compilation_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        fileName TEXT NOT NULL,
                        outputFileName TEXT NOT NULL,
                        compilerVersion TEXT NOT NULL,
                        statusLabel TEXT NOT NULL,
                        succeeded INTEGER NOT NULL,
                        errorCount INTEGER NOT NULL,
                        warningCount INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        outputLog TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_compilation_history_projectId ON compilation_history(projectId)")
            }
        }

        fun getInstance(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "cs16_plugin_studio.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

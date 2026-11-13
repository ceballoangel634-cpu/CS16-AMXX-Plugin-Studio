package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class StudioFileType(val extension: String, val label: String) {
    SMA("sma", "Pawn Source"),
    INC("inc", "Pawn Include"),
    CFG("cfg", "Config CFG"),
    INI("ini", "INI Config"),
    AMXX("amxx", "AMXX Binary"),
    OTHER("txt", "Archivo");

    companion object {
        fun fromFileName(name: String): StudioFileType {
            val lower = name.lowercase()
            return when {
                lower.endsWith(".sma") -> SMA
                lower.endsWith(".inc") -> INC
                lower.endsWith(".cfg") -> CFG
                lower.endsWith(".ini") -> INI
                lower.endsWith(".amxx") -> AMXX
                else -> OTHER
            }
        }
    }
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val author: String = "Developer",
    val version: String = "1.0",
    val targetMod: String = "AMX Mod X 1.9 / 1.10",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "project_files",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class ProjectFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val fileName: String,
    val relativePath: String,
    val fileType: String,
    val content: String,
    val isBinaryPlaceholder: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun resolvedType(): StudioFileType =
        runCatching { StudioFileType.valueOf(fileType) }.getOrElse { StudioFileType.fromFileName(fileName) }
}

@Entity(
    tableName = "change_history",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class ChangeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val fileId: Long,
    val fileName: String,
    val summary: String,
    val snapshotContent: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "compilation_history",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class CompilationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val fileName: String,
    val outputFileName: String,
    val compilerVersion: String,
    val statusLabel: String,
    val succeeded: Boolean,
    val errorCount: Int,
    val warningCount: Int,
    val durationMs: Long,
    val outputLog: String,
    val timestamp: Long = System.currentTimeMillis()
)

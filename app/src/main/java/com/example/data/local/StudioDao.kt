package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY fileType ASC, fileName ASC")
    fun observeFilesForProject(projectId: Long): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE projectId = :projectId")
    suspend fun getFilesForProjectOnce(projectId: Long): List<ProjectFileEntity>

    @Query("SELECT * FROM project_files WHERE id = :fileId LIMIT 1")
    suspend fun getFileById(fileId: Long): ProjectFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity): Long

    @Update
    suspend fun updateFile(file: ProjectFileEntity)

    @Query("DELETE FROM project_files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: Long)

    @Query("SELECT * FROM change_history WHERE projectId = :projectId ORDER BY timestamp DESC LIMIT 100")
    fun observeHistoryForProject(projectId: Long): Flow<List<ChangeHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: ChangeHistoryEntity): Long

    @Query("SELECT * FROM compilation_history WHERE projectId = :projectId ORDER BY timestamp DESC LIMIT 100")
    fun observeCompilationsForProject(projectId: Long): Flow<List<CompilationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompilationHistory(entry: CompilationHistoryEntity): Long
}

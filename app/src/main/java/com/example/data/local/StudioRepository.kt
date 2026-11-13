package com.example.data.local

import com.example.domain.AmxxTemplates
import kotlinx.coroutines.flow.Flow

class StudioRepository(private val dao: StudioDao) {

    val projectsFlow: Flow<List<ProjectEntity>> = dao.observeProjects()

    fun observeFiles(projectId: Long): Flow<List<ProjectFileEntity>> =
        dao.observeFilesForProject(projectId)

    fun observeHistory(projectId: Long): Flow<List<ChangeHistoryEntity>> =
        dao.observeHistoryForProject(projectId)

    fun observeCompilations(projectId: Long): Flow<List<CompilationHistoryEntity>> =
        dao.observeCompilationsForProject(projectId)

    suspend fun recordCompilation(entry: CompilationHistoryEntity): Long =
        dao.insertCompilationHistory(entry)

    suspend fun ensureStarterProject(): Long {
        if (dao.getProjectCount() > 0) {
            return -1L
        }
        val now = System.currentTimeMillis()
        val projectId = dao.insertProject(
            ProjectEntity(
                name = "Servidor CS 1.6 Base",
                description = "Proyecto inicial con plugin VIP de ejemplo, amxx.cfg y configuraciones de Metamod.",
                author = "CS 1.6 Plugin Studio",
                version = "1.0",
                targetMod = "AMX Mod X 1.9.0",
                createdAt = now,
                updatedAt = now
            )
        )

        val starterSma = AmxxTemplates.allTemplates.first { it.id == "vip_system" }
        val smaId = dao.insertFile(
            ProjectFileEntity(
                projectId = projectId,
                fileName = starterSma.fileName,
                relativePath = "addons/amxmodx/scripting/${starterSma.fileName}",
                fileType = StudioFileType.SMA.name,
                content = starterSma.code,
                updatedAt = now
            )
        )
        dao.insertHistory(
            ChangeHistoryEntity(
                projectId = projectId,
                fileId = smaId,
                fileName = starterSma.fileName,
                summary = "Creación inicial desde plantilla VIP",
                snapshotContent = starterSma.code,
                timestamp = now
            )
        )

        dao.insertFile(
            ProjectFileEntity(
                projectId = projectId,
                fileName = "server_custom.cfg",
                relativePath = "cstrike/server_custom.cfg",
                fileType = StudioFileType.CFG.name,
                content = """// Configuración CS 1.6 generada por CS 1.6 Plugin Studio
hostname "CS 1.6 Tactical Server"
mp_roundtime 2.0
mp_freezetime 3
mp_startmoney 800
sv_alltalk 1
bind "F4" "say /shop"
alias "fast_rr" "amx_cvar sv_restart 1"
""",
                updatedAt = now
            )
        )

        dao.insertFile(
            ProjectFileEntity(
                projectId = projectId,
                fileName = "plugins.ini",
                relativePath = "addons/amxmodx/configs/plugins.ini",
                fileType = StudioFileType.INI.name,
                content = """; Plugins base de AMX Mod X
admin.amxx
admincmd.amxx
adminhelp.amxx
menufront.amxx
cmdmenu.amxx
plmenu.amxx
mapsmenu.amxx
; Plugins personalizados del proyecto
vip_system.amxx
""",
                updatedAt = now
            )
        )

        dao.insertFile(
            ProjectFileEntity(
                projectId = projectId,
                fileName = "modules.ini",
                relativePath = "addons/amxmodx/configs/modules.ini",
                fileType = StudioFileType.INI.name,
                content = """; Módulos activos de AMX Mod X
fun
engine
fakemeta
cstrike
csx
hamsandwich
nvault
""",
                updatedAt = now
            )
        )

        dao.insertFile(
            ProjectFileEntity(
                projectId = projectId,
                fileName = "metamod_plugins.ini",
                relativePath = "addons/metamod/plugins.ini",
                fileType = StudioFileType.INI.name,
                content = """; Metamod plugins.ini (Nativos .so/.dll)
linux addons/amxmodx/dlls/amxmodx_mm_i386.so
; win32 addons\amxmodx\dlls\amxmodx_mm.dll
""",
                updatedAt = now
            )
        )

        return projectId
    }

    suspend fun createProject(name: String, description: String, author: String, targetMod: String): Long {
        val now = System.currentTimeMillis()
        val id = dao.insertProject(
            ProjectEntity(
                name = name.trim(),
                description = description.trim(),
                author = author.trim().ifEmpty { "Developer" },
                targetMod = targetMod,
                createdAt = now,
                updatedAt = now
            )
        )
        val baseSma = """#include <amxmodx>
#include <amxmisc>

#pragma semicolon 1

public plugin_init()
{
    register_plugin("${name.trim()}", "1.0", "${author.trim().ifEmpty { "Developer" }}");
}
"""
        val fileId = dao.insertFile(
            ProjectFileEntity(
                projectId = id,
                fileName = "main_plugin.sma",
                relativePath = "addons/amxmodx/scripting/main_plugin.sma",
                fileType = StudioFileType.SMA.name,
                content = baseSma,
                updatedAt = now
            )
        )
        dao.insertHistory(
            ChangeHistoryEntity(
                projectId = id,
                fileId = fileId,
                fileName = "main_plugin.sma",
                summary = "Proyecto creado",
                snapshotContent = baseSma,
                timestamp = now
            )
        )
        return id
    }

    suspend fun deleteProject(projectId: Long) = dao.deleteProjectById(projectId)

    suspend fun createOrUpdateFile(
        projectId: Long,
        fileName: String,
        relativePath: String,
        content: String,
        historySummary: String
    ): Long {
        val now = System.currentTimeMillis()
        val existing = dao.getFilesForProjectOnce(projectId).firstOrNull {
            it.fileName.equals(fileName, ignoreCase = true)
        }
        val type = StudioFileType.fromFileName(fileName)
        val fileId = if (existing != null) {
            dao.updateFile(existing.copy(content = content, relativePath = relativePath, updatedAt = now))
            existing.id
        } else {
            dao.insertFile(
                ProjectFileEntity(
                    projectId = projectId,
                    fileName = fileName,
                    relativePath = relativePath,
                    fileType = type.name,
                    content = content,
                    updatedAt = now
                )
            )
        }
        dao.insertHistory(
            ChangeHistoryEntity(
                projectId = projectId,
                fileId = fileId,
                fileName = fileName,
                summary = historySummary,
                snapshotContent = content,
                timestamp = now
            )
        )
        dao.getProjectById(projectId)?.let {
            dao.updateProject(it.copy(updatedAt = now))
        }
        return fileId
    }

    suspend fun saveFileContent(file: ProjectFileEntity, newContent: String, summary: String = "Guardado manual") {
        val now = System.currentTimeMillis()
        dao.updateFile(file.copy(content = newContent, updatedAt = now))
        dao.insertHistory(
            ChangeHistoryEntity(
                projectId = file.projectId,
                fileId = file.id,
                fileName = file.fileName,
                summary = summary,
                snapshotContent = newContent,
                timestamp = now
            )
        )
        dao.getProjectById(file.projectId)?.let {
            dao.updateProject(it.copy(updatedAt = now))
        }
    }

    suspend fun renameFile(file: ProjectFileEntity, newFileName: String) {
        val cleanName = newFileName.trim()
        if (cleanName.isEmpty()) return
        val dir = file.relativePath.substringBeforeLast('/', "")
        val newRel = if (dir.isEmpty()) cleanName else "$dir/$cleanName"
        val newType = StudioFileType.fromFileName(cleanName)
        dao.updateFile(
            file.copy(
                fileName = cleanName,
                relativePath = newRel,
                fileType = newType.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun duplicateFile(file: ProjectFileEntity): Long {
        val base = file.fileName.substringBeforeLast('.')
        val ext = file.fileName.substringAfterLast('.', "sma")
        val copyName = "${base}_copia.$ext"
        return createOrUpdateFile(
            projectId = file.projectId,
            fileName = copyName,
            relativePath = file.relativePath.replace(file.fileName, copyName),
            content = file.content,
            historySummary = "Duplicado desde ${file.fileName}"
        )
    }

    suspend fun deleteFile(fileId: Long) = dao.deleteFileById(fileId)
}

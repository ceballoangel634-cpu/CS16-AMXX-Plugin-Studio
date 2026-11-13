package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

object StudioRoutes {
    const val HOME = "home"
    const val SMA_EDITOR = "sma_editor"
    const val AI_ASSISTANT = "ai_assistant"
    const val COMPILER = "compiler"
    const val TEMPLATES = "templates"
    const val CFG_EDITOR = "cfg_editor"
    const val AMXX_METAMOD = "amxx_metamod"
    const val COMPATIBILITY = "compatibility"
    const val PROJECTS = "projects"
    const val DOCS = "docs"
}

data class StudioNavDestination(
    val route: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
    val moduleTag: String
)

val studioDestinations = listOf(
    StudioNavDestination(StudioRoutes.HOME, "Inicio", "Inicio", Icons.Filled.Home, "Principal"),
    StudioNavDestination(StudioRoutes.SMA_EDITOR, "Editor SMA", "Editor", Icons.Filled.Code, "Módulo 1"),
    StudioNavDestination(StudioRoutes.AI_ASSISTANT, "Asistente Gemini IA", "IA", Icons.Filled.AutoAwesome, "Módulo 2"),
    StudioNavDestination(StudioRoutes.COMPILER, "Compilador AMXX", "Compila", Icons.Filled.Build, "Módulo 3"),
    StudioNavDestination(StudioRoutes.TEMPLATES, "Creador de Plugins", "Plantillas", Icons.Filled.DashboardCustomize, "Módulo 4"),
    StudioNavDestination(StudioRoutes.CFG_EDITOR, "Editor CFG", "CFG", Icons.Filled.Tune, "Módulo 5"),
    StudioNavDestination(StudioRoutes.AMXX_METAMOD, "AMX Mod X y Metamod", "AMXX", Icons.Filled.Extension, "Módulo 6"),
    StudioNavDestination(StudioRoutes.COMPATIBILITY, "cstrike y Compatibilidad", "cstrike", Icons.Filled.Memory, "Módulo 7"),
    StudioNavDestination(StudioRoutes.PROJECTS, "Gestor de Proyectos", "Proyecto", Icons.Filled.FolderOpen, "Módulo 8"),
    StudioNavDestination(StudioRoutes.DOCS, "Documentación Pawn", "Docs", Icons.AutoMirrored.Filled.MenuBook, "Módulo 9")
)

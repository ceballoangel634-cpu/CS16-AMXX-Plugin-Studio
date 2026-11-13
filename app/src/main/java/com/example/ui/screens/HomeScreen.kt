package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProjectEntity
import com.example.domain.PlatformArchitectureInfo
import com.example.ui.components.GamingActionTile
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioSectionHeader
import com.example.ui.navigation.StudioRoutes
import com.example.ui.theme.StudioBgObsidian
import com.example.ui.theme.StudioBluePrimary
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCobaltSecondary
import com.example.ui.theme.StudioCyanAccent
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioWarning

@Composable
fun HomeScreen(
    activeProject: ProjectEntity?,
    fileCount: Int,
    archInfo: PlatformArchitectureInfo,
    isWideScreen: Boolean,
    onCreateQuickSma: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = if (isWideScreen) 4 else 2

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Tactical Hero Banner
        item(span = { GridItemSpan(columns) }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, StudioBluePrimary.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_banner),
                        contentDescription = "Banner táctico de CS 1.6 Plugin Studio",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        StudioBgObsidian.copy(alpha = 0.94f),
                                        StudioBgObsidian.copy(alpha = 0.72f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(text = "AMX Mod X & Metamod IDE", color = StudioCyanAccent)
                            StatusPill(text = "ABI: ${archInfo.primaryAbi}", color = StudioBluePrimary)
                        }
                        Column {
                            Text(
                                text = "CS 1.6 PLUGIN STUDIO",
                                style = MaterialTheme.typography.displaySmall,
                                color = StudioTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (activeProject != null) {
                                    "Proyecto activo: ${activeProject.name} • $fileCount archivos"
                                } else {
                                    "Crea, compila y administra plugins Pawn y CFG"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = StudioTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Section Header: 8 Primary Quick Actions
        item(span = { GridItemSpan(columns) }) {
            StudioSectionHeader(
                title = "Centro de Operaciones",
                subtitle = "Accesos directos principales del estudio",
                badge = "8 Módulos"
            )
        }

        // 1. Nuevo plugin SMA
        item {
            GamingActionTile(
                title = "1. Nuevo plugin SMA",
                subtitle = "Crear archivo Pawn .sma y abrir editor",
                icon = Icons.Filled.AddCircle,
                accentColor = StudioBluePrimary,
                testTag = "home_btn_new_sma",
                onClick = onCreateQuickSma
            )
        }

        // 2. Abrir proyecto
        item {
            GamingActionTile(
                title = "2. Abrir proyecto",
                subtitle = "Explorar archivos o importar ZIP",
                icon = Icons.Filled.FolderOpen,
                accentColor = StudioCyanAccent,
                testTag = "home_btn_open_project",
                onClick = { onNavigate(StudioRoutes.PROJECTS) }
            )
        }

        // 3. Crear plugin con IA
        item {
            GamingActionTile(
                title = "3. Crear plugin con IA",
                subtitle = "Asistente Gemini 3.8 Flash para Pawn",
                icon = Icons.Filled.AutoAwesome,
                accentColor = Color(0xFF7C4DFF),
                testTag = "home_btn_ai_gemini",
                onClick = { onNavigate(StudioRoutes.AI_ASSISTANT) }
            )
        }

        // 4. Editor CFG
        item {
            GamingActionTile(
                title = "4. Editor CFG",
                subtitle = "Binds, alias y cvars de CS 1.6",
                icon = Icons.Filled.Tune,
                accentColor = StudioSuccess,
                testTag = "home_btn_cfg_editor",
                onClick = { onNavigate(StudioRoutes.CFG_EDITOR) }
            )
        }

        // 5. Compilar SMA
        item {
            GamingActionTile(
                title = "5. Compilar SMA",
                subtitle = "AMX Mod X Compiler + diagnóstico",
                icon = Icons.Filled.Build,
                accentColor = StudioWarning,
                testTag = "home_btn_compile_sma",
                onClick = { onNavigate(StudioRoutes.COMPILER) }
            )
        }

        // 6. Administrador AMXX
        item {
            GamingActionTile(
                title = "6. Administrador AMXX",
                subtitle = "plugins.ini, modules.ini y .amxx",
                icon = Icons.Filled.Extension,
                accentColor = StudioCobaltSecondary,
                testTag = "home_btn_amxx_admin",
                onClick = { onNavigate(StudioRoutes.AMXX_METAMOD) }
            )
        }

        // 7. Metamod
        item {
            GamingActionTile(
                title = "7. Metamod",
                subtitle = "Módulos nativos .so / .dll y errores",
                icon = Icons.Filled.Hub,
                accentColor = Color(0xFF00BFA5),
                testTag = "home_btn_metamod",
                onClick = { onNavigate(StudioRoutes.AMXX_METAMOD) }
            )
        }

        // 8. Mis proyectos
        item {
            GamingActionTile(
                title = "8. Mis proyectos",
                subtitle = "Workspace local, ZIP e historial",
                icon = Icons.Filled.FolderCopy,
                accentColor = StudioBluePrimary,
                testTag = "home_btn_my_projects",
                onClick = { onNavigate(StudioRoutes.PROJECTS) }
            )
        }

        // Secondary Tools Strip (Modules 4, 7, 9)
        item(span = { GridItemSpan(columns) }) {
            Spacer(modifier = Modifier.height(4.dp))
            StudioSectionHeader(
                title = "Herramientas Especializadas",
                subtitle = "Plantillas, carpetas cstrike y referencia Pawn"
            )
        }

        item(span = { GridItemSpan(columns) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactToolRow(
                    title = "Creador de Plugins (13 Plantillas)",
                    description = "VIP, Tienda, XP, Admin Menu, Respawn, HUD, Anti-Cheat, Bots y más",
                    icon = Icons.Filled.DashboardCustomize,
                    accent = StudioCyanAccent,
                    onClick = { onNavigate(StudioRoutes.TEMPLATES) }
                )
                CompactToolRow(
                    title = "Compatibilidad Android / PC y Carpeta cstrike",
                    description = "Importar y exportar hacia addons, amxmodx, configs, gfx y touch_profiles",
                    icon = Icons.Filled.Memory,
                    accent = StudioSuccess,
                    onClick = { onNavigate(StudioRoutes.COMPATIBILITY) }
                )
                CompactToolRow(
                    title = "Documentación Pawn & AMX Mod X",
                    description = "Natives, forwards, includes, ejemplos y tutoriales paso a paso",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accent = StudioBluePrimary,
                    onClick = { onNavigate(StudioRoutes.DOCS) }
                )
            }
        }
    }
}

@Composable
private fun CompactToolRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = StudioSurfaceElevated,
        border = BorderStroke(1.dp, StudioBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = accent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StudioTextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextSecondary
                )
            }
            Icon(
                imageVector = Icons.Filled.SettingsInputComponent,
                contentDescription = null,
                tint = accent.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

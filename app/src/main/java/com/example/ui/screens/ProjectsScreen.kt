package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ChangeHistoryEntity
import com.example.data.local.CompilationHistoryEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.data.local.StudioFileType
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioSectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioBgObsidian
import com.example.ui.theme.StudioBluePrimary
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCyanAccent
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(
    projects: List<ProjectEntity>,
    activeProject: ProjectEntity?,
    projectFiles: List<ProjectFileEntity>,
    historyEntries: List<ChangeHistoryEntity>,
    compilationHistory: List<CompilationHistoryEntity>,
    compiledBinaries: Map<String, ByteArray>,
    onSelectProject: (Long) -> Unit,
    onCreateProject: (String, String, String, String) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onOpenFileInEditor: (ProjectFileEntity) -> Unit,
    onImportFileUri: (Uri) -> Unit,
    onExportAmxxUri: (Uri, String) -> Unit,
    onImportZipUri: (Uri) -> Unit,
    onExportZipUri: (Uri) -> Unit,
    onRestoreHistory: (ChangeHistoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var pendingAmxxExportName by remember { mutableStateOf<String?>(null) }

    val fileImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportFileUri(uri)
    }

    val amxxExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val name = pendingAmxxExportName
        if (uri != null && name != null) {
            onExportAmxxUri(uri, name)
        }
        pendingAmxxExportName = null
    }

    val zipImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportZipUri(uri)
    }

    val zipExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) onExportZipUri(uri)
    }

    val timeFmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StudioSectionHeader(
            title = "Gestor de Proyectos",
            subtitle = "SMA, Includes (.inc), binarios .amxx, historial y ZIP",
            badge = "${projects.size} proyectos"
        )

        // Action Bar: New, Import .SMA/.INC, Import ZIP, Export ZIP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showNewProjectDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("projects_create_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nuevo")
            }
            OutlinedButton(
                onClick = { fileImportLauncher.launch(arrayOf("*/*")) },
                enabled = activeProject != null,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("projects_import_sma_inc_btn")
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Importar .SMA/.INC")
            }
            OutlinedButton(
                onClick = { zipImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("projects_import_zip_btn")
            ) {
                Icon(Icons.Filled.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Importar ZIP")
            }
            OutlinedButton(
                onClick = {
                    val safeName = (activeProject?.name ?: "cs16_project").replace(" ", "_") + ".zip"
                    zipExportLauncher.launch(safeName)
                },
                enabled = activeProject != null,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("projects_export_zip_btn")
            ) {
                Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Exportar ZIP")
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Proyectos") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Archivos (${projectFiles.size})") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Historial (${historyEntries.size})") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Compilac. (${compilationHistory.size})") })
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(projects, key = { it.id }) { proj ->
                        val isActive = proj.id == activeProject?.id
                        Surface(
                            onClick = { onSelectProject(proj.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) StudioBluePrimary.copy(alpha = 0.14f) else StudioSurfaceElevated,
                            border = BorderStroke(1.dp, if (isActive) StudioBluePrimary else StudioBorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = if (isActive) StudioCyanAccent else StudioBluePrimary
                                    )
                                    Column {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(proj.name, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                                            if (isActive) StatusPill(text = "Activo", color = StudioSuccess)
                                        }
                                        Text(proj.description, style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
                                        Text("${proj.targetMod} • ${proj.author}", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                                    }
                                }
                                if (projects.size > 1) {
                                    IconButton(onClick = { onDeleteProject(proj.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar proyecto", tint = StudioError)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                val grouped = remember(projectFiles) {
                    projectFiles.groupBy { it.resolvedType() }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioFileType.entries.forEach { type ->
                        val itemsOfType = grouped[type].orEmpty()
                        if (itemsOfType.isNotEmpty()) {
                            item {
                                StatusPill(text = "${type.label} (${itemsOfType.size})", color = StudioBluePrimary)
                            }
                            items(itemsOfType, key = { it.id }) { file ->
                                val amxxBytes = compiledBinaries[file.fileName]
                                Surface(
                                    onClick = { onOpenFileInEditor(file) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = StudioSurfaceElevated,
                                    border = BorderStroke(1.dp, StudioBorderSubtle)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(file.fileName, fontFamily = JetBrainsMonoFamily, color = StudioTextPrimary)
                                            Text(file.relativePath, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                                        }
                                        if (type == StudioFileType.AMXX && amxxBytes != null) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${amxxBytes.size} B", style = MaterialTheme.typography.labelSmall, color = StudioSuccess)
                                                IconButton(
                                                    onClick = {
                                                        pendingAmxxExportName = file.fileName
                                                        amxxExportLauncher.launch(file.fileName)
                                                    }
                                                ) {
                                                    Icon(Icons.Filled.Download, contentDescription = "Descargar ${file.fileName}", tint = StudioSuccess)
                                                }
                                            }
                                        } else {
                                            Text("${file.content.lines().size} líneas", style = MaterialTheme.typography.bodySmall, color = StudioCyanAccent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyEntries, key = { it.id }) { entry ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioSurfaceElevated,
                            border = BorderStroke(1.dp, StudioBorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.History, contentDescription = null, tint = StudioBluePrimary)
                                    Column {
                                        Text(entry.fileName, fontFamily = JetBrainsMonoFamily, color = StudioTextPrimary)
                                        Text(entry.summary, style = MaterialTheme.typography.bodySmall, color = StudioCyanAccent)
                                        Text(timeFmt.format(Date(entry.timestamp)), style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                                    }
                                }
                                OutlinedButton(onClick = { onRestoreHistory(entry) }) {
                                    Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Restaurar")
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (compilationHistory.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioSurfaceElevated,
                                border = BorderStroke(1.dp, StudioBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Build, contentDescription = null, tint = StudioBluePrimary)
                                    Text(
                                        "Aún no hay compilaciones reales registradas en este proyecto.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioTextSecondary
                                    )
                                }
                            }
                        }
                    }
                    items(compilationHistory, key = { it.id }) { item ->
                        val color = if (item.succeeded) StudioSuccess else StudioWarning
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioSurfaceElevated,
                            border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.fileName} → ${item.outputFileName}",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioTextPrimary
                                    )
                                    StatusPill(text = item.statusLabel, color = color)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(item.compilerVersion, style = MaterialTheme.typography.labelSmall, color = StudioCyanAccent)
                                    Text("• ${item.errorCount} err / ${item.warningCount} warn", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                                    Text("• ${item.durationMs} ms", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                                    Text("• ${timeFmt.format(Date(item.timestamp))}", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioBgObsidian
                                ) {
                                    Text(
                                        text = item.outputLog.take(360),
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioTextSecondary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var author by remember { mutableStateOf("Developer") }
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Crear nuevo proyecto CS 1.6") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del proyecto") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") }, singleLine = true)
                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Autor") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onCreateProject(name, desc.ifBlank { "Proyecto de plugins AMX Mod X" }, author, "AMX Mod X 1.10.0")
                    }
                    showNewProjectDialog = false
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

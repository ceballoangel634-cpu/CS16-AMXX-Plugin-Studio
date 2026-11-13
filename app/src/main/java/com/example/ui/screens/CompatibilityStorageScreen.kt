package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.files.SafDocumentItem
import com.example.data.local.ProjectFileEntity
import com.example.domain.PlatformArchitectureInfo
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioSectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioBluePrimary
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCyanAccent
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioWarning

@Composable
fun CompatibilityStorageScreen(
    archInfo: PlatformArchitectureInfo,
    savedCstrikeUri: String,
    cstrikeStack: List<Pair<String, String>>,
    cstrikeItems: List<SafDocumentItem>,
    projectFiles: List<ProjectFileEntity>,
    onOpenCstrikeTree: (Uri) -> Unit,
    onEnterCstrikeFolder: (SafDocumentItem) -> Unit,
    onNavigateUpFolder: () -> Unit,
    onImportFileUri: (Uri) -> Unit,
    onExportProjectFileToCstrike: (ProjectFileEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExportPicker by remember { mutableStateOf(false) }

    val treeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onOpenCstrikeTree(uri)
    }

    val singleFileImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportFileUri(uri)
    }

    val csSubfolders = listOf("cstrike", "addons", "amxmodx", "configs", "gfx", "touch_profiles")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StudioSectionHeader(
                title = "Compatibilidad y Carpeta cstrike",
                subtitle = "Windows, Linux y Android (Xash3D / CS 1.6 Mobile)",
                badge = archInfo.primaryAbi
            )
        }

        // Architecture & Binary Compatibility Matrix
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StudioSurfaceElevated,
                border = BorderStroke(1.dp, StudioBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Memory, contentDescription = null, tint = StudioCyanAccent)
                        Text("Verificación de Arquitectura", style = MaterialTheme.typography.titleMedium, color = StudioTextPrimary)
                    }
                    Text(
                        text = "• Dispositivo actual: Android ${archInfo.androidVersion} (${archInfo.supportedAbis.joinToString()})\n" +
                            "• Binarios Windows (.dll / .exe): Solo compatibles con servidores/PC Windows x86/x64.\n" +
                            "• Binarios Linux (.so / amxxpc): Compatibles con HLDS/ReHLDS Linux x86/amd64.\n" +
                            "• Plugins compilados (.amxx) y scripts (.sma / .cfg / touch_profiles / gfx): Portables entre Windows, Linux y Android Xash3D.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            text = if (archInfo.isDesktopBinaryCompatible) "x86 Compatible" else "ARM Nativo (No x86)",
                            color = if (archInfo.isDesktopBinaryCompatible) StudioSuccess else StudioWarning
                        )
                        StatusPill(text = "SAF Cero-Permisos", color = StudioBluePrimary)
                    }
                }
            }
        }

        // Standard cstrike folder chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Estructura soportada del juego:",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioTextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(csSubfolders) { folder ->
                        AssistChip(
                            onClick = { treeLauncher.launch(null) },
                            label = { Text(folder, fontFamily = JetBrainsMonoFamily) }
                        )
                    }
                }
            }
        }

        // SAF Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { treeLauncher.launch(null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("compat_open_cstrike_btn")
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (savedCstrikeUri.isEmpty()) "Abrir carpeta cstrike" else "Cambiar carpeta")
                }
                OutlinedButton(
                    onClick = { singleFileImportLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.testTag("compat_import_file_btn")
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Importar archivo")
                }
            }
        }

        // Opened cstrike Directory Explorer
        if (savedCstrikeUri.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioSurfaceElevated,
                    border = BorderStroke(1.dp, StudioBluePrimary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (cstrikeStack.isNotEmpty()) {
                                IconButton(onClick = onNavigateUpFolder) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Subir carpeta", tint = StudioBluePrimary)
                                }
                            }
                            Text(
                                text = "Ruta: /${cstrikeStack.joinToString("/") { it.second }}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioCyanAccent
                            )
                        }
                        Button(
                            onClick = { showExportPicker = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("compat_export_here_btn")
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Exportar aquí")
                        }
                    }
                }
            }

            if (cstrikeItems.isEmpty()) {
                item {
                    Text(
                        text = "Carpeta vacía o selecciona otra subcarpeta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary
                    )
                }
            } else {
                items(cstrikeItems, key = { it.documentId }) { doc ->
                    Surface(
                        onClick = {
                            if (doc.isDirectory) onEnterCstrikeFolder(doc)
                            else onImportFileUri(doc.uri)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, StudioBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (doc.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                contentDescription = null,
                                tint = if (doc.isDirectory) StudioWarning else StudioBluePrimary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.name,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioTextPrimary
                                )
                                Text(
                                    text = if (doc.isDirectory) "Carpeta — Toca para abrir" else "${doc.sizeBytes} bytes — Toca para importar al proyecto",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportPicker) {
        AlertDialog(
            onDismissRequest = { showExportPicker = false },
            title = { Text("Exportar archivo a esta carpeta") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(projectFiles, key = { it.id }) { file ->
                        OutlinedButton(
                            onClick = {
                                onExportProjectFileToCstrike(file)
                                showExportPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${file.fileName} (${file.resolvedType().label})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportPicker = false }) { Text("Cerrar") }
            }
        )
    }
}

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.example.data.local.ProjectFileEntity
import com.example.data.local.StudioFileType
import com.example.ui.components.StudioSectionHeader
import com.example.ui.components.SyntaxCodeEditor
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioBluePrimary
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary

@Composable
fun CfgEditorScreen(
    files: List<ProjectFileEntity>,
    activeCfgFileId: Long?,
    cfgContent: String,
    hasUnsavedChanges: Boolean,
    fontSizeSp: Int,
    onSelectCfg: (ProjectFileEntity) -> Unit,
    onCfgChange: (String) -> Unit,
    onSaveCfg: () -> Unit,
    onCreateCfg: (String) -> Unit,
    onImportUri: (Uri) -> Unit,
    onExportUri: (Uri, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cfgFiles = remember(files) { files.filter { it.resolvedType() == StudioFileType.CFG } }
    val activeFile = cfgFiles.firstOrNull { it.id == activeCfgFileId }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showBindBuilder by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportUri(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) onExportUri(uri, cfgContent, activeFile?.fileName ?: "autoexec.cfg")
    }

    val quickCvars = listOf(
        "mp_roundtime 2.0",
        "mp_freezetime 3",
        "mp_startmoney 800",
        "sv_alltalk 1",
        "sv_gravity 800",
        "amx_show_activity 2",
        "cl_cmdrate 101",
        "cl_updaterate 101"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StudioSectionHeader(
            title = "Editor CFG & Binds",
            subtitle = "Configuración CS 1.6, binds, alias y comandos de consola",
            badge = "${cfgFiles.size} CFG"
        )

        // File selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cfgFiles, key = { it.id }) { file ->
                    FilterChip(
                        selected = file.id == activeCfgFileId,
                        onClick = { onSelectCfg(file) },
                        label = { Text(file.fileName, fontFamily = JetBrainsMonoFamily) }
                    )
                }
            }
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo archivo CFG", tint = StudioBluePrimary)
            }
        }

        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSaveCfg,
                enabled = activeFile != null,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("cfg_save_btn")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (hasUnsavedChanges) "Guardar*" else "Guardar")
            }
            OutlinedButton(
                onClick = { showBindBuilder = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("cfg_bind_builder_btn")
            ) {
                Icon(Icons.Filled.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Bind / Alias")
            }
            IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                Icon(Icons.Filled.FileDownload, contentDescription = "Importar CFG", tint = StudioBluePrimary)
            }
            IconButton(onClick = { exportLauncher.launch(activeFile?.fileName ?: "config.cfg") }) {
                Icon(Icons.Filled.FileUpload, contentDescription = "Exportar CFG", tint = StudioBluePrimary)
            }
        }

        // Quick Console Commands Strip
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(quickCvars) { cvar ->
                AssistChip(
                    onClick = {
                        val updated = if (cfgContent.isEmpty() || cfgContent.endsWith("\n")) {
                            "$cfgContent$cvar\n"
                        } else {
                            "$cfgContent\n$cvar\n"
                        }
                        onCfgChange(updated)
                    },
                    label = { Text(cvar, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        SyntaxCodeEditor(
            code = cfgContent,
            onCodeChange = onCfgChange,
            fileType = StudioFileType.CFG,
            fontSizeSp = fontSizeSp,
            testTag = "cfg_code_editor",
            modifier = Modifier.weight(1f)
        )
    }

    if (showCreateDialog) {
        var newCfgName by remember { mutableStateOf("amxx_custom.cfg") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear archivo .cfg") },
            text = {
                OutlinedTextField(
                    value = newCfgName,
                    onValueChange = { newCfgName = it },
                    label = { Text("Nombre del archivo") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newCfgName.isNotBlank()) onCreateCfg(newCfgName)
                    showCreateDialog = false
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showBindBuilder) {
        val keys = listOf("F1", "F2", "F3", "F4", "F5", "V", "X", "Z", "MOUSE3", "MWHEELDOWN", "SPACE")
        var selectedKey by remember { mutableStateOf("F4") }
        var isAlias by remember { mutableStateOf(false) }
        var aliasName by remember { mutableStateOf("compra_rapida") }
        var commandText by remember { mutableStateOf("say /shop") }

        AlertDialog(
            onDismissRequest = { showBindBuilder = false },
            title = { Text("Generador de Bind / Alias") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !isAlias, onClick = { isAlias = false }, label = { Text("Bind") })
                        FilterChip(selected = isAlias, onClick = { isAlias = true }, label = { Text("Alias") })
                    }
                    if (!isAlias) {
                        Text("Tecla:", style = MaterialTheme.typography.labelMedium, color = StudioTextPrimary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(keys) { key ->
                                FilterChip(
                                    selected = selectedKey == key,
                                    onClick = { selectedKey = key },
                                    label = { Text(key) }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = aliasName,
                            onValueChange = { aliasName = it },
                            label = { Text("Nombre del alias") },
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        label = { Text("Comando a ejecutar") },
                        singleLine = true
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, StudioBorderSubtle)
                    ) {
                        val preview = if (isAlias) {
                            "alias \"$aliasName\" \"$commandText\""
                        } else {
                            "bind \"$selectedKey\" \"$commandText\""
                        }
                        Text(
                            text = preview,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val line = if (isAlias) {
                        "alias \"$aliasName\" \"$commandText\""
                    } else {
                        "bind \"$selectedKey\" \"$commandText\""
                    }
                    val updated = if (cfgContent.isEmpty() || cfgContent.endsWith("\n")) {
                        "$cfgContent$line\n"
                    } else {
                        "$cfgContent\n$line\n"
                    }
                    onCfgChange(updated)
                    showBindBuilder = false
                }) { Text("Insertar") }
            },
            dismissButton = {
                TextButton(onClick = { showBindBuilder = false }) { Text("Cancelar") }
            }
        )
    }
}

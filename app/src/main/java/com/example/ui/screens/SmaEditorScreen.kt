package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ProjectFileEntity
import com.example.data.local.StudioFileType
import com.example.domain.AmxxDocumentation
import com.example.domain.DiagnosticSeverity
import com.example.domain.PawnDiagnostic
import com.example.ui.components.StatusPill
import com.example.ui.components.SyntaxCodeEditor
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioBluePrimary
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCyanAccent
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioWarning

@Composable
fun SmaEditorScreen(
    files: List<ProjectFileEntity>,
    activeFileId: Long?,
    code: String,
    hasUnsavedChanges: Boolean,
    autoSaveEnabled: Boolean,
    autoSaveStatus: String?,
    canUndo: Boolean,
    canRedo: Boolean,
    diagnostics: List<PawnDiagnostic>,
    fontSizeSp: Int,
    highlightLine: Int?,
    onSelectFile: (ProjectFileEntity) -> Unit,
    onCodeChange: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleAutoSave: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCreateFile: (String) -> Unit,
    onRenameFile: (ProjectFileEntity, String) -> Unit,
    onDuplicateFile: (ProjectFileEntity) -> Unit,
    onDeleteFile: (ProjectFileEntity) -> Unit,
    onReplaceText: (String, String, Boolean) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onImportUri: (Uri) -> Unit,
    onExportUri: (Uri, String, String) -> Unit,
    onRunStaticAnalysis: () -> Unit,
    onSendToCompile: () -> Unit,
    onSendToAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val smaFiles = remember(files) {
        files.filter {
            val t = it.resolvedType()
            t == StudioFileType.SMA || t == StudioFileType.INC
        }
    }
    val incFiles = remember(files) {
        files.filter { it.resolvedType() == StudioFileType.INC }
    }
    val activeFile = smaFiles.firstOrNull { it.id == activeFileId }
    val clipboard = LocalClipboardManager.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var createDefaultName by remember { mutableStateOf("mi_plugin.sma") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showFindReplace by remember { mutableStateOf(false) }
    var showFilesPanel by remember { mutableStateOf(false) }
    var showFileMenu by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportUri(uri)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            onExportUri(uri, code, activeFile?.fileName ?: "plugin.sma")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // File Tabs Row + Project Files Panel Toggle + Create New SMA/INC
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = { showFilesPanel = !showFilesPanel },
                modifier = Modifier.testTag("sma_files_panel_btn")
            ) {
                Icon(
                    Icons.Filled.FolderOpen,
                    contentDescription = "Panel de archivos del proyecto",
                    tint = if (showFilesPanel) StudioCyanAccent else StudioBluePrimary
                )
            }
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(smaFiles, key = { it.id }) { file ->
                    val selected = file.id == activeFileId
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFile(file) },
                        label = {
                            Text(
                                text = if (selected && hasUnsavedChanges) "${file.fileName} *" else file.fileName,
                                fontFamily = JetBrainsMonoFamily
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioBluePrimary.copy(alpha = 0.22f),
                            selectedLabelColor = StudioCyanAccent
                        )
                    )
                }
            }
            IconButton(
                onClick = {
                    createDefaultName = "mi_plugin.sma"
                    showCreateDialog = true
                },
                modifier = Modifier.testTag("sma_new_file_btn")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo archivo SMA o INC", tint = StudioBluePrimary)
            }
            IconButton(
                onClick = { showFileMenu = true },
                modifier = Modifier.testTag("sma_file_menu_btn")
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Opciones de archivo", tint = StudioTextPrimary)
                DropdownMenu(
                    expanded = showFileMenu,
                    onDismissRequest = { showFileMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Importar .sma / .inc desde almacenamiento") },
                        leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                        onClick = {
                            showFileMenu = false
                            importLauncher.launch(arrayOf("*/*"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Exportar archivo actual") },
                        leadingIcon = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
                        onClick = {
                            showFileMenu = false
                            exportLauncher.launch(activeFile?.fileName ?: "plugin.sma")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Crear include (.inc)") },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        onClick = {
                            showFileMenu = false
                            createDefaultName = "custom_natives.inc"
                            showCreateDialog = true
                        }
                    )
                    if (activeFile != null) {
                        DropdownMenuItem(
                            text = { Text("Duplicar archivo") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = {
                                showFileMenu = false
                                onDuplicateFile(activeFile)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Renombrar archivo") },
                            leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null) },
                            onClick = {
                                showFileMenu = false
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar archivo") },
                            leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = StudioError) },
                            onClick = {
                                showFileMenu = false
                                onDeleteFile(activeFile)
                            }
                        )
                    }
                }
            }
        }

        // Collapsible Project Files & Includes Panel
        AnimatedVisibility(visible = showFilesPanel) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StudioSurfaceCard,
                border = BorderStroke(1.dp, StudioBluePrimary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Archivos del proyecto (${files.size}) • ${incFiles.size} includes (.inc)",
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioTextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    createDefaultName = "custom_include.inc"
                                    showCreateDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("+ .inc", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("*/*")) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Importar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(smaFiles, key = { it.id }) { file ->
                            val isSelected = file.id == activeFileId
                            Surface(
                                onClick = {
                                    onSelectFile(file)
                                    showFilesPanel = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) StudioBluePrimary.copy(alpha = 0.2f) else StudioSurfaceElevated
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.fileName, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
                                        Text(file.relativePath, style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                                    }
                                    StatusPill(
                                        text = file.resolvedType().extension.uppercase(),
                                        color = if (file.resolvedType() == StudioFileType.INC) StudioCyanAccent else StudioBluePrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Toolbar: Save, Undo/Redo, Clipboard, Find, Static Analyze, Real Compile, AI, Auto-Save
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StudioSurfaceElevated,
            border = BorderStroke(1.dp, StudioBorderSubtle)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onSave,
                        enabled = activeFile != null,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sma_save_btn")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Guardar", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (hasUnsavedChanges) "Guardar*" else "Guardar")
                    }

                    IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.testTag("sma_undo_btn")) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer",
                            tint = if (canUndo) StudioCyanAccent else StudioTextSecondary.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.testTag("sma_redo_btn")) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Rehacer",
                            tint = if (canRedo) StudioCyanAccent else StudioTextSecondary.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(code)) },
                        enabled = code.isNotEmpty(),
                        modifier = Modifier.testTag("sma_copy_btn")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar todo", tint = StudioTextPrimary)
                    }
                    IconButton(
                        onClick = {
                            if (code.isNotEmpty()) {
                                clipboard.setText(AnnotatedString(code))
                                onCodeChange("")
                            }
                        },
                        enabled = code.isNotEmpty(),
                        modifier = Modifier.testTag("sma_cut_btn")
                    ) {
                        Icon(Icons.Filled.ContentCut, contentDescription = "Cortar todo", tint = StudioTextPrimary)
                    }
                    IconButton(
                        onClick = {
                            val clip = clipboard.getText()?.text
                            if (!clip.isNullOrEmpty()) {
                                val updated = if (code.isEmpty() || code.endsWith("\n")) "$code$clip" else "$code\n$clip"
                                onCodeChange(updated)
                            }
                        },
                        modifier = Modifier.testTag("sma_paste_btn")
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = "Pegar desde portapapeles", tint = StudioTextPrimary)
                    }

                    OutlinedButton(
                        onClick = { showFindReplace = !showFindReplace },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sma_find_btn")
                    ) {
                        Icon(Icons.Filled.FindReplace, contentDescription = "Buscar y reemplazar", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Buscar")
                    }

                    IconButton(onClick = { onFontSizeChange(fontSizeSp - 1) }) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Reducir fuente", tint = StudioTextSecondary)
                    }
                    Text("${fontSizeSp}sp", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                    IconButton(onClick = { onFontSizeChange(fontSizeSp + 1) }) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Aumentar fuente", tint = StudioTextSecondary)
                    }

                    OutlinedButton(
                        onClick = onRunStaticAnalysis,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sma_static_analyze_btn")
                    ) {
                        Icon(Icons.Filled.BugReport, contentDescription = "Analizador estático", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Analizar")
                    }

                    Button(
                        onClick = {
                            onSave()
                            onSendToCompile()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioWarning.copy(alpha = 0.2f), contentColor = StudioWarning),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sma_compile_btn")
                    ) {
                        Icon(Icons.Filled.Build, contentDescription = "Compilar con amxxpc", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Compilar")
                    }

                    Button(
                        onClick = onSendToAi,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioBluePrimary.copy(alpha = 0.2f), contentColor = StudioCyanAccent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sma_ai_btn")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Asistente IA", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gemini IA")
                    }
                }

                // Auto-save compact strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Autoguardado", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                        Switch(checked = autoSaveEnabled, onCheckedChange = onToggleAutoSave)
                    }
                    if (!autoSaveStatus.isNullOrBlank()) {
                        StatusPill(text = autoSaveStatus, color = StudioSuccess)
                    } else if (hasUnsavedChanges) {
                        StatusPill(text = "Cambios pendientes", color = StudioWarning)
                    }
                }
            }
        }

        // Find & Replace Drawer
        AnimatedVisibility(visible = showFindReplace) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StudioSurfaceCard,
                border = BorderStroke(1.dp, StudioBluePrimary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = findText,
                            onValueChange = { findText = it },
                            label = { Text("Buscar") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = replaceText,
                            onValueChange = { replaceText = it },
                            label = { Text("Reemplazar por") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onReplaceText(findText, replaceText, false) }) {
                            Text("Reemplazar uno")
                        }
                        OutlinedButton(onClick = { onReplaceText(findText, replaceText, true) }) {
                            Text("Reemplazar todo")
                        }
                    }
                }
            }
        }

        // Pawn Autocomplete Strip
        val lastWord = remember(code) {
            code.takeLast(40).substringAfterLast('\n').substringAfterLast(' ').substringAfterLast('\t')
        }
        val suggestions = remember(lastWord) {
            val filtered = if (lastWord.length >= 2) {
                AmxxDocumentation.autocompleteTokens.filter {
                    it.first.contains(lastWord, ignoreCase = true)
                }
            } else {
                emptyList()
            }
            filtered.ifEmpty { AmxxDocumentation.autocompleteTokens.take(10) }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(suggestions) { (label, snippet) ->
                AssistChip(
                    onClick = {
                        val updated = if (code.isEmpty() || code.endsWith("\n")) {
                            "$code$snippet\n"
                        } else {
                            "$code\n$snippet"
                        }
                        onCodeChange(updated)
                    },
                    label = {
                        Text(label, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = StudioSurfaceElevated,
                        labelColor = StudioCyanAccent
                    )
                )
            }
        }

        // Main Syntax-Highlighted Editor
        SyntaxCodeEditor(
            code = code,
            onCodeChange = onCodeChange,
            fileType = activeFile?.resolvedType() ?: StudioFileType.SMA,
            fontSizeSp = fontSizeSp,
            highlightLine = highlightLine ?: diagnostics.firstOrNull { it.severity == DiagnosticSeverity.ERROR }?.line,
            testTag = "sma_code_editor",
            modifier = Modifier.weight(1f)
        )

        // Live Static Diagnostics Footer (explicitly labeled as static analysis, NOT compilation)
        val errors = diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
        val warnings = diagnostics.count { it.severity == DiagnosticSeverity.WARNING }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = StudioSurfaceElevated,
            border = BorderStroke(1.dp, StudioBorderSubtle)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Analizador estático (no compila .amxx)",
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioTextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            text = "$errors errores",
                            color = if (errors > 0) StudioError else StudioSuccess
                        )
                        StatusPill(
                            text = "$warnings avisos",
                            color = if (warnings > 0) StudioWarning else StudioBluePrimary
                        )
                    }
                }
                if (diagnostics.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 84.dp)) {
                        items(diagnostics.take(5)) { diag ->
                            val color = when (diag.severity) {
                                DiagnosticSeverity.ERROR -> StudioError
                                DiagnosticSeverity.WARNING -> StudioWarning
                                DiagnosticSeverity.INFO -> StudioBluePrimary
                            }
                            Text(
                                text = "L${diag.line}: [${diag.code}] ${diag.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var newName by remember(createDefaultName) { mutableStateOf(createDefaultName) }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nuevo archivo Pawn (.sma / .inc)") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nombre del archivo (.sma o .inc)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) onCreateFile(newName)
                    showCreateDialog = false
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showRenameDialog && activeFile != null) {
        var renameValue by remember { mutableStateOf(activeFile.fileName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renombrar ${activeFile.fileName}") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameFile(activeFile, renameValue)
                    showRenameDialog = false
                }) { Text("Renombrar") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

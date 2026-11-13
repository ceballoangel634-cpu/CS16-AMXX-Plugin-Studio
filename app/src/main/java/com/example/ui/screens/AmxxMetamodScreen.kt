package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ProjectFileEntity
import com.example.domain.AmxxMetamodAnalyzer
import com.example.domain.ConfigIssue
import com.example.domain.DiagnosticSeverity
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioSectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioBluePrimary
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCyanAccent
import com.example.ui.theme.StudioError
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioWarning

@Composable
fun AmxxMetamodScreen(
    files: List<ProjectFileEntity>,
    targetServerOs: String,
    configIssues: List<ConfigIssue>,
    onSelectServerOs: (String) -> Unit,
    onUpdateConfigFile: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("AMXX plugins.ini", "modules.ini", "Metamod", "Auditoría")

    val pluginsIniContent = remember(files) {
        files.firstOrNull { it.fileName.equals("plugins.ini", true) && !it.relativePath.contains("metamod", true) }?.content.orEmpty()
    }
    val modulesIniContent = remember(files) {
        files.firstOrNull { it.fileName.equals("modules.ini", true) }?.content.orEmpty()
    }
    val metamodIniContent = remember(files) {
        files.firstOrNull { it.fileName.contains("metamod", true) || it.relativePath.contains("metamod", true) }?.content.orEmpty()
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StudioSectionHeader(
            title = "AMX Mod X & Metamod",
            subtitle = "Diferencia plugins .amxx de extensiones nativas .so / .dll",
            badge = "${configIssues.size} alertas"
        )

        // Target Server OS Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Servidor destino:", style = MaterialTheme.typography.labelMedium, color = StudioTextSecondary)
            listOf("Linux (.so)", "Windows (.dll)").forEach { os ->
                FilterChip(
                    selected = targetServerOs == os,
                    onClick = { onSelectServerOs(os) },
                    label = { Text(os) }
                )
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, maxLines = 1) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                val entries = remember(pluginsIniContent) {
                    AmxxMetamodAnalyzer.parseAmxxPluginsIni(pluginsIniContent)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(text = "AMX Mod X (.amxx)", color = StudioBluePrimary)
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("amxx_add_plugin_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir .amxx")
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioSurfaceElevated,
                            border = BorderStroke(1.dp, StudioBorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Extension, contentDescription = null, tint = StudioBluePrimary)
                                    Column {
                                        Text(
                                            text = entry.pluginName,
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (entry.isEnabled) StudioTextPrimary else StudioTextSecondary
                                        )
                                        Text(
                                            text = "${entry.targetOsLabel} • Línea ${entry.lineNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StudioTextSecondary
                                        )
                                    }
                                }
                                Switch(
                                    checked = entry.isEnabled,
                                    onCheckedChange = { enabled ->
                                        val updatedLines = pluginsIniContent.lines().toMutableList()
                                        val idx = entry.lineNumber - 1
                                        if (idx in updatedLines.indices) {
                                            val clean = updatedLines[idx].trim().trimStart(';', '/', ' ')
                                            updatedLines[idx] = if (enabled) clean else "; $clean"
                                            onUpdateConfigFile(
                                                "plugins.ini",
                                                "addons/amxmodx/configs/plugins.ini",
                                                "plugins.ini",
                                                updatedLines.joinToString("\n")
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                val modLines = remember(modulesIniContent) {
                    modulesIniContent.lines().mapIndexedNotNull { idx, raw ->
                        val t = raw.trim()
                        if (t.isEmpty() || t.startsWith("; ") && t.length > 25) null
                        else Triple(idx + 1, t.trimStart(';', ' '), !t.startsWith(";"))
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(modLines) { (lineNum, name, active) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioSurfaceElevated,
                            border = BorderStroke(1.dp, StudioBorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(name, fontFamily = JetBrainsMonoFamily, color = StudioTextPrimary)
                                    Text("Módulo AMX Mod X • Línea $lineNum", style = MaterialTheme.typography.bodySmall, color = StudioTextSecondary)
                                }
                                Switch(
                                    checked = active,
                                    onCheckedChange = { enable ->
                                        val lines = modulesIniContent.lines().toMutableList()
                                        val idx = lineNum - 1
                                        if (idx in lines.indices) {
                                            val clean = lines[idx].trim().trimStart(';', ' ')
                                            lines[idx] = if (enable) clean else ";$clean"
                                            onUpdateConfigFile(
                                                "modules.ini",
                                                "addons/amxmodx/configs/modules.ini",
                                                "modules.ini",
                                                lines.joinToString("\n")
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                val metamodEntries = remember(metamodIniContent) {
                    AmxxMetamodAnalyzer.parseMetamodPluginsIni(metamodIniContent)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(text = "Metamod Nativo (.so / .dll)", color = Color(0xFF00BFA5))
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("metamod_add_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir módulo")
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(metamodEntries) { entry ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioSurfaceElevated,
                            border = BorderStroke(1.dp, Color(0xFF00BFA5).copy(alpha = 0.4f))
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
                                    Icon(Icons.Filled.Hub, contentDescription = null, tint = Color(0xFF00BFA5))
                                    Column {
                                        Text(
                                            text = entry.pluginName,
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StudioTextPrimary
                                        )
                                        Text(
                                            text = entry.targetOsLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StudioCyanAccent
                                        )
                                    }
                                }
                                Switch(
                                    checked = entry.isEnabled,
                                    onCheckedChange = { enable ->
                                        val lines = metamodIniContent.lines().toMutableList()
                                        val idx = entry.lineNumber - 1
                                        if (idx in lines.indices) {
                                            val clean = lines[idx].trim().trimStart(';', '/', ' ')
                                            lines[idx] = if (enable) clean else "; $clean"
                                            onUpdateConfigFile(
                                                "metamod",
                                                "addons/metamod/plugins.ini",
                                                "metamod_plugins.ini",
                                                lines.joinToString("\n")
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            3 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (configIssues.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StudioSuccess.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, StudioSuccess)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StudioSuccess)
                                    Text("Sin errores de compatibilidad entre AMX Mod X, modules.ini y Metamod.", color = StudioTextPrimary)
                                }
                            }
                        }
                    } else {
                        items(configIssues) { issue ->
                            val color = when (issue.severity) {
                                DiagnosticSeverity.ERROR -> StudioError
                                DiagnosticSeverity.WARNING -> StudioWarning
                                DiagnosticSeverity.INFO -> StudioBluePrimary
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StudioSurfaceElevated,
                                border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (issue.severity == DiagnosticSeverity.ERROR) Icons.Filled.Error else Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = color
                                    )
                                    Column {
                                        Text(
                                            text = "${issue.fileName} (Línea ${issue.line})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = color
                                        )
                                        Text(issue.message, style = MaterialTheme.typography.bodySmall, color = StudioTextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val isMetamod = selectedTab == 2
        var newEntry by remember {
            mutableStateOf(
                if (isMetamod) "linux addons/podbot/podbot_mm_i386.so" else "mi_plugin.amxx"
            )
        }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (isMetamod) "Añadir módulo Metamod" else "Añadir plugin AMX Mod X") },
            text = {
                OutlinedTextField(
                    value = newEntry,
                    onValueChange = { newEntry = it },
                    label = { Text(if (isMetamod) "Ej: linux addons/.../mod.so" else "Ej: vip_system.amxx") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newEntry.isNotBlank()) {
                        if (isMetamod) {
                            onUpdateConfigFile(
                                "metamod",
                                "addons/metamod/plugins.ini",
                                "metamod_plugins.ini",
                                "$metamodIniContent\n${newEntry.trim()}\n"
                            )
                        } else {
                            onUpdateConfigFile(
                                "plugins.ini",
                                "addons/amxmodx/configs/plugins.ini",
                                "plugins.ini",
                                "$pluginsIniContent\n${newEntry.trim()}\n"
                            )
                        }
                    }
                    showAddDialog = false
                }) { Text("Añadir") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

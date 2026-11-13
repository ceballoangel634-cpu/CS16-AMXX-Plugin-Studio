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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.CompilationResult
import com.example.domain.CompilerExecutionState
import com.example.domain.DiagnosticSeverity
import com.example.domain.PlatformArchitectureInfo
import com.example.domain.ServerHealthResult
import com.example.domain.ServerHealthStatus
import com.example.domain.StaticAnalysisReport
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioSectionHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioBgObsidian
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
fun CompilerScreen(
    activeSmaFileName: String?,
    customIncludeCount: Int,
    compilerVersion: String,
    compilerServerUrl: String,
    archInfo: PlatformArchitectureInfo,
    isCompiling: Boolean,
    compilerState: CompilerExecutionState,
    compileProgress: Float,
    serverHealth: ServerHealthResult,
    compilationResult: CompilationResult?,
    staticAnalysisReport: StaticAnalysisReport?,
    onSelectCompilerVersion: (String) -> Unit,
    onSaveServerUrl: (String) -> Unit,
    onCheckServerHealth: (String) -> Unit,
    onRunStaticAnalysis: () -> Unit,
    onCompile: () -> Unit,
    onJumpToEditorLine: (Int) -> Unit,
    onExportAmxxUri: (Uri, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val versions = listOf("AMX Mod X 1.10.0", "AMX Mod X 1.9.0", "AMX Mod X 1.8.2")
    var urlInput by remember(compilerServerUrl) { mutableStateOf(compilerServerUrl) }
    var showServerGuide by remember { mutableStateOf(false) }

    val amxxExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && compilationResult != null) {
            onExportAmxxUri(uri, compilationResult.outputFileName)
        }
    }

    val stateColor = when (compilerState) {
        CompilerExecutionState.IDLE -> StudioBluePrimary
        CompilerExecutionState.CONNECTING -> StudioCyanAccent
        CompilerExecutionState.COMPILING -> StudioCyanAccent
        CompilerExecutionState.SUCCESS -> StudioSuccess
        CompilerExecutionState.COMPILE_ERROR -> StudioError
        CompilerExecutionState.SERVER_UNAVAILABLE -> StudioWarning
    }

    val healthColor = when (serverHealth.status) {
        ServerHealthStatus.ONLINE -> StudioSuccess
        ServerHealthStatus.CHECKING -> StudioCyanAccent
        ServerHealthStatus.OFFLINE -> StudioError
        ServerHealthStatus.NOT_CONFIGURED -> StudioWarning
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StudioSectionHeader(
                title = "Compilador Real AMXX (amxxpc)",
                subtitle = "Archivo: ${activeSmaFileName ?: "Ninguno"} • $customIncludeCount include(s) .inc del proyecto",
                badge = compilerState.label
            )
        }

        // Section 1: Compiler Configuration & Server Health
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StudioSurfaceElevated,
                border = BorderStroke(1.dp, StudioBorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Memory, contentDescription = null, tint = StudioCyanAccent)
                            Text("Servidor Compilador amxxpc", style = MaterialTheme.typography.titleMedium, color = StudioTextPrimary)
                        }
                        IconButton(onClick = { showServerGuide = true }) {
                            Icon(Icons.Filled.HelpOutline, contentDescription = "Guía del servidor Docker/PC", tint = StudioBluePrimary)
                        }
                    }

                    // Server status pill + version chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (serverHealth.status == ServerHealthStatus.ONLINE) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = healthColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = serverHealth.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = healthColor
                            )
                        }
                    }

                    Text(
                        text = "Versión oficial amxxpc:",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudioTextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        versions.forEach { ver ->
                            FilterChip(
                                selected = compilerVersion == ver,
                                onClick = { onSelectCompilerVersion(ver) },
                                label = { Text(ver.removePrefix("AMX Mod X ")) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("URL del servidor amxxpc") },
                            placeholder = { Text("http://192.168.1.50:8080") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("compiler_server_url_input")
                        )
                        OutlinedButton(
                            onClick = {
                                onSaveServerUrl(urlInput)
                                onCheckServerHealth(urlInput)
                            },
                            modifier = Modifier.testTag("compiler_verify_server_btn")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Verificar servidor", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Verificar")
                        }
                    }

                    Text(
                        text = "Dispositivo: Android ${archInfo.androidVersion} (${archInfo.primaryAbi}). " +
                            if (archInfo.isDesktopBinaryCompatible) {
                                "Arquitectura x86 detectada."
                            } else {
                                "ARM no ejecuta binarios x86 de PC de forma nativa; las compilaciones reales se ejecutan en el servidor Docker/Node.js con amxxpc."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary
                    )
                }
            }
        }

        // Section 2: Real Compilation Action & Progress & 6-State Banner
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StudioSurfaceCard,
                border = BorderStroke(1.dp, stateColor.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estado de compilación",
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioTextPrimary
                        )
                        StatusPill(text = compilerState.label, color = stateColor)
                    }

                    if (isCompiling || compileProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { compileProgress.coerceIn(0f, 1f) },
                            color = stateColor,
                            trackColor = StudioBgObsidian,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onSaveServerUrl(urlInput)
                                onCompile()
                            },
                            enabled = !isCompiling && activeSmaFileName != null,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("compiler_run_btn")
                        ) {
                            if (isCompiling) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(compilerState.label)
                            } else {
                                Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("COMPILAR SMA → AMXX")
                            }
                        }

                        Button(
                            onClick = {
                                compilationResult?.let { amxxExportLauncher.launch(it.outputFileName) }
                            },
                            enabled = compilationResult?.succeeded == true && compilationResult.amxxBytes != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioSuccess,
                                contentColor = StudioBgObsidian
                            ),
                            modifier = Modifier.testTag("compiler_download_amxx_btn")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Descargar .amxx")
                        }
                    }
                }
            }
        }

        // Section 3: Authentic amxxpc Result Banner, Metrics, Console & Diagnostics
        if (compilationResult != null) {
            val resultColor = when (compilationResult.executionState) {
                CompilerExecutionState.SUCCESS -> StudioSuccess
                CompilerExecutionState.COMPILE_ERROR -> StudioError
                CompilerExecutionState.SERVER_UNAVAILABLE -> StudioWarning
                else -> StudioBluePrimary
            }
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StudioSurfaceElevated,
                    border = BorderStroke(1.dp, resultColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (compilationResult.executionState) {
                                        CompilerExecutionState.SUCCESS -> Icons.Filled.CheckCircle
                                        CompilerExecutionState.COMPILE_ERROR -> Icons.Filled.Error
                                        else -> Icons.Filled.Warning
                                    },
                                    contentDescription = null,
                                    tint = resultColor
                                )
                                Column {
                                    Text(
                                        text = if (compilationResult.succeeded) {
                                            "Generado: ${compilationResult.outputFileName} (${compilationResult.amxxBytes?.size ?: 0} bytes)"
                                        } else {
                                            compilationResult.executionState.label
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioTextPrimary
                                    )
                                    Text(
                                        text = compilationResult.modeUsed,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioTextSecondary
                                    )
                                }
                            }
                            StatusPill(
                                text = compilationResult.compilerVersion.removePrefix("AMX Mod X "),
                                color = StudioBluePrimary
                            )
                        }

                        // Real compiler telemetry row: errors, warnings, duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusPill(
                                text = "${compilationResult.errorCount} errores",
                                color = if (compilationResult.errorCount > 0) StudioError else StudioSuccess
                            )
                            StatusPill(
                                text = "${compilationResult.warningCount} advertencias",
                                color = if (compilationResult.warningCount > 0) StudioWarning else StudioBluePrimary
                            )
                            StatusPill(
                                text = "${compilationResult.durationMs} ms",
                                color = StudioCyanAccent
                            )
                        }

                        Text(
                            text = "Consola oficial de amxxpc:",
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioTextSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StudioBgObsidian,
                            border = BorderStroke(1.dp, StudioBorderSubtle)
                        ) {
                            Text(
                                text = compilationResult.consoleOutput,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            )
                        }
                    }
                }
            }

            if (compilationResult.diagnostics.isNotEmpty()) {
                item {
                    Text(
                        text = "Errores y advertencias reales (toca para ir a la línea)",
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioTextPrimary
                    )
                }
                items(compilationResult.diagnostics) { diag ->
                    val color = when (diag.severity) {
                        DiagnosticSeverity.ERROR -> StudioError
                        DiagnosticSeverity.WARNING -> StudioWarning
                        DiagnosticSeverity.INFO -> StudioBluePrimary
                    }
                    Surface(
                        onClick = { onJumpToEditorLine(diag.line) },
                        shape = RoundedCornerShape(10.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (diag.severity == DiagnosticSeverity.ERROR) Icons.Filled.Error else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = color
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Línea ${diag.line} • Código ${diag.code}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = color
                                )
                                Text(
                                    text = diag.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Strictly Separated Static Pawn Analyzer Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StudioSurfaceElevated,
                border = BorderStroke(1.dp, StudioBluePrimary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BugReport, contentDescription = null, tint = StudioBluePrimary)
                            Column {
                                Text("Analizador Estático Pawn (Local)", style = MaterialTheme.typography.titleSmall, color = StudioTextPrimary)
                                Text(
                                    "Revisión previa en el dispositivo — NO compila ni genera .amxx",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioTextSecondary
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onRunStaticAnalysis,
                            enabled = activeSmaFileName != null,
                            modifier = Modifier.testTag("compiler_static_analyze_btn")
                        ) {
                            Text("Analizar")
                        }
                    }

                    if (staticAnalysisReport != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(
                                text = "${staticAnalysisReport.errorCount} errores sintácticos",
                                color = if (staticAnalysisReport.errorCount > 0) StudioError else StudioBluePrimary
                            )
                            StatusPill(
                                text = "${staticAnalysisReport.warningCount} advertencias",
                                color = if (staticAnalysisReport.warningCount > 0) StudioWarning else StudioBluePrimary
                            )
                        }
                        staticAnalysisReport.diagnostics.take(8).forEach { diag ->
                            val c = if (diag.severity == DiagnosticSeverity.ERROR) StudioError else StudioWarning
                            Surface(
                                onClick = { onJumpToEditorLine(diag.line) },
                                shape = RoundedCornerShape(8.dp),
                                color = StudioBgObsidian
                            ) {
                                Text(
                                    text = "L${diag.line} [${diag.code}]: ${diag.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c,
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

    if (showServerGuide) {
        AlertDialog(
            onDismissRequest = { showServerGuide = false },
            title = { Text("Servidor Compilador Real (amxxpc)") },
            text = {
                Text(
                    text = "El proyecto incluye un servidor aislado en /compiler-server con Docker y Node.js ejecutando amxxpc oficial de AMX Mod X 1.9.0 y 1.10.0.\n\n" +
                        "En tu PC o VPS ejecuta:\n" +
                        "cd compiler-server && docker compose up --build\n\n" +
                        "Luego escribe la IP y puerto (ej. http://192.168.1.50:8080) en el campo URL y pulsa Verificar.\n\n" +
                        "Endpoints:\n" +
                        "• GET /health (estado de amxxpc 1.9.0 y 1.10.0)\n" +
                        "• POST /compile (envía .sma + .inc y devuelve consola real + .amxx)",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showServerGuide = false }) { Text("Entendido") }
            }
        )
    }
}

package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiTaskMode
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
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiUiState

private data class AiQuickPreset(
    val label: String,
    val mode: AiTaskMode,
    val prompt: String
)

private val aiQuickPresets = listOf(
    AiQuickPreset(
        label = "Menú de armas",
        mode = AiTaskMode.CREATE_AMXX_MENU,
        prompt = "Crea un menú de armas /armas para CS 1.6 con AK47, M4A1, AWP y Deagle, con recarga completa y bloqueo en ronda de pistolas."
    ),
    AiQuickPreset(
        label = "Sistema VIP",
        mode = AiTaskMode.CREATE_FROM_IDEA,
        prompt = "Crea un sistema VIP completo (flag ADMIN_LEVEL_H) con doble salto, +15 HP por kill, chaleco gratis al respawn y menú /vipmenu."
    ),
    AiQuickPreset(
        label = "Sistema Admin",
        mode = AiTaskMode.CREATE_FROM_IDEA,
        prompt = "Crea un plugin de administración con menú /adminmenu para kick, slay, slap, ban temporal y cambio de mapa con logs."
    ),
    AiQuickPreset(
        label = "Dinero y recompensas",
        mode = AiTaskMode.CREATE_FROM_IDEA,
        prompt = "Crea un sistema de recompensas de dinero ($) por kill, headshot, plantar/desactivar bomba y límite configurable por cvar."
    ),
    AiQuickPreset(
        label = "Respawn automático",
        mode = AiTaskMode.CREATE_FROM_IDEA,
        prompt = "Crea un sistema de respawn automático con protección spawn de 2 segundos, efecto glow y cvars para activarlo o desactivarlo."
    ),
    AiQuickPreset(
        label = "Munición infinita",
        mode = AiTaskMode.CREATE_FROM_IDEA,
        prompt = "Crea un plugin de munición infinita sin recarga usando HamSandwich/CurWeapon con cvar para activar solo a VIP o a todos."
    ),
    AiQuickPreset(
        label = "Comandos personalizados",
        mode = AiTaskMode.CREATE_AMXX_MENU,
        prompt = "Crea un plugin con comandos de chat /reglas, /resetscore, /vida y /hora con mensajes de color y menú informativo."
    ),
    AiQuickPreset(
        label = "Bots compatibles",
        mode = AiTaskMode.CREATE_FROM_IDEA,
        prompt = "Crea un plugin de gestor automático de bots (CSBot / PODBot) con balanceo de equipos y comandos para ajustar cuota de bots."
    )
)

@Composable
fun AiAssistantScreen(
    uiState: AiUiState,
    messages: List<AiChatMessage>,
    activeSmaFileName: String?,
    onSendPrompt: (AiTaskMode, String, Boolean, String, Boolean) -> Unit,
    onApplyCodeToEditor: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(AiTaskMode.CREATE_FROM_IDEA) }
    var promptText by remember { mutableStateOf("") }
    var attachActiveSma by remember { mutableStateOf(false) }
    var showPasteBox by remember { mutableStateOf(false) }
    var pastedCode by remember { mutableStateOf("") }
    var useFastModel by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StudioSectionHeader(
            title = "Asistente Gemini Pawn IA",
            subtitle = "Plugins completos, análisis SMA, corrección y migración AMXX 1.9/1.10",
            badge = if (useFastModel) "Flash-Lite" else "3.8 Flash"
        )

        // Task mode chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AiTaskMode.entries) { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = {
                        selectedMode = mode
                        attachActiveSma = mode.autoAttachActiveFile
                        if (mode == AiTaskMode.MODIFY_CODE) showPasteBox = true
                    },
                    label = { Text(mode.title) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioBluePrimary.copy(alpha = 0.25f),
                        selectedLabelColor = StudioCyanAccent
                    )
                )
            }
        }

        // Quick CS 1.6 preset chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(aiQuickPresets) { preset ->
                AssistChip(
                    onClick = {
                        selectedMode = preset.mode
                        attachActiveSma = preset.mode.autoAttachActiveFile
                        promptText = preset.prompt
                    },
                    label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = StudioSurfaceCard,
                        labelColor = StudioCyanAccent
                    )
                )
            }
        }

        // Context toggles: attach active SMA, paste code, fast model toggle
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StudioSurfaceElevated,
            border = BorderStroke(1.dp, StudioBorderSubtle)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = StudioCyanAccent, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Modo ultra rápido (Flash-Lite)",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextPrimary
                        )
                    }
                    Switch(
                        checked = useFastModel,
                        onCheckedChange = { useFastModel = it }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeSmaFileName != null) "Adjuntar archivo activo ($activeSmaFileName)" else "Sin archivo SMA activo",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary
                    )
                    Switch(
                        checked = attachActiveSma && activeSmaFileName != null,
                        enabled = activeSmaFileName != null,
                        onCheckedChange = { attachActiveSma = it }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pegar código Pawn personalizado",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary
                    )
                    Switch(
                        checked = showPasteBox,
                        onCheckedChange = { showPasteBox = it }
                    )
                }
                AnimatedVisibility(visible = showPasteBox) {
                    OutlinedTextField(
                        value = pastedCode,
                        onValueChange = { pastedCode = it },
                        label = { Text("Pega aquí tu código .sma para analizar o modificar") },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }
            }
        }

        // Chat / Streaming Area
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && uiState is AiUiState.Idle) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, StudioBorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = StudioCyanAccent)
                            Text(
                                text = "Asistente experto en AMX Mod X & Metamod",
                                style = MaterialTheme.typography.titleMedium,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = "Describe tu idea (ej: \"Plugin VIP con doble salto, menú /vipmenu y 15 HP por kill\") o elige un modo arriba para corregir o explicar tu plugin actual.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextSecondary
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (msg.isUser) StudioBluePrimary.copy(alpha = 0.14f) else StudioSurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (msg.isUser) StudioBluePrimary.copy(alpha = 0.45f) else StudioBorderSubtle
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPill(
                            text = if (msg.isUser) "Tú • ${msg.modeTitle}" else msg.modeTitle,
                            color = if (msg.isUser) StudioBluePrimary else StudioCyanAccent
                        )
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioTextPrimary
                        )
                        if (!msg.extractedCode.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StudioBgObsidian,
                                border = BorderStroke(1.dp, StudioBluePrimary.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Código Pawn detectado (${msg.extractedCode.lines().size} líneas)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StudioSuccess
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = msg.extractedCode,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StudioTextSecondary,
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .heightIn(max = 180.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onApplyCodeToEditor(msg.extractedCode, null) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Aplicar al Editor")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onApplyCodeToEditor(msg.extractedCode, "gemini_plugin_${msg.id % 1000}.sma")
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Nuevo .sma")
                                        }
                                        OutlinedButton(
                                            onClick = { clipboard.setText(AnnotatedString(msg.extractedCode)) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar código", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState is AiUiState.Loading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = if (useFastModel) "Generando con Gemini 3.5 Flash-Lite..." else "Conectando con Gemini 3.8 Flash...",
                            color = StudioTextSecondary
                        )
                    }
                }
            } else if (uiState is AiUiState.Streaming) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioSurfaceCard,
                        border = BorderStroke(1.dp, StudioCyanAccent.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            StatusPill(text = "Generando código en vivo...", color = StudioCyanAccent)
                            Spacer(Modifier.height(6.dp))
                            Text(uiState.text, style = MaterialTheme.typography.bodyMedium, color = StudioTextPrimary)
                        }
                    }
                }
            } else if (uiState is AiUiState.Error) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioError.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, StudioError)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = StudioError)
                            Text(uiState.message, style = MaterialTheme.typography.bodySmall, color = StudioError)
                        }
                    }
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = { Text("Ej: Crea un menú /armas para CT con M4A1 y Deagle") },
                maxLines = 3,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input")
            )
            Button(
                onClick = {
                    onSendPrompt(selectedMode, promptText, attachActiveSma, pastedCode, useFastModel)
                    promptText = ""
                },
                enabled = uiState !is AiUiState.Loading && uiState !is AiUiState.Streaming &&
                    (promptText.isNotBlank() || attachActiveSma || pastedCode.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = StudioBluePrimary),
                modifier = Modifier.testTag("ai_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar a Gemini")
            }
        }
    }
}

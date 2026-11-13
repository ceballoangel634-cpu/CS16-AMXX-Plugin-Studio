package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiTaskMode
import com.example.data.ai.GeminiAssistantRepository
import com.example.data.files.FilePortabilityManager
import com.example.data.files.SafDocumentItem
import com.example.data.local.ChangeHistoryEntity
import com.example.data.local.CompilationHistoryEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.data.local.StudioDatabase
import com.example.data.local.StudioFileType
import com.example.data.local.StudioRepository
import com.example.data.preferences.StudioPreferences
import com.example.data.preferences.StudioSettings
import com.example.domain.AmxxMetamodAnalyzer
import com.example.domain.CompilationResult
import com.example.domain.CompilerExecutionState
import com.example.domain.CompilerService
import com.example.domain.ConfigIssue
import com.example.domain.CustomIncludePayload
import com.example.domain.PawnDiagnostic
import com.example.domain.PawnStaticAnalyzer
import com.example.domain.PlatformArchitectureInfo
import com.example.domain.PluginTemplate
import com.example.domain.ServerHealthResult
import com.example.domain.ServerHealthStatus
import com.example.domain.StaticAnalysisReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "StudioViewModel"

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

data class AiChatMessage(
    val id: Long = System.currentTimeMillis(),
    val isUser: Boolean,
    val modeTitle: String,
    val content: String,
    val extractedCode: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = StudioDatabase.getInstance(application).studioDao()
    private val repository = StudioRepository(dao)
    private val preferences = StudioPreferences(application)
    val fileManager = FilePortabilityManager(application)
    private val compilerService = CompilerService(application)
    private val geminiRepo = GeminiAssistantRepository()

    val settings: StateFlow<StudioSettings> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioSettings())

    val projects: StateFlow<List<ProjectEntity>> = repository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProject: StateFlow<ProjectEntity?> = combine(projects, settings) { list, prefs ->
        list.firstOrNull { it.id == prefs.activeProjectId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val projectFiles: StateFlow<List<ProjectFileEntity>> = activeProject
        .flatMapLatest { proj ->
            if (proj == null) flowOf(emptyList()) else repository.observeFiles(proj.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyEntries: StateFlow<List<ChangeHistoryEntity>> = activeProject
        .flatMapLatest { proj ->
            if (proj == null) flowOf(emptyList()) else repository.observeHistory(proj.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val compilationHistory: StateFlow<List<CompilationHistoryEntity>> = activeProject
        .flatMapLatest { proj ->
            if (proj == null) flowOf(emptyList()) else repository.observeCompilations(proj.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SMA Editor State + Undo/Redo + Auto-save
    private val _activeSmaFileId = MutableStateFlow<Long?>(null)
    val activeSmaFileId: StateFlow<Long?> = _activeSmaFileId.asStateFlow()

    private val _smaContent = MutableStateFlow("")
    val smaContent: StateFlow<String> = _smaContent.asStateFlow()

    private val _smaUnsaved = MutableStateFlow(false)
    val smaUnsaved: StateFlow<Boolean> = _smaUnsaved.asStateFlow()

    private val _autoSaveStatus = MutableStateFlow<String?>(null)
    val autoSaveStatus: StateFlow<String?> = _autoSaveStatus.asStateFlow()

    private val _undoStack = MutableStateFlow<List<String>>(emptyList())
    private val _redoStack = MutableStateFlow<List<String>>(emptyList())
    val canUndo: StateFlow<Boolean> = MutableStateFlow(false).apply {
        viewModelScope.launch { _undoStack.collect { value = it.isNotEmpty() } }
    }
    val canRedo: StateFlow<Boolean> = MutableStateFlow(false).apply {
        viewModelScope.launch { _redoStack.collect { value = it.isNotEmpty() } }
    }
    private var autoSaveJob: Job? = null

    private val _smaDiagnostics = MutableStateFlow<List<PawnDiagnostic>>(emptyList())
    val smaDiagnostics: StateFlow<List<PawnDiagnostic>> = _smaDiagnostics.asStateFlow()

    private val _staticAnalysisReport = MutableStateFlow<StaticAnalysisReport?>(null)
    val staticAnalysisReport: StateFlow<StaticAnalysisReport?> = _staticAnalysisReport.asStateFlow()

    private val _jumpToLineRequest = MutableStateFlow<Int?>(null)
    val jumpToLineRequest: StateFlow<Int?> = _jumpToLineRequest.asStateFlow()

    // CFG Editor State
    private val _activeCfgFileId = MutableStateFlow<Long?>(null)
    val activeCfgFileId: StateFlow<Long?> = _activeCfgFileId.asStateFlow()

    private val _cfgContent = MutableStateFlow("")
    val cfgContent: StateFlow<String> = _cfgContent.asStateFlow()

    private val _cfgUnsaved = MutableStateFlow(false)
    val cfgUnsaved: StateFlow<Boolean> = _cfgUnsaved.asStateFlow()

    // AI Assistant State
    private val _aiUiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiUiState: StateFlow<AiUiState> = _aiUiState.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    // Compiler State
    val archInfo: PlatformArchitectureInfo = compilerService.inspectArchitecture()

    private val _isCompiling = MutableStateFlow(false)
    val isCompiling: StateFlow<Boolean> = _isCompiling.asStateFlow()

    private val _compilerState = MutableStateFlow(CompilerExecutionState.IDLE)
    val compilerState: StateFlow<CompilerExecutionState> = _compilerState.asStateFlow()

    private val _compileProgress = MutableStateFlow(0f)
    val compileProgress: StateFlow<Float> = _compileProgress.asStateFlow()

    private val _serverHealth = MutableStateFlow(
        ServerHealthResult(ServerHealthStatus.NOT_CONFIGURED, "Configura la URL del servidor compilador amxxpc.")
    )
    val serverHealth: StateFlow<ServerHealthResult> = _serverHealth.asStateFlow()

    private val _compilationResult = MutableStateFlow<CompilationResult?>(null)
    val compilationResult: StateFlow<CompilationResult?> = _compilationResult.asStateFlow()

    private val _compiledBinaries = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val compiledBinaries: StateFlow<Map<String, ByteArray>> = _compiledBinaries.asStateFlow()

    // cstrike / SAF Directory Explorer State
    private val _cstrikeItems = MutableStateFlow<List<SafDocumentItem>>(emptyList())
    val cstrikeItems: StateFlow<List<SafDocumentItem>> = _cstrikeItems.asStateFlow()

    private val _cstrikeStack = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val cstrikeStack: StateFlow<List<Pair<String, String>>> = _cstrikeStack.asStateFlow()

    // AMXX & Metamod Config Issues
    val configIssues: StateFlow<List<ConfigIssue>> = combine(projectFiles, settings) { files, prefs ->
        val pluginsIni = files.firstOrNull { it.fileName.equals("plugins.ini", true) && !it.relativePath.contains("metamod", true) }?.content.orEmpty()
        val modulesIni = files.firstOrNull { it.fileName.equals("modules.ini", true) }?.content.orEmpty()
        val metamodIni = files.firstOrNull { it.fileName.contains("metamod", true) || it.relativePath.contains("metamod", true) }?.content.orEmpty()
        val smaSources = files.filter { it.resolvedType() == StudioFileType.SMA }.map { it.content }
        AmxxMetamodAnalyzer.validateAll(pluginsIni, modulesIni, metamodIni, prefs.targetServerOs, smaSources)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _snackbarFlow = MutableSharedFlow<String>()
    val snackbarFlow = _snackbarFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            val starterId = repository.ensureStarterProject()
            if (starterId > 0) {
                preferences.setActiveProjectId(starterId)
            }
        }
        viewModelScope.launch {
            activeProject.collect { proj ->
                if (proj != null) {
                    _compiledBinaries.value = compilerService.loadPersistedBinaries(proj.id)
                }
            }
        }
        viewModelScope.launch {
            projectFiles.collect { files ->
                val smaFiles = files.filter {
                    val t = it.resolvedType()
                    t == StudioFileType.SMA || t == StudioFileType.INC
                }
                val currentSma = smaFiles.firstOrNull { it.id == _activeSmaFileId.value } ?: smaFiles.firstOrNull()
                if (currentSma != null && _activeSmaFileId.value != currentSma.id) {
                    _activeSmaFileId.value = currentSma.id
                    _smaContent.value = currentSma.content
                    _smaUnsaved.value = false
                    _undoStack.value = emptyList()
                    _redoStack.value = emptyList()
                    _smaDiagnostics.value = PawnStaticAnalyzer.analyze(currentSma.content)
                }

                val cfgFiles = files.filter { it.resolvedType() == StudioFileType.CFG }
                val currentCfg = cfgFiles.firstOrNull { it.id == _activeCfgFileId.value } ?: cfgFiles.firstOrNull()
                if (currentCfg != null && _activeCfgFileId.value != currentCfg.id) {
                    _activeCfgFileId.value = currentCfg.id
                    _cfgContent.value = currentCfg.content
                    _cfgUnsaved.value = false
                }
            }
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch { _snackbarFlow.emit(msg) }
    }

    fun selectProject(projectId: Long) {
        viewModelScope.launch {
            _activeSmaFileId.value = null
            _activeCfgFileId.value = null
            preferences.setActiveProjectId(projectId)
        }
    }

    fun createProject(name: String, description: String, author: String, targetMod: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createProject(name, description, author, targetMod)
            preferences.setActiveProjectId(id)
            emitToast("Proyecto '$name' creado.")
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            emitToast("Proyecto eliminado.")
        }
    }

    // SMA Editor operations
    fun openSmaFile(file: ProjectFileEntity) {
        autoSaveJob?.cancel()
        _activeSmaFileId.value = file.id
        _smaContent.value = file.content
        _smaUnsaved.value = false
        _autoSaveStatus.value = null
        _undoStack.value = emptyList()
        _redoStack.value = emptyList()
        _smaDiagnostics.value = PawnStaticAnalyzer.analyze(file.content)
    }

    fun updateSmaContent(newText: String) {
        val previous = _smaContent.value
        if (newText == previous) return
        _undoStack.value = (_undoStack.value + previous).takeLast(40)
        _redoStack.value = emptyList()
        _smaContent.value = newText
        _smaUnsaved.value = true
        _autoSaveStatus.value = null
        _smaDiagnostics.value = PawnStaticAnalyzer.analyze(newText)

        if (settings.value.autoSaveEnabled) {
            autoSaveJob?.cancel()
            autoSaveJob = viewModelScope.launch {
                delay(2000L)
                val file = projectFiles.value.firstOrNull { it.id == _activeSmaFileId.value } ?: return@launch
                repository.saveFileContent(file, _smaContent.value, "Autoguardado")
                _smaUnsaved.value = false
                _autoSaveStatus.value = "Autoguardado ✓"
            }
        }
    }

    fun undoSma() {
        val stack = _undoStack.value
        if (stack.isEmpty()) return
        val previous = stack.last()
        _undoStack.value = stack.dropLast(1)
        _redoStack.value = (_redoStack.value + _smaContent.value).takeLast(40)
        _smaContent.value = previous
        _smaUnsaved.value = true
        _smaDiagnostics.value = PawnStaticAnalyzer.analyze(previous)
    }

    fun redoSma() {
        val stack = _redoStack.value
        if (stack.isEmpty()) return
        val next = stack.last()
        _redoStack.value = stack.dropLast(1)
        _undoStack.value = (_undoStack.value + _smaContent.value).takeLast(40)
        _smaContent.value = next
        _smaUnsaved.value = true
        _smaDiagnostics.value = PawnStaticAnalyzer.analyze(next)
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoSaveEnabled(enabled) }
    }

    fun saveActiveSma(summary: String = "Edición en Editor SMA") {
        autoSaveJob?.cancel()
        val file = projectFiles.value.firstOrNull { it.id == _activeSmaFileId.value } ?: return
        viewModelScope.launch {
            repository.saveFileContent(file, _smaContent.value, summary)
            _smaUnsaved.value = false
            _autoSaveStatus.value = "Guardado ✓"
            emitToast("Guardado: ${file.fileName}")
        }
    }

    fun createNewSmaFile(fileName: String, initialContent: String? = null) {
        val proj = activeProject.value ?: return
        val normalized = if (fileName.endsWith(".sma", true) || fileName.endsWith(".inc", true)) {
            fileName.trim()
        } else {
            "${fileName.trim()}.sma"
        }
        val code = initialContent ?: """#include <amxmodx>
#include <amxmisc>

#pragma semicolon 1

public plugin_init()
{
    register_plugin("${normalized.substringBeforeLast('.')}", "1.0", "${proj.author}");
}
"""
        viewModelScope.launch {
            val relPath = if (normalized.endsWith(".inc", true)) {
                "addons/amxmodx/scripting/include/$normalized"
            } else {
                "addons/amxmodx/scripting/$normalized"
            }
            val id = repository.createOrUpdateFile(proj.id, normalized, relPath, code, "Archivo creado")
            _activeSmaFileId.value = id
            _smaContent.value = code
            _smaUnsaved.value = false
            _smaDiagnostics.value = PawnStaticAnalyzer.analyze(code)
            emitToast("Archivo $normalized creado.")
        }
    }

    fun applyTemplateToEditor(template: PluginTemplate) {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            val id = repository.createOrUpdateFile(
                projectId = proj.id,
                fileName = template.fileName,
                relativePath = "addons/amxmodx/scripting/${template.fileName}",
                content = template.code,
                historySummary = "Plantilla aplicada: ${template.title}"
            )
            _activeSmaFileId.value = id
            _smaContent.value = template.code
            _smaUnsaved.value = false
            _smaDiagnostics.value = PawnStaticAnalyzer.analyze(template.code)
            emitToast("Plantilla '${template.title}' cargada en el editor.")
        }
    }

    fun renameFile(file: ProjectFileEntity, newName: String) {
        viewModelScope.launch {
            repository.renameFile(file, newName)
            emitToast("Renombrado a $newName")
        }
    }

    fun duplicateFile(file: ProjectFileEntity) {
        viewModelScope.launch {
            val id = repository.duplicateFile(file)
            if (file.resolvedType() == StudioFileType.SMA || file.resolvedType() == StudioFileType.INC) {
                _activeSmaFileId.value = id
            }
            emitToast("Archivo duplicado.")
        }
    }

    fun deleteFile(file: ProjectFileEntity) {
        viewModelScope.launch {
            repository.deleteFile(file.id)
            if (_activeSmaFileId.value == file.id) _activeSmaFileId.value = null
            if (_activeCfgFileId.value == file.id) _activeCfgFileId.value = null
            emitToast("Eliminado: ${file.fileName}")
        }
    }

    fun replaceInSma(find: String, replace: String, replaceAll: Boolean) {
        if (find.isEmpty()) return
        val current = _smaContent.value
        val updated = if (replaceAll) current.replace(find, replace) else current.replaceFirst(find, replace)
        if (updated != current) {
            updateSmaContent(updated)
            emitToast("Reemplazo aplicado.")
        } else {
            emitToast("No se encontraron coincidencias para '$find'.")
        }
    }

    fun requestJumpToLine(line: Int) {
        _jumpToLineRequest.value = line
    }

    fun clearJumpToLine() {
        _jumpToLineRequest.value = null
    }

    // CFG Editor operations
    fun openCfgFile(file: ProjectFileEntity) {
        _activeCfgFileId.value = file.id
        _cfgContent.value = file.content
        _cfgUnsaved.value = false
    }

    fun updateCfgContent(newText: String) {
        _cfgContent.value = newText
        _cfgUnsaved.value = true
    }

    fun saveActiveCfg() {
        val file = projectFiles.value.firstOrNull { it.id == _activeCfgFileId.value } ?: return
        viewModelScope.launch {
            repository.saveFileContent(file, _cfgContent.value, "Configuración CFG guardada")
            _cfgUnsaved.value = false
            emitToast("CFG guardado: ${file.fileName}")
        }
    }

    fun createNewCfgFile(fileName: String) {
        val proj = activeProject.value ?: return
        val clean = if (fileName.endsWith(".cfg", true)) fileName.trim() else "${fileName.trim()}.cfg"
        val defaultCfg = "// Archivo CFG creado con CS 1.6 Plugin Studio\n"
        viewModelScope.launch {
            val id = repository.createOrUpdateFile(
                proj.id,
                clean,
                "cstrike/$clean",
                defaultCfg,
                "CFG creado"
            )
            _activeCfgFileId.value = id
            _cfgContent.value = defaultCfg
            _cfgUnsaved.value = false
            emitToast("Archivo $clean creado.")
        }
    }

    // AMXX & Metamod Config Updates
    fun updateConfigFileContent(fileNameContains: String, defaultRelativePath: String, defaultFileName: String, newContent: String) {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            val existing = projectFiles.value.firstOrNull {
                it.fileName.contains(fileNameContains, true) || it.relativePath.contains(fileNameContains, true)
            }
            if (existing != null) {
                repository.saveFileContent(existing, newContent, "Actualización $defaultFileName")
            } else {
                repository.createOrUpdateFile(proj.id, defaultFileName, defaultRelativePath, newContent, "Creado $defaultFileName")
            }
            emitToast("$defaultFileName actualizado.")
        }
    }

    fun restoreHistorySnapshot(entry: ChangeHistoryEntity) {
        viewModelScope.launch {
            val existing = projectFiles.value.firstOrNull { it.id == entry.fileId }
                ?: projectFiles.value.firstOrNull { it.fileName == entry.fileName }
            if (existing != null) {
                repository.saveFileContent(existing, entry.snapshotContent, "Restaurado desde historial")
                if (existing.id == _activeSmaFileId.value) {
                    _smaContent.value = entry.snapshotContent
                    _smaUnsaved.value = false
                    _smaDiagnostics.value = PawnStaticAnalyzer.analyze(entry.snapshotContent)
                }
                emitToast("Versión de ${entry.fileName} restaurada.")
            }
        }
    }

    // Gemini AI Assistant
    fun sendAiPrompt(
        mode: AiTaskMode,
        userInput: String,
        includeActiveSma: Boolean,
        pastedCode: String,
        useFastModel: Boolean = false
    ) {
        val activeFile = projectFiles.value.firstOrNull { it.id == _activeSmaFileId.value }
        val codeContext = when {
            pastedCode.isNotBlank() -> pastedCode
            includeActiveSma && _smaContent.value.isNotBlank() -> _smaContent.value
            else -> ""
        }
        val diagnosticsSummary = if (mode == AiTaskMode.FIX_ERRORS && _smaDiagnostics.value.isNotEmpty()) {
            "\nDiagnósticos detectados:\n" + _smaDiagnostics.value.joinToString("\n") {
                "Línea ${it.line}: [${it.severity}] ${it.message}"
            }
        } else ""

        val fullPrompt = buildString {
            appendLine(mode.promptPrefix)
            if (userInput.isNotBlank()) {
                appendLine()
                appendLine("Instrucciones del usuario: $userInput")
            }
            if (codeContext.isNotBlank()) {
                appendLine()
                appendLine("Archivo: ${activeFile?.fileName ?: "codigo_pegado.sma"}")
                appendLine("```pawn")
                appendLine(codeContext)
                appendLine("```")
            }
            if (diagnosticsSummary.isNotBlank()) {
                appendLine(diagnosticsSummary)
            }
        }

        val userBubbleText = buildString {
            append(userInput.ifBlank { mode.title })
            if (codeContext.isNotBlank()) {
                append("\n[Código adjunto: ${codeContext.lines().size} líneas]")
            }
        }
        _aiMessages.value = _aiMessages.value + AiChatMessage(
            isUser = true,
            modeTitle = mode.title,
            content = userBubbleText
        )

        val modelLabel = if (useFastModel) "Gemini 3.5 Flash-Lite" else "Gemini 3.8 Flash"

        viewModelScope.launch {
            _aiUiState.value = AiUiState.Loading
            var lastStreamed = ""
            try {
                geminiRepo.streamPrompt(fullPrompt, useFastModel = useFastModel).collect { partial ->
                    lastStreamed = partial
                    _aiUiState.value = AiUiState.Streaming(partial)
                }
                if (lastStreamed.isNotBlank()) {
                    _aiUiState.value = AiUiState.Success(lastStreamed)
                    _aiMessages.value = _aiMessages.value + AiChatMessage(
                        isUser = false,
                        modeTitle = modelLabel,
                        content = lastStreamed,
                        extractedCode = extractFirstCodeBlock(lastStreamed)
                    )
                } else {
                    _aiUiState.value = AiUiState.Error("El modelo devolvió una respuesta vacía. Intenta de nuevo.")
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "Gemini AI inference timed out", e)
                if (lastStreamed.isNotBlank()) {
                    _aiMessages.value = _aiMessages.value + AiChatMessage(
                        isUser = false,
                        modeTitle = "$modelLabel (Parcial)",
                        content = lastStreamed,
                        extractedCode = extractFirstCodeBlock(lastStreamed)
                    )
                    _aiUiState.value = AiUiState.Error("La generación tardó más de 120s; se conservó el código parcial recibido.")
                } else {
                    _aiUiState.value = AiUiState.Error("Tiempo de espera agotado (120s). Prueba activar 'Modo Rápido' o simplificar la petición.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini AI inference error", e)
                _aiUiState.value = AiUiState.Error(
                    e.localizedMessage ?: "Error al consultar Gemini mediante Firebase AI Logic."
                )
            }
        }
    }

    fun applyAiCodeToEditor(code: String, asNewFileName: String? = null) {
        if (asNewFileName != null) {
            createNewSmaFile(asNewFileName, code)
        } else if (_activeSmaFileId.value != null) {
            updateSmaContent(code)
            emitToast("Código aplicado al Editor SMA (recuerda guardar).")
        } else {
            createNewSmaFile("ai_generated_plugin.sma", code)
        }
    }

    private fun extractFirstCodeBlock(markdown: String): String? {
        val regex = Regex("```(?:pawn|sma|c|cpp|cfg|ini)?\\s*\\n([\\s\\S]*?)```")
        return regex.find(markdown)?.groupValues?.getOrNull(1)?.trim()
    }

    // Compiler & Static Analyzer operations (strictly separated)
    fun setCompilerVersion(version: String) {
        viewModelScope.launch { preferences.setCompilerVersion(version) }
    }

    fun setCompilerServerUrl(url: String) {
        viewModelScope.launch {
            preferences.setCompilerServerUrl(url)
            checkCompilerServerHealth(url)
        }
    }

    fun checkCompilerServerHealth(urlOverride: String? = null) {
        val url = urlOverride ?: settings.value.compilerServerUrl
        viewModelScope.launch {
            _serverHealth.value = ServerHealthResult(ServerHealthStatus.CHECKING, "Comprobando servidor...")
            _serverHealth.value = compilerService.checkServerHealth(url)
        }
    }

    fun setTargetServerOs(os: String) {
        viewModelScope.launch { preferences.setTargetServerOs(os) }
    }

    fun setEditorFontSize(sizeSp: Int) {
        viewModelScope.launch { preferences.setEditorFontSizeSp(sizeSp) }
    }

    fun runStaticAnalysisOnly() {
        val activeFile = projectFiles.value.firstOrNull { it.id == _activeSmaFileId.value }
        val fileName = activeFile?.fileName ?: "plugin.sma"
        val report = compilerService.runStaticAnalysis(fileName, _smaContent.value)
        _staticAnalysisReport.value = report
        _smaDiagnostics.value = report.diagnostics
        emitToast("Análisis estático: ${report.errorCount} error(es), ${report.warningCount} aviso(s) (sin compilar .amxx).")
    }

    fun compileActiveSma() {
        val activeFile = projectFiles.value.firstOrNull { it.id == _activeSmaFileId.value }
        val fileName = activeFile?.fileName ?: "plugin.sma"
        val source = _smaContent.value
        if (source.isBlank()) {
            emitToast("No hay código .sma para compilar.")
            return
        }
        val customIncludes = projectFiles.value
            .filter { it.resolvedType() == StudioFileType.INC }
            .map { CustomIncludePayload(it.fileName, it.content) }

        viewModelScope.launch {
            _isCompiling.value = true
            _compilerState.value = CompilerExecutionState.CONNECTING
            _compileProgress.value = 0.2f

            val res = compilerService.compileWithRealCompiler(
                fileName = fileName,
                sourceCode = source,
                compilerVersion = settings.value.compilerVersion,
                serverUrl = settings.value.compilerServerUrl,
                customIncludes = customIncludes
            ) { state, progress ->
                _compilerState.value = state
                _compileProgress.value = progress
            }

            _compilerState.value = res.executionState
            _compileProgress.value = if (res.succeeded) 1f else 0f
            _compilationResult.value = res

            if (res.executionState == CompilerExecutionState.SERVER_UNAVAILABLE) {
                _serverHealth.value = ServerHealthResult(
                    ServerHealthStatus.OFFLINE,
                    "Servidor no disponible o sin configurar"
                )
            } else {
                _serverHealth.value = ServerHealthResult(
                    ServerHealthStatus.ONLINE,
                    "Conectado • ${res.compilerVersion}"
                )
            }

            activeProject.value?.let { proj ->
                repository.recordCompilation(
                    CompilationHistoryEntity(
                        projectId = proj.id,
                        fileName = res.fileName,
                        outputFileName = res.outputFileName,
                        compilerVersion = res.compilerVersion,
                        statusLabel = res.executionState.label,
                        succeeded = res.succeeded,
                        errorCount = res.errorCount,
                        warningCount = res.warningCount,
                        durationMs = res.durationMs,
                        outputLog = res.consoleOutput
                    )
                )
                if (res.succeeded && res.amxxBytes != null) {
                    compilerService.persistCompiledBinary(proj.id, res.outputFileName, res.amxxBytes)
                    _compiledBinaries.value = _compiledBinaries.value + (res.outputFileName to res.amxxBytes)
                    repository.createOrUpdateFile(
                        projectId = proj.id,
                        fileName = res.outputFileName,
                        relativePath = "addons/amxmodx/plugins/${res.outputFileName}",
                        content = "// Binario real .amxx compilado con ${res.compilerVersion} (${res.amxxBytes.size} bytes)",
                        historySummary = "Compilado real ${res.outputFileName}"
                    )
                    emitToast("Compilación real exitosa: ${res.outputFileName}")
                }
            }
            _isCompiling.value = false
        }
    }

    // File Portability (SAF Import / Export / cstrike / ZIP)
    fun importFileFromUri(uri: Uri) {
        val proj = activeProject.value ?: return
        viewModelScope.launch {
            val name = fileManager.queryDisplayName(uri)
            val textRes = fileManager.readTextFromUri(uri)
            textRes.onSuccess { text ->
                val type = StudioFileType.fromFileName(name)
                val rel = when (type) {
                    StudioFileType.SMA -> "addons/amxmodx/scripting/$name"
                    StudioFileType.INC -> "addons/amxmodx/scripting/include/$name"
                    StudioFileType.CFG -> "cstrike/$name"
                    StudioFileType.INI -> "addons/amxmodx/configs/$name"
                    else -> name
                }
                val fileId = repository.createOrUpdateFile(proj.id, name, rel, text, "Importado desde almacenamiento")
                if (type == StudioFileType.SMA || type == StudioFileType.INC) {
                    _activeSmaFileId.value = fileId
                    _smaContent.value = text
                    _smaUnsaved.value = false
                    _smaDiagnostics.value = PawnStaticAnalyzer.analyze(text)
                } else if (type == StudioFileType.CFG) {
                    _activeCfgFileId.value = fileId
                    _cfgContent.value = text
                    _cfgUnsaved.value = false
                }
                emitToast("Importado: $name")
            }.onFailure { err ->
                emitToast("Error al importar: ${err.localizedMessage}")
            }
        }
    }

    fun exportTextToUri(uri: Uri, content: String, label: String) {
        viewModelScope.launch {
            fileManager.writeTextToUri(uri, content)
                .onSuccess { emitToast("Exportado: $label") }
                .onFailure { emitToast("Error al exportar: ${it.localizedMessage}") }
        }
    }

    fun exportCompiledAmxxToUri(uri: Uri, fileName: String) {
        val bytes = _compiledBinaries.value[fileName] ?: return
        viewModelScope.launch {
            fileManager.writeBytesToUri(uri, bytes)
                .onSuccess { emitToast("Binario .amxx guardado: $fileName") }
                .onFailure { emitToast("Error al guardar .amxx: ${it.localizedMessage}") }
        }
    }

    fun exportActiveProjectZip(uri: Uri) {
        val proj = activeProject.value ?: return
        val files = projectFiles.value
        viewModelScope.launch {
            fileManager.exportProjectToZip(uri, proj.name, files, _compiledBinaries.value)
                .onSuccess { count -> emitToast("ZIP exportado con $count archivos.") }
                .onFailure { emitToast("Error al exportar ZIP: ${it.localizedMessage}") }
        }
    }

    fun importProjectFromZip(uri: Uri) {
        viewModelScope.launch {
            val zipName = fileManager.queryDisplayName(uri).substringBeforeLast('.')
            fileManager.importProjectFromZip(uri)
                .onSuccess { items ->
                    if (items.isEmpty()) {
                        emitToast("El archivo ZIP está vacío.")
                        return@onSuccess
                    }
                    val projId = repository.createProject(
                        name = zipName.ifBlank { "Proyecto Importado" },
                        description = "Importado desde archivo ZIP (${items.size} archivos)",
                        author = "Importado",
                        targetMod = settings.value.compilerVersion
                    )
                    items.forEach { entry ->
                        repository.createOrUpdateFile(
                            projectId = projId,
                            fileName = entry.fileName,
                            relativePath = entry.relativePath,
                            content = entry.content,
                            historySummary = "Importado desde ZIP"
                        )
                    }
                    preferences.setActiveProjectId(projId)
                    emitToast("Proyecto '$zipName' importado (${items.size} archivos).")
                }
                .onFailure { err ->
                    emitToast("Error al importar ZIP: ${err.localizedMessage}")
                }
        }
    }

    fun openCstrikeTree(uri: Uri) {
        fileManager.takePersistableTreePermission(uri)
        viewModelScope.launch {
            preferences.setSavedCstrikeUri(uri.toString())
            _cstrikeStack.value = emptyList()
            loadCstrikeDirectory(uri, null)
        }
    }

    fun navigateIntoCstrikeFolder(item: SafDocumentItem) {
        val uriStr = settings.value.savedCstrikeUri
        if (uriStr.isEmpty() || !item.isDirectory) return
        val treeUri = Uri.parse(uriStr)
        _cstrikeStack.value = _cstrikeStack.value + (item.documentId to item.name)
        viewModelScope.launch {
            loadCstrikeDirectory(treeUri, item.documentId)
        }
    }

    fun navigateUpCstrikeFolder() {
        val uriStr = settings.value.savedCstrikeUri
        if (uriStr.isEmpty()) return
        val treeUri = Uri.parse(uriStr)
        val trimmed = _cstrikeStack.value.dropLast(1)
        _cstrikeStack.value = trimmed
        viewModelScope.launch {
            loadCstrikeDirectory(treeUri, trimmed.lastOrNull()?.first)
        }
    }

    private suspend fun loadCstrikeDirectory(treeUri: Uri, parentDocId: String?) {
        fileManager.listDirectory(treeUri, parentDocId)
            .onSuccess { _cstrikeItems.value = it }
            .onFailure { emitToast("No se pudo listar la carpeta: ${it.localizedMessage}") }
    }

    fun exportFileToCurrentCstrikeFolder(file: ProjectFileEntity) {
        val uriStr = settings.value.savedCstrikeUri
        if (uriStr.isEmpty()) {
            emitToast("Selecciona primero la carpeta cstrike o destino.")
            return
        }
        val treeUri = Uri.parse(uriStr)
        val parentDocId = _cstrikeStack.value.lastOrNull()?.first
        val binary = _compiledBinaries.value[file.fileName]
        val bytes = binary ?: file.content.toByteArray(Charsets.UTF_8)
        val mime = if (binary != null) "application/octet-stream" else "text/plain"
        viewModelScope.launch {
            fileManager.exportFileToDirectory(treeUri, parentDocId, file.fileName, mime, bytes)
                .onSuccess {
                    emitToast("Exportado a carpeta: ${file.fileName}")
                    loadCstrikeDirectory(treeUri, parentDocId)
                }
                .onFailure { emitToast("Error al exportar: ${it.localizedMessage}") }
        }
    }
}

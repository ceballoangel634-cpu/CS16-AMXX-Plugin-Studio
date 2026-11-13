package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.StudioFileType
import com.example.ui.components.StatusPill
import com.example.ui.navigation.StudioRoutes
import com.example.ui.navigation.studioDestinations
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AmxxMetamodScreen
import com.example.ui.screens.CfgEditorScreen
import com.example.ui.screens.CompatibilityStorageScreen
import com.example.ui.screens.CompilerScreen
import com.example.ui.screens.DocsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SmaEditorScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.StudioBgObsidian
import com.example.ui.theme.StudioBluePrimary
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCyanAccent
import com.example.ui.theme.StudioSurfaceNavy
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CSPluginStudioApp(
    viewModel: StudioViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: StudioRoutes.HOME

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val projectFiles by viewModel.projectFiles.collectAsStateWithLifecycle()
    val historyEntries by viewModel.historyEntries.collectAsStateWithLifecycle()
    val compilationHistory by viewModel.compilationHistory.collectAsStateWithLifecycle()

    val activeSmaFileId by viewModel.activeSmaFileId.collectAsStateWithLifecycle()
    val smaContent by viewModel.smaContent.collectAsStateWithLifecycle()
    val smaUnsaved by viewModel.smaUnsaved.collectAsStateWithLifecycle()
    val autoSaveStatus by viewModel.autoSaveStatus.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val smaDiagnostics by viewModel.smaDiagnostics.collectAsStateWithLifecycle()
    val staticAnalysisReport by viewModel.staticAnalysisReport.collectAsStateWithLifecycle()
    val jumpLine by viewModel.jumpToLineRequest.collectAsStateWithLifecycle()

    val activeCfgFileId by viewModel.activeCfgFileId.collectAsStateWithLifecycle()
    val cfgContent by viewModel.cfgContent.collectAsStateWithLifecycle()
    val cfgUnsaved by viewModel.cfgUnsaved.collectAsStateWithLifecycle()

    val aiUiState by viewModel.aiUiState.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()

    val isCompiling by viewModel.isCompiling.collectAsStateWithLifecycle()
    val compilerState by viewModel.compilerState.collectAsStateWithLifecycle()
    val compileProgress by viewModel.compileProgress.collectAsStateWithLifecycle()
    val serverHealth by viewModel.serverHealth.collectAsStateWithLifecycle()
    val compilationResult by viewModel.compilationResult.collectAsStateWithLifecycle()
    val compiledBinaries by viewModel.compiledBinaries.collectAsStateWithLifecycle()

    val cstrikeItems by viewModel.cstrikeItems.collectAsStateWithLifecycle()
    val cstrikeStack by viewModel.cstrikeStack.collectAsStateWithLifecycle()
    val configIssues by viewModel.configIssues.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.snackbarFlow.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateTo(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val currentDest = studioDestinations.firstOrNull { it.route == currentRoute } ?: studioDestinations.first()
    val activeSmaFile = projectFiles.firstOrNull { it.id == activeSmaFileId }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBgObsidian)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        val bottomBarDestinations = remember {
            listOf(
                studioDestinations[0], // Inicio
                studioDestinations[1], // Editor SMA
                studioDestinations[2], // Gemini IA
                studioDestinations[3], // Compilar
                studioDestinations[8]  // Proyectos
            )
        }

        val appScaffold: @Composable () -> Unit = {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = StudioBgObsidian,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = currentDest.label,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = StudioTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeProject?.name ?: "CS 1.6 Plugin Studio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioCyanAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            if (!isWideScreen) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("nav_drawer_open_btn")
                                ) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Abrir menú lateral", tint = StudioBluePrimary)
                                }
                            }
                        },
                        actions = {
                            StatusPill(
                                text = currentDest.moduleTag,
                                color = StudioBluePrimary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = StudioSurfaceNavy
                        )
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = StudioSurfaceNavy,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            bottomBarDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.shortLabel, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = StudioSurfaceNavy,
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.height(8.dp))
                            studioDestinations.forEach { dest ->
                                NavigationRailItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateTo(dest.route) },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.shortLabel, maxLines = 1) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = StudioRoutes.HOME
                        ) {
                            composable(StudioRoutes.HOME) {
                                HomeScreen(
                                    activeProject = activeProject,
                                    fileCount = projectFiles.size,
                                    archInfo = viewModel.archInfo,
                                    isWideScreen = isWideScreen,
                                    onCreateQuickSma = {
                                        viewModel.createNewSmaFile("plugin_${System.currentTimeMillis() % 1000}.sma")
                                        navigateTo(StudioRoutes.SMA_EDITOR)
                                    },
                                    onNavigate = { route -> navigateTo(route) }
                                )
                            }

                            composable(StudioRoutes.SMA_EDITOR) {
                                SmaEditorScreen(
                                    files = projectFiles,
                                    activeFileId = activeSmaFileId,
                                    code = smaContent,
                                    hasUnsavedChanges = smaUnsaved,
                                    autoSaveEnabled = settings.autoSaveEnabled,
                                    autoSaveStatus = autoSaveStatus,
                                    canUndo = canUndo,
                                    canRedo = canRedo,
                                    diagnostics = smaDiagnostics,
                                    fontSizeSp = settings.editorFontSizeSp,
                                    highlightLine = jumpLine,
                                    onSelectFile = {
                                        viewModel.clearJumpToLine()
                                        viewModel.openSmaFile(it)
                                    },
                                    onCodeChange = {
                                        viewModel.clearJumpToLine()
                                        viewModel.updateSmaContent(it)
                                    },
                                    onUndo = { viewModel.undoSma() },
                                    onRedo = { viewModel.redoSma() },
                                    onToggleAutoSave = { viewModel.setAutoSaveEnabled(it) },
                                    onSave = { viewModel.saveActiveSma() },
                                    onCreateFile = { viewModel.createNewSmaFile(it) },
                                    onRenameFile = { f, n -> viewModel.renameFile(f, n) },
                                    onDuplicateFile = { viewModel.duplicateFile(it) },
                                    onDeleteFile = { viewModel.deleteFile(it) },
                                    onReplaceText = { find, rep, all -> viewModel.replaceInSma(find, rep, all) },
                                    onFontSizeChange = { viewModel.setEditorFontSize(it) },
                                    onImportUri = { viewModel.importFileFromUri(it) },
                                    onExportUri = { uri, txt, lbl -> viewModel.exportTextToUri(uri, txt, lbl) },
                                    onRunStaticAnalysis = { viewModel.runStaticAnalysisOnly() },
                                    onSendToCompile = { navigateTo(StudioRoutes.COMPILER) },
                                    onSendToAi = { navigateTo(StudioRoutes.AI_ASSISTANT) }
                                )
                            }

                            composable(StudioRoutes.AI_ASSISTANT) {
                                AiAssistantScreen(
                                    uiState = aiUiState,
                                    messages = aiMessages,
                                    activeSmaFileName = activeSmaFile?.fileName,
                                    onSendPrompt = { mode, input, attach, pasted, fast ->
                                        viewModel.sendAiPrompt(mode, input, attach, pasted, fast)
                                    },
                                    onApplyCodeToEditor = { code, newName ->
                                        viewModel.applyAiCodeToEditor(code, newName)
                                        navigateTo(StudioRoutes.SMA_EDITOR)
                                    }
                                )
                            }

                            composable(StudioRoutes.COMPILER) {
                                val incCount = remember(projectFiles) {
                                    projectFiles.count { it.resolvedType() == StudioFileType.INC }
                                }
                                CompilerScreen(
                                    activeSmaFileName = activeSmaFile?.fileName,
                                    customIncludeCount = incCount,
                                    compilerVersion = settings.compilerVersion,
                                    compilerServerUrl = settings.compilerServerUrl,
                                    archInfo = viewModel.archInfo,
                                    isCompiling = isCompiling,
                                    compilerState = compilerState,
                                    compileProgress = compileProgress,
                                    serverHealth = serverHealth,
                                    compilationResult = compilationResult,
                                    staticAnalysisReport = staticAnalysisReport,
                                    onSelectCompilerVersion = { viewModel.setCompilerVersion(it) },
                                    onSaveServerUrl = { viewModel.setCompilerServerUrl(it) },
                                    onCheckServerHealth = { viewModel.checkCompilerServerHealth(it) },
                                    onRunStaticAnalysis = { viewModel.runStaticAnalysisOnly() },
                                    onCompile = { viewModel.compileActiveSma() },
                                    onJumpToEditorLine = { line ->
                                        viewModel.requestJumpToLine(line)
                                        navigateTo(StudioRoutes.SMA_EDITOR)
                                    },
                                    onExportAmxxUri = { uri, fname ->
                                        viewModel.exportCompiledAmxxToUri(uri, fname)
                                    }
                                )
                            }

                            composable(StudioRoutes.TEMPLATES) {
                                TemplatesScreen(
                                    onApplyTemplate = { tpl ->
                                        viewModel.applyTemplateToEditor(tpl)
                                        navigateTo(StudioRoutes.SMA_EDITOR)
                                    }
                                )
                            }

                            composable(StudioRoutes.CFG_EDITOR) {
                                CfgEditorScreen(
                                    files = projectFiles,
                                    activeCfgFileId = activeCfgFileId,
                                    cfgContent = cfgContent,
                                    hasUnsavedChanges = cfgUnsaved,
                                    fontSizeSp = settings.editorFontSizeSp,
                                    onSelectCfg = { viewModel.openCfgFile(it) },
                                    onCfgChange = { viewModel.updateCfgContent(it) },
                                    onSaveCfg = { viewModel.saveActiveCfg() },
                                    onCreateCfg = { viewModel.createNewCfgFile(it) },
                                    onImportUri = { viewModel.importFileFromUri(it) },
                                    onExportUri = { uri, txt, lbl -> viewModel.exportTextToUri(uri, txt, lbl) }
                                )
                            }

                            composable(StudioRoutes.AMXX_METAMOD) {
                                AmxxMetamodScreen(
                                    files = projectFiles,
                                    targetServerOs = settings.targetServerOs,
                                    configIssues = configIssues,
                                    onSelectServerOs = { viewModel.setTargetServerOs(it) },
                                    onUpdateConfigFile = { key, rel, name, content ->
                                        viewModel.updateConfigFileContent(key, rel, name, content)
                                    }
                                )
                            }

                            composable(StudioRoutes.COMPATIBILITY) {
                                CompatibilityStorageScreen(
                                    archInfo = viewModel.archInfo,
                                    savedCstrikeUri = settings.savedCstrikeUri,
                                    cstrikeStack = cstrikeStack,
                                    cstrikeItems = cstrikeItems,
                                    projectFiles = projectFiles,
                                    onOpenCstrikeTree = { viewModel.openCstrikeTree(it) },
                                    onEnterCstrikeFolder = { viewModel.navigateIntoCstrikeFolder(it) },
                                    onNavigateUpFolder = { viewModel.navigateUpCstrikeFolder() },
                                    onImportFileUri = { viewModel.importFileFromUri(it) },
                                    onExportProjectFileToCstrike = { viewModel.exportFileToCurrentCstrikeFolder(it) }
                                )
                            }

                            composable(StudioRoutes.PROJECTS) {
                                ProjectsScreen(
                                    projects = projects,
                                    activeProject = activeProject,
                                    projectFiles = projectFiles,
                                    historyEntries = historyEntries,
                                    compilationHistory = compilationHistory,
                                    compiledBinaries = compiledBinaries,
                                    onSelectProject = { viewModel.selectProject(it) },
                                    onCreateProject = { n, d, a, m -> viewModel.createProject(n, d, a, m) },
                                    onDeleteProject = { viewModel.deleteProject(it) },
                                    onOpenFileInEditor = { file ->
                                        when (file.resolvedType()) {
                                            StudioFileType.SMA, StudioFileType.INC -> {
                                                viewModel.openSmaFile(file)
                                                navigateTo(StudioRoutes.SMA_EDITOR)
                                            }
                                            StudioFileType.CFG -> {
                                                viewModel.openCfgFile(file)
                                                navigateTo(StudioRoutes.CFG_EDITOR)
                                            }
                                            StudioFileType.INI -> navigateTo(StudioRoutes.AMXX_METAMOD)
                                            else -> navigateTo(StudioRoutes.SMA_EDITOR)
                                        }
                                    },
                                    onImportFileUri = { viewModel.importFileFromUri(it) },
                                    onExportAmxxUri = { uri, fname -> viewModel.exportCompiledAmxxToUri(uri, fname) },
                                    onImportZipUri = { viewModel.importProjectFromZip(it) },
                                    onExportZipUri = { viewModel.exportActiveProjectZip(it) },
                                    onRestoreHistory = { viewModel.restoreHistorySnapshot(it) }
                                )
                            }

                            composable(StudioRoutes.DOCS) {
                                DocsScreen(
                                    onInsertSnippetToSma = { snippet ->
                                        val updated = if (smaContent.isEmpty() || smaContent.endsWith("\n")) {
                                            "$smaContent$snippet\n"
                                        } else {
                                            "$smaContent\n$snippet\n"
                                        }
                                        viewModel.updateSmaContent(updated)
                                        navigateTo(StudioRoutes.SMA_EDITOR)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isWideScreen) {
            appScaffold()
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = StudioSurfaceNavy,
                        modifier = Modifier.width(300.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "CS 1.6 PLUGIN STUDIO",
                                style = MaterialTheme.typography.headlineSmall,
                                color = StudioCyanAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            Text(
                                text = "IDE AMX Mod X & Metamod",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = StudioBorderSubtle)
                            Spacer(Modifier.height(8.dp))
                            studioDestinations.forEach { dest ->
                                NavigationDrawerItem(
                                    label = { Text(dest.label) },
                                    badge = { Text(dest.moduleTag, style = MaterialTheme.typography.labelSmall) },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    selected = currentRoute == dest.route,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navigateTo(dest.route)
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = StudioBluePrimary.copy(alpha = 0.2f),
                                        selectedIconColor = StudioCyanAccent,
                                        selectedTextColor = StudioCyanAccent,
                                        unselectedTextColor = StudioTextPrimary,
                                        unselectedIconColor = StudioTextSecondary
                                    ),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                content = appScaffold
            )
        }
    }
}

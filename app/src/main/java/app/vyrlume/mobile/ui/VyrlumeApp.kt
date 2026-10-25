package app.vyrlume.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vyrlume.mobile.VyrlumeUiState
import app.vyrlume.mobile.VyrlumeViewModel
import app.vyrlume.mobile.data.IdeSection
import app.vyrlume.mobile.data.WorkspaceKind
import app.vyrlume.mobile.ui.components.AeroBackdrop
import app.vyrlume.mobile.ui.components.CodeEditor
import app.vyrlume.mobile.ui.components.CodingKeyboard
import app.vyrlume.mobile.ui.components.KeyboardRequest
import app.vyrlume.mobile.ui.components.RemoteWorkbench
import app.vyrlume.mobile.ui.screens.FilesScreen
import app.vyrlume.mobile.ui.screens.PreviewScreen
import app.vyrlume.mobile.ui.screens.SourceControlScreen
import app.vyrlume.mobile.ui.screens.TerminalScreen
import app.vyrlume.mobile.ui.screens.WelcomeScreen
import app.vyrlume.mobile.ui.theme.AeroMint
import app.vyrlume.mobile.ui.theme.AeroSky
import app.vyrlume.mobile.util.ServerUrl

@Composable
fun VyrlumeApp(viewModel: VyrlumeViewModel) {
    val state by viewModel.ui.collectAsState()
    AeroBackdrop {
        if (!state.enteredWorkspace) {
            WelcomeScreen(
                servers = state.servers,
                onDemo = viewModel::enterDemo,
                onConnect = { viewModel.showServerSheet(true) },
                onServer = viewModel::selectServer,
            )
        } else {
            WorkspaceScreen(state, viewModel)
        }
        WorkspaceOverlays(state, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceScreen(state: VyrlumeUiState, viewModel: VyrlumeViewModel) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeNotice()
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = "Vyrlume",
                            tint = AeroSky,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    },
                    title = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(
                                if (state.workspaceKind == WorkspaceKind.DEMO) state.openFile.path.substringAfterLast('/') else state.activeServer?.name.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (state.workspaceKind == WorkspaceKind.DEMO) "● LOCAL DEMO" else "● REMOTE WORKSPACE",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (state.workspaceKind == WorkspaceKind.DEMO) AeroMint else AeroSky,
                            )
                        }
                    },
                    actions = {
                        if (state.workspaceKind == WorkspaceKind.DEMO && state.section == IdeSection.EDITOR) {
                            IconButton(onClick = viewModel::saveOpenFile, modifier = Modifier.testTag("save_file")) {
                                Icon(Icons.Default.Save, contentDescription = "Save file")
                            }
                        }
                        IconButton(onClick = { viewModel.showCommandPalette(true) }, modifier = Modifier.testTag("command_palette")) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "Command palette")
                        }
                        IconButton(onClick = { viewModel.showServerSheet(true) }, modifier = Modifier.testTag("workspaces")) {
                            Icon(Icons.Default.Cloud, contentDescription = "Workspaces")
                        }
                    },
                )
            },
            bottomBar = {
                if (!wide) {
                    NavigationBar {
                        IdeSection.entries.forEach { section ->
                            NavigationBarItem(
                                selected = state.section == section,
                                onClick = { viewModel.selectSection(section) },
                                icon = { Icon(section.icon(), contentDescription = section.label) },
                                label = { Text(section.label) },
                                modifier = Modifier.testTag("nav_${section.name}"),
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (wide) {
                    NavigationRail {
                        IdeSection.entries.forEach { section ->
                            NavigationRailItem(
                                selected = state.section == section,
                                onClick = { viewModel.selectSection(section) },
                                icon = { Icon(section.icon(), contentDescription = section.label) },
                                label = { Text(section.label) },
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxSize()) {
                    WorkspaceContent(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceContent(state: VyrlumeUiState, viewModel: VyrlumeViewModel) {
    if (state.workspaceKind == WorkspaceKind.REMOTE) {
        val server = state.activeServer ?: return
        if (state.section == IdeSection.PREVIEW) {
            PreviewScreen(
                port = state.previewPort,
                remoteUrl = ServerUrl.previewUrl(server.url, state.previewPort),
                onPort = viewModel::setPreviewPort,
            )
        } else {
            RemoteWorkbench(
                server = server,
                section = state.section,
            )
        }
        return
    }

    when (state.section) {
        IdeSection.EDITOR -> DemoEditor(state, viewModel)
        IdeSection.FILES -> FilesScreen(
            files = state.files,
            onOpen = viewModel::openFile,
            onNewFile = { viewModel.showNewFileDialog(true) },
        )
        IdeSection.TERMINAL -> TerminalScreen(
            lines = state.terminalLines,
            onRun = viewModel::runDemoCommand,
        )
        IdeSection.SOURCE_CONTROL -> SourceControlScreen(state.files)
        IdeSection.PREVIEW -> PreviewScreen(
            port = state.previewPort,
            remoteUrl = null,
            onPort = viewModel::setPreviewPort,
        )
    }
}

@Composable
private fun DemoEditor(state: VyrlumeUiState, viewModel: VyrlumeViewModel) {
    var request by remember { mutableStateOf<KeyboardRequest?>(null) }
    Column(Modifier.fillMaxSize()) {
        CodeEditor(
            file = state.openFile,
            onChange = viewModel::updateOpenFile,
            modifier = Modifier.weight(1f),
            keyboardRequest = request,
            onKeyboardHandled = { request = null },
        )
        CodingKeyboard(
            onText = { request = KeyboardRequest(System.nanoTime(), text = it) },
            onMove = { request = KeyboardRequest(System.nanoTime(), move = it) },
            onSpecial = {},
        )
    }
}

@Composable
private fun WorkspaceOverlays(state: VyrlumeUiState, viewModel: VyrlumeViewModel) {
    if (state.showServerSheet) {
        ServerSheet(
            servers = state.servers,
            activeServer = state.activeServer,
            connectionError = state.notice,
            onDismiss = { viewModel.showServerSheet(false) },
            onConnect = viewModel::connect,
            onSelect = viewModel::selectServer,
            onRemove = viewModel::removeServer,
            onDemo = viewModel::enterDemo,
        )
    }
    if (state.showCommandPalette) {
        CommandPalette(
            workspaceKind = state.workspaceKind,
            onDismiss = { viewModel.showCommandPalette(false) },
            onAction = { action ->
                viewModel.showCommandPalette(false)
                when (action) {
                    "files" -> viewModel.selectSection(IdeSection.FILES)
                    "terminal" -> viewModel.selectSection(IdeSection.TERMINAL)
                    "preview" -> viewModel.selectSection(IdeSection.PREVIEW)
                    "save" -> viewModel.saveOpenFile()
                    "reset" -> viewModel.resetDemo()
                    "servers" -> viewModel.showServerSheet(true)
                }
            },
        )
    }
    if (state.showNewFileDialog) {
        NewFileDialog(
            onDismiss = { viewModel.showNewFileDialog(false) },
            onCreate = viewModel::addFile,
        )
    }
}

private fun IdeSection.icon(): ImageVector = when (this) {
    IdeSection.EDITOR -> Icons.Default.Description
    IdeSection.FILES -> Icons.Default.Folder
    IdeSection.TERMINAL -> Icons.Default.Terminal
    IdeSection.SOURCE_CONTROL -> Icons.Default.CallSplit
    IdeSection.PREVIEW -> Icons.Default.Language
}

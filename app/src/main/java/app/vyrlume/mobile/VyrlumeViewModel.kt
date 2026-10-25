package app.vyrlume.mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.vyrlume.mobile.data.DemoWorkspace
import app.vyrlume.mobile.data.IdeSection
import app.vyrlume.mobile.data.ServerConnection
import app.vyrlume.mobile.data.TerminalLine
import app.vyrlume.mobile.data.WorkspaceFile
import app.vyrlume.mobile.data.WorkspaceKind
import app.vyrlume.mobile.data.WorkspaceStore
import app.vyrlume.mobile.demo.DemoTerminal
import app.vyrlume.mobile.util.ServerUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VyrlumeUiState(
    val enteredWorkspace: Boolean = false,
    val workspaceKind: WorkspaceKind = WorkspaceKind.DEMO,
    val section: IdeSection = IdeSection.EDITOR,
    val files: List<WorkspaceFile> = DemoWorkspace.files,
    val openFilePath: String = "src/main/kotlin/Main.kt",
    val servers: List<ServerConnection> = emptyList(),
    val activeServer: ServerConnection? = null,
    val showServerSheet: Boolean = false,
    val showCommandPalette: Boolean = false,
    val showNewFileDialog: Boolean = false,
    val terminalLines: List<TerminalLine> = DemoTerminal.welcome,
    val previewPort: Int = 3000,
    val notice: String? = null,
) {
    val openFile: WorkspaceFile
        get() = files.firstOrNull { it.path == openFilePath } ?: files.first()
}

class VyrlumeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = WorkspaceStore(application)
    private val initialServers = store.loadServers()
    private val initialActive = initialServers.firstOrNull { it.id == store.loadActiveServerId() }
    private val _ui = MutableStateFlow(
        VyrlumeUiState(
            enteredWorkspace = store.hasEnteredWorkspace(),
            workspaceKind = if (initialActive == null) WorkspaceKind.DEMO else WorkspaceKind.REMOTE,
            activeServer = initialActive,
            servers = initialServers,
            files = store.loadDemoFiles(),
        ),
    )
    val ui: StateFlow<VyrlumeUiState> = _ui.asStateFlow()

    fun enterDemo() {
        store.setEnteredWorkspace(true)
        store.saveActiveServerId(null)
        _ui.update { it.copy(enteredWorkspace = true, workspaceKind = WorkspaceKind.DEMO, activeServer = null, showServerSheet = false) }
    }

    fun showServerSheet(show: Boolean) = _ui.update {
        it.copy(showServerSheet = show, notice = if (show) it.notice else null)
    }

    fun showCommandPalette(show: Boolean) = _ui.update { it.copy(showCommandPalette = show) }

    fun showNewFileDialog(show: Boolean) = _ui.update { it.copy(showNewFileDialog = show) }

    fun selectSection(section: IdeSection) = _ui.update { it.copy(section = section) }

    fun connect(name: String, rawUrl: String) {
        val result = ServerUrl.normalize(rawUrl)
        result.onSuccess { url ->
            val server = ServerConnection(
                name = name.trim().ifBlank { url.substringAfter("://").substringBefore('/') },
                url = url,
            )
            val servers = (listOf(server) + _ui.value.servers.filterNot { it.url == url })
                .map { if (it.url == url) it.copy(id = server.id, name = server.name, lastUsed = server.lastUsed) else it }
            store.saveServers(servers)
            store.saveActiveServerId(server.id)
            store.setEnteredWorkspace(true)
            _ui.update {
                it.copy(
                    enteredWorkspace = true,
                    workspaceKind = WorkspaceKind.REMOTE,
                    activeServer = server,
                    servers = servers,
                    section = IdeSection.EDITOR,
                    showServerSheet = false,
                    notice = null,
                )
            }
        }.onFailure { error ->
            _ui.update { it.copy(notice = error.message ?: "Invalid server address.") }
        }
    }

    fun selectServer(server: ServerConnection) {
        val updated = server.copy(lastUsed = System.currentTimeMillis())
        val servers = listOf(updated) + _ui.value.servers.filterNot { it.id == server.id }
        store.saveServers(servers)
        store.saveActiveServerId(updated.id)
        store.setEnteredWorkspace(true)
        _ui.update {
            it.copy(
                enteredWorkspace = true,
                workspaceKind = WorkspaceKind.REMOTE,
                activeServer = updated,
                servers = servers,
                section = IdeSection.EDITOR,
                showServerSheet = false,
            )
        }
    }

    fun removeServer(server: ServerConnection) {
        val servers = _ui.value.servers.filterNot { it.id == server.id }
        store.saveServers(servers)
        if (_ui.value.activeServer?.id == server.id) store.saveActiveServerId(null)
        _ui.update {
            it.copy(
                servers = servers,
                activeServer = if (it.activeServer?.id == server.id) null else it.activeServer,
                workspaceKind = if (it.activeServer?.id == server.id) WorkspaceKind.DEMO else it.workspaceKind,
            )
        }
    }

    fun openFile(path: String) {
        if (_ui.value.files.any { it.path == path }) {
            _ui.update { it.copy(openFilePath = path, section = IdeSection.EDITOR) }
        }
    }

    fun updateOpenFile(content: String) {
        _ui.update { state ->
            state.copy(files = state.files.map { file ->
                if (file.path == state.openFilePath && file.content != content) file.copy(content = content, dirty = true) else file
            })
        }
    }

    fun saveOpenFile() {
        val file = _ui.value.openFile
        store.saveDemoFile(file)
        _ui.update { state ->
            state.copy(
                files = state.files.map { if (it.path == file.path) it.copy(dirty = false) else it },
                notice = if (state.workspaceKind == WorkspaceKind.DEMO) "Saved ${file.path}" else null,
            )
        }
    }

    fun addFile(path: String) {
        val clean = path.trim().trimStart('/')
        if (clean.isBlank() || clean.contains("..")) {
            _ui.update { it.copy(notice = "Enter a valid relative file path.") }
            return
        }
        if (_ui.value.files.any { it.path == clean }) {
            _ui.update { it.copy(notice = "That file already exists.") }
            return
        }
        val file = WorkspaceFile(path = clean, language = clean.substringAfterLast('.', "Plain text"), content = "", dirty = true)
        store.saveDemoFile(file)
        _ui.update { it.copy(files = it.files + file, openFilePath = clean, section = IdeSection.EDITOR, showNewFileDialog = false) }
    }

    fun runDemoCommand(command: String) {
        if (command.trim().equals("clear", true)) {
            _ui.update { it.copy(terminalLines = emptyList()) }
            return
        }
        val lines = DemoTerminal.execute(command, _ui.value.files)
        _ui.update { it.copy(terminalLines = (it.terminalLines + lines).takeLast(250)) }
    }

    fun setPreviewPort(port: Int) {
        if (port in 1..65535) _ui.update { it.copy(previewPort = port) }
    }

    fun consumeNotice() = _ui.update { it.copy(notice = null) }

    fun resetDemo() {
        val files = DemoWorkspace.files
        store.resetDemoFiles()
        _ui.update {
            it.copy(
                files = files,
                openFilePath = files.first().path,
                terminalLines = DemoTerminal.welcome,
                notice = "Demo workspace restored",
            )
        }
    }
}

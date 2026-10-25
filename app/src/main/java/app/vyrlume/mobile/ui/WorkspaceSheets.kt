package app.vyrlume.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import app.vyrlume.mobile.data.ServerConnection
import app.vyrlume.mobile.data.WorkspaceKind
import app.vyrlume.mobile.ui.theme.AeroMint
import app.vyrlume.mobile.ui.theme.AeroSky

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSheet(
    servers: List<ServerConnection>,
    activeServer: ServerConnection?,
    connectionError: String?,
    onDismiss: () -> Unit,
    onConnect: (String, String) -> Unit,
    onSelect: (ServerConnection) -> Unit,
    onRemove: (ServerConnection) -> Unit,
    onDemo: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Your workspaces", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Connect to an existing code-server over HTTPS or your private Tailscale network.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
            }
            items(servers, key = { it.id }) { server ->
                ServerCard(
                    server = server,
                    active = activeServer?.id == server.id,
                    onSelect = { onSelect(server) },
                    onRemove = { onRemove(server) },
                )
            }
            item {
                OutlinedButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Code, contentDescription = null)
                    Text("  Open local demo workspace")
                }
            }
            item {
                Text(
                    "ADD A SERVER",
                    style = MaterialTheme.typography.labelLarge,
                    color = AeroMint,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().testTag("server_name"),
                    label = { Text("Workspace name") },
                    placeholder = { Text("Home workstation") },
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth().testTag("server_url"),
                    label = { Text("Server URL") },
                    placeholder = { Text("https://code.example.ts.net") },
                    singleLine = true,
                )
            }
            if (url.trim().startsWith("http://")) {
                item {
                    Text(
                        "HTTP is recommended only on a trusted local or private network.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            connectionError?.let { message ->
                item {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("server_error"),
                    )
                }
            }
            item {
                Button(
                    onClick = { onConnect(name, url) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_server"),
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null)
                    Text("  Save and connect")
                }
            }
            item {
                Text(
                    "Vyrlume stores server names and URLs, not passwords. Sign-in happens inside the server’s own workbench. TLS errors are never bypassed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: ServerConnection,
    active: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(server.name, style = MaterialTheme.typography.titleMedium)
                Text(server.url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (active) Text("CONNECTED", style = MaterialTheme.typography.labelLarge, color = AeroSky)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove ${server.name}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandPalette(
    workspaceKind: WorkspaceKind,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val actions = buildList {
        add(PaletteAction("Open files", "Browse the demo workspace", Icons.Default.FolderOpen, "files"))
        add(PaletteAction("Open terminal", if (workspaceKind == WorkspaceKind.DEMO) "Interactive demo shell" else "Remote workbench terminal", Icons.Default.Terminal, "terminal"))
        add(PaletteAction("Open preview", "Development server on port 3000", Icons.Default.Language, "preview"))
        if (workspaceKind == WorkspaceKind.DEMO) {
            add(PaletteAction("Save current file", "Persist changes on this device", Icons.Default.Save, "save"))
            add(PaletteAction("Restore demo workspace", "Reset bundled files and terminal", Icons.Default.Refresh, "reset"))
        }
        add(PaletteAction("Connect a server", "Manage code-server workspaces", Icons.Default.Cloud, "servers"))
    }.filter { it.title.contains(query, true) || it.subtitle.contains(query, true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().testTag("command_search"),
                label = { Text("Command palette") },
                placeholder = { Text("What do you want to do?") },
                singleLine = true,
            )
            LazyColumn(Modifier.padding(top = 12.dp)) {
                items(actions, key = { it.id }) { action ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onAction(action.id) }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(action.icon, contentDescription = null, tint = AeroSky)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(action.title, style = MaterialTheme.typography.titleMedium)
                            Text(action.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private data class PaletteAction(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val id: String,
)

@Composable
fun NewFileDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var path by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        title = { Text("New demo file") },
        text = {
            OutlinedTextField(
                value = path,
                onValueChange = { path = it },
                modifier = Modifier.testTag("new_file_path"),
                label = { Text("Relative path") },
                placeholder = { Text("src/NewFile.kt") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(path) }, modifier = Modifier.testTag("create_file")) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

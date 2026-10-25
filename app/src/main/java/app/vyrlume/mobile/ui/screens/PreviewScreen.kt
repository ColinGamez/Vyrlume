package app.vyrlume.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.vyrlume.mobile.ui.components.DemoPreviewHtml
import app.vyrlume.mobile.ui.components.PreviewSurface

@Composable
fun PreviewScreen(
    port: Int,
    remoteUrl: String?,
    onPort: (Int) -> Unit,
) {
    var reloadKey by remember { mutableIntStateOf(0) }
    var portText by remember(port) { mutableStateOf(port.toString()) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (remoteUrl == null) "Interactive demo" else "Forwarded port", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (remoteUrl == null) "Bundled web preview" else "Through code-server /proxy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = portText,
                onValueChange = { value ->
                    portText = value.filter { it.isDigit() }.take(5)
                    portText.toIntOrNull()?.let(onPort)
                },
                modifier = Modifier.padding(start = 12.dp).fillMaxWidth(0.34f).testTag("preview_port"),
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            IconButton(onClick = { reloadKey++ }, modifier = Modifier.testTag("reload_preview")) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload preview")
            }
        }
        PreviewSurface(
            url = remoteUrl,
            demoHtml = if (remoteUrl == null) DemoPreviewHtml else null,
            reloadKey = reloadKey,
        )
    }
}

package app.vyrlume.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vyrlume.mobile.data.ServerConnection
import app.vyrlume.mobile.ui.components.AeroBackdrop
import app.vyrlume.mobile.ui.theme.AeroMint
import app.vyrlume.mobile.ui.theme.AeroSky

@Composable
fun WelcomeScreen(
    servers: List<ServerConnection>,
    onDemo: () -> Unit,
    onConnect: () -> Unit,
    onServer: (ServerConnection) -> Unit,
) {
    AeroBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                tonalElevation = 8.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = AeroSky, modifier = Modifier.size(18.dp))
                    Text("MOBILE IDE · ANDROID", style = MaterialTheme.typography.labelLarge, color = AeroSky)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Vyrlume", style = MaterialTheme.typography.displaySmall)
            Text(
                "Your next build,\nright in your pocket.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "A touch-first workspace for quick fixes, real terminals and full remote development. No tiny desktop sidebars required.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 20.dp),
            )
            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeaturePill("Edit")
                FeaturePill("Run")
                FeaturePill("Preview")
            }
            Button(
                onClick = onDemo,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp).height(54.dp).testTag("enter_demo"),
            ) {
                Text("Explore the demo workspace")
                Spacer(Modifier.size(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
            OutlinedButton(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp).testTag("connect_server"),
            ) {
                Icon(Icons.Default.Cloud, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Connect a code-server")
            }
            if (servers.isNotEmpty()) {
                Text(
                    "RECENT WORKSPACES",
                    style = MaterialTheme.typography.labelLarge,
                    color = AeroMint,
                    modifier = Modifier.padding(top = 32.dp, bottom = 12.dp),
                )
                servers.take(3).forEach { server ->
                    Card(
                        onClick = { onServer(server) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(server.name, style = MaterialTheme.typography.titleMedium)
                            Text(server.url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text(
                "Original software built on open standards. Not affiliated with Microsoft.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun FeaturePill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

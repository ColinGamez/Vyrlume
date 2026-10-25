package app.vyrlume.mobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vyrlume.mobile.data.TerminalLine
import app.vyrlume.mobile.ui.theme.AeroMint
import app.vyrlume.mobile.ui.theme.AeroMuted
import app.vyrlume.mobile.ui.theme.AeroSky

@Composable
fun TerminalScreen(
    lines: List<TerminalLine>,
    onRun: (String) -> Unit,
) {
    var command by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    fun submit() {
        if (command.isNotBlank()) {
            onRun(command)
            command = ""
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("terminal_output"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            items(lines) { line ->
                Text(
                    text = line.text,
                    color = when (line.tone) {
                        TerminalLine.Tone.INPUT -> AeroSky
                        TerminalLine.Tone.SUCCESS -> AeroMint
                        TerminalLine.Tone.ERROR -> MaterialTheme.colorScheme.error
                        TerminalLine.Tone.MUTED -> AeroMuted
                        TerminalLine.Tone.OUTPUT -> MaterialTheme.colorScheme.onSurface
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f).testTag("terminal_command"),
                placeholder = { Text("Try ‘help’ or ‘npm test’") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
            )
            IconButton(onClick = { submit() }, modifier = Modifier.testTag("run_command")) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Run command")
            }
        }
    }
}

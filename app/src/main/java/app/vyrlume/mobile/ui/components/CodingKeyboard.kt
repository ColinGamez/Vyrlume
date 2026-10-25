package app.vyrlume.mobile.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun CodingKeyboard(
    onText: (String) -> Unit,
    onMove: (Int) -> Unit,
    onSpecial: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            KeyboardKey("Esc") { onSpecial("escape") }
            KeyboardKey("Tab") { onText("\t") }
            KeyboardKey("←") { onMove(-1) }
            KeyboardKey("→") { onMove(1) }
            listOf("{", "}", "[", "]", "(", ")", "=", ":", ";", "/", "_", "\"", "<", ">").forEach { symbol ->
                KeyboardKey(symbol) { onText(symbol) }
            }
        }
    }
}

@Composable
private fun KeyboardKey(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 44.dp).testTag("key_$label"),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

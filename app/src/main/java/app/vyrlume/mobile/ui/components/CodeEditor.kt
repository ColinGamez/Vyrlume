package app.vyrlume.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vyrlume.mobile.data.WorkspaceFile
import app.vyrlume.mobile.ui.theme.AeroMuted
import app.vyrlume.mobile.ui.theme.AeroSky

@Composable
fun CodeEditor(
    file: WorkspaceFile,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardRequest: KeyboardRequest? = null,
    onKeyboardHandled: () -> Unit = {},
) {
    var value by remember(file.path) {
        mutableStateOf(TextFieldValue(file.content, TextRange(file.content.length)))
    }
    val scroll = rememberScrollState()
    val codeStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    )

    LaunchedEffect(file.path, file.content) {
        if (file.content != value.text) value = value.copy(text = file.content)
    }
    LaunchedEffect(keyboardRequest?.id) {
        keyboardRequest?.let { request ->
            value = applyKeyboardRequest(value, request)
            onChange(value.text)
            onKeyboardHandled()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(file.language, style = MaterialTheme.typography.labelLarge, color = AeroSky)
            Text(
                "${value.text.lineCount()} lines${if (file.dirty) "  •  unsaved" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(bottom = 24.dp),
        ) {
            Column(
                Modifier.width(52.dp).padding(top = 4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                repeat(value.text.lineCount()) { index ->
                    Text(
                        text = "${index + 1}",
                        style = codeStyle,
                        color = AeroMuted.copy(alpha = 0.65f),
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            }
            BasicTextField(
                value = value,
                onValueChange = {
                    value = it
                    onChange(it.text)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp, top = 4.dp)
                    .testTag("code_editor"),
                textStyle = codeStyle,
                cursorBrush = SolidColor(AeroSky),
                visualTransformation = remember { CodeSyntaxTransformation() },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.None,
                ),
            )
        }
    }
}

data class KeyboardRequest(val id: Long, val text: String? = null, val move: Int? = null)

private fun applyKeyboardRequest(value: TextFieldValue, request: KeyboardRequest): TextFieldValue {
    request.text?.let { insert ->
        val start = value.selection.min
        val end = value.selection.max
        val text = value.text.replaceRange(start, end, insert)
        val cursor = start + insert.length
        return value.copy(text = text, selection = TextRange(cursor))
    }
    request.move?.let { delta ->
        val cursor = (value.selection.start + delta).coerceIn(0, value.text.length)
        return value.copy(selection = TextRange(cursor))
    }
    return value
}

private class CodeSyntaxTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = buildAnnotatedString {
            append(text.text)
            val patterns = listOf(
                Regex("\\b(package|data class|fun|val|var|return|check|const|export|type|import|class|true|false|null)\\b") to AeroSky,
                Regex("\\b\\d+(?:\\.\\d+)?\\b") to MaterialNumber,
                Regex("(\"(?:\\\\.|[^\"])*\"|`(?:\\\\.|[^`])*`)") to MaterialString,
                Regex("(#.*|//.*|\\*/?|/\\*)") to AeroMuted,
            )
            patterns.forEach { (regex, color) ->
                regex.findAll(text.text).forEach { match ->
                    addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
                }
            }
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

private val MaterialNumber = androidx.compose.ui.graphics.Color(0xFFD8A7FF)
private val MaterialString = androidx.compose.ui.graphics.Color(0xFFA5E65A)

private fun String.lineCount(): Int = if (isEmpty()) 1 else count { it == '\n' } + 1

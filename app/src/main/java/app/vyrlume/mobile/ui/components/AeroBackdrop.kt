package app.vyrlume.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.vyrlume.mobile.ui.theme.AeroMidnight

@Composable
fun AeroBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A4562), AeroMidnight, Color(0xFF041421)),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawAeroBubble(Offset(size.width * 0.9f, size.height * 0.08f), size.width * 0.32f)
            drawAeroBubble(Offset(size.width * 0.05f, size.height * 0.65f), size.width * 0.18f)
            drawAeroBubble(Offset(size.width * 0.85f, size.height * 0.88f), size.width * 0.22f)
        }
        content()
    }
}

private fun DrawScope.drawAeroBubble(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x2258D8FF), Color(0x0058D8FF)),
            center = center,
            radius = radius,
        ),
        center = center,
        radius = radius,
    )
}

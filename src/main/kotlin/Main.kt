import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import sun.swing.SwingUtilities2.drawRect

@Composable
@Preview
fun app() {
    MaterialTheme {
        Canvas(modifier = Modifier.fillMaxSize()) {
            withTransform({
                scale(scaleX = size.width / 156, scaleY = size.height / 156)
                translate(left = size.width / 2, top = size.height / 2)
            }) {
                drawField()
            }
        }
    }
}

fun DrawScope.drawField() {
    for (i in -72..72 step 24) {
        drawLine(
            Color.Gray,
            start = Offset(i.toFloat(), -72f),
            end = Offset(i.toFloat(), 72f),
            strokeWidth = .5.dp.toPx(),
        )
        drawLine(
            Color.Gray,
            start = Offset(-72f, i.toFloat()),
            end = Offset(72f, i.toFloat()),
            strokeWidth = .5.dp.toPx(),
        )
    }
    drawRect(
        Color.DarkGray,
        topLeft = Offset(-72f, -72f),
        size = Size(144f, 144f),
        style =
            Stroke(
                width = 1f.dp.toPx(),
            ),
    )
}

fun main() =
    singleWindowApplication(
        title = "My CHR App",
        state = WindowState(width = 800.dp, height = 800.dp),
        alwaysOnTop = true,
    ) {
        app()
    }

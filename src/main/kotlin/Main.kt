
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import kotlin.math.abs

data class Robot(
    val position: Offset,
    val theta: Float,
) {
    constructor(x: Float, y: Float, theta: Float) : this(Offset(x, y), theta)

    val x: Float get() = position.x
    val y: Float get() = position.y

    fun setPosition(
        x: Float,
        y: Float,
    ) = Robot(x, y, theta)

    fun setPosition(position: Offset) = Robot(position, theta)

    fun setTheta(theta: Float) = Robot(x, y, theta)

    operator fun plus(other: Robot): Robot = Robot(x + other.x, y + other.y, theta + other.theta)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun app() {
    var robot by remember { mutableStateOf(Robot(x = 0f, y = 0f, theta = 0f)) }

    MaterialTheme {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                robot = robot.setPosition(it.toFieldCoords(size))
                            },
                        )
                    }.pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                robot = robot.setPosition(change.position.toFieldCoords(size))
                            },
                        )
                    }.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                run {
                                    event.changes.forEach { change ->
                                        if (event.buttons.isSecondaryPressed) {
                                            val position = change.position.toFieldCoords(size)
                                            // make the robot look at the position
                                            robot = robot.setTheta(robot.position.angleTo(position))
                                            event.changes.fastForEach { it.consume() }
                                            return@run
                                        }
                                    }
                                }
                            }
                        }
                    },
        ) {
            withTransform({
                scale(scaleX = size.width / 156, scaleY = -size.height / 156)
                translate(left = size.width / 2, top = size.height / 2)
            }) {
                drawField()
                drawRobot(robot)
                val distances = calculateDistances(robot)
                drawDistances(distances)
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

fun DrawScope.drawRobot(robot: Robot) {
    translate(left = robot.x, top = robot.y) {
        rotate(degrees = Math.toDegrees(robot.theta.toDouble()).toFloat(), pivot = Offset(0f, 0f)) {
            drawRect(
                color = Color.Red,
                alpha = 0.5f,
                topLeft = Offset(-10f, -10f),
                size = Size(20.dp.toPx(), 20.dp.toPx()),
            )
            drawRect(
                color = Color.Red,
                topLeft = Offset(-10f, -10f),
                size = Size(20.dp.toPx(), 20.dp.toPx()),
                style =
                    Stroke(
                        width = 1f.dp.toPx(),
                    ),
            )
            drawCircle(
                color = Color.Blue,
                radius = 1.dp.toPx(),
                center = Offset(0f, 0f),
            )
            drawLine(
                color = Color.Blue,
                start = Offset(0f, 0f),
                end = Offset(10.5.dp.toPx(), 0.dp.toPx()),
                strokeWidth = 1f.dp.toPx(),
            )
        }
    }
}

/**
 * @return a list of 4 distances, going counterclockwise
 */
fun calculateDistances(robot: Robot): List<Float> {
    val fieldSize = Size(156f, 156f)

    val relativeRobot =
        Robot(
            robot.position.rotate(-robot.theta),
            0f,
        )

    val topLeft = Offset(-fieldSize.width / 2, fieldSize.height / 2).rotate(-robot.theta)
    val topRight = Offset(fieldSize.width / 2, fieldSize.height / 2).rotate(-robot.theta)
    val bottomLeft = Offset(-fieldSize.width / 2, -fieldSize.height / 2).rotate(-robot.theta)
    val bottomRight = Offset(fieldSize.width / 2, -fieldSize.height / 2).rotate(-robot.theta)

    if (robot.theta.toDegrees() % 90.0 == 0.0) {
        val left = -fieldSize.width / 2 - relativeRobot.x
        val right = fieldSize.width / 2 - relativeRobot.x
        val top = fieldSize.height / 2 - relativeRobot.y
        val bottom = -fieldSize.height / 2 - relativeRobot.y

        return listOf(right, top, left, bottom).map { abs(it) }
    }

    val left = Line(topLeft, bottomLeft)
    val right = Line(topRight, bottomRight)
    val top = Line(topLeft, topRight)
    val bottom = Line(bottomLeft, bottomRight)

    val horizontalDistances =
        listOf(left, right, top, bottom)
            .map {
                Offset((relativeRobot.y - it.intercept) / it.slope, relativeRobot.y)
            }.map { it.x - relativeRobot.x }

    val verticalDistances =
        listOf(left, right, top, bottom)
            .map {
                Offset(relativeRobot.x, it.slope * relativeRobot.x + it.intercept)
            }.map { it.y - relativeRobot.y }

    val leftDistance = horizontalDistances.filter { it < 0 }.minOfOrNull { abs(it) } ?: 0f
    val rightDistance = horizontalDistances.filter { it > 0 }.minOfOrNull { abs(it) } ?: 0f
    val topDistance = verticalDistances.filter { it > 0 }.minOfOrNull { abs(it) } ?: 0f
    val bottomDistance = verticalDistances.filter { it < 0 }.minOfOrNull { abs(it) } ?: 0f

    return listOf(rightDistance, topDistance, leftDistance, bottomDistance)
}

fun DrawScope.drawDistances(distances: List<Float>) {
    println(distances)
}

fun main() =
    singleWindowApplication(
        title = "My CHR App",
        state = WindowState(width = 800.dp, height = 800.dp),
        alwaysOnTop = true,
    ) {
        app()
    }

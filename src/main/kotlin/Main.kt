
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
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import util.LineSegment
import util.Ray
import util.Rectangle
import util.RectangularArea
import util.angleTo
import util.coerceIn
import util.distanceTo
import util.normalize360
import util.rotate
import util.toDegrees
import util.toFieldCoords
import util.toRadians
import kotlin.math.PI

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
    var obstacle by remember {
        mutableStateOf(
            Rectangle(
                center = Offset(36f, 12f),
                size = Size(18f, 18f),
                theta = 22.5f.toRadians(),
            ),
        )
    }
    val useObstacle = false

    MaterialTheme {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                robot =
                                    robot.setPosition(
                                        it.toFieldCoords(size).coerceIn(
                                            Offset(-71f, -71f),
                                            Offset(71f, 71f),
                                        ),
                                    )
                            },
                        )
                    }.pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                robot =
                                    robot.setPosition(
                                        change.position.toFieldCoords(size).coerceIn(
                                            Offset(-71f, -71f),
                                            Offset(71f, 71f),
                                        ),
                                    )
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
                                        } else if (event.buttons.isTertiaryPressed) {
                                            val position = change.position.toFieldCoords(size)
                                            obstacle = obstacle.setCenter(position)
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
                val distances = calculateDistances(robot, if (useObstacle) listOf(obstacle) else emptyList())
                drawDistances(robot, distances)
                drawRobot(robot)
                if (useObstacle) drawRobot(Robot(obstacle.center, obstacle.theta))
                val pose = calculatePose(robot.theta, distances, robot.position)
                if (pose.distanceTo(robot.position) > 1f) {
                    println("(${pose.x} ${pose.y}) (${robot.x} ${robot.y}) | ${pose.distanceTo(robot.position)}")
                }
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
    val robotSize = Size(16f.dp.toPx(), 16f.dp.toPx())

    translate(left = robot.x, top = robot.y) {
        rotate(degrees = Math.toDegrees(robot.theta.toDouble()).toFloat(), pivot = Offset(0f, 0f)) {
            drawRect(
                color = Color.Red,
                alpha = 0.5f,
                topLeft = Offset(-robotSize.width, -robotSize.height) / 2f,
                size = robotSize,
            )
            drawRect(
                color = Color.Red,
                topLeft = Offset(-robotSize.width, -robotSize.height) / 2f,
                size = robotSize,
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
                end = Offset(robotSize.width / 2 + 0.5.dp.toPx(), 0.dp.toPx()),
                strokeWidth = 1f.dp.toPx(),
            )
        }
    }
}

/**
 * @return a list of 4 distances, going counterclockwise
 */
fun calculateDistances(
    robot: Robot,
    obstacles: List<Rectangle>,
): List<Float> {
    val fieldSize = Size(144f, 144f)

    val relativeRobot =
        Robot(
            robot.position.rotate(-robot.theta),
            0f,
        )

    val topLeft = Offset(-fieldSize.width / 2, fieldSize.height / 2)
    val topRight = Offset(fieldSize.width / 2, fieldSize.height / 2)
    val bottomLeft = Offset(-fieldSize.width / 2, -fieldSize.height / 2)
    val bottomRight = Offset(fieldSize.width / 2, -fieldSize.height / 2)

    val left = LineSegment(topLeft, bottomLeft)
    val right = LineSegment(topRight, bottomRight)
    val top = LineSegment(topLeft, topRight)
    val bottom = LineSegment(bottomLeft, bottomRight)
    val all = listOf(left, right, top, bottom) + obstacles.map { it.sides }.flatten()

    val frontDistance =
        all.mapNotNull { Ray(robot.position, robot.theta).intersects(it) }.minOfOrNull { it.distanceTo(robot.position) }
    val backDistance =
        all.mapNotNull { Ray(robot.position, PI + robot.theta).intersects(it) }.minOfOrNull { it.distanceTo(robot.position) }
    val leftDistance =
        all.mapNotNull { Ray(robot.position, PI / 2 + robot.theta).intersects(it) }.minOfOrNull { it.distanceTo(robot.position) }
    val rightDistance =
        all.mapNotNull { Ray(robot.position, 3 * PI / 2 + robot.theta).intersects(it) }.minOfOrNull { it.distanceTo(robot.position) }

    return listOf(frontDistance, leftDistance, backDistance, rightDistance).map { it ?: 0f }
}

fun DrawScope.drawDistances(
    robot: Robot,
    distances: List<Float>,
) {
    translate(left = robot.x, top = robot.y) {
        rotate(degrees = Math.toDegrees(robot.theta.toDouble()).toFloat(), pivot = Offset(0f, 0f)) {
//            drawLine(
//                start = Offset(0f, 0f),
//                end = Offset(distances[0], 0f),
//                color = Color.Green,
//                strokeWidth = 1f.dp.toPx(),
//            )
//            drawLine(
//                start = Offset(0f, 0f),
//                end = Offset(0f, distances[1]),
//                color = Color.Green,
//                strokeWidth = 1f.dp.toPx(),
//            )
//            drawLine(
//                start = Offset(0f, 0f),
//                end = Offset(-distances[2], 0f),
//                color = Color.Green,
//                strokeWidth = 1f.dp.toPx(),
//            )
//            drawLine(
//                start = Offset(0f, 0f),
//                end = Offset(0f, -distances[3]),
//                color = Color.Green,
//                strokeWidth = 1f.dp.toPx(),
//            )
        }
    }
//    println(distances)
}

fun DrawScope.calculatePose(
    theta: Float,
    distances: List<Float>,
    robotPosition: Offset,
): Offset {
    val fieldSize = Size(144f, 144f)
    val left = LineSegment(Offset(-1f, 1f), Offset(-1f, -1f)) * (fieldSize / 2f)
    val right = LineSegment(Offset(1f, 1f), Offset(1f, -1f)) * (fieldSize / 2f)
    val top = LineSegment(Offset(-1f, 1f), Offset(1f, 1f)) * (fieldSize / 2f)
    val bottom = LineSegment(Offset(-1f, -1f), Offset(1f, -1f)) * (fieldSize / 2f)

    val walls = listOf(right, top, left, bottom).zipWithNext() + (bottom to right)

    val startingPossible = (theta.normalize360().toDegrees() / 90).toInt()
    val possibleFront = walls[startingPossible].toList()
    val possibleLeft = walls[(startingPossible + 1) % 4].toList()
    val possibleBack = walls[(startingPossible + 2) % 4].toList()
    val possibleRight = walls[(startingPossible + 3) % 4].toList()

    val sensorArea = 2f

    val newFront =
        possibleFront.map {
            RectangularArea.fromLineSegment(
                it.translate(Offset(distances[0], 0f).rotate(180f.toRadians() + theta)),
                sensorArea,
            )
        }
    val newLeft =
        possibleLeft.map {
            RectangularArea.fromLineSegment(
                it.translate(Offset(distances[1], 0f).rotate(270f.toRadians() + theta)),
                sensorArea,
            )
        }
    val newBack =
        possibleBack.map {
            RectangularArea.fromLineSegment(
                it.translate(Offset(distances[2], 0f).rotate(theta)),
                sensorArea,
            )
        }
    val newRight =
        possibleRight.map {
            RectangularArea.fromLineSegment(
                it.translate(Offset(distances[3], 0f).rotate(90f.toRadians() + theta)),
                sensorArea,
            )
        }

//    println(startingPossible)

//    drawRect(
//        color = Color.Red,
//        topLeft = newFront[0].topLeft,
//        size = newFront[0].size,
//        alpha = .25f,
//    )
//    drawRect(
//        color = Color.Red,
//        topLeft = newFront[1].topLeft,
//        size = newFront[1].size,
//        alpha = .25f,
//    )
//
//    drawRect(
//        color = Color.Blue,
//        topLeft = newLeft[0].topLeft,
//        size = newLeft[0].size,
//        alpha = .25f,
//    )
//    drawRect(
//        color = Color.Blue,
//        topLeft = newLeft[1].topLeft,
//        size = newLeft[1].size,
//        alpha = .25f,
//    )
//
//    drawRect(
//        color = Color.Green,
//        topLeft = newBack[0].topLeft,
//        size = newBack[0].size,
//        alpha = .25f,
//    )
//    drawRect(
//        color = Color.Green,
//        topLeft = newBack[1].topLeft,
//        size = newBack[1].size,
//        alpha = .25f,
//    )
//
//    drawRect(
//        color = Color.Yellow,
//        topLeft = newRight[0].topLeft,
//        size = newRight[0].size,
//        alpha = .25f,
//    )
//    drawRect(
//        color = Color.Yellow,
//        topLeft = newRight[1].topLeft,
//        size = newRight[1].size,
//        alpha = .25f,
//    )

    val overlapsFL = newFront[0].intersections(newLeft) + newFront[1].intersections(newLeft)
    val overlapsFLB = overlapsFL.map { it.intersections(newBack) }.flatten()
    val overlapsFLBR = overlapsFLB.map { it.intersections(newRight) }.flatten()

    overlapsFLBR.forEach {
        drawRect(
            color = Color.Cyan,
            topLeft = it.topLeft,
            size = it.size,
            alpha = 1f,
        )
    }

    val result =
        if (overlapsFLBR.size > 1) {
            overlapsFLBR[0].intersections(overlapsFLBR.drop(1)).getOrNull(0)?.center ?: Offset.Zero
        } else if (overlapsFLBR.size < 0) {
            println("ERROR ${overlapsFLBR.size}")
            Offset.Zero
        } else {
            overlapsFLBR[0].center
        }

    return result
}

fun main() =
    singleWindowApplication(
        title = "My CHR App",
        state = WindowState(width = 800.dp, height = 800.dp),
        alwaysOnTop = true,
    ) {
        app()
    }

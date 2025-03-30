
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
    var obstacle by remember { mutableStateOf(Rectangle(center = Offset(36f, 12f), size = Size(18f, 18f), theta = 22.5f.toRadians())) }

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
                val distances = calculateDistances(robot, listOf(obstacle))
                drawDistances(robot, distances)
//                val pose = calculatePose(robot.theta, distances, robot.position)
//                print("(${pose.x} ${pose.y}) (${robot.x} ${robot.y})")
                drawRobot(robot)
                drawRobot(Robot(obstacle.center, obstacle.theta))
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
fun DrawScope.calculateDistances(
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
            drawLine(
                start = Offset(0f, 0f),
                end = Offset(distances[0], 0f),
                color = Color.Green,
                strokeWidth = 1f.dp.toPx(),
            )
            drawLine(
                start = Offset(0f, 0f),
                end = Offset(0f, distances[1]),
                color = Color.Green,
                strokeWidth = 1f.dp.toPx(),
            )
            drawLine(
                start = Offset(0f, 0f),
                end = Offset(-distances[2], 0f),
                color = Color.Green,
                strokeWidth = 1f.dp.toPx(),
            )
            drawLine(
                start = Offset(0f, 0f),
                end = Offset(0f, -distances[3]),
                color = Color.Green,
                strokeWidth = 1f.dp.toPx(),
            )
        }
    }
    println(distances)
}

// fun DrawScope.calculatePose(
//    theta: Float,
//    distances: List<Float>,
//    robotPosition: Offset,
// ): Offset {
//    val fieldSize = Size(144f, 144f)
//
//    val topLeft = Offset(-fieldSize.width / 2, fieldSize.height / 2).rotate(-theta)
//    val topRight = Offset(fieldSize.width / 2, fieldSize.height / 2).rotate(-theta)
//    val bottomLeft = Offset(-fieldSize.width / 2, -fieldSize.height / 2).rotate(-theta)
//    val bottomRight = Offset(fieldSize.width / 2, -fieldSize.height / 2).rotate(-theta)
//
//    val left = LineSegment(topLeft, bottomLeft)
//    val right = LineSegment(topRight, bottomRight)
//    val top = LineSegment(topLeft, topRight)
//    val bottom = LineSegment(bottomLeft, bottomRight)
//
//    val walls = listOf(right, top, left, bottom).zipWithNext() + (top to left)
//    val startingPossible = (theta.normalize360().toDegrees() / 90).toInt()
//    val possibleFront = walls[startingPossible].toList()
//    val possibleLeft = walls[(startingPossible + 1) % 4].toList()
//    val possibleBack = walls[(startingPossible + 2) % 4].toList()
//    val possibleRight = walls[(startingPossible + 3) % 4].toList()
//
//    val newFront = possibleFront.map { it.translate(Offset(distances[0], 0f)) }
//    val newBack = possibleBack.map { it.translate(Offset(distances[2], 0f)) }
//    val newLeft = possibleLeft.map { it.translate(Offset(0f, distances[1])) }
//    val newRight = possibleRight.map { it.translate(Offset(0f, distances[3])) }
//
//    println(startingPossible)
//    rotate(theta.toDegrees(), pivot = Offset(0f, 0f)) {
//        possibleFront.forEach {
//            drawLine(
//                start = Offset(-72f, -72f * it.slope + it.intercept),
//                end = Offset(72f, 72f * it.slope + it.intercept),
//                color = Color.Red,
//                strokeWidth = 2f,
//            )
//        }
//        println(possibleFront)
//    }
//
//    return findEstimate(newLeft, newFront, newRight, newBack)
// }
//
// fun DrawScope.findEstimate(
//    left: List<Line>,
//    top: List<Line>,
//    right: List<Line>,
//    bottom: List<Line>,
// ): Offset {
//    val leftsIntersections = left.map { it.intersections(top + right + bottom) }
//    val topsIntersections = top.map { it.intersections(left + right + bottom) }
//    val rightsIntersections = right.map { it.intersections(top + left + bottom) }
//    val bottomsIntersections = bottom.map { it.intersections(top + left + right) }
//
//    val allIntersections = (leftsIntersections + topsIntersections + rightsIntersections + bottomsIntersections).flatten()
//
//    // map to occurance
//    var maxOccurances = 0
//    var maxPoint: Offset? = null
//
//    allIntersections.filterNotNull().forEach { a ->
//        val occurances = allIntersections.count { it == a }
//        if (occurances > maxOccurances) {
//            maxOccurances = occurances
//            maxPoint = a
//        }
//    }
//
//    return maxPoint!!
// }

fun main() =
    singleWindowApplication(
        title = "My CHR App",
        state = WindowState(width = 800.dp, height = 800.dp),
        alwaysOnTop = true,
    ) {
        app()
    }

package util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

data class LineSegment(
    val p1: Offset,
    val p2: Offset,
) {
    val slope: Float
        get() = (p2.y - p1.y) / (p2.x - p1.x)

    val intercept: Float
        get() = p1.y - slope * p1.x

    fun translate(offset: Offset) = LineSegment(p1 + offset, p2 + offset)

    operator fun times(scalar: Float): LineSegment = LineSegment(p1 * scalar, p2 * scalar)

    operator fun times(scalar: Size): LineSegment =
        LineSegment(
            Offset(p1.x * scalar.width, p1.y * scalar.height),
            Offset(p2.x * scalar.width, p2.y * scalar.height),
        )

    operator fun plus(offset: Offset): LineSegment = translate(offset)
}

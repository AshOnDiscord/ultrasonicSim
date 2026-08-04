package util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs

data class LineSegment(
    val p1: Offset,
    val p2: Offset,
) {
    val slope: Float
        get() = (p2.y - p1.y) / (p2.x - p1.x)

    val intercept: Float
        get() = p1.y - slope * p1.x

    fun intersection(other: LineSegment): Offset? {
        val diff = p2 - p1
        val otherDiff = other.p2 - other.p1
        val det = -diff.x * otherDiff.y + diff.y * otherDiff.x
        if (abs(det) < 1e-6) {
            return null
        }

        val t = ((p1.x - other.p1.x) * otherDiff.y - (p1.y - other.p1.y) * otherDiff.x) / det
        val u = ((p1.x - other.p1.x) * diff.y - (p1.y - other.p1.y) * diff.x) / det

        if (t in 0f..1f && u in 0f..1f) {
            return Offset(p1.x + t * diff.x, p1.y + t * diff.y)
        }
        return null
    }

    fun intersections(others: List<LineSegment>): List<Offset> = others.mapNotNull { intersection(it) }

    fun translate(offset: Offset) = LineSegment(p1 + offset, p2 + offset)

    operator fun times(scalar: Float): LineSegment = LineSegment(p1 * scalar, p2 * scalar)

    operator fun times(scalar: Size): LineSegment =
        LineSegment(
            Offset(p1.x * scalar.width, p1.y * scalar.height),
            Offset(p2.x * scalar.width, p2.y * scalar.height),
        )

    operator fun plus(offset: Offset): LineSegment = translate(offset)
}

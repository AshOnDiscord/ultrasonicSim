package util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

// no rotation support
data class RectangularArea(
    val topLeft: Offset,
    val bottomRight: Offset,
) {
    val size: Size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)

    val center = topLeft + (size / 2f).toOffset()

    fun intersection(other: RectangularArea): RectangularArea? {
        val topLeft =
            Offset(
                maxOf(topLeft.x, other.topLeft.x),
                maxOf(topLeft.y, other.topLeft.y),
            )
        val bottomRight =
            Offset(
                minOf(bottomRight.x, other.bottomRight.x),
                minOf(bottomRight.y, other.bottomRight.y),
            )
        return if (topLeft.x <= bottomRight.x && topLeft.y <= bottomRight.y) {
            RectangularArea(topLeft, bottomRight)
        } else {
            null
        }
    }

    fun intersections(others: List<RectangularArea>): List<RectangularArea> = others.mapNotNull { intersection(it) }

    companion object {
        fun fromLineSegment(
            segment: LineSegment,
            size: Float,
        ): RectangularArea {
            val leftMost = minOf(segment.p1.x, segment.p2.x)
            val rightMost = maxOf(segment.p1.x, segment.p2.x)
            val topMost = minOf(segment.p1.y, segment.p2.y)
            val bottomMost = maxOf(segment.p1.y, segment.p2.y)

            val topLeft = Offset(leftMost - size / 2, topMost - size / 2)
            val bottomRight = Offset(rightMost + size / 2, bottomMost + size / 2)
            return RectangularArea(topLeft, bottomRight)
        }
    }
}

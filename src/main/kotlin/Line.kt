import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs

data class LineSegment(
    val p1: Offset,
    val p2: Offset,
)

data class Ray(
    val p: Offset,
    val angle: Float,
) {
    constructor(p: Offset, angle: Double) : this(p, angle.toFloat())

    fun intersects(line: LineSegment): Offset? {
        val vector = Offset(1f, 0f).rotate(angle)

        val diff = line.p2 - line.p1

        val det = -vector.x * diff.y + vector.y * diff.x
        if (abs(det) < 1e-6) {
            return null
        }

        val t = ((p.x - line.p1.x) * diff.y - (p.y - line.p1.y) * diff.x) / det
        val u = ((p.x - line.p1.x) * vector.y - (p.y - line.p1.y) * vector.x) / det

        if (t >= 0 && u in 0f..1f) {
            return Offset(p.x + t * vector.x, p.y + t * vector.y)
        }
        return null
    }
}

data class Rectangle(
    val center: Offset,
    val size: Size,
    val theta: Float,
) {
    val topLeft: Offset = Offset(-size.width / 2, -size.height / 2).rotate(theta) + center
    val topRight: Offset = Offset(+size.width / 2, -size.height / 2).rotate(theta) + center
    val bottomLeft: Offset = Offset(-size.width / 2, +size.height / 2).rotate(theta) + center
    val bottomRight: Offset = Offset(+size.width / 2, +size.height / 2).rotate(theta) + center

    val points: List<Offset> = listOf(topLeft, topRight, bottomRight, bottomLeft)

    val left: LineSegment = LineSegment(topLeft, bottomLeft)
    val right: LineSegment = LineSegment(topRight, bottomRight)
    val top: LineSegment = LineSegment(topLeft, topRight)
    val bottom: LineSegment = LineSegment(bottomLeft, bottomRight)

    val sides = listOf(left, right, top, bottom)

    fun setCenter(center: Offset) = Rectangle(center, size, theta)

    fun setSize(size: Size) = Rectangle(center, size, theta)

    fun setTheta(theta: Float) = Rectangle(center, size, theta)
}

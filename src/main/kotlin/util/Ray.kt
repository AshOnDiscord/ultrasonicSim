package util

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

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

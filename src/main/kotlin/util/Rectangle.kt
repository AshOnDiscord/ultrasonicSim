package util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

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

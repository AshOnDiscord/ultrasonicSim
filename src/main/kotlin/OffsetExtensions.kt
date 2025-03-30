import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun Offset.toFieldScale(screen: IntSize): Offset =
    Offset(
        x = x / screen.width * 156f,
        y = y / screen.height * 156f,
    )

fun Offset.toFieldCoords(screen: IntSize): Offset =
    Offset(
        x = x / screen.width * 156f - 78,
        y = -(y / screen.height * 156f - 78),
    )

fun Offset.angleTo(other: Offset): Float = atan2(other.y - y, other.x - x)

fun Offset.rotate(theta: Float = 0f): Offset =
    Offset(
        x = x * cos(theta) - y * sin(theta),
        y = x * sin(theta) + y * cos(theta),
    )

fun Offset.distanceTo(other: Offset) = sqrt((other.x - x).pow(2) + (other.y - y).pow(2))

fun Offset.coerceIn(
    min: Offset,
    max: Offset,
): Offset =
    Offset(
        x = x.coerceIn(min.x, max.x),
        y = y.coerceIn(min.y, max.y),
    )

import androidx.compose.ui.geometry.Offset

data class Line(
    val slope: Float,
    val intercept: Float,
) {
    constructor(a: Offset, b: Offset) : this(
        slope =
            try {
                (b.y - a.y) / (b.x - a.x)
            } catch (e: ArithmeticException) {
                Float.POSITIVE_INFINITY
            },
        intercept =
            try {
                a.y - ((b.y - a.y) / (b.x - a.x)) * a.x
            } catch (e: ArithmeticException) {
                a.x
            },
    )
}

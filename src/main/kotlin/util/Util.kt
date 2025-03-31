package util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun Float.toDegrees(): Float = Math.toDegrees(this.toDouble()).toFloat()

fun Float.toRadians(): Float = Math.toRadians(this.toDouble()).toFloat()

fun Float.normalize(): Float = atan2(sin(this), cos(this))

fun Float.normalize360(): Float = ((this + 2 * Math.PI) % (2 * Math.PI)).toFloat()

fun Size.toOffset(): Offset = Offset(width, height)

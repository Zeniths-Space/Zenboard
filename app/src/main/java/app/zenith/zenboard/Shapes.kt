package app.zenith.zenboard

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Scalloped "cookie" badge (M3 Expressive), drawn as a polar sine wave so no shapes library is needed:
 * 8 lobes, valleys at 88% of the radius, which matches Kontract's rounded-star ScallopShape.
 */
val ScallopShape = polarShape(lobes = 8, depth = 0.12f)

/**
 * A rounded star with [lobes] bumps whose valleys sit at (1 - [depth]) of the radius, turned by
 * [rotation] radians. The M3 Expressive cookie, clover and soft-polygon shapes without a library.
 */
fun polarShape(lobes: Int, depth: Float, rotation: Double = 0.0) = GenericShape { size, _ ->
    val r = min(size.width, size.height) / 2
    val cx = size.width / 2
    val cy = size.height / 2
    val steps = 160
    for (i in 0..steps) {
        val t = 2 * PI * i / steps
        val radius = r * (1 - depth * (1 - cos(lobes * (t - rotation))) / 2).toFloat()
        val x = cx + radius * cos(t).toFloat()
        val y = cy + radius * sin(t).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Icon shapes for the settings home rows, one per category so they read apart at a glance. */
val CookieShape = polarShape(lobes = 9, depth = 0.10f)
val CloverShape = polarShape(lobes = 4, depth = 0.34f, rotation = PI / 4)
val SquircleShape = RoundedCornerShape(percent = 32)
val SoftPentagonShape = polarShape(lobes = 5, depth = 0.10f, rotation = -PI / 2)

val StadiumShape = RoundedCornerShape(percent = 50)

/** Grouped-list corner: large outer, small inner. */
fun groupShape(index: Int, count: Int): RoundedCornerShape {
    val top = if (index == 0) 28.dp else 8.dp
    val bottom = if (index == count - 1) 28.dp else 8.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

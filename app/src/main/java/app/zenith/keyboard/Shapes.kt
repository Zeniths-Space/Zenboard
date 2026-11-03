package app.zenith.keyboard

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
val ScallopShape = GenericShape { size, _ ->
    val lobes = 8
    val depth = 0.12f
    val r = min(size.width, size.height) / 2
    val cx = size.width / 2
    val cy = size.height / 2
    val steps = 160
    for (i in 0..steps) {
        val t = 2 * PI * i / steps
        val radius = r * (1 - depth * (1 - cos(lobes * t)) / 2).toFloat()
        val x = cx + radius * cos(t).toFloat()
        val y = cy + radius * sin(t).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

val StadiumShape = RoundedCornerShape(percent = 50)

/** Grouped-list corner: large outer, small inner. */
fun groupShape(index: Int, count: Int): RoundedCornerShape {
    val top = if (index == 0) 28.dp else 8.dp
    val bottom = if (index == count - 1) 28.dp else 8.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

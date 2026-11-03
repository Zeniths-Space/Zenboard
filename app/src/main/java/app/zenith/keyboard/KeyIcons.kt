package app.zenith.keyboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Icons as vector paths on a 24-unit grid. The arrows, backspace, search, send, check, keyboard, lock and emoji face are
 * Material Icons paths (Apache-2.0); shift and globe are drawn here. Avoids the multi-megabyte
 * material-icons-extended artifact for a dozen glyphs.
 */
object KeyIcons {
    val Backspace = filled(
        "M22,3H7c-0.69,0 -1.23,0.35 -1.59,0.88L0,12l5.41,8.11c0.36,0.53 0.9,0.89 1.59,0.89h15" +
            "c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM19,15.59L17.59,17 14,13.41 10.41,17 9,15.59 " +
            "12.59,12 9,8.41 10.41,7 14,10.59 17.59,7 19,8.41 15.41,12 19,15.59z",
    )
    val Return = filled("M19,7v4H5.83l3.58,-3.59L8,6l-6,6 6,6 1.41,-1.41L5.83,13H21V7z")
    val Search = filled(
        "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 " +
            "5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5z" +
            "M9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
    )
    val Send = filled("M2.01,21L23,12 2.01,3 2,10l15,2 -15,2z")
    val Next = filled("M12,4l-1.41,1.41L16.17,11H4v2h12.17l-5.58,5.59L12,20l8,-8z")
    val Previous = filled("M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")
    val Done = filled("M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z")

    val Keyboard = filled(
        "M20,5H4c-1.1,0 -1.99,0.9 -1.99,2L2,17c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V7c0,-1.1 -0.9,-2 -2,-2z" +
            "M11,8h2v2h-2V8zM11,11h2v2h-2v-2zM8,8h2v2H8V8zM8,11h2v2H8v-2zM7,13H5v-2h2v2zM7,10H5V8h2v2z" +
            "M16,17H8v-2h8v2zM16,13h-2v-2h2v2zM16,10h-2V8h2v2zM19,13h-2v-2h2v2zM19,10h-2V8h2v2z",
    )
    val Lock = filled(
        "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12" +
            "c1.1,0 2,-0.9 2,-2V10c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2" +
            " -0.9,2 -2,2zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z",
    )

    private const val ShiftArrow = "M12,3.5L3.5,12.5H8.5V19.5H15.5V12.5H20.5Z"

    /** Shift off: outlined arrow. */
    val ShiftOff = stroked(ShiftArrow)

    /** Shift once: filled arrow. */
    val ShiftOn = filled(ShiftArrow)

    /** Caps lock: filled arrow with a bar under it. */
    val CapsLock = filled("M12,2L3.5,11H8.5V16H15.5V11H20.5ZM8.5,18.5H15.5V21H8.5Z")

    val Emoji = filled(
        "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2z" +
            "M12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z" +
            "M15.5,11c0.83,0 1.5,-0.67 1.5,-1.5S16.33,8 15.5,8 14,8.67 14,9.5s0.67,1.5 1.5,1.5z" +
            "M8.5,11c0.83,0 1.5,-0.67 1.5,-1.5S9.33,8 8.5,8 7,8.67 7,9.5 7.67,11 8.5,11z" +
            "M12,17.5c2.33,0 4.31,-1.46 5.11,-3.5L6.89,14c0.8,2.04 2.78,3.5 5.11,3.5z",
    )

    /** Globe, shown small on the space bar as the "hold to change keyboard" hint. */
    val Globe = stroked(
        "M12,3A9,9 0,1 1,11.99,3ZM3,12H21M4.5,7.5H19.5M4.5,16.5H19.5" +
            "M12,3C9.5,5.5 8.5,8.7 8.5,12S9.5,18.5 12,21C14.5,18.5 15.5,15.3 15.5,12S14.5,5.5 12,3Z",
    )

    private fun builder() = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    )

    private fun filled(path: String) = builder()
        .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        .build()

    private fun stroked(path: String) = builder()
        .addPath(
            PathParser().parsePathString(path).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}

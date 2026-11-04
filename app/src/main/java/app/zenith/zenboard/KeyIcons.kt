package app.zenith.zenboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Icons as vector paths on a 24-unit grid. The arrows, backspace, search, send, check, keyboard,
 * lock, emoji face, moon, info, speech bubble and pin are Material Icons paths (Apache-2.0); shift,
 * globe and paste are drawn here. The other toolbar icons are Material Symbols Rounded
 * (Apache-2.0). Avoids the multi-megabyte material-icons-extended artifact for a dozen glyphs.
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

    val DarkMode = filled(
        "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36 " +
            "-0.98,1.37 -2.58,2.26 -4.4,2.26 -2.98,0 -5.4,-2.42 -5.4,-5.4 0,-1.81 0.89,-3.42 2.26,-4.4C12.92,3.04 12.46,3 12,3z",
    )
    val Info = filled(
        "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-6h2v6zM13,9h-2L11,7h2v2z",
    )
    val Bubble = filled("M20,2H4c-1.1,0 -2,0.9 -2,2v18l4,-4h14c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2z")

    /** Short text lines: word suggestions. */
    val Words = filled("M4,6h16v2H4zM4,11h16v2H4zM4,16h10v2H4z")
    val Vibration = filled(
        "M0,15h2V9H0v6zM3,17h2V7H3v10zM22,9v6h2V9h-2zM19,17h2V7h-2v10zM16.5,3h-9C6.67,3 6,3.67 6,4.5v15c0,0.83 0.67,1.5 1.5,1.5h9c0.83,0 1.5,-0.67 1.5,-1.5v-15c0,-0.83 -0.67,-1.5 -1.5,-1.5zM16,19H8V5h8v14z",
    )
    val Sound = filled(
        "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z",
    )
    // Toolbar: Material Symbols Rounded (Apache-2.0), drawn on their own 960-unit grid.
    /** Clipboard history (content_paste). */
    val Clipboard = symbol(
        "M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h167q11-35 43-57.5t70-22.5q40 0 71.5 22.5T594-840h" +
            "166q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560h-80v80q0 17-11.5 28.5T640-640H320q-17" +
            " 0-28.5-11.5T280-680v-80h-80v560Zm280-560q17 0 28.5-11.5T520-800q0-17-11.5-28.5T480-840q-17 0-28.5 11.5T440-80" +
            "0q0 17 11.5 28.5T480-760Z",
    )
    val SelectAll = symbol(
        "M160-760q-17 0-28.5-11.5T120-800q0-17 11.5-28.5T160-840q17 0 28.5 11.5T200-800q0 17-11.5 28.5T160-760Zm160 0q-" +
            "17 0-28.5-11.5T280-800q0-17 11.5-28.5T320-840q17 0 28.5 11.5T360-800q0 17-11.5 28.5T320-760Zm160 0q-17 0-28.5-" +
            "11.5T440-800q0-17 11.5-28.5T480-840q17 0 28.5 11.5T520-800q0 17-11.5 28.5T480-760Zm160 0q-17 0-28.5-11.5T600-8" +
            "00q0-17 11.5-28.5T640-840q17 0 28.5 11.5T680-800q0 17-11.5 28.5T640-760Zm160 0q-17 0-28.5-11.5T760-800q0-17 11" +
            ".5-28.5T800-840q17 0 28.5 11.5T840-800q0 17-11.5 28.5T800-760ZM160-600q-17 0-28.5-11.5T120-640q0-17 11.5-28.5T" +
            "160-680q17 0 28.5 11.5T200-640q0 17-11.5 28.5T160-600Zm640 0q-17 0-28.5-11.5T760-640q0-17 11.5-28.5T800-680q17" +
            " 0 28.5 11.5T840-640q0 17-11.5 28.5T800-600ZM160-440q-17 0-28.5-11.5T120-480q0-17 11.5-28.5T160-520q17 0 28.5 " +
            "11.5T200-480q0 17-11.5 28.5T160-440Zm640 0q-17 0-28.5-11.5T760-480q0-17 11.5-28.5T800-520q17 0 28.5 11.5T840-4" +
            "80q0 17-11.5 28.5T800-440ZM160-280q-17 0-28.5-11.5T120-320q0-17 11.5-28.5T160-360q17 0 28.5 11.5T200-320q0 17-" +
            "11.5 28.5T160-280Zm640 0q-17 0-28.5-11.5T760-320q0-17 11.5-28.5T800-360q17 0 28.5 11.5T840-320q0 17-11.5 28.5T" +
            "800-280ZM160-120q-17 0-28.5-11.5T120-160q0-17 11.5-28.5T160-200q17 0 28.5 11.5T200-160q0 17-11.5 28.5T160-120Z" +
            "m160 0q-17 0-28.5-11.5T280-160q0-17 11.5-28.5T320-200q17 0 28.5 11.5T360-160q0 17-11.5 28.5T320-120Zm160 0q-17" +
            " 0-28.5-11.5T440-160q0-17 11.5-28.5T480-200q17 0 28.5 11.5T520-160q0 17-11.5 28.5T480-120Zm160 0q-17 0-28.5-11" +
            ".5T600-160q0-17 11.5-28.5T640-200q17 0 28.5 11.5T680-160q0 17-11.5 28.5T640-120Zm160 0q-17 0-28.5-11.5T760-160" +
            "q0-17 11.5-28.5T800-200q17 0 28.5 11.5T840-160q0 17-11.5 28.5T800-120ZM360-280q-33 0-56.5-23.5T280-360v-240q0-" +
            "33 23.5-56.5T360-680h240q33 0 56.5 23.5T680-600v240q0 33-23.5 56.5T600-280H360Zm0-80h240v-240H360v240Z",
    )
    val Cut = symbol(
        "m480-400-94 94q8 15 11 32t3 34q0 66-47 113T240-80q-66 0-113-47T80-240q0-66 47-113t113-47q17 0 34 3t32 11l94-94" +
            "-94-94q-15 8-32 11t-34 3q-66 0-113-47T80-720q0-66 47-113t113-47q66 0 113 47t47 113q0 17-3 34t-11 32l438 438q27" +
            " 27 12 61.5T783-120q-11 0-21.5-4.5T743-137L480-400Zm120-120-80-80 223-223q8-8 18.5-12.5T783-840q38 0 52.5 35T8" +
            "23-743L600-520ZM240-640q33 0 56.5-23.5T320-720q0-33-23.5-56.5T240-800q-33 0-56.5 23.5T160-720q0 33 23.5 56.5T2" +
            "40-640Zm240 180q8 0 14-6t6-14q0-8-6-14t-14-6q-8 0-14 6t-6 14q0 8 6 14t14 6ZM240-160q33 0 56.5-23.5T320-240q0-3" +
            "3-23.5-56.5T240-320q-33 0-56.5 23.5T160-240q0 33 23.5 56.5T240-160Z",
    )
    val Copy = symbol(
        "M360-240q-33 0-56.5-23.5T280-320v-480q0-33 23.5-56.5T360-880h360q33 0 56.5 23.5T800-800v480q0 33-23.5 56.5T720" +
            "-240H360Zm0-80h360v-480H360v480ZM200-80q-33 0-56.5-23.5T120-160v-520q0-17 11.5-28.5T160-720q17 0 28.5 11.5T200" +
            "-680v520h400q17 0 28.5 11.5T640-120q0 17-11.5 28.5T600-80H200Zm160-240v-480 480Z",
    )
    /**
     * Paste: a clipboard with a sheet of paper in front, drawn here in the Symbols' rounded style.
     * The clipboard is clipped around the paper, so the two read apart without a fill.
     */
    val Paste = builder()
        .addGroup(clipPathData = PathParser().parsePathString("M0,0H24V7.6H8.6V24H0Z").toNodes())
        .addPath(
            PathParser().parsePathString(
                "M7.5,4.5H5.5A1.8,1.8 0 0,0 3.7,6.3V19.7A1.8,1.8 0 0,0 5.5,21.5H13.5A1.8,1.8 0 0,0 15.3,19.7V6.3" +
                    "A1.8,1.8 0 0,0 13.5,4.5H11.5" +
                    "M8.3,2.7H10.7A0.8,0.8 0 0,1 11.5,3.5V5.5A0.8,0.8 0 0,1 10.7,6.3H8.3A0.8,0.8 0 0,1 7.5,5.5V3.5" +
                    "A0.8,0.8 0 0,1 8.3,2.7Z",
            ).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .clearGroup()
        .addPath(
            PathParser().parsePathString(
                "M11.6,9.4H16.4L20.3,13.3V20.7A0.9,0.9 0 0,1 19.4,21.6H11.6A0.9,0.9 0 0,1 10.7,20.7V10.3" +
                    "A0.9,0.9 0 0,1 11.6,9.4ZM13.4,16H17.6M13.4,18.8H17.6",
            ).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
    val Settings = symbol(
        "M433-80q-27 0-46.5-18T363-142l-9-66q-13-5-24.5-12T307-235l-62 26q-25 11-50 2t-39-32l-47-82q-14-23-8-49t27-43l5" +
            "3-40q-1-7-1-13.5v-27q0-6.5 1-13.5l-53-40q-21-17-27-43t8-49l47-82q14-23 39-32t50 2l62 26q11-8 23-15t24-12l9-66q" +
            "4-26 23.5-44t46.5-18h94q27 0 46.5 18t23.5 44l9 66q13 5 24.5 12t22.5 15l62-26q25-11 50-2t39 32l47 82q14 23 8 49" +
            "t-27 43l-53 40q1 7 1 13.5v27q0 6.5-2 13.5l53 40q21 17 27 43t-8 49l-48 82q-14 23-39 32t-50-2l-60-26q-11 8-23 15" +
            "t-24 12l-9 66q-4 26-23.5 44T527-80h-94Zm7-80h79l14-106q31-8 57.5-23.5T639-327l99 41 39-68-86-65q5-14 7-29.5t2-" +
            "31.5q0-16-2-31.5t-7-29.5l86-65-39-68-99 42q-22-23-48.5-38.5T533-694l-13-106h-79l-14 106q-31 8-57.5 23.5T321-63" +
            "3l-99-41-39 68 86 64q-5 15-7 30t-2 32q0 16 2 31t7 30l-86 65 39 68 99-42q22 23 48.5 38.5T427-266l13 106Zm42-180" +
            "q58 0 99-41t41-99q0-58-41-99t-99-41q-59 0-99.5 41T342-480q0 58 40.5 99t99.5 41Zm-2-140Z",
    )

    /** One-handed mode (mobile_hand). */
    val OneHanded = symbol(
        "M240-160v-640 640ZM637-40q-26 0-49-10.5T548-80L369-294q-10-12-9-27.5t12-26.5l19-20q18-18 44-22.5t49 7.5l116 58" +
            "v-355h80q66 0 113 47t47 113v320q0 66-47 113T680-40h-43ZM240-80q-33 0-56.5-23.5T160-160v-640q0-33 23.5-56.5T240" +
            "-880h360q33 0 56.5 23.5T680-800v120h-80v-120H240v640h241l67 80H240Zm180-600q17 0 28.5-11.5T460-720q0-17-11.5-2" +
            "8.5T420-760q-17 0-28.5 11.5T380-720q0 17 11.5 28.5T420-680Zm217 560h43q33 0 56.5-23t23.5-57v-320q0-33-23.5-56." +
            "5T680-600v340q0 23-19 34.5t-39 1.5l-155-77 139 167q6 7 14 10.5t17 3.5Z",
    )
    /** Move the one-handed keyboard to the other side (swap_horiz). */
    val SwapSides = symbol(
        "m233-320 75 75q11 11 11 27.5T308-189q-12 12-28.5 12T251-189L108-332q-6-6-8.5-13T97-360q0-8 2.5-15t8.5-13l144-1" +
            "44q12-12 28-11.5t28 12.5q11 12 11.5 28T308-475l-75 75h247q17 0 28.5 11.5T520-360q0 17-11.5 28.5T480-320H233Zm4" +
            "94-240H480q-17 0-28.5-11.5T440-600q0-17 11.5-28.5T480-640h247l-75-75q-11-11-11-27.5t11-28.5q12-12 28.5-12t28.5" +
            " 12l143 143q6 6 8.5 13t2.5 15q0 8-2.5 15t-8.5 13L708-428q-12 12-28 11.5T652-429q-11-12-11.5-28t11.5-28l75-75Z",
    )
    /** Back to the full-width keyboard (open_in_full). */
    val FullWidth = symbol(
        "M160-120q-17 0-28.5-11.5T120-160v-240q0-17 11.5-28.5T160-440q17 0 28.5 11.5T200-400v144l504-504H560q-17 0-28.5" +
            "-11.5T520-800q0-17 11.5-28.5T560-840h240q17 0 28.5 11.5T840-800v240q0 17-11.5 28.5T800-520q-17 0-28.5-11.5T760" +
            "-560v-144L256-200h144q17 0 28.5 11.5T440-160q0 17-11.5 28.5T400-120H160Z",
    )

    /** Material "push_pin". */
    val Pin = filled(
        "M16,9V4l1,0c0.55,0 1,-0.45 1,-1v0c0,-0.55 -0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3v0c0,0.55 0.45,1 1,1l1,0v5" +
            "c0,1.66 -1.34,3 -3,3h0v2h5.97v7l1,1l1,-1v-7H19v-2h0C17.34,12 16,10.66 16,9z",
    )
    val Close = filled("M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z")
    val Height = filled("M13,6.99h3L12,3 8,6.99h3v10.02H8L12,21l4,-3.99h-3z")

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

    /** A Material Symbols path: viewBox 0 -960 960 960, so the group shifts it down onto the grid. */
    private fun symbol(path: String) = ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
    )
        .addGroup(translationY = 960f)
        .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        .clearGroup()
        .build()

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

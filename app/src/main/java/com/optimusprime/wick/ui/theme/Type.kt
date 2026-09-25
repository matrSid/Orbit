package com.optimusprime.wick.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.optimusprime.wick.R

// Fraunces carries the personality: a warm editorial serif for the moments
// that matter (screen titles, the timer itself). Manrope handles everything
// that needs to be read quickly and often. Both are bundled as static
// instances cut from the variable sources, so no runtime font-loading is
// involved.
val FrauncesDisplayBig = FontFamily(Font(R.font.fraunces_display_big, FontWeight.Normal))
val FrauncesDisplay = FontFamily(Font(R.font.fraunces_display_semibold, FontWeight.Normal))
val FrauncesText = FontFamily(
    Font(R.font.fraunces_text_regular, FontWeight.Normal),
    Font(R.font.fraunces_text_medium, FontWeight.Medium)
)
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold)
)

/**
 * A small, deliberate type scale — used directly (WickType.headline, etc.)
 * rather than forced through Material3's fixed role names.
 */
object WickType {
    val display = TextStyle(
        fontFamily = FrauncesDisplayBig,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.5).sp
    )
    val headline = TextStyle(
        fontFamily = FrauncesDisplay,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
    val title = TextStyle(
        fontFamily = FrauncesText,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 24.sp
    )
    val statNumber = TextStyle(
        fontFamily = FrauncesDisplay,
        fontSize = 32.sp,
        lineHeight = 36.sp
    )
    val body = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    )
    val bodyStrong = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    )
    val label = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    val caption = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
    val button = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )
    val italicNote = TextStyle(
        fontFamily = FrauncesText,
        fontStyle = FontStyle.Italic,
        fontSize = 15.sp,
        lineHeight = 21.sp
    )
}

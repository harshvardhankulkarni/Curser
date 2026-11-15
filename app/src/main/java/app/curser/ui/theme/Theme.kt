package app.curser.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class CurserColors(
    val blue: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val background: Color,
    val cell: Color,
    val sheet: Color,
    val sheetCell: Color,
    val separator: Color,
    val label: Color,
    val secondary: Color,
    val fill: Color,
    val track: Color,
    val highlight: Color,
    val segmentThumb: Color,
    val grabber: Color,
    val switchOff: Color,
)

val LightColors = CurserColors(
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    orange = Color(0xFFFF9500),
    background = Color(0xFFF2F2F7),
    cell = Color(0xFFFFFFFF),
    sheet = Color(0xFFF2F2F7),
    sheetCell = Color(0xFFFFFFFF),
    separator = Color(0x4A3C3C43),
    label = Color(0xFF000000),
    secondary = Color(0x993C3C43),
    fill = Color(0x1F767680),
    track = Color(0xFFE5E5EA),
    highlight = Color(0xFFD1D1D6),
    segmentThumb = Color(0xFFFFFFFF),
    grabber = Color(0x4D3C3C43),
    switchOff = Color(0xFFE9E9EA),
)

val DarkColors = CurserColors(
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    orange = Color(0xFFFF9F0A),
    background = Color(0xFF000000),
    cell = Color(0xFF1C1C1E),
    sheet = Color(0xFF1C1C1E),
    sheetCell = Color(0xFF2C2C2E),
    separator = Color(0x99545458),
    label = Color(0xFFFFFFFF),
    secondary = Color(0x99EBEBF5),
    fill = Color(0x3D767680),
    track = Color(0xFF39393D),
    highlight = Color(0xFF3A3A3C),
    segmentThumb = Color(0xFF636366),
    grabber = Color(0x4DEBEBF5),
    switchOff = Color(0xFF39393D),
)

/** Sheets sit one elevation level above the page, so cells get the lighter fill. */
fun CurserColors.forSheet(): CurserColors = copy(background = sheet, cell = sheetCell)

val LocalColors = staticCompositionLocalOf { LightColors }

object CurserTheme {
    val colors: CurserColors
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current
}

@Composable
fun CurserTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColors provides (if (darkTheme) DarkColors else LightColors),
        content = content,
    )
}

object CurserType {
    val LargeTitle = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
    val Title = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
    val Headline = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
    val Body = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.4).sp)
    val Footnote = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.1).sp)
    val Caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
}

object Motion {
    fun <T> soft(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
    // 0.9 keeps the sheet from overshooting past its resting position
    fun <T> sheet(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
}

val TrackpadBackground = Color(0xFF050507)
val GlowBlue = Color(0xFF3D9BFF)

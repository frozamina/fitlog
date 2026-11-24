package com.fitlog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 版式层级（Type Scale）
 * 规范要求：层级不超过 3 个主要级别，字重搭配有对比
 */
object Typography2 {
    val pageTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).sp)
    val sectionTitle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    val cardTitle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal)
    val numberBig = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).sp)
    val numberMid = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

/** 动态配色（浅色/深色）—— 页面通过它取色，支持深色模式 */
data class FitLogColors(
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val line: Color,
    val outline: Color,
    val outlineStrong: Color,
    val textMain: Color,
    val textSub: Color,
    val textFaint: Color,
    val isDark: Boolean
)

private val LightExtras = FitLogColors(
    bg = BgGray,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    line = LineGray,
    outline = Outline,
    outlineStrong = OutlineStrong,
    textMain = TextMain,
    textSub = TextSub,
    textFaint = TextFaint,
    isDark = false
)

private val DarkExtras = FitLogColors(
    bg = DarkBg,
    surface = DarkSurface1,
    surfaceVariant = DarkSurface2,
    line = DarkLine,
    outline = DarkSurface3,
    outlineStrong = DarkSurface3,
    textMain = DarkTextMain,
    textSub = DarkTextSub,
    textFaint = DarkTextSub,
    isDark = true
)

val LocalFitLogColors = staticCompositionLocalOf { LightExtras }

object FitTheme {
    val colors: FitLogColors
        @Composable get() = LocalFitLogColors.current
}

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Primary,
    background = BgGray,
    onBackground = TextMain,
    surface = Surface,
    onSurface = TextMain,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSub,
    outline = Outline,
    error = SemanticError
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FA3F0),
    onPrimary = Color(0xFF0E1526),
    primaryContainer = Color(0xFF24365E),
    onPrimaryContainer = Color(0xFFD3E0FF),
    secondary = Color(0xFF7FA3F0),
    background = DarkBg,
    onBackground = DarkTextMain,
    surface = DarkSurface1,
    onSurface = DarkTextMain,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkTextSub,
    outline = DarkSurface3,
    error = SemanticError
)

@Composable
fun FitLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalFitLogColors provides if (darkTheme) DarkExtras else LightExtras
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content
        )
    }
}

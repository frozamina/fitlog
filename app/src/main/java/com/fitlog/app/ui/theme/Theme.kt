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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.fitlog.app.R
import androidx.compose.ui.unit.sp

/**
 * 版式层级（Type Scale）
 * 规范要求：层级不超过 3 个主要级别，字重搭配有对比
 */
object StudioFonts {
    val display=FontFamily(Font(R.font.source_han_serif))
    val body=FontFamily(Font(R.font.source_han_sans))
    val latin=FontFamily(Font(R.font.newsreader))
    val numbers=FontFamily(Font(R.font.work_sans))
}
object Typography2 {
    val pageTitle=TextStyle(fontFamily=StudioFonts.display,fontSize=30.sp,lineHeight=39.sp,color=TextMain)
    val sectionTitle=TextStyle(fontFamily=StudioFonts.body,fontSize=18.sp,lineHeight=28.sp,fontWeight=FontWeight.Medium)
    val cardTitle=TextStyle(fontFamily=StudioFonts.body,fontSize=16.sp,lineHeight=26.sp,fontWeight=FontWeight.Medium)
    val body=TextStyle(fontFamily=StudioFonts.body,fontSize=16.sp,lineHeight=28.sp,letterSpacing=.3.sp)
    val caption=TextStyle(fontFamily=StudioFonts.body,fontSize=13.sp,lineHeight=22.sp)
    val numberBig=TextStyle(fontFamily=StudioFonts.numbers,fontSize=32.sp,lineHeight=42.sp)
    val numberMid=TextStyle(fontFamily=StudioFonts.numbers,fontSize=18.sp)
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
    onSecondary = Color.White,
    secondaryContainer = PrimaryLight,
    onSecondaryContainer = PrimaryDark,
    tertiary = Primary,
    onTertiary = Color.White,
    tertiaryContainer = PrimaryLight,
    onTertiaryContainer = PrimaryDark,
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
    primary = Color(0xFFA9CDB2),
    onPrimary = DarkBg,
    primaryContainer = DarkSurface3,
    onPrimaryContainer = DarkTextMain,
    secondary = Color(0xFFA9CDB2),
    onSecondary = DarkBg,
    secondaryContainer = DarkSurface3,
    onSecondaryContainer = DarkTextMain,
    tertiary = Color(0xFFA9CDB2),
    onTertiary = DarkBg,
    tertiaryContainer = DarkSurface3,
    onTertiaryContainer = DarkTextMain,
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
            typography=androidx.compose.material3.Typography(
                bodyLarge=Typography2.body,bodyMedium=Typography2.body,bodySmall=Typography2.caption,
                titleLarge=Typography2.sectionTitle,titleMedium=Typography2.cardTitle,titleSmall=Typography2.cardTitle,
                labelLarge=Typography2.cardTitle,labelMedium=Typography2.caption,labelSmall=Typography2.caption,
                headlineLarge=Typography2.pageTitle,headlineMedium=Typography2.pageTitle,headlineSmall=Typography2.sectionTitle),
            content = content
        )
    }
}

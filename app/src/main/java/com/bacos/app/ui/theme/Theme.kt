package com.bacos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class BacosColors(
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentDim: Color,
    val ai: Color,
    val aiDim: Color,
    val warning: Color,
    val warningDim: Color,
    val danger: Color,
    val dangerDim: Color,
    val info: Color,
)

val LocalBacosColors = staticCompositionLocalOf { darkPalette }

val darkPalette = BacosColors(
    bg = Bg,
    surface1 = Surface1,
    surface2 = Surface2,
    surface3 = Surface3,
    border = BorderDefault,
    borderSubtle = BorderSubtle,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary,
    accent = Mint,
    accentDeep = MintDeep,
    accentDim = MintDim,
    ai = Violet,
    aiDim = VioletDim,
    warning = Amber,
    warningDim = AmberDim,
    danger = Danger,
    dangerDim = DangerDim,
    info = Info,
)

val lightPalette = BacosColors(
    bg = BgLight,
    surface1 = Surface1Light,
    surface2 = Surface2Light,
    surface3 = Surface3Light,
    border = BorderLight,
    borderSubtle = BorderLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textTertiary = TextTertiaryLight,
    accent = MintDeepLight,
    accentDeep = MintDeepLight,
    accentDim = Color(0x140E9C6F),
    ai = Color(0xFF6D5BD0),
    aiDim = Color(0x146D5BD0),
    warning = Color(0xFFB97F14),
    warningDim = Color(0x14B97F14),
    danger = Color(0xFFD14352),
    dangerDim = Color(0x14D14352),
    info = Color(0xFF2B6FD6),
)

@Composable
fun BacosTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (darkTheme) darkColorScheme(
        primary = Mint,
        onPrimary = Bg,
        background = Bg,
        onBackground = TextPrimary,
        surface = Surface1,
        onSurface = TextPrimary,
        surfaceVariant = Surface2,
        onSurfaceVariant = TextSecondary,
        outline = BorderDefault,
        error = Danger,
    ) else lightColorScheme(
        primary = MintDeepLight,
        onPrimary = Color.White,
        background = BgLight,
        onBackground = TextPrimaryLight,
        surface = Surface1Light,
        onSurface = TextPrimaryLight,
        surfaceVariant = Surface2Light,
        onSurfaceVariant = TextSecondaryLight,
        outline = BorderLight,
        error = Color(0xFFD14352),
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalBacosColors provides (if (darkTheme) darkPalette else lightPalette)
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = BacosTypography,
            content = content,
        )
    }
}

object Bacos {
    val c: BacosColors
        @Composable get() = LocalBacosColors.current
}

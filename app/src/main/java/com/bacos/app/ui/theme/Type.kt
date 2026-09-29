package com.bacos.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bacos.app.R

val PlexArabic = FontFamily(
    Font(R.font.plex_arabic_regular, FontWeight.Normal),
    Font(R.font.plex_arabic_medium, FontWeight.Medium),
    Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.plex_arabic_bold, FontWeight.Bold),
)

val PlexMono = FontFamily(
    Font(R.font.plex_mono_medium, FontWeight.Medium),
)

// ─── Typography system (Arabic-first, tuned tracking) ───

val Display = TextStyle(
    fontFamily = PlexArabic,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 44.sp,
    letterSpacing = 0.sp,
)

val H1 = TextStyle(
    fontFamily = PlexArabic,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 34.sp,
    letterSpacing = 0.sp,
)

val H2 = TextStyle(
    fontFamily = PlexArabic,
    fontWeight = FontWeight.SemiBold,
    fontSize = 21.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp,
)

val H3 = TextStyle(
    fontFamily = PlexArabic,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp,
)

val Body = TextStyle(
    fontFamily = PlexArabic,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp,
)

val BodyMedium = TextStyle(
    fontFamily = PlexArabic,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp,
)

val BodySmall = TextStyle(
    fontFamily = PlexArabic,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp,
)

val Caption = TextStyle(
    fontFamily = PlexArabic,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp,
)

val Numbers = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 20.sp,
)

val NumbersLarge = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Medium,
    fontSize = 42.sp,
    lineHeight = 48.sp,
)

val ButtonText = TextStyle(
    fontFamily = PlexArabic,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp,
)

val BacosTypography = Typography(
    displayLarge = Display,
    headlineLarge = H1,
    headlineMedium = H2,
    headlineSmall = H3,
    bodyLarge = Body,
    bodyMedium = BodyMedium,
    bodySmall = BodySmall,
    labelSmall = Caption,
    labelLarge = ButtonText,
)

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    themeType: String = "Sistem",
    accentType: String = "Biru", // Blue is the default accent now
    fontScaleMultiplier: Float = 1f,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeType) {
        "Terang" -> false
        "Gelap" -> true
        else -> isSystemInDarkTheme()
    }

    val primaryColor = when (accentType) {
        "Teal" -> if (darkTheme) TealDarkPrimary else TealPrimary
        "Hijau" -> if (darkTheme) GreenDarkPrimary else GreenPrimary
        "Emas" -> if (darkTheme) AmberDarkPrimary else AmberPrimary
        "Ungu" -> if (darkTheme) PolishDarkPrimary else PolishPrimary
        else -> if (darkTheme) BlueDarkPrimary else BluePrimary // Default: Biru (Blue)
    }

    val secondaryColor = when (accentType) {
        "Teal" -> if (darkTheme) TealDarkSecondary else TealSecondary
        "Hijau" -> if (darkTheme) GreenDarkSecondary else GreenSecondary
        "Emas" -> if (darkTheme) AmberDarkSecondary else AmberSecondary
        "Ungu" -> if (darkTheme) PolishDarkSecondary else PolishSecondary
        else -> if (darkTheme) BlueDarkSecondary else BlueSecondary
    }

    val tertiaryColor = when (accentType) {
        "Teal" -> if (darkTheme) TealDarkTertiary else TealTertiary
        "Hijau" -> if (darkTheme) GreenDarkTertiary else GreenTertiary
        "Emas" -> if (darkTheme) AmberDarkTertiary else AmberTertiary
        "Ungu" -> if (darkTheme) PolishDarkTertiary else PolishTertiary
        else -> if (darkTheme) BlueDarkTertiary else BlueTertiary
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = tertiaryColor,
            background = PolishDarkBackground,
            surface = PolishDarkSurface,
            onPrimary = PolishDarkBackground,
            onSecondary = PolishDarkBackground,
            onTertiary = PolishDarkBackground,
            onBackground = PolishLightBackground,
            onSurface = PolishLightBackground
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = tertiaryColor,
            background = PolishLightBackground,
            surface = PolishLightSurface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFF0F172A), // Slate 900
            onSurface = Color(0xFF0F172A),     // Slate 900
            surfaceVariant = Color(0xFFEFF3F8), // Slate 100 style
            onSurfaceVariant = Color(0xFF64748B) // Slate 500 style
        )
    }
    
    val scaledTypography = if (fontScaleMultiplier != 1f) {
        androidx.compose.material3.Typography(
            displayLarge = Typography.displayLarge.copy(fontSize = Typography.displayLarge.fontSize * fontScaleMultiplier),
            displayMedium = Typography.displayMedium.copy(fontSize = Typography.displayMedium.fontSize * fontScaleMultiplier),
            displaySmall = Typography.displaySmall.copy(fontSize = Typography.displaySmall.fontSize * fontScaleMultiplier),
            headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize * fontScaleMultiplier),
            headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize * fontScaleMultiplier),
            headlineSmall = Typography.headlineSmall.copy(fontSize = Typography.headlineSmall.fontSize * fontScaleMultiplier),
            titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize * fontScaleMultiplier),
            titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize * fontScaleMultiplier),
            titleSmall = Typography.titleSmall.copy(fontSize = Typography.titleSmall.fontSize * fontScaleMultiplier),
            bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize * fontScaleMultiplier),
            bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize * fontScaleMultiplier),
            bodySmall = Typography.bodySmall.copy(fontSize = Typography.bodySmall.fontSize * fontScaleMultiplier),
            labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize * fontScaleMultiplier),
            labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize * fontScaleMultiplier),
            labelSmall = Typography.labelSmall.copy(fontSize = Typography.labelSmall.fontSize * fontScaleMultiplier)
        )
    } else {
        Typography
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content
    )
}

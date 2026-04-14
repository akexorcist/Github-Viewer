package dev.akexorcist.githubviewer.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val primaryLight = Color(0xFF0969DA)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFDFEEFF)
private val onPrimaryContainerLight = Color(0xFF00315C)
private val secondaryLight = Color(0xFF545F71)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFD8E3F8)
private val onSecondaryContainerLight = Color(0xFF111C2B)
private val surfaceLight = Color(0xFFF6F8FA)
private val onSurfaceLight = Color(0xFF1A1C1E)
private val surfaceVariantLight = Color(0xFFDFE3EB)
private val outlineLight = Color(0xFF6F7889)

private val primaryDark = Color(0xFF58A6FF)
private val onPrimaryDark = Color(0xFF003061)
private val primaryContainerDark = Color(0xFF004789)
private val onPrimaryContainerDark = Color(0xFFD6E4FF)
private val secondaryDark = Color(0xFFBCC7DC)
private val onSecondaryDark = Color(0xFF263141)
private val secondaryContainerDark = Color(0xFF3C4758)
private val onSecondaryContainerDark = Color(0xFFD8E3F8)
private val surfaceDark = Color(0xFF0D1117)
private val onSurfaceDark = Color(0xFFE2E2E6)
private val surfaceVariantDark = Color(0xFF21262D)
private val outlineDark = Color(0xFF8A9199)

val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    outline = outlineLight,
)

val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    outline = outlineDark,
)

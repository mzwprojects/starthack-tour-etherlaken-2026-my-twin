package com.mzwprojects.mytwin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    // ── Primary
    primary = TealBase,
    onPrimary = OnErrorLight,         // White
    primaryContainer = TealPale,
    onPrimaryContainer = TealDeep,
    inversePrimary = TealInverse,
    // ── Secondary
    secondary = SlateBase,
    onSecondary = SlateOnBase,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = SlateContDark,
    // ── Tertiary
    tertiary = VitalBase,
    onTertiary = VitalOnBase,
    tertiaryContainer = VitalContainer,
    onTertiaryContainer = VitalContDeep,
    // ── Background / Surface
    background = BgLight,
    onBackground = OnBgLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVarLight,
    surfaceTint = SurfaceTintLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    // ── Surface Containers
    surfaceBright = SurfBrightLight,
    surfaceDim = SurfDimLight,
    surfaceContainer = SurfContLight,
    surfaceContainerHigh = SurfContHighLight,
    surfaceContainerHighest = SurfContHighestL,
    surfaceContainerLow = SurfContLowLight,
    surfaceContainerLowest = SurfContLowestL,
    // ── Outline
    outline = OutlineLight,
    outlineVariant = OutlineVarLight,
    // ── Error
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContLight,
    onErrorContainer = OnErrorContLight,
    // ── Scrim
    scrim = Scrim,
    // ── Fixed
    primaryFixed = TealFixed,
    primaryFixedDim = TealFixedDim,
    onPrimaryFixed = TealFixedOn,
    onPrimaryFixedVariant = TealFixedOnVar,
    secondaryFixed = SlateFixed,
    secondaryFixedDim = SlateFixedDim,
    onSecondaryFixed = SlateFixedOn,
    onSecondaryFixedVariant = SlateFixedOnVar,
    tertiaryFixed = VitalFixed,
    tertiaryFixedDim = VitalFixedDim,
    onTertiaryFixed = VitalFixedOn,
    onTertiaryFixedVariant = VitalFixedOnVar,
)

private val DarkColorScheme = darkColorScheme(
    // ── Primary
    primary = TealBright,
    onPrimary = TealOnBright,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealPale,
    inversePrimary = TealBase,
    // ── Secondary
    secondary = SlateBright,
    onSecondary = SlateOnBright,
    secondaryContainer = SlateContDark,
    onSecondaryContainer = SlateContainer,
    // ── Tertiary
    tertiary = VitalBright,
    onTertiary = VitalOnBright,
    tertiaryContainer = VitalContDark,
    onTertiaryContainer = VitalContainer,
    // ── Background / Surface
    background = BgDark,
    onBackground = OnBgDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVarDark,
    surfaceTint = SurfaceTintDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    // ── Surface Containers
    surfaceBright = SurfBrightDark,
    surfaceDim = SurfDimDark,
    surfaceContainer = SurfContDark,
    surfaceContainerHigh = SurfContHighDark,
    surfaceContainerHighest = SurfContHighestDark,
    surfaceContainerLow = SurfContLowDark,
    surfaceContainerLowest = SurfContLowestDark,
    // ── Outline
    outline = OutlineDark,
    outlineVariant = OutlineVarDark,
    // ── Error
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContDark,
    onErrorContainer = OnErrorContDark,
    // ── Scrim
    scrim = Scrim,
    // ── Fixed (identisch mit Light – per M3 Spec)
    primaryFixed = TealFixed,
    primaryFixedDim = TealFixedDim,
    onPrimaryFixed = TealFixedOn,
    onPrimaryFixedVariant = TealFixedOnVar,
    secondaryFixed = SlateFixed,
    secondaryFixedDim = SlateFixedDim,
    onSecondaryFixed = SlateFixedOn,
    onSecondaryFixedVariant = SlateFixedOnVar,
    tertiaryFixed = VitalFixed,
    tertiaryFixedDim = VitalFixedDim,
    onTertiaryFixed = VitalFixedOn,
    onTertiaryFixedVariant = VitalFixedOnVar,
)

@Composable
fun MyTwinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
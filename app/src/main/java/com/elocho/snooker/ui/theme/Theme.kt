package com.elocho.snooker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ElOchoColorScheme = darkColorScheme(
    primary = Burgundy,
    onPrimary = PureWhite,
    primaryContainer = BurgundyDark,
    onPrimaryContainer = OffWhite,
    secondary = Gold,
    onSecondary = DarkSurface,
    secondaryContainer = GoldDark,
    onSecondaryContainer = OffWhite,
    tertiary = BurgundyLight,
    onTertiary = PureWhite,
    background = DarkSurface,
    onBackground = OffWhite,
    surface = DarkSurfaceVariant,
    onSurface = OffWhite,
    surfaceVariant = MediumGray,
    onSurfaceVariant = LightGray,
    error = ErrorRed,
    onError = PureWhite,
    outline = LightGray,
    outlineVariant = MediumGray
)

@Composable
fun ElOchoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ElOchoColorScheme,
        typography = ElOchoTypography,
        shapes = ElOchoShapes,
        content = content
    )
}

package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LeovaneyColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = ObsidianBlack,
    primaryContainer = ElevatedCharcoal,
    onPrimaryContainer = PureWhite,
    secondary = MutedSilver,
    onSecondary = ObsidianBlack,
    secondaryContainer = SurfaceCharcoal,
    onSecondaryContainer = SoftWhite,
    tertiary = IncomeGreen,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = PureWhite,
    surface = DeepCharcoal,
    onSurface = PureWhite,
    surfaceVariant = SurfaceCharcoal,
    onSurfaceVariant = MutedSilver,
    outline = GlassBorderWhite28,
    outlineVariant = GlassBorderWhite15,
    error = ExpenseRed,
    onError = PureWhite,
    errorContainer = ExpenseRedSoft,
    onErrorContainer = ExpenseRed
)

val LeovaneyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun LeovaneyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LeovaneyColorScheme,
        typography = Typography,
        shapes = LeovaneyShapes,
        content = content
    )
}

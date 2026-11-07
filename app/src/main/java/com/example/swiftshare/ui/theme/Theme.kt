package com.example.swiftshare.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors as MaterialColors
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.swiftshare.ui.theme.Colors.Background
import com.example.swiftshare.ui.theme.Colors.Divider as DividerColor
import com.example.swiftshare.ui.theme.Colors.Error
import com.example.swiftshare.ui.theme.Colors.OnPrimary
import com.example.swiftshare.ui.theme.Colors.OnSecondary
import com.example.swiftshare.ui.theme.Colors.OnSurface
import com.example.swiftshare.ui.theme.Colors.Primary
import com.example.swiftshare.ui.theme.Colors.PrimaryContainer
import com.example.swiftshare.ui.theme.Colors.Secondary
import com.example.swiftshare.ui.theme.Colors.SurfaceColor
import com.example.swiftshare.ui.theme.Colors.SurfaceElevated

/**
 * Original SwiftShare theme built on Compose Material.
 *
 * Arabic is the default resource language, so the layout is forced to RTL unless the
 * current locale is English. This keeps Arabic readable even on devices whose system
 * language is not Arabic (the default strings are Arabic).
 */
@Composable
fun SwiftShareTheme(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val language = if (configuration.locales.isEmpty) "" else configuration.locales[0].language
    val layoutDirection = if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colors = MaterialColors(
                primary = Primary,
                primaryVariant = PrimaryContainer,
                secondary = Secondary,
                secondaryVariant = Secondary,
                background = Background,
                surface = SurfaceColor,
                error = Error,
                onPrimary = OnPrimary,
                onSecondary = OnSecondary,
                onBackground = OnSurface,
                onSurface = OnSurface,
                onError = OnPrimary,
                isLight = false
            ),
            typography = SwiftTypography
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                content()
            }
        }
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        elevation = elevation
    ) {
        content()
    }
}

@Composable
fun DividerLine(
    thickness: Dp = 1.dp,
    color: Color = DividerColor
) {
    Divider(modifier = Modifier.fillMaxWidth(), thickness = thickness, color = color)
}

package com.websitecheck.app.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.websitecheck.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF7DF8E4),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF05201C),
    tertiary = Color(0xFF456179),
    background = Color(0xFFF5FBF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF5FBF8),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFF6F7977),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EDBC9),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF7DF8E4),
    secondary = Color(0xFFB1CCC6),
    onSecondary = Color(0xFF1C3532),
    secondaryContainer = Color(0xFF334B48),
    onSecondaryContainer = Color(0xFFCCE8E2),
    tertiary = Color(0xFFADCAE5),
    background = Color(0xFF0E1513),
    onBackground = Color(0xFFDDE4E1),
    surface = Color(0xFF0E1513),
    onSurface = Color(0xFFDDE4E1),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    outline = Color(0xFF899390),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

val SuccessColor = Color(0xFF2E9E6B)
val WarningColor = Color(0xFFD68A1E)

private val LightNavigationScrim = AndroidColor.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationScrim = AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b)

@Composable
fun WebsiteCheckTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val view = LocalView.current
    val activity = view.context.findComponentActivity()

    LaunchedEffect(darkTheme, activity) {
        if (activity != null && !view.isInEditMode) {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    AndroidColor.TRANSPARENT,
                    AndroidColor.TRANSPARENT,
                    detectDarkMode = { darkTheme },
                ),
                navigationBarStyle = SystemBarStyle.auto(
                    LightNavigationScrim,
                    DarkNavigationScrim,
                    detectDarkMode = { darkTheme },
                ),
            )
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

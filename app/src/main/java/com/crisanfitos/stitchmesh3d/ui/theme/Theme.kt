package com.crisanfitos.stitchmesh3d.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Esquema de color Material 3 de StitchMesh 3D.
 * Enfocado en modo oscuro nativo para aplicaciones CAD técnicas.
 */
private val StitchMeshDarkColorScheme = darkColorScheme(
    primary = StitchMeshTerracotta,
    onPrimary = StitchMeshOnAccent,
    primaryContainer = StitchMeshTerracottaDark,
    onPrimaryContainer = StitchMeshTextPrimary,

    secondary = StitchMeshYarnGold,
    onSecondary = StitchMeshNeutralDark,
    secondaryContainer = StitchMeshSurfaceHigh,
    onSecondaryContainer = StitchMeshTextPrimary,

    tertiary = StitchMeshSageGreen,
    onTertiary = StitchMeshOnAccent,

    background = StitchMeshNeutralDark,
    onBackground = StitchMeshTextPrimary,

    surface = StitchMeshSurfaceContainer,
    onSurface = StitchMeshTextPrimary,
    surfaceVariant = StitchMeshSurfaceHigh,
    onSurfaceVariant = StitchMeshTextSecondary,

    error = StitchMeshCoralRed,
    onError = StitchMeshOnAccent,

    outline = StitchMeshSurfaceBorder
)

@Composable
fun StitchMesh3DTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = StitchMeshDarkColorScheme,
        typography = Typography,
        content = content
    )
}
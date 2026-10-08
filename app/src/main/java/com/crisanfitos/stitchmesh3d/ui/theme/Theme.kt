package com.crisanfitos.stitchmesh3d.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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
    MaterialTheme(
        colorScheme = StitchMeshDarkColorScheme,
        typography = Typography,
        content = content
    )
}
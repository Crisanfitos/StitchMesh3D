package com.crisanfitos.stitchmesh3d.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta cromática oficial de StitchMesh 3D según Design Brief §1 y TRD §1.
 * Diseñada para un entorno CAD oscuro de alto contraste y precisión técnica.
 */

// Fondos y Superficies CAD
val StitchMeshNeutralDark = Color(0xFF121316)          // Fondo principal de la interfaz
val StitchMeshSurfaceContainer = Color(0xFF1E1F24)     // Superficie de paneles y contenedores
val StitchMeshSurfaceHigh = Color(0xFF282A30)          // Tarjetas elevadas y campos de entrada
val StitchMeshSurfaceBorder = Color(0xFF383B44)        // Separadores y bordes estructurales

// Acentos Principales y Estados
val StitchMeshTerracotta = Color(0xFFE06D53)           // Terracotta / Clay: Acento primario de costura
val StitchMeshTerracottaDark = Color(0xFFB54C34)       // Variante oscura para presionado/focus
val StitchMeshSageGreen = Color(0xFF52A474)            // Sage Green: Estado de éxito / validez formal
val StitchMeshCoralRed = Color(0xFFE54D42)             // Coral Red: Estado de error / incompatibilidad
val StitchMeshYarnGold = Color(0xFFF2C94C)             // Dorado hilado para marcadores y selecciones

// Textos y Contraste
val StitchMeshTextPrimary = Color(0xFFEDEDED)          // Texto principal de alta legibilidad
val StitchMeshTextSecondary = Color(0xFF9E9EA7)        // Texto secundario / leyendas técnicas
val StitchMeshTextDisabled = Color(0xFF5C5E66)         // Texto deshabilitado / placeholders
val StitchMeshOnAccent = Color(0xFFFFFFFF)             // Texto sobre acentos terracotta/sage/coral
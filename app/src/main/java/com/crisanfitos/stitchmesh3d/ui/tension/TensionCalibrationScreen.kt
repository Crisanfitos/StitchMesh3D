package com.crisanfitos.stitchmesh3d.ui.tension

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Pantalla de calibración de tensión de muestra 10x10 cm (RF-2.3, Regla 13).
 * Delega en la implementación completa y adaptativa [TensionCalibratorScreen].
 */
@Composable
fun TensionCalibrationScreen(
    onBackClick: () -> Unit,
    viewModel: TensionViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    TensionCalibratorScreen(
        onBackClick = onBackClick,
        viewModel = viewModel,
        modifier = modifier
    )
}

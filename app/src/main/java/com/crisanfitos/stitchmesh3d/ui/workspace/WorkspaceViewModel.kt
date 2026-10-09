package com.crisanfitos.stitchmesh3d.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser
import com.crisanfitos.stitchmesh3d.core.engine.validator.ArithmeticValidator
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.core.geometry.AdaptiveTessellator
import com.crisanfitos.stitchmesh3d.core.geometry.RingProfile
import com.crisanfitos.stitchmesh3d.core.geometry.RingProfileGenerator
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.LinterValidationStatus
import com.crisanfitos.stitchmesh3d.ui.workspace.components.RoundItemUiModel
import com.crisanfitos.stitchmesh3d.ui.workspace.keyboard.CrochetTokenFormatter
import com.crisanfitos.stitchmesh3d.di.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel del espacio de trabajo CAD de StitchMesh 3D.
 * Implementa el patrón MVI / UDF conforme a app_flow_navigation.md (§2) y SM-029.
 *
 * Responsabilidades:
 * - Ciclo reactivo: Carga -> Parsing -> Validación Aritmética -> Teselación 3D en Dispatchers.Default (RND-2).
 * - Congelamiento de geometría 3D ante erratas sintácticas/aritméticas, preservando la última malla válida.
 * - Regeneración inmediata y continua de la malla 3D cuando el patrón vuelve a ser válido (RF-3.5).
 * - Selección de fila activa y formateo de tokens para el teclado contextual de crochet (SM-030).
 */
@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    constructor() : this(Dispatchers.Default)

    private val _state = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<WorkspaceEffect>()
    val effects: SharedFlow<WorkspaceEffect> = _effects.asSharedFlow()

    init {
        // Inicializar la geometría con las vueltas predeterminadas
        recalculateAndRebuildMesh(_state.value.rounds)
    }

    fun processIntent(intent: WorkspaceIntent) {
        when (intent) {
            is WorkspaceIntent.LoadProject -> {
                _state.update {
                    it.copy(
                        projectId = intent.projectId,
                        projectTitle = "Proyecto ${intent.projectId.take(8)}"
                    )
                }
            }

            is WorkspaceIntent.EditRoundInstruction -> {
                val updatedRounds = _state.value.rounds.map { round ->
                    if (round.id == intent.roundId) {
                        round.copy(rawInstruction = intent.newInstruction)
                    } else {
                        round
                    }
                }
                recalculateAndRebuildMesh(updatedRounds)
            }

            is WorkspaceIntent.ChangeRoundColor -> {
                val updatedRounds = _state.value.rounds.map { round ->
                    if (round.id == intent.roundId) {
                        round.copy(colorHex = intent.colorHex)
                    } else {
                        round
                    }
                }
                _state.update { it.copy(rounds = updatedRounds) }
            }

            is WorkspaceIntent.SelectRound -> {
                _state.update { it.copy(selectedRoundId = intent.roundId) }
            }

            is WorkspaceIntent.AddRound -> {
                val currentRounds = _state.value.rounds
                val nextNumber = (currentRounds.maxOfOrNull { it.roundNumber } ?: 0) + 1
                val lastRoundProduced = currentRounds.lastOrNull()?.producedStitches ?: 6
                val newRoundId = "r$nextNumber"
                val newRound = RoundItemUiModel(
                    id = newRoundId,
                    roundNumber = nextNumber,
                    rawInstruction = "$lastRoundProduced pb",
                    producedStitches = lastRoundProduced,
                    consumedStitches = lastRoundProduced,
                    declaredStitches = null,
                    isValid = true,
                    colorHex = currentRounds.lastOrNull()?.colorHex ?: "#E06D53"
                )
                val newRounds = currentRounds + newRound
                _state.update {
                    it.copy(
                        selectedRoundId = newRoundId,
                        currentPeelRound = nextNumber
                    )
                }
                recalculateAndRebuildMesh(newRounds)
                viewModelScope.launch {
                    _effects.emit(WorkspaceEffect.ScrollToRound(newRoundId))
                }
            }

            is WorkspaceIntent.DeleteRound -> {
                val currentRounds = _state.value.rounds
                if (currentRounds.size > 1) {
                    val filtered = currentRounds.filter { it.id != intent.roundId }
                    val newActiveId = if (_state.value.selectedRoundId == intent.roundId) {
                        filtered.lastOrNull()?.id
                    } else {
                        _state.value.selectedRoundId
                    }
                    _state.update { it.copy(selectedRoundId = newActiveId) }
                    recalculateAndRebuildMesh(filtered)
                }
            }

            is WorkspaceIntent.ApplySuggestion -> {
                val updatedRounds = _state.value.rounds.map { round ->
                    if (round.id == intent.roundId) {
                        round.copy(rawInstruction = intent.suggestion.correctedText)
                    } else {
                        round
                    }
                }
                recalculateAndRebuildMesh(updatedRounds)
            }

            is WorkspaceIntent.SelectPart -> {
                _state.update { it.copy(selectedPartId = intent.partId) }
            }

            is WorkspaceIntent.AddPart -> {
                val currentParts = _state.value.parts
                val nextNum = currentParts.size + 1
                val newPart = ProjectPartUiModel("p$nextNum", "Parte $nextNum", 0, "#E06D53")
                _state.update {
                    it.copy(
                        parts = currentParts + newPart,
                        selectedPartId = newPart.id
                    )
                }
            }

            is WorkspaceIntent.SelectPeelRound -> {
                _state.update { it.copy(currentPeelRound = intent.roundNumber) }
                // Reconstruir malla con el límite del peel slider
                recalculateAndRebuildMesh(_state.value.rounds, peelLimit = intent.roundNumber)
            }

            is WorkspaceIntent.ToggleWireframe -> {
                _state.update { it.copy(isWireframe = intent.isWireframe) }
            }

            is WorkspaceIntent.ToggleFullscreen -> {
                _state.update { it.copy(isFullscreenViewport = intent.isFullscreen) }
            }

            is WorkspaceIntent.ToggleKeyboard -> {
                _state.update { it.copy(showQuickKeyboard = intent.show) }
            }

            is WorkspaceIntent.InsertTokenAtActiveRound -> {
                val activeId = _state.value.selectedRoundId
                val target = _state.value.rounds.firstOrNull { it.id == activeId }
                    ?: _state.value.rounds.lastOrNull()
                if (target != null) {
                    val updatedText = CrochetTokenFormatter.insertToken(target.rawInstruction, intent.token)
                    processIntent(WorkspaceIntent.EditRoundInstruction(target.id, updatedText))
                }
            }

            is WorkspaceIntent.DeleteCharFromActiveRound -> {
                val activeId = _state.value.selectedRoundId
                val target = _state.value.rounds.firstOrNull { it.id == activeId }
                    ?: _state.value.rounds.lastOrNull()
                if (target != null) {
                    val updatedText = CrochetTokenFormatter.deleteLastChar(target.rawInstruction)
                    processIntent(WorkspaceIntent.EditRoundInstruction(target.id, updatedText))
                }
            }
        }
    }

    private fun recalculateAndRebuildMesh(
        updatedRounds: List<RoundItemUiModel>,
        peelLimit: Int? = null
    ) {
        viewModelScope.launch {
            val (validatedRounds, hasErrors, errorCount) = withContext(defaultDispatcher) {
                var prevStitches = 0
                var errorCounter = 0
                val validated = updatedRounds.mapIndexed { index, round ->
                    val result = ArithmeticValidator.validateRound(
                        rawLine = round.rawInstruction,
                        previousRoundStitches = if (index == 0) 0 else prevStitches
                    )
                    if (!result.isValid) {
                        errorCounter++
                    }
                    val newRound = round.copy(
                        producedStitches = result.totalProduced,
                        consumedStitches = result.totalConsumed,
                        declaredStitches = result.declaredCount,
                        isValid = result.isValid,
                        errorMessage = if (result.isValid) null else result.message
                    )
                    if (result.isValid) {
                        prevStitches = result.totalProduced
                    }
                    newRound
                }
                Triple(validated, errorCounter > 0, errorCounter)
            }

            val status = if (hasErrors) LinterValidationStatus.HAS_ERRORS else LinterValidationStatus.VALID

            // Generar o congelar geometría 3D
            val hookMm = _state.value.hookSizeMm
            val yarnName = _state.value.yarnWeightName
            val effectiveLimit = peelLimit ?: _state.value.currentPeelRound

            val validRounds = validatedRounds.filter { it.isValid }
            val effectiveRounds = if (effectiveLimit in 1..validRounds.size) {
                validRounds.take(effectiveLimit)
            } else {
                validRounds
            }

            var newMesh = _state.value.meshGeometry
            var lastValid = _state.value.lastValidMesh

            if (!hasErrors && effectiveRounds.isNotEmpty()) {
                val meshResult = withContext(defaultDispatcher) {
                    try {
                        val gauge = YarnGaugeStandard(
                            id = 1,
                            yarnWeightCategory = YarnWeightCategory.MEDIUM,
                            categoryName = yarnName,
                            hookSizeMm = hookMm,
                            stitchWidthMm = hookMm * 1.1f,
                            stitchHeightMm = hookMm * 1.25f,
                            stitchThicknessMm = hookMm * 0.55f
                        )
                        val ringProfiles = ArrayList<RingProfile>()
                        var prevRing: RingProfile? = null
                        for ((idx, r) in effectiveRounds.withIndex()) {
                            val parsed = CrochetParser.parse(r.rawInstruction)
                            // Expansión polar de Anillo Mágico a N puntos individuales para generar perfil circunferencial completo
                            val stitches = parsed.ast?.flatten()?.flatMap { node ->
                                if (node.stitchType is StitchType.MagicRing) {
                                    val count = (node.stitchType as StitchType.MagicRing).stitchCount
                                    List(count) { StitchType.SingleCrochet }
                                } else {
                                    listOf(node.stitchType)
                                }
                            } ?: emptyList()

                            if (stitches.isNotEmpty()) {
                                val ring = RingProfileGenerator.generateRing(idx + 1, stitches, gauge, prevRing)
                                ringProfiles.add(ring)
                                prevRing = ring
                            }
                        }
                        if (ringProfiles.isNotEmpty()) {
                            AdaptiveTessellator.tessellate(ringProfiles, includePolarCap = true)
                        } else null
                    } catch (_: Throwable) {
                        null
                    }
                }

                if (meshResult != null) {
                    newMesh = meshResult
                    lastValid = meshResult
                }
            } else if (hasErrors) {
                // Congelamiento de malla previa sin crashear el renderizado 3D (SM-029)
                newMesh = lastValid
            }

            _state.update {
                it.copy(
                    rounds = validatedRounds,
                    validationStatus = status,
                    inconsistencyCount = errorCount,
                    isMeshFrozenDueToError = hasErrors && lastValid != null,
                    meshGeometry = newMesh,
                    lastValidMesh = lastValid
                )
            }
        }
    }
}

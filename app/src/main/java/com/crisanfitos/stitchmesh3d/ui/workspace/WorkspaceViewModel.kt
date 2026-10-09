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
import com.crisanfitos.stitchmesh3d.core.geometry.StuffingInflationFilter
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
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
                syncPartRoundsAndRebuild(updatedRounds)
            }

            is WorkspaceIntent.ChangeRoundColor -> {
                val updatedRounds = _state.value.rounds.map { round ->
                    if (round.id == intent.roundId) {
                        round.copy(colorHex = intent.colorHex)
                    } else {
                        round
                    }
                }
                syncPartRoundsAndRebuild(updatedRounds)
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
                        newlyCreatedRoundId = newRoundId,
                        currentPeelRound = nextNumber
                    )
                }
                syncPartRoundsAndRebuild(newRounds)
                viewModelScope.launch {
                    _effects.emit(WorkspaceEffect.ScrollToRound(newRoundId))
                }
            }

            is WorkspaceIntent.ConsumeInitialFocus -> {
                _state.update { it.copy(newlyCreatedRoundId = null) }
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
                    syncPartRoundsAndRebuild(filtered)
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
                syncPartRoundsAndRebuild(updatedRounds)
            }

            is WorkspaceIntent.SelectPart -> {
                val targetPartId = intent.partId
                if (targetPartId == _state.value.selectedPartId) return
                val currentPartId = _state.value.selectedPartId
                val currentMap = _state.value.roundsByPartId.toMutableMap()
                currentMap[currentPartId] = _state.value.rounds
                val targetRounds = currentMap[targetPartId] ?: listOf(
                    RoundItemUiModel(
                        id = "${targetPartId}_r1",
                        roundNumber = 1,
                        rawInstruction = "AM 6",
                        producedStitches = 6,
                        consumedStitches = 0,
                        declaredStitches = null,
                        isValid = true,
                        colorHex = _state.value.parts.find { it.id == targetPartId }?.colorHex ?: "#E06D53"
                    )
                )
                currentMap[targetPartId] = targetRounds

                _state.update {
                    it.copy(
                        selectedPartId = targetPartId,
                        roundsByPartId = currentMap,
                        selectedRoundId = targetRounds.lastOrNull()?.id,
                        currentPeelRound = targetRounds.size
                    )
                }
                recalculateAndRebuildMesh(targetRounds)
            }

            is WorkspaceIntent.AddPart -> {
                processIntent(WorkspaceIntent.OpenAddPartDialog)
            }

            is WorkspaceIntent.OpenAddPartDialog -> {
                _state.update { it.copy(showAddPartDialog = true, partActionError = null) }
            }

            is WorkspaceIntent.DismissPartDialogs -> {
                _state.update {
                    it.copy(
                        showAddPartDialog = false,
                        showRenamePartDialog = false,
                        showDeletePartDialog = false,
                        partActionTarget = null,
                        partActionError = null
                    )
                }
            }

            is WorkspaceIntent.CreatePart -> {
                val trimmed = intent.name.trim()
                val currentParts = _state.value.parts
                if (trimmed.isEmpty()) {
                    _state.update { it.copy(partActionError = "El nombre no puede estar vacío") }
                    return
                }
                if (currentParts.any { it.name.equals(trimmed, ignoreCase = true) }) {
                    _state.update { it.copy(partActionError = "Ya existe una pieza con el nombre '$trimmed'") }
                    return
                }
                val nextNum = (currentParts.maxOfOrNull { it.id.removePrefix("p").toIntOrNull() ?: 0 } ?: 0) + 1
                val newPartId = "p$nextNum"
                val initialInstruction = when (intent.topologyType) {
                    PartTopologyType.FLAT_PANEL -> "10 cad"
                    else -> "AM 6"
                }
                val initialProduced = when (intent.topologyType) {
                    PartTopologyType.FLAT_PANEL -> 10
                    else -> 6
                }
                val initialRound = RoundItemUiModel(
                    id = "${newPartId}_r1",
                    roundNumber = 1,
                    rawInstruction = initialInstruction,
                    producedStitches = initialProduced,
                    consumedStitches = 0,
                    declaredStitches = null,
                    isValid = true,
                    colorHex = intent.colorHex
                )
                val newPart = ProjectPartUiModel(
                    id = newPartId,
                    name = trimmed,
                    roundCount = 1,
                    colorHex = intent.colorHex,
                    isValid = true,
                    sortOrder = currentParts.size,
                    topologyType = intent.topologyType
                )
                val currentMap = _state.value.roundsByPartId.toMutableMap()
                currentMap[_state.value.selectedPartId] = _state.value.rounds
                currentMap[newPartId] = listOf(initialRound)

                _state.update {
                    it.copy(
                        parts = currentParts + newPart,
                        selectedPartId = newPartId,
                        roundsByPartId = currentMap,
                        selectedRoundId = initialRound.id,
                        currentPeelRound = 1,
                        showAddPartDialog = false,
                        partActionError = null
                    )
                }
                recalculateAndRebuildMesh(listOf(initialRound))
                viewModelScope.launch {
                    _effects.emit(WorkspaceEffect.ShowToast("Pieza '$trimmed' creada"))
                }
            }

            is WorkspaceIntent.OpenRenamePartDialog -> {
                val target = _state.value.parts.find { it.id == intent.partId }
                if (target != null) {
                    _state.update {
                        it.copy(
                            showRenamePartDialog = true,
                            partActionTarget = target,
                            partActionError = null
                        )
                    }
                }
            }

            is WorkspaceIntent.ConfirmRenamePart -> {
                val trimmed = intent.newName.trim()
                val currentParts = _state.value.parts
                if (trimmed.isEmpty()) {
                    _state.update { it.copy(partActionError = "El nombre no puede estar vacío") }
                    return
                }
                if (currentParts.any { it.id != intent.partId && it.name.equals(trimmed, ignoreCase = true) }) {
                    _state.update { it.copy(partActionError = "Ya existe otra pieza con el nombre '$trimmed'") }
                    return
                }
                val updatedParts = currentParts.map {
                    if (it.id == intent.partId) it.copy(name = trimmed) else it
                }
                _state.update {
                    it.copy(
                        parts = updatedParts,
                        showRenamePartDialog = false,
                        partActionTarget = null,
                        partActionError = null
                    )
                }
                viewModelScope.launch {
                    _effects.emit(WorkspaceEffect.ShowToast("Pieza renombrada a '$trimmed'"))
                }
            }

            is WorkspaceIntent.DuplicatePart -> {
                val source = _state.value.parts.find { it.id == intent.partId } ?: return
                val currentMap = _state.value.roundsByPartId.toMutableMap()
                currentMap[_state.value.selectedPartId] = _state.value.rounds
                val sourceRounds = currentMap[intent.partId] ?: emptyList()

                var dupName = "${source.name} (Copia)"
                var counter = 2
                while (_state.value.parts.any { it.name.equals(dupName, ignoreCase = true) }) {
                    dupName = "${source.name} (Copia $counter)"
                    counter++
                }

                val nextNum = (_state.value.parts.maxOfOrNull { it.id.removePrefix("p").toIntOrNull() ?: 0 } ?: 0) + 1
                val newPartId = "p$nextNum"
                val clonedRounds = sourceRounds.map { r ->
                    r.copy(id = "${newPartId}_r${r.roundNumber}")
                }
                val duplicatedPart = ProjectPartUiModel(
                    id = newPartId,
                    name = dupName,
                    roundCount = clonedRounds.size,
                    colorHex = source.colorHex,
                    isValid = source.isValid,
                    sortOrder = _state.value.parts.size,
                    topologyType = source.topologyType
                )
                currentMap[newPartId] = clonedRounds

                _state.update {
                    it.copy(
                        parts = it.parts + duplicatedPart,
                        selectedPartId = newPartId,
                        roundsByPartId = currentMap,
                        selectedRoundId = clonedRounds.lastOrNull()?.id,
                        currentPeelRound = clonedRounds.size
                    )
                }
                recalculateAndRebuildMesh(clonedRounds)
                viewModelScope.launch {
                    _effects.emit(WorkspaceEffect.ShowToast("Pieza duplicada: '$dupName'"))
                }
            }

            is WorkspaceIntent.OpenDeletePartDialog -> {
                val target = _state.value.parts.find { it.id == intent.partId }
                if (target != null) {
                    if (_state.value.parts.size <= 1) {
                        viewModelScope.launch {
                            _effects.emit(WorkspaceEffect.ShowToast("No se puede eliminar la única pieza del proyecto"))
                        }
                    } else {
                        _state.update {
                            it.copy(
                                showDeletePartDialog = true,
                                partActionTarget = target
                            )
                        }
                    }
                }
            }

            is WorkspaceIntent.ConfirmDeletePart -> {
                val currentParts = _state.value.parts
                if (currentParts.size <= 1) {
                    viewModelScope.launch {
                        _effects.emit(WorkspaceEffect.ShowToast("No se puede eliminar la única pieza del proyecto"))
                    }
                    _state.update { it.copy(showDeletePartDialog = false, partActionTarget = null) }
                    return
                }
                val remainingParts = currentParts.filter { it.id != intent.partId }
                val currentMap = _state.value.roundsByPartId.toMutableMap()
                currentMap.remove(intent.partId)

                val newSelectedId = if (_state.value.selectedPartId == intent.partId) {
                    remainingParts.first().id
                } else {
                    _state.value.selectedPartId
                }
                val newRounds = currentMap[newSelectedId] ?: emptyList()

                _state.update {
                    it.copy(
                        parts = remainingParts,
                        selectedPartId = newSelectedId,
                        roundsByPartId = currentMap,
                        selectedRoundId = newRounds.lastOrNull()?.id,
                        currentPeelRound = newRounds.size,
                        showDeletePartDialog = false,
                        partActionTarget = null
                    )
                }
                recalculateAndRebuildMesh(newRounds)
                viewModelScope.launch {
                    _effects.emit(WorkspaceEffect.ShowToast("Pieza eliminada"))
                }
            }

            is WorkspaceIntent.ReorderPart -> {
                val currentParts = _state.value.parts.toMutableList()
                val index = currentParts.indexOfFirst { it.id == intent.partId }
                if (index != -1 && intent.toIndex in currentParts.indices && index != intent.toIndex) {
                    val item = currentParts.removeAt(index)
                    currentParts.add(intent.toIndex, item)
                    val reindexed = currentParts.mapIndexed { idx, p -> p.copy(sortOrder = idx) }
                    _state.update { it.copy(parts = reindexed) }
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

            is WorkspaceIntent.ToggleStuffingSimulation -> {
                _state.update { it.copy(isStuffingSimulationEnabled = intent.enabled) }
                recalculateAndRebuildMesh(_state.value.rounds)
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

    private fun syncPartRoundsAndRebuild(
        updatedRounds: List<RoundItemUiModel>,
        peelLimit: Int? = null
    ) {
        val currentPartId = _state.value.selectedPartId
        val updatedMap = _state.value.roundsByPartId.toMutableMap()
        updatedMap[currentPartId] = updatedRounds
        val updatedParts = _state.value.parts.map {
            if (it.id == currentPartId) it.copy(roundCount = updatedRounds.size) else it
        }
        _state.update {
            it.copy(
                parts = updatedParts,
                roundsByPartId = updatedMap
            )
        }
        recalculateAndRebuildMesh(updatedRounds, peelLimit)
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
                            val instances = parsed.ast?.flatten()?.flatMap { inst ->
                                if (inst.stitchType is StitchType.MagicRing) {
                                    val count = (inst.stitchType as StitchType.MagicRing).stitchCount
                                    List(count) {
                                        com.crisanfitos.stitchmesh3d.core.engine.parser.StitchInstance(
                                            stitchType = StitchType.SingleCrochet,
                                            indexInRound = 0,
                                            colorHex = inst.colorHex
                                        )
                                    }
                                } else {
                                    listOf(inst)
                                }
                            } ?: emptyList()

                            if (instances.isNotEmpty()) {
                                val ring = RingProfileGenerator.generateRingFromInstances(idx + 1, instances, gauge, prevRing)
                                ringProfiles.add(ring)
                                prevRing = ring
                            }
                        }
                        if (ringProfiles.isNotEmpty()) {
                            val rawMesh = AdaptiveTessellator.tessellate(ringProfiles, includePolarCap = true)
                            val currentTopology = _state.value.parts.find { it.id == _state.value.selectedPartId }?.topologyType
                                ?: PartTopologyType.CLOSED_FILLED
                            if (_state.value.isStuffingSimulationEnabled) {
                                StuffingInflationFilter.applyInflation(rawMesh, currentTopology)
                            } else {
                                rawMesh
                            }
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

            _state.update { current ->
                val currentPartId = current.selectedPartId
                val updatedMap = current.roundsByPartId.toMutableMap()
                updatedMap[currentPartId] = validatedRounds
                val updatedParts = current.parts.map { p ->
                    if (p.id == currentPartId) p.copy(roundCount = validatedRounds.size, isValid = !hasErrors) else p
                }
                current.copy(
                    parts = updatedParts,
                    roundsByPartId = updatedMap,
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

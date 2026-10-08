package com.crisanfitos.stitchmesh3d.core.engine.validator

import java.util.UUID

/**
 * Validador global del árbol de proyecto y partes compuestas según RF-1.4.
 *
 * Evalúa secuencial e independientemente cada componente del proyecto (ej. Cabeza, Cuerpo, Extremidades)
 * y determina la validez holística del proyecto.
 */
object ProjectValidator {

    /**
     * Valida una parte o componente individual de un proyecto.
     *
     * @param part Definición de la parte (identificador, nombre e instrucciones).
     * @param language Idioma para los mensajes de error diagnósticos.
     * @return [PartValidationResult] con el diagnóstico vuelta a vuelta de la pieza.
     */
    fun validatePart(
        part: ProjectPartDefinition,
        language: ValidationLanguage = ValidationLanguage.ES
    ): PartValidationResult {
        val roundResults = ArithmeticValidator.validatePattern(
            lines = part.instructions,
            language = language
        )
        return PartValidationResult(
            partId = part.id,
            partName = part.name,
            roundResults = roundResults
        )
    }

    /**
     * Validador de conveniencia para una parte dada por su nombre y lista de instrucciones.
     */
    fun validatePart(
        partName: String,
        instructions: List<String>,
        partId: String = UUID.randomUUID().toString(),
        language: ValidationLanguage = ValidationLanguage.ES
    ): PartValidationResult {
        return validatePart(
            part = ProjectPartDefinition(
                id = partId,
                name = partName,
                instructions = instructions
            ),
            language = language
        )
    }

    /**
     * Valida un proyecto amigurumi completo con todas sus partes compuestas.
     *
     * @param project Definición completa del proyecto con sus piezas.
     * @param language Idioma para los mensajes de diagnóstico.
     * @return [ProjectValidationTree] con el árbol jerárquico de validez.
     */
    fun validateProject(
        project: ProjectDefinition,
        language: ValidationLanguage = ValidationLanguage.ES
    ): ProjectValidationTree {
        val partResults = project.parts.map { part ->
            validatePart(part = part, language = language)
        }

        return ProjectValidationTree(
            projectId = project.id,
            projectName = project.name,
            parts = partResults
        )
    }

    /**
     * Validador de conveniencia para un proyecto dado por su nombre y un mapa de partes (nombre -> instrucciones).
     */
    fun validateProject(
        projectName: String,
        parts: Map<String, List<String>>,
        projectId: String = UUID.randomUUID().toString(),
        language: ValidationLanguage = ValidationLanguage.ES
    ): ProjectValidationTree {
        val partDefinitions = parts.map { (name, instructions) ->
            ProjectPartDefinition(
                name = name,
                instructions = instructions
            )
        }

        return validateProject(
            project = ProjectDefinition(
                id = projectId,
                name = projectName,
                parts = partDefinitions
            ),
            language = language
        )
    }
}

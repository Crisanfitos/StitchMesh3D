package com.crisanfitos.stitchmesh3d.core.engine.model

/**
 * Modificadores topológicos de inserción y relieve según TRD §2.1 y §2.3.
 */
enum class TopologyFlag {
    /** Inserción estándar a través de ambas hebras (Both Loops). */
    NORMAL,

    /** Solo hebra trasera (Back Loop Only) - crea arista viva a 90°. */
    BLO,

    /** Solo hebra delantera (Front Loop Only) - genera solapa visible. */
    FLO,

    /** Relieve delantero abrazando el poste (Front Post) - desplaza hacia el exterior (+Δr). */
    FRONT_POST,

    /** Relieve trasero abrazando el poste (Back Post) - desplaza hacia el interior (-Δr). */
    BACK_POST
}

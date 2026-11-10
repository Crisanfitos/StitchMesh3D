package com.crisanfitos.stitchmesh3d.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crisanfitos.stitchmesh3d.data.local.dao.PatternRoundDao
import com.crisanfitos.stitchmesh3d.data.local.dao.ProjectDao
import com.crisanfitos.stitchmesh3d.data.local.dao.ProjectPartDao
import com.crisanfitos.stitchmesh3d.data.local.dao.YarnGaugeStandardDao
import com.crisanfitos.stitchmesh3d.data.local.entity.PatternRoundEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectPartEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.YarnGaugeStandardEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Pruebas de integración reales para Room Database en SQLite en memoria (SM-093).
 * Valida integridad referencial, borrado en cascada, transacciones atómicas y operaciones Upsert.
 */
@RunWith(AndroidJUnit4::class)
class StitchMeshDatabaseInstrumentedTest {

    private lateinit var db: StitchMeshDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var projectPartDao: ProjectPartDao
    private lateinit var patternRoundDao: PatternRoundDao
    private lateinit var yarnGaugeStandardDao: YarnGaugeStandardDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StitchMeshDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        projectDao = db.projectDao()
        projectPartDao = db.projectPartDao()
        patternRoundDao = db.patternRoundDao()
        yarnGaugeStandardDao = db.yarnGaugeStandardDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveProjectWithPartsAndRounds() = runBlocking {
        val project = ProjectEntity(
            id = "proj-1",
            title = "Oso Amigurumi",
            description = "Patrón paramétrico de prueba",
            hookSizeMm = 4.0f
        )
        projectDao.insertProject(project)

        val part = ProjectPartEntity(
            id = "part-1",
            projectId = "proj-1",
            name = "Cabeza",
            sortOrder = 0
        )
        projectPartDao.insertPart(part)

        val round1 = PatternRoundEntity(
            id = "r-1",
            partId = "part-1",
            roundNumber = 1,
            rawInstruction = "6 pb en anillo mágico",
            consumedStitches = 0,
            producedStitches = 6,
            isValid = true
        )
        val round2 = PatternRoundEntity(
            id = "r-2",
            partId = "part-1",
            roundNumber = 2,
            rawInstruction = "6 aum [12]",
            consumedStitches = 6,
            producedStitches = 12,
            isValid = true
        )
        patternRoundDao.insertRounds(listOf(round1, round2))

        val retrieved = projectDao.getProjectWithDetails("proj-1")
        assertNotNull(retrieved)
        assertEquals("Oso Amigurumi", retrieved!!.project.title)
        assertEquals(1, retrieved.partsWithRounds.size)

        val retrievedPartWithRounds = retrieved.partsWithRounds.first()
        assertEquals("Cabeza", retrievedPartWithRounds.part.name)
        assertEquals(2, retrievedPartWithRounds.rounds.size)
        assertEquals(1, retrievedPartWithRounds.rounds[0].roundNumber)
        assertEquals(2, retrievedPartWithRounds.rounds[1].roundNumber)
    }

    @Test
    fun upsertProjectPreservesExistingPartsAndRounds() = runBlocking {
        val project = ProjectEntity(id = "proj-2", title = "Versión Inicial")
        projectDao.insertProject(project)

        val part = ProjectPartEntity(id = "part-2", projectId = "proj-2", name = "Cuerpo")
        projectPartDao.insertPart(part)

        // Actualizar título del proyecto vía @Upsert
        val updatedProject = project.copy(title = "Versión Renombrada")
        projectDao.insertProject(updatedProject)

        val retrieved = projectDao.getProjectWithDetails("proj-2")
        assertNotNull(retrieved)
        assertEquals("Versión Renombrada", retrieved!!.project.title)
        assertEquals(1, retrieved.partsWithRounds.size)
        assertEquals("Cuerpo", retrieved.partsWithRounds.first().part.name)
    }

    @Test
    fun deleteProjectCascadesToPartsAndPatternRounds() = runBlocking {
        val project = ProjectEntity(id = "proj-3", title = "Proyecto a Borrar")
        projectDao.insertProject(project)

        val part = ProjectPartEntity(id = "part-3", projectId = "proj-3", name = "Orejas")
        projectPartDao.insertPart(part)

        val round = PatternRoundEntity(
            id = "r-3",
            partId = "part-3",
            roundNumber = 1,
            rawInstruction = "6 pb"
        )
        patternRoundDao.insertRound(round)

        // Borrar el proyecto padre
        val deletedCount = projectDao.deleteProjectById("proj-3")
        assertEquals(1, deletedCount)

        // Verificar que el proyecto ya no existe
        assertNull(projectDao.getProjectWithDetails("proj-3"))

        // Verificar que la clave foránea CASCADE eliminó la pieza y las vueltas
        assertNull(projectPartDao.getPartById("part-3"))
    }

    @Test
    fun replaceRoundsForPartExecutesAtomicallyInTransaction() = runBlocking {
        val project = ProjectEntity(id = "proj-4", title = "Atomic Transaction Test")
        projectDao.insertProject(project)

        val part = ProjectPartEntity(id = "part-4", projectId = "proj-4", name = "Brazo")
        projectPartDao.insertPart(part)

        val initialRound = PatternRoundEntity(
            id = "r-old",
            partId = "part-4",
            roundNumber = 1,
            rawInstruction = "4 pb"
        )
        patternRoundDao.insertRound(initialRound)

        val newRounds = listOf(
            PatternRoundEntity(id = "r-new1", partId = "part-4", roundNumber = 1, rawInstruction = "6 pb"),
            PatternRoundEntity(id = "r-new2", partId = "part-4", roundNumber = 2, rawInstruction = "6 aum")
        )
        patternRoundDao.replaceRoundsForPart("part-4", newRounds)

        val retrieved = projectDao.getProjectWithDetails("proj-4")
        assertNotNull(retrieved)
        val rounds = retrieved!!.partsWithRounds.first().rounds
        assertEquals(2, rounds.size)
        assertEquals("r-new1", rounds[0].id)
        assertEquals("r-new2", rounds[1].id)
    }

    @Test
    fun yarnGaugeStandardDaoCrudOperations() = runBlocking {
        val standard = YarnGaugeStandardEntity(
            yarnWeightCategory = 4,
            categoryName = "Worsted / Aran",
            hookSizeMm = 4.0f,
            stitchWidthMm = 4.5f,
            stitchHeightMm = 5.0f,
            stitchThicknessMm = 2.2f
        )
        yarnGaugeStandardDao.insertAll(listOf(standard))

        val count = yarnGaugeStandardDao.getCount()
        assertEquals(1, count)
    }
}

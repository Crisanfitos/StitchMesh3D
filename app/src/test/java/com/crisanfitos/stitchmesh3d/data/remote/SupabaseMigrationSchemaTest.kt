package com.crisanfitos.stitchmesh3d.data.remote

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseMigrationSchemaTest {

    private fun resolveMigrationFile(): File {
        val candidates = listOf(
            File("supabase/migrations/20261027000001_initial_stitchmesh_schema.sql"),
            File("../supabase/migrations/20261027000001_initial_stitchmesh_schema.sql")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("No se encontró el archivo de migración SQL de Supabase")
    }

    @Test
    fun `migration file defines all required tables and indices`() {
        val sql = resolveMigrationFile().readText()

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.profiles"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.yarn_gauge_standards"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.projects"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.project_parts"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.pattern_rounds"))

        assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS idx_projects_user ON public.projects(user_id)"))
        assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS idx_parts_project ON public.project_parts(project_id)"))
        assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS idx_rounds_part_order ON public.pattern_rounds(part_id, round_number)"))
    }

    @Test
    fun `migration file enables RLS and defines ownership policies for all tables`() {
        val sql = resolveMigrationFile().readText()

        assertTrue(sql.contains("ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY"))
        assertTrue(sql.contains("ALTER TABLE public.yarn_gauge_standards ENABLE ROW LEVEL SECURITY"))
        assertTrue(sql.contains("ALTER TABLE public.projects ENABLE ROW LEVEL SECURITY"))
        assertTrue(sql.contains("ALTER TABLE public.project_parts ENABLE ROW LEVEL SECURITY"))
        assertTrue(sql.contains("ALTER TABLE public.pattern_rounds ENABLE ROW LEVEL SECURITY"))

        assertTrue(sql.contains("USING (auth.uid() = user_id)"))
        assertTrue(sql.contains("WITH CHECK (auth.uid() = user_id)"))
    }

    @Test
    fun `migration file respects terminology rule 13`() {
        val sql = resolveMigrationFile().readText()
        assertFalse(
            "La migración no debe contener el término prohibido por la Regla 13",
            sql.lowercase().contains("galga")
        )
    }
}

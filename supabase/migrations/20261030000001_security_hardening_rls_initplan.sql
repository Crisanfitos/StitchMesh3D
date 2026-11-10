-- =====================================================================
-- StitchMesh 3D - Supabase Security Hardening & RLS InitPlan Optimization (SM-089)
-- =====================================================================

-- 1. Endurecimiento de Seguridad: Revocar permisos de ejecución pública en función SECURITY DEFINER
-- Previene que anon o authenticated invoquen /rest/v1/rpc/handle_new_auth_user
REVOKE EXECUTE ON FUNCTION public.handle_new_auth_user() FROM PUBLIC, anon, authenticated;

-- 2. Fijar search_path explícito vacío ('') para mitigar secuestro de esquemas
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    NEW.updated_at = TIMEZONE('utc', NOW());
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    INSERT INTO public.profiles (id, username, display_name)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username', SPLIT_PART(NEW.email, '@', 1) || '_' || SUBSTR(NEW.id::TEXT, 1, 6)),
        COALESCE(NEW.raw_user_meta_data->>'display_name', SPLIT_PART(NEW.email, '@', 1))
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

-- 3. Optimización de Rendimiento RLS (auth_rls_initplan)
-- Sustituye la evaluación fila por fila de auth.uid() por (select auth.uid()) para evaluación en InitPlan

-- Profiles
DROP POLICY IF EXISTS "Gestión de perfil propio" ON public.profiles;
CREATE POLICY "Gestión de perfil propio"
ON public.profiles FOR ALL TO authenticated
USING ((select auth.uid()) = id)
WITH CHECK ((select auth.uid()) = id);

-- Projects
DROP POLICY IF EXISTS "Gestión de proyectos propios" ON public.projects;
CREATE POLICY "Gestión de proyectos propios"
ON public.projects FOR ALL TO authenticated
USING ((select auth.uid()) = user_id)
WITH CHECK ((select auth.uid()) = user_id);

-- Project Parts
DROP POLICY IF EXISTS "Gestión de partes propias" ON public.project_parts;
CREATE POLICY "Gestión de partes propias"
ON public.project_parts FOR ALL TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.projects
        WHERE projects.id = project_parts.project_id
          AND projects.user_id = (select auth.uid())
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.projects
        WHERE projects.id = project_parts.project_id
          AND projects.user_id = (select auth.uid())
    )
);

-- Pattern Rounds
DROP POLICY IF EXISTS "Gestión de vueltas propias" ON public.pattern_rounds;
CREATE POLICY "Gestión de vueltas propias"
ON public.pattern_rounds FOR ALL TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.project_parts
        JOIN public.projects ON projects.id = project_parts.project_id
        WHERE project_parts.id = pattern_rounds.part_id
          AND projects.user_id = (select auth.uid())
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.project_parts
        JOIN public.projects ON projects.id = project_parts.project_id
        WHERE project_parts.id = pattern_rounds.part_id
          AND projects.user_id = (select auth.uid())
    )
);

-- 4. Índice faltante en clave foránea projects(selected_gauge_id)
CREATE INDEX IF NOT EXISTS idx_projects_selected_gauge_id ON public.projects(selected_gauge_id);

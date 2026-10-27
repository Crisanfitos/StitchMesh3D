-- =====================================================================
-- StitchMesh 3D - Supabase PostgreSQL Schema & RLS (SM-035)
-- =====================================================================

-- Habilitar extensión UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Función auxiliar para actualizar automáticamente la columna updated_at en UTC
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = TIMEZONE('utc', NOW());
    RETURN NEW;
END;
$$;

-- 1. TABLA: Perfiles de Usuario
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL,
    display_name TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

DROP TRIGGER IF EXISTS trg_profiles_updated_at ON public.profiles;
CREATE TRIGGER trg_profiles_updated_at
BEFORE UPDATE ON public.profiles
FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- Creación automática de perfil al registrar nuevo usuario en auth.users
CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = public
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

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_auth_user();

-- 2. TABLA MAESTRA: Matriz de Estándares de Muestra de Tensión (CYC Yarn & Hook Standard Matrix)
CREATE TABLE IF NOT EXISTS public.yarn_gauge_standards (
    id SERIAL PRIMARY KEY,
    yarn_weight_category INT NOT NULL,          -- 0 (Lace) a 7 (Jumbo)
    category_name TEXT NOT NULL,                -- ej. 'Worsted / Medium'
    hook_size_mm NUMERIC(4,2) NOT NULL,         -- ej. 3.50, 4.00
    stitch_width_mm NUMERIC(4,2) NOT NULL,      -- mm por punto bajo en muestra de tensión
    stitch_height_mm NUMERIC(4,2) NOT NULL,     -- mm por hilera en muestra de tensión
    stitch_thickness_mm NUMERIC(4,2) NOT NULL,
    CONSTRAINT uq_tension_standard UNIQUE (yarn_weight_category, hook_size_mm)
);

-- Datos semilla CYC para la matriz de Muestra de Tensión (10x10 cm)
INSERT INTO public.yarn_gauge_standards
(yarn_weight_category, category_name, hook_size_mm, stitch_width_mm, stitch_height_mm, stitch_thickness_mm)
VALUES
(0, 'Lace', 1.50, 1.40, 1.30, 1.10),
(1, 'Super Fine / Fingering', 2.00, 1.80, 1.70, 1.40),
(2, 'Fine / Sport', 2.50, 2.20, 2.10, 1.80),
(3, 'Light / DK', 3.00, 2.80, 2.60, 2.20),
(4, 'Worsted / Medium', 3.50, 3.20, 3.00, 2.50),
(4, 'Worsted / Medium', 4.00, 3.60, 3.30, 2.80),
(5, 'Bulky', 5.50, 5.00, 4.80, 4.00),
(6, 'Super Bulky', 8.00, 7.20, 6.80, 5.80),
(7, 'Jumbo', 12.00, 11.00, 10.50, 9.00)
ON CONFLICT (yarn_weight_category, hook_size_mm) DO NOTHING;

-- 3. TABLA: Proyectos de Amigurumi y Crochet
CREATE TABLE IF NOT EXISTS public.projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    structure_type TEXT DEFAULT 'amigurumi_3d' NOT NULL,
    yarn_weight_category INT DEFAULT 4 NOT NULL,
    hook_size_mm NUMERIC(4,2) DEFAULT 3.50 NOT NULL,
    selected_gauge_id INT REFERENCES public.yarn_gauge_standards(id),
    custom_stitch_width_mm NUMERIC(4,2),        -- Muestra de tensión personalizada (ancho mm)
    custom_stitch_height_mm NUMERIC(4,2),       -- Muestra de tensión personalizada (alto mm)
    is_valid BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

DROP TRIGGER IF EXISTS trg_projects_updated_at ON public.projects;
CREATE TRIGGER trg_projects_updated_at
BEFORE UPDATE ON public.projects
FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- 4. TABLA: Partes del Proyecto (Piezas individuales a tejer y ensamblar en 3D)
CREATE TABLE IF NOT EXISTS public.project_parts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES public.projects(id) ON DELETE CASCADE,
    name TEXT NOT NULL,                         -- ej. 'Cabeza', 'Brazo Derecho'
    topology_type TEXT DEFAULT 'closed_filled' NOT NULL,
    color_hex VARCHAR(7) DEFAULT '#E06D53' NOT NULL,
    sort_order INT DEFAULT 0 NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE NOT NULL,
    is_locked BOOLEAN DEFAULT FALSE NOT NULL,
    transform_position_x NUMERIC(6,2) DEFAULT 0.0 NOT NULL,
    transform_position_y NUMERIC(6,2) DEFAULT 0.0 NOT NULL,
    transform_position_z NUMERIC(6,2) DEFAULT 0.0 NOT NULL,
    transform_rotation_x NUMERIC(6,2) DEFAULT 0.0 NOT NULL,
    transform_rotation_y NUMERIC(6,2) DEFAULT 0.0 NOT NULL,
    transform_rotation_z NUMERIC(6,2) DEFAULT 0.0 NOT NULL,
    transform_scale_x NUMERIC(6,2) DEFAULT 1.0 NOT NULL,
    transform_scale_y NUMERIC(6,2) DEFAULT 1.0 NOT NULL,
    transform_scale_z NUMERIC(6,2) DEFAULT 1.0 NOT NULL,
    snap_anchor_target TEXT,
    is_valid BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

DROP TRIGGER IF EXISTS trg_project_parts_updated_at ON public.project_parts;
CREATE TRIGGER trg_project_parts_updated_at
BEFORE UPDATE ON public.project_parts
FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- 5. TABLA: Vueltas / Hileras del Patrón
CREATE TABLE IF NOT EXISTS public.pattern_rounds (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    part_id UUID NOT NULL REFERENCES public.project_parts(id) ON DELETE CASCADE,
    round_number INT NOT NULL,                  -- Número secuencial de vuelta (1, 2, 3...)
    raw_instruction TEXT NOT NULL,              -- ej. '[1 pb, 1 aum] * 6'
    color_hex VARCHAR(7) DEFAULT '#E06D53' NOT NULL,
    consumed_stitches INT NOT NULL DEFAULT 0,
    produced_stitches INT NOT NULL DEFAULT 0,
    declared_stitches INT,                      -- Valor declarado en la fórmula (18)
    is_valid BOOLEAN DEFAULT FALSE NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    CONSTRAINT unique_round_per_part UNIQUE (part_id, round_number)
);

DROP TRIGGER IF EXISTS trg_pattern_rounds_updated_at ON public.pattern_rounds;
CREATE TRIGGER trg_pattern_rounds_updated_at
BEFORE UPDATE ON public.pattern_rounds
FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- =====================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- =====================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.yarn_gauge_standards ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.project_parts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pattern_rounds ENABLE ROW LEVEL SECURITY;

-- Políticas de perfil propio
DROP POLICY IF EXISTS "Gestión de perfil propio" ON public.profiles;
CREATE POLICY "Gestión de perfil propio"
ON public.profiles FOR ALL TO authenticated
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

-- Tablas maestras de Muestra de Tensión legibles por usuarios autenticados y anónimos
DROP POLICY IF EXISTS "Lectura pública de estándares de muestra de tensión" ON public.yarn_gauge_standards;
CREATE POLICY "Lectura pública de estándares de muestra de tensión"
ON public.yarn_gauge_standards FOR SELECT TO anon, authenticated
USING (true);

-- Propietario de proyectos
DROP POLICY IF EXISTS "Gestión de proyectos propios" ON public.projects;
CREATE POLICY "Gestión de proyectos propios"
ON public.projects FOR ALL TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Gestión en cascada autorizada a través de la propiedad del proyecto
DROP POLICY IF EXISTS "Gestión de partes propias" ON public.project_parts;
CREATE POLICY "Gestión de partes propias"
ON public.project_parts FOR ALL TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.projects
        WHERE projects.id = project_parts.project_id
          AND projects.user_id = auth.uid()
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.projects
        WHERE projects.id = project_parts.project_id
          AND projects.user_id = auth.uid()
    )
);

DROP POLICY IF EXISTS "Gestión de vueltas propias" ON public.pattern_rounds;
CREATE POLICY "Gestión de vueltas propias"
ON public.pattern_rounds FOR ALL TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.project_parts
        JOIN public.projects ON projects.id = project_parts.project_id
        WHERE project_parts.id = pattern_rounds.part_id
          AND projects.user_id = auth.uid()
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.project_parts
        JOIN public.projects ON projects.id = project_parts.project_id
        WHERE project_parts.id = pattern_rounds.part_id
          AND projects.user_id = auth.uid()
    )
);

-- Índices de consulta frecuente
CREATE INDEX IF NOT EXISTS idx_projects_user ON public.projects(user_id);
CREATE INDEX IF NOT EXISTS idx_parts_project ON public.project_parts(project_id);
CREATE INDEX IF NOT EXISTS idx_rounds_part_order ON public.pattern_rounds(part_id, round_number);

# StitchMesh 3D

**Amigurumi & Crochet CAD Linter** — Diseño asistido por ordenador para tejedores de ganchillo.

## Descripción

StitchMesh 3D es un entorno de diseño CAD paramétrico y verificación formal de patrones de crochet y amigurumi. Permite introducir instrucciones vuelta por vuelta, detectar errores aritméticos en los recuentos de puntos, calibrar la escala dimensional según el hilo y la aguja, y renderizar una malla 3D interactiva — optimizado para tablets Android.

## Características

- **Linter & parser multilingüe** (español / inglés) con verificación de invariantes aritméticos
- **Matriz de galga** con calibración dimensional según grosor de hilo y aguja
- **Visualizador 3D paramétrico** (Google Filament) con anillos asimétricos y relieve de puntos
- **Soporte multipieza** con colores, ensamblado y gizmos de posicionamiento 3D
- **Persistencia offline** (Room) y **sincronización en la nube** (Supabase)
- **Interfaz adaptativa** para tablets mediante Jetpack Compose

## Stack tecnológico

| Capa | Tecnología |
| --- | --- |
| Lenguaje | Kotlin 2.0+ |
| UI | Jetpack Compose + Material 3 (Compose Adaptive Layouts) |
| Motor 3D | Google Filament / Sceneview |
| Local | Room DB + Coroutines & Flow |
| Backend | Supabase (PostgREST, Auth, Storage) |
| Arquitectura | Clean Architecture + MVI (UDF) |

## Requisitos

- Android Studio 2026.2.1 o superior
- JDK 17+
- Android SDK (mínimo API 26)

## Inicio rápido

1. Clona el repositorio
2. Abre el proyecto en Android Studio
3. Espera la sincronización de Gradle
4. Ejecuta la aplicación en un emulador o dispositivo

## Licencia

Licencia pendiente de definir.
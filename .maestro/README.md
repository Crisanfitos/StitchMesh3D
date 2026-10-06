# Maestro E2E & Smoke Testing 🧶

Este directorio contiene las suites de pruebas End-to-End (E2E) y de humo (smoke tests) para **StitchMesh 3D** utilizando [Maestro](https://maestro.mobile.dev/).

---

## 🚀 Requisitos Previos

1. Instalar CLI de Maestro:
   ```bash
   curl -FsSL "https://get.maestro.mobile.dev" | bash
   export PATH="$PATH:$HOME/.maestro/bin"
   ```

2. Compilar e instalar la app debug en el emulador o dispositivo conectado:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📱 Configuración de Emulador Recomendada (Tablet)

StitchMesh 3D está optimizado principalmente para pantallas tablet (Compose Adaptive / `SupportingPaneScaffold` con vista split 40/60 para editor de patrones y visor Filament 3D).

### Perfil de Dispositivo AVD sugerido:
- **Dispositivo base:** Pixel Tablet o Nexus 9 / 10.1" WQXGA Tablet.
- **Resolución:** 2560 × 1600 px (Landscape) a 320 dpi (≥840 dp de ancho mínimo para activar el layout expandido de tablet).
- **API Level:** Android 14 (API 34) o Android 15 (API 35), Google APIs x86_64.
- **RAM:** 4096 MB (recomendado para el motor de render 3D Filament).
- **Gráficos:** Hardware - GLES 2.0 / 3.0 para soporte óptimo de shaders PBR.

---

## 🧪 Ejecución de Flujos

### Smoke Test de Lanzamiento:
```bash
maestro test .maestro/smoke-app-launch.yaml
```

### Ejecutar todas las suites de Maestro:
```bash
maestro test .maestro/
```

### Modo interactivo / Maestro Studio:
```bash
maestro studio
```

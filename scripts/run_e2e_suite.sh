#!/usr/bin/env bash
set -eo pipefail

echo "=========================================================="
echo "🧶 StitchMesh 3D — Suite Automatizada de Pruebas E2E (Maestro)"
echo "=========================================================="

# 1. Comprobación de conectividad ADB
if ! command -v adb &> /dev/null; then
    echo "❌ Error: 'adb' no está instalado en el PATH."
    exit 1
fi

CONNECTED_DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
if [ "$CONNECTED_DEVICES" -eq 0 ]; then
    echo "⚠️ Advertencia: No hay emuladores ni dispositivos Android conectados vía ADB."
    echo "Registrando INSTRUMENTED_NOT_AVAILABLE / E2E_NOT_AVAILABLE conforme a la regla 06."
    exit 2
fi

echo "✅ Dispositivo detectado:"
adb devices -l | grep "device "

# 2. Comprobación de instalación de Maestro
if ! command -v maestro &> /dev/null; then
    echo "❌ Error: 'maestro' CLI no está instalado en el PATH."
    exit 1
fi

echo "🚀 Iniciando ejecución de los 5 flujos E2E de usuario..."

FLOWS=(
    ".maestro/e2e-01-project-lifecycle.yaml"
    ".maestro/e2e-02-workbench-linter.yaml"
    ".maestro/e2e-03-tension-calibration.yaml"
    ".maestro/e2e-04-assembly-workflow.yaml"
    ".maestro/e2e-05-export-workflow.yaml"
)

TOTAL=${#FLOWS[@]}
PASSED=0

for i in "${!FLOWS[@]}"; do
    FLOW="${FLOWS[$i]}"
    INDEX=$((i + 1))
    echo ""
    echo "----------------------------------------------------------"
    echo "[$INDEX/$TOTAL] Ejecutando: $FLOW"
    echo "----------------------------------------------------------"
    
    if maestro test "$FLOW"; then
        echo "✅ Paso [$INDEX/$TOTAL] exitoso: $FLOW"
        PASSED=$((PASSED + 1))
    else
        echo "❌ Fallo en [$INDEX/$TOTAL]: $FLOW"
        exit 1
    fi
done

echo ""
echo "=========================================================="
echo "🎉 ¡Todos los $PASSED flujos E2E de StitchMesh 3D han pasado en VERDE!"
echo "=========================================================="

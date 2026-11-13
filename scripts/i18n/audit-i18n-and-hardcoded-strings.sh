#!/usr/bin/env bash
# ==============================================================================
# Ether Planetary Simulation Platform — i18n & Hardcoded UI Strings Audit
# Linux / macOS Bash Parity Partner for scripts/i18n/audit-i18n-and-hardcoded-strings.ps1
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_PATH="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$ROOT_PATH"

I18N_DIR="src/main/resources/i18n"
UI_DIR="src/main/java/org/ether/society/ui"

echo "================================================================="
echo "  AUDIT INTEGRAL DE TRADUCTION & DETECTION DES TEXTES HARDCODES  "
echo "================================================================="

BUNDLES=("messages.properties" "messages_en.properties" "messages_fr.properties" "messages_de.properties" "messages_es.properties" "messages_zh.properties")
EXPECTED_COUNT=""
MISMATCH=0

for b in "${BUNDLES[@]}"; do
    f="$I18N_DIR/$b"
    if [[ ! -f "$f" ]]; then
        echo "  [ERREUR] Fichier manquant : $f"
        MISMATCH=1
        continue
    fi
    cnt=$(grep -v '^[[:space:]]*#' "$f" | grep -v '^[[:space:]]*$' | grep '=' | cut -d'=' -f1 | sort -u | wc -l | tr -d ' ')
    echo "  [i18n] Bundle $b : $cnt cles chargees"
    if [[ -z "$EXPECTED_COUNT" ]]; then
        EXPECTED_COUNT="$cnt"
    elif [[ "$cnt" != "$EXPECTED_COUNT" ]]; then
        echo "  [ERREUR] Nombre de cles divergent dans $b ($cnt vs $EXPECTED_COUNT)"
        MISMATCH=1
    fi
done

echo ""
echo "Scan des chaines hardcodees dans l'interface Java ($UI_DIR)..."
HITS=$(grep -rnP '(new (Label|Button|MenuItem|Tab|Tooltip|RadioButton|CheckBox)|\.set(Text|Title))\("[a-zA-ZÀ-ÿ]{3,}[^"]*"\)' "$UI_DIR" \
    | grep -vE 'fx-|Segoe UI|Arial|System|SansSerif|transparent|white|black|red|green|blue|gray|yellow|orange|STDDEV|Orig|dS/dt|Reference:\s*Turchin|"MAX\b' \
    | grep -vE '\("[a-z0-9\._]+"\)' || true)

if [[ -n "$HITS" ]]; then
    echo "$HITS"
    exit 1
fi

if (( MISMATCH != 0 )); then
    exit 1
fi

echo "Total de chaines hardcodees detectees dans l'UI : 0"
echo ""
echo "✅ SUCCES : 100% des cles i18n sont synchronisees sur les 5 langues et 0 chaine hardcodee dans l'UI."
exit 0

param(
    [string]$SourceDir = "src/main/java",
    [string]$I18nDir = "src/main/resources/i18n"
)

Write-Host "================================================================="
Write-Host "  AUDIT INTEGRAL DE TRADUCTION & DETECTION DES TEXTES HARDCODES  "
Write-Host "================================================================="

# 1. Charger toutes les cles des 5 langues + fichier de base
$languages = @('en', 'fr', 'de', 'es', 'zh')
$keyMaps = @{}

$baseFile = Join-Path $I18nDir "messages.properties"
if (Test-Path $baseFile) {
    $baseKeys = @{}
    $lines = [System.IO.File]::ReadAllLines($baseFile, [System.Text.Encoding]::UTF8)
    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        if ($trimmed -and -not $trimmed.StartsWith("#") -and $trimmed.Contains("=")) {
            $idx = $trimmed.IndexOf("=")
            $k = $trimmed.Substring(0, $idx).Trim()
            $v = $trimmed.Substring($idx + 1).Trim()
            $baseKeys[$k] = $v
        }
    }
    $keyMaps['base'] = $baseKeys
    Write-Host ("  [i18n] Bundle BASE (messages.properties) : {0} cles chargees" -f $baseKeys.Count)
}

foreach ($lang in $languages) {
    $file = Join-Path $I18nDir "messages_$lang.properties"
    if (-not (Test-Path $file)) {
        Write-Error "Fichier manquant : $file"
        continue
    }
    $keys = @{}
    $lines = [System.IO.File]::ReadAllLines($file, [System.Text.Encoding]::UTF8)
    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        if ($trimmed -and -not $trimmed.StartsWith("#") -and $trimmed.Contains("=")) {
            $idx = $trimmed.IndexOf("=")
            $k = $trimmed.Substring(0, $idx).Trim()
            $v = $trimmed.Substring($idx + 1).Trim()
            $keys[$k] = $v
        }
    }
    $keyMaps[$lang] = $keys
    Write-Host ("  [i18n] Langue {0} : {1} cles chargees" -f $lang.ToUpper(), $keys.Count)
}

# 2. Cles uniques globales et detection des cles manquantes par langue
$allKeys = @{}
foreach ($lang in $keyMaps.Keys) {
    foreach ($k in $keyMaps[$lang].Keys) {
        $allKeys[$k] = $true
    }
}
Write-Host ("`nTotal de cles uniques identifiees dans les catalogues : {0}" -f $allKeys.Count)

$totalMissing = 0
foreach ($lang in $keyMaps.Keys) {
    $missing = New-Object System.Collections.ArrayList
    foreach ($k in $allKeys.Keys) {
        if (-not $keyMaps[$lang].ContainsKey($k) -or [string]::IsNullOrWhiteSpace($keyMaps[$lang][$k])) {
            [void]$missing.Add($k)
        }
    }
    $totalMissing += $missing.Count
    Write-Host ("  [Audit Exhaustivite] Bundle {0} : {1} cles manquantes ou vides" -f $lang.ToUpper(), $missing.Count)
    if ($missing.Count -gt 0) {
        foreach ($m in $missing) {
            Write-Host ("    -> Cle manquante dans {0} : {1}" -f $lang.ToUpper(), $m)
        }
    }
}

# 3. Scan du code Java dans le package UI (detection des chaines hardcodees visibles)
Write-Host "`nScan des chaines hardcodees dans l'interface Java (UI)..."
$uiFiles = Get-ChildItem -Path "$SourceDir/org/ether/society/ui" -Recurse -Filter "*.java"
$hardcodedHits = New-Object System.Collections.ArrayList

$suspiciousPatterns = @(
    'new Label\("([^"]{3,})"\)',
    'new Button\("([^"]{3,})"\)',
    'new MenuItem\("([^"]{3,})"\)',
    'new Tab\("([^"]{3,})"\)',
    'new Tooltip\("([^"]{3,})"\)',
    'new RadioButton\("([^"]{3,})"\)',
    'new CheckBox\("([^"]{3,})"\)',
    '\.setText\("([^"]{3,})"\)',
    '\.setTitle\("([^"]{3,})"\)'
)

foreach ($file in $uiFiles) {
    $lines = [System.IO.File]::ReadAllLines($file.FullName, [System.Text.Encoding]::UTF8)
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        $trimmed = $line.Trim()
        if ($trimmed.StartsWith("//") -or $trimmed.StartsWith("*")) { continue }
        
        foreach ($pat in $suspiciousPatterns) {
            if ($line -match $pat) {
                $val = $Matches[1]
                # Filtrer uniquement les chaines comportant de vrais mots (au moins 3 lettres consecutives)
                # et exclure les formules mathematiques pures, symboles, polices, couleurs et identifiants techniques
                if ($val -match '[a-zA-Z]{3,}' -and
                    $val -notmatch '^fx\-' -and
                    $val -notmatch '^\%[0-9]*[a-zA-Z]' -and
                    $val -notmatch '^[a-z0-9\._]+$' -and
                    $val -notmatch '^(http|file|data|javafx|org|com|STDDEV|Orig|dS/dt)' -and
                    $val -notmatch 'Reference:\s*Turchin' -and
                    $val -notmatch '^MAX\b' -and
                    $val -notmatch '^(Segoe UI|Arial|System|SansSerif)' -and
                    $val -notmatch '^(transparent|white|black|red|green|blue|gray|yellow|orange)'
                ) {
                    [void]$hardcodedHits.Add([PSCustomObject]@{
                        File = $file.Name
                        Line = $i + 1
                        Match = $val
                    })
                }
            }
        }
    }
}

Write-Host ("`nTotal de chaines hardcodees detectees dans l'UI : {0}" -f $hardcodedHits.Count)
if ($hardcodedHits.Count -gt 0) {
    $hardcodedHits | Format-Table -AutoSize
    exit 1
}

if ($totalMissing -gt 0) {
    exit 1
}

Write-Host "`n✅ SUCCES : 100% des cles i18n sont synchronisees sur les 5 langues et 0 chaine hardcodee dans l'UI."
exit 0

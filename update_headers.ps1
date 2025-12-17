$files = Get-ChildItem -Path "src/main/java" -Recurse -Filter "*.java"
foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match "Copyright \(c\) 2024 Silvere Martin-Michiellot" -and -not ($content -match "Gemini AI Assistant")) {
        $newContent = $content -replace "Copyright \(c\) 2024 Silvere Martin-Michiellot", "Copyright (c) 2024 Gemini AI Assistant`r`n * Copyright (c) 2024 Silvere Martin-Michiellot"
        Set-Content -Path $file.FullName -Value $newContent -Encoding UTF8
        Write-Host "Updated $($file.Name)"
    }
}

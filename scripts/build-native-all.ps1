# Build all multi-platform Ether Native Rust targets (Windows, Linux, macOS)
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "🦀 Building Ether Native Core for Windows, Linux & macOS..." -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$cargoPath = "$env:USERPROFILE\.cargo\bin\cargo.exe"
if (-not (Test-Path $cargoPath)) {
    $cargoPath = "cargo"
}

Push-Location "native/ether-core-native"

try {
    # 1. Windows x86_64 (.dll)
    Write-Host "`n[1/4] 🪟 Building Windows x86_64 (.dll)..." -ForegroundColor Yellow
    & $cargoPath build --release
    if ($LASTEXITCODE -ne 0) { throw "Failed to build Windows target" }

    # 2. Linux x86_64 (.so)
    Write-Host "`n[2/4] 🐧 Building Linux x86_64 (.so)..." -ForegroundColor Yellow
    & $cargoPath build --release --target x86_64-unknown-linux-gnu
    if ($LASTEXITCODE -ne 0) { throw "Failed to build Linux x86_64 target" }

    # 3. Linux ARM64 (.so)
    Write-Host "`n[3/4] 🐧 Building Linux ARM64 (.so)..." -ForegroundColor Yellow
    & $cargoPath build --release --target aarch64-unknown-linux-gnu
    if ($LASTEXITCODE -ne 0) { throw "Failed to build Linux aarch64 target" }

    # 4. macOS Intel & Apple Silicon (.dylib)
    Write-Host "`n[4/4] 🍏 Building macOS x86_64 & Apple Silicon aarch64 (.dylib)..." -ForegroundColor Yellow
    & $cargoPath build --release --target x86_64-apple-darwin
    & $cargoPath build --release --target aarch64-apple-darwin
    if ($LASTEXITCODE -ne 0) { throw "Failed to build macOS targets" }

    Write-Host "`n✅ All multi-platform native libraries successfully compiled!" -ForegroundColor Green
} finally {
    Pop-Location
}

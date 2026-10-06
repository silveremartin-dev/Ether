@echo off
rem ==============================================================================
rem Ether Native Core Builder (Windows .dll, Linux .so, macOS .dylib via Cargo)
rem ==============================================================================

setlocal enabledelayedexpansion

echo ==========================================================
echo  Building Ether Native Core for Windows, Linux and macOS...
echo ==========================================================

cd /d "%~dp0..\..\ether-core-native" 2>nul
if %errorlevel% neq 0 (
    echo [INFO] Native crate directory not found. Skipping native build.
    exit /b 0
)

echo [1/3] Building Windows native (.dll)...
cargo build --release
if %errorlevel% neq 0 (
    echo [WARN] Cargo build returned code %errorlevel%
)

echo [2/3] Building Linux targets (if cross-toolchain installed)...
cargo build --release --target x86_64-unknown-linux-gnu 2>nul || echo [INFO] Linux target skipped

echo [3/3] Building macOS targets (if cross-toolchain installed)...
cargo build --release --target x86_64-apple-darwin 2>nul || echo [INFO] macOS target skipped

echo Build script completed.
endlocal

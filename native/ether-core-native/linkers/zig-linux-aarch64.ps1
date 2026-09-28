$zig = "C:\Users\silve\AppData\Local\Microsoft\WinGet\Packages\zig.zig_Microsoft.Winget.Source_8wekyb3d8bbwe\zig-x86_64-windows-0.16.0\zig.exe"
$filteredArgs = @()
foreach ($arg in $args) {
    if ($arg -notmatch "--fix-cortex-a53-843419") {
        $filteredArgs += $arg
    }
}
& $zig cc -target aarch64-linux-gnu $filteredArgs
exit $LASTEXITCODE

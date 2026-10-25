@echo off
setlocal
set "VYRLUME_SCRIPT=%~dp0companion\Start-Vyrlume.ps1"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%VYRLUME_SCRIPT%" %*
set "VYRLUME_EXIT=%ERRORLEVEL%"

if not "%VYRLUME_EXIT%"=="0" (
    echo.
    echo Vyrlume USB launcher failed with exit code %VYRLUME_EXIT%.
    pause
)

exit /b %VYRLUME_EXIT%

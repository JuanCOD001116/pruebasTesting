@echo off
rem Verificacion del proyecto en un solo paso. Opciones: -Rapido, -SoloKarate, -SinNavegador
rem La logica vive en scripts\verificar-todo.ps1; este archivo solo la lanza.
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\verificar-todo.ps1" %*
set "CODIGO=%ERRORLEVEL%"
echo.
pause
exit /b %CODIGO%

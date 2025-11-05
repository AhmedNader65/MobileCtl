@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "JAR_PATH=%SCRIPT_DIR%cli\build\libs\mobilectl.jar"

if not exist "%JAR_PATH%" (
    echo JAR not found at %JAR_PATH%
    call gradlew.bat cli:build
    if not exist "%JAR_PATH%" (
        echo Build failed
        exit /b 1
    )
)

java -Dfile.encoding=UTF-8 -jar "%JAR_PATH%" %*
endlocal

@echo off
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
    echo.
    echo [ERROR] javac was not found. Please install a JDK ^(not only a JRE^), version 8 or newer,
    echo         for example Temurin / Oracle JDK 17 or 21, then open a NEW Command Prompt.
    echo         Check with:  javac -version
    echo.
    if "%1"=="" pause
    exit /b 1
)
if not exist out mkdir out
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
    echo.
    echo Compilation FAILED
    if "%1"=="" pause
    exit /b 1
)
echo Compilation successful
if "%1"=="" pause

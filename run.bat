@echo off
cd /d "%~dp0"
rem Opens the graphical Cab Booking Simulator (compiles first when a JDK is available).
where javac >nul 2>nul
if not errorlevel 1 call compile.bat nopause
if not exist out\GuiMain.class (
    echo.
    echo [ERROR] The program is not compiled yet and no JDK was found. Install a JDK and run again.
    pause
    exit /b 1
)
where javaw >nul 2>nul
if errorlevel 1 (
    java -cp out GuiMain
) else (
    start "" javaw -cp out GuiMain
)

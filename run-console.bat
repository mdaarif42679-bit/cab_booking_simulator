@echo off
cd /d "%~dp0"
rem Original console version of the simulator (unchanged menu program).
where javac >nul 2>nul
if not errorlevel 1 call compile.bat nopause
java -cp out Main
pause

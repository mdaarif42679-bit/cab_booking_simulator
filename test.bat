@echo off
cd /d "%~dp0"
where javac >nul 2>nul
if not errorlevel 1 call compile.bat nopause
echo.
echo ===== Original console tests (TestRunner) =====
java -cp out TestRunner
set R1=%errorlevel%
echo.
echo ===== GUI tests (GuiTestRunner) =====
java -cp out GuiTestRunner
set R2=%errorlevel%
echo.
if "%R1%%R2%"=="00" (echo ALL TESTS PASSED) else (echo SOME TESTS FAILED)
if "%1"=="" pause

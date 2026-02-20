@echo off
echo ========================================
echo  UNIVERSAL SKILL DEVELOPMENT CENTRE
echo ========================================
echo.

echo [1/3] Stopping Java processes...
taskkill /F /IM java.exe 2>nul
timeout /t 2 /nobreak >nul

echo [2/3] Cleaning target folder...
if exist target rmdir /S /Q target

echo [3/3] Starting application...
echo.
echo Application starting at http://localhost:8080
echo Opening browser...
echo Press Ctrl+C to stop
echo.

start http://localhost:8080
call mvnw.cmd spring-boot:run

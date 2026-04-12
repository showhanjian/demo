@echo off
setlocal enabledelayedexpansion

echo 1. Stopping application...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
    echo   Killed PID %%a
)

echo 2. Clearing logs...
if exist "logs" (
    del /q "logs\*" 2>nul
    for /d %%d in ("logs\*") do rmdir /s /q "%%d" 2>nul
)
echo   Done.

echo 3. Clearing sessions...
for /d %%d in ("src\main\resources\sessions_repo\*") do (
    rmdir /s /q "%%d" 2>nul
)
echo   Done.

echo 4. Starting application...
mvn spring-boot:run -DskipTests

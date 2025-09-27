@echo off
title Config
cd /d "%~dp0"

mvn spring-boot:run -DskipTests

echo.
echo Press any key to exit...
pause > nul

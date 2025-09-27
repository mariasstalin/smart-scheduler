@echo off
title Discovery
cd /d "%~dp0"

mvn spring-boot:run -DskipTests

echo.
echo Press any key to exit...
pause > nul

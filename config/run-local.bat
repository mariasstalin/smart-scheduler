@echo off
title Config
cd /d "%~dp0"

mvn spring-boot:run -Dspring-boot.run.profiles=local,native -DskipTests

echo.
echo Press any key to exit...
pause > nul

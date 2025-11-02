@echo off
title Demo
cd /d "%~dp0"

mvn spring-boot:run -Dspring-boot.run.profiles=local -DskipTests

echo.
echo Press any key to exit...
pause > nul

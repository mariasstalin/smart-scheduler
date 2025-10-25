@echo off
title Appointment
cd /d "%~dp0"

mvn spring-boot:run -Dspring-boot.run.profiles=local -DskipTests -Dspring-boot.run.arguments="--debug"

echo.
echo Press any key to exit...
pause > nul

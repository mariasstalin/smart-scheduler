@echo off
title Ngrok
cd /d "%~dp0"

ngrok http 8080

echo.
echo Press any key to exit...
pause > nul

@echo off
title Appointment
cd /d "%~dp0"

set ZOHO_TOKEN_BASE_URL=https://accounts.zoho.in
set ZOHO_CLIENT_ID=1000.RMOOGMGPQII9GYSJVD7ZY3S8PJWF4E
set ZOHO_CLIENT_SECRET=05fc217a0cbb4a93dd7e0c8cccf15cac43b025aaf2
set ZOHO_REFRESH_TOKEN=1000.4ee1140006fbd1c2f3d9c4d6de2848bf.2708d56fc0a39cc9476ab078c360b3d6
set ZOHO_API_BASE_URL=https://www.zohoapis.in

set NGROK_AUTHTOKEN=33BhwaUKMEe3g8QjSkKFw67lsY0_42qmqyUuy412WNwa7EGzd
set NGROK_DOMAIN=

set NOTIFICATION_MESSAGING_PROVIDER=demo
set NOTIFICATION_SYSTEM_PHONE=919999999999

set NOTIFICATION_PATIENT_PRIORITY_ENGINE=ai-based

mvn spring-boot:run -Dspring-boot.run.profiles=local -DskipTests -Dspring-boot.run.arguments="--debug"

echo.
echo Press any key to exit...
pause > nul

@echo off
ECHO --- Building, Tagging, and Pushing Smart Scheduler Images ---

REM --- IMPORTANT: You must be logged into Docker Hub before running this script: docker login ---

SET REPO=mariasstalin/smart-scheduler

REM --- Duckling ---
ECHO.
ECHO Building and Pushing duckling...
docker build -t smart-scheduler-duckling -f duckling/Dockerfile ./duckling
docker tag smart-scheduler-duckling %REPO%:duckling-latest
docker push %REPO%:duckling-latest

REM --- Rasa Actions ---
ECHO.
ECHO Building and Pushing rasa-actions...
docker build -t smart-scheduler-rasa-actions -f rasa-actions/Dockerfile ./rasa-actions
docker tag smart-scheduler-rasa-actions %REPO%:rasa-actions-latest
docker push %REPO%:rasa-actions-latest

REM --- Light GBM ---
ECHO.
ECHO Building and Pushing light-gbm...
docker build -t smart-scheduler-light-gbm -f light-gbm/Dockerfile ./light-gbm
docker tag smart-scheduler-light-gbm %REPO%:light-gbm-latest
docker push %REPO%:light-gbm-latest

REM --- Discovery (Spring Boot) ---
ECHO.
ECHO Building and Pushing discovery...
docker build -t smart-scheduler-discovery -f discovery/Dockerfile .
docker tag smart-scheduler-discovery %REPO%:discovery-latest
docker push %REPO%:discovery-latest

REM --- Config (Spring Boot) ---
ECHO.
ECHO Building and Pushing config...
docker build -t smart-scheduler-config -f config/Dockerfile .
docker tag smart-scheduler-config %REPO%:config-latest
docker push %REPO%:config-latest

REM --- Gateway (Spring Boot) ---
ECHO.
ECHO Building and Pushing gateway...
docker build -t smart-scheduler-gateway -f gateway/Dockerfile .
docker tag smart-scheduler-gateway %REPO%:gateway-latest
docker push %REPO%:gateway-latest

REM --- Appointment (Spring Boot) ---
ECHO.
ECHO Building and Pushing appointment...
docker build -t smart-scheduler-appointment -f appointment/Dockerfile .
docker tag smart-scheduler-appointment %REPO%:appointment-latest
docker push %REPO%:appointment-latest

REM --- Notification (Spring Boot) ---
ECHO.
ECHO Building and Pushing notification...
docker build -t smart-scheduler-notification -f notification/Dockerfile .
docker tag smart-scheduler-notification %REPO%:notification-latest
docker push %REPO%:notification-latest

REM --- Demo (Spring Boot) ---
ECHO.
ECHO Building and Pushing demo...
docker build -t smart-scheduler-demo -f demo/Dockerfile .
docker tag smart-scheduler-demo %REPO%:demo-latest
docker push %REPO%:demo-latest

ECHO.
ECHO --- ALL IMAGES BUILT, TAGGED, AND PUSHED SUCCESSFULLY! ---
pause
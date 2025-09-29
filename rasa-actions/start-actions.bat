@echo off
REM ----------------------------------------
REM Activate virtual environment
REM ----------------------------------------
call .venv\Scripts\activate.bat

REM ----------------------------------------
REM Run Rasa Action Server
REM ----------------------------------------
rasa run actions --actions actions --port 5055

REM ----------------------------------------
REM Keep window open after server stops
REM ----------------------------------------
pause

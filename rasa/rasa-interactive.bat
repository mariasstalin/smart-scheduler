@echo off
REM ----------------------------------------
REM Activate virtual environment
REM ----------------------------------------
call .venv\Scripts\activate.bat

REM ----------------------------------------
REM Run Rasa Server
REM ----------------------------------------
rasa interactive

REM ----------------------------------------
REM Keep window open after server stops
REM ----------------------------------------
pause

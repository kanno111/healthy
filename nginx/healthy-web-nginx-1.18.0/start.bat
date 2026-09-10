@echo off
cd /d "%~dp0"
start "Healthy Web Nginx" nginx.exe
echo Healthy Web started: http://localhost:8088

@echo off
cd /d "%~dp0\.."
del /q plugins\packlodge-system.jar
ren plugins\packlodge-system-*.jar packlodge-system.jar
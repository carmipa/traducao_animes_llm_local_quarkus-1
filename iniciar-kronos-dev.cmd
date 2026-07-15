@echo off
rem Inicia o KRONOS em modo dev independente do diretorio de onde for chamado.
rem stdout/stderr vao para console-web.log; stdin fica livre (nunca redirecionar).
cd /d "%~dp0"
call "%~dp0gradlew.bat" quarkusDev > console-web.log 2>&1

@echo off
setlocal

set "JAVA_HOME=D:\Java\jdk-17"
set "DB_PASSWORD=1234"

cd /d "D:\ZYY Project\generic-sys-admin\backend"

echo Starting Spring Boot with JAVA_HOME=%JAVA_HOME%...
echo.

mvn spring-boot:run -Dspring-boot.run.profiles=dev
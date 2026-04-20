@echo off
setlocal

set JAVA_HOME=D:\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
set DB_PASSWORD=1234

cd /d D:\ZYY Project\generic-sys-admin\backend

start cmd /c "title SpringBoot Backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev 2>&1"
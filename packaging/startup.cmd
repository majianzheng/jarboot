@echo off

if not exist "%JAVA_HOME%\bin\java.exe" echo Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better! & EXIT /B 1
set "JAVA=%JAVA_HOME%\bin\java.exe"

setlocal enabledelayedexpansion

set BASE_DIR=%~dp0
rem added double quotation marks to avoid the issue caused by the folder names containing spaces.
rem removed the last 5 chars(which means \bin\) to get the base DIR.
set BASE_DIR="%BASE_DIR:~0,-5%"

set SERVER=jarboot-server

rem JVM Configuration
set "JARBOOT_JVM_OPTS=-Xms256m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=%BASE_DIR%\logs\java_heapdump.hprof -XX:-UseLargePages"

set "JARBOOT_OPTS=%NACOS_OPTS% -jar %BASE_DIR%\%SERVER%.jar"


set COMMAND="%JAVA%" %JARBOOT_JVM_OPTS% %JARBOOT_OPTS% jarboot.jarboot %*

rem start jarboot command
%COMMAND%

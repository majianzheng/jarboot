@echo off

if not exist "%JAVA_HOME%\bin\java.exe" echo Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better! & EXIT /B 1
set "JAVA=%JAVA_HOME%\bin\java.exe"

setlocal enabledelayedexpansion

set JARBOOT_HOME=%~dp0

set SERVER=jarboot-server

rem JVM Configuration
set "JARBOOT_JVM_OPTS=-Xms256m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=%BASE_DIR%\logs\java_heapdump.hprof -XX:-UseLargePages"

set "JARBOOT_OPTS=-Dloader.path=%JARBOOT_HOME%/plugins/server"
set "JARBOOT_OPTS=%JARBOOT_OPTS% -jar %JARBOOT_HOME%\bin\%SERVER%.jar"


set COMMAND="%JAVA%" %JARBOOT_JVM_OPTS% %JARBOOT_OPTS% jarboot.jarboot %*

rem start jarboot command
%COMMAND%

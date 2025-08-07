@echo off

if not exist "%JAVA_HOME%\bin\javaw.exe" echo Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better! & EXIT /B 1
set "JAVA=%JAVA_HOME%\bin\javaw.exe"

setlocal enabledelayedexpansion
chcp 65001 >nul
set JARBOOT_HOME=%~dp0
set JARBOOT_HOME=%JARBOOT_HOME:~0,-13%

pushd "%JARBOOT_HOME%"

set "SERVER=components\jarboot-server.jar"

if not exist ".cache" (
  mkdir ".cache"
)

rem JVM Configuration
set "JARBOOT_JVM_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=5000 -XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs\java_heapdump.hprof"

set JARBOOT_OPTS=-Djdk.attach.allowAttachSelf=true -Dloader.path="components\lib,plugins\server" -Dfile.encoding=UTF-8 -Djava.io.tmpdir=.cache

set COMMAND="%JAVA%" %JARBOOT_JVM_OPTS% %JARBOOT_OPTS% -jar "%SERVER%" jarboot.jarboot %*

rem start jarboot command
echo Starting jarboot server...
start "" %COMMAND%
echo Jarboot server started.

set "TOOL_JAR=components/jarboot-tools.jar io.github.majianzheng.jarboot.tools.daemon.ServerDaemon"
set "DAEMON_VM=-Xms50m -Xmx100m -XX:+UseG1GC -XX:MaxGCPauseMillis=5000 -Djava.io.tmpdir=.cache
set "DAEMON_CMD="%JAVA%" %DAEMON_VM% -cp %TOOL_JAR% jarboot.daemon %*"

echo Starting jarboot daemon...
start "" %DAEMON_CMD%
echo Jarboot daemon started.
echo Start Jarboot success.

echo You can check %JARBOOT_HOME%/logs/jarboot.log
pause

@echo off

if not exist "%JAVA_HOME%\bin\javaw.exe" echo Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better! & EXIT /B 1
set "JAVA=%JAVA_HOME%\bin\javaw.exe"

setlocal enabledelayedexpansion

set JARBOOT_HOME=%~dp0
set JARBOOT_HOME=%JARBOOT_HOME:~0,-5%

set "SERVER=%JARBOOT_HOME%\components\jarboot-server.jar"

rem JVM Configuration
set "JARBOOT_JVM_OPTS=-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError -XX:-UseLargePages"
set JARBOOT_JVM_OPTS=%JARBOOT_JVM_OPTS% -XX:HeapDumpPath="%JARBOOT_HOME%\logs\java_heapdump.hprof"

set JARBOOT_OPTS=-Djdk.attach.allowAttachSelf=true -Dloader.path="%JARBOOT_HOME%\plugins\server" -Dfile.encoding=UTF-8

set COMMAND="%JAVA%" %JARBOOT_JVM_OPTS% %JARBOOT_OPTS% -jar "%SERVER%" jarboot.jarboot %*

rem start jarboot command
%COMMAND%
echo "Starting jarboot server daemon..."
set TOOL_JAR="%JARBOOT_HOME%/components/jarboot-tools.jar io.github.majianzheng.tools.daemon.ServerDaemon"
set DAEMON_VM="-Xms10m -Xmx10m -XX:+UseG1GC -XX:MaxGCPauseMillis=500"
%JAVA% $DAEMON_VM -cp %TOOL_JAR% jarboot.daemon
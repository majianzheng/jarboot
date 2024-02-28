@echo off

if not exist "%JAVA_HOME%\bin\javaw.exe" echo Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better! & EXIT /B 1
set "JAVA=%JAVA_HOME%\bin\java.exe"

setlocal enabledelayedexpansion

set JARBOOT_HOME=%~dp0
set JARBOOT_HOME=%JARBOOT_HOME:~0,-13%


set "TOOL_JAR=%JARBOOT_HOME%/components/jarboot-tools.jar io.github.majianzheng.jarboot.tools.shell.CheckStatus"
set "DAEMON_VM=-Xms10m -Xmx10m -XX:+UseG1GC -XX:MaxGCPauseMillis=500 -DJARBOOT_HOME=%JARBOOT_HOME%"
set "DAEMON_CMD="%JAVA%" %DAEMON_VM% -cp %TOOL_JAR% jarboot.status %*"

%DAEMON_CMD%
echo Done.

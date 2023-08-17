@echo off

if not exist "%JAVA_HOME%\bin\java.exe" echo Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better! & EXIT /B 1

setlocal enabledelayedexpansion

set JARBOOT_HOME=%~dp0
set JARBOOT_HOME=%JARBOOT_HOME:~0,-13%
set "JAVA=%JAVA_HOME%\bin\java.exe"

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto endInit

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto endInit

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto endInit
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:endInit

set CLASS_PATH=%JARBOOT_HOME%\components\jarboot-tools.jar
set "VM_OPT=-DJARBOOT_HOME=%JARBOOT_HOME% -Xms5m -Xmx15m -XX:+UseG1GC -XX:MaxGCPauseMillis=500"

set COMMAND="%JAVA%" %VM_OPT% -jar "%CLASS_PATH%" %CMD_LINE_ARGS%

rem start jarboot command
%COMMAND%

@echo off
@REM Copyright (c) 2015 IBM Corp.
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM
@REM ------------------------------------------------------------------------


setlocal enabledelayedexpansion
set errorlevel=

set CURRENT_DIR="%~dp0"
set CURRENT_DIR=!CURRENT_DIR:"=!
set INVOKED=%0

@REM De-quote input environment variables.
if defined JRE_HOME set JRE_HOME=!JRE_HOME:"=!
if defined JAVA_HOME set JAVA_HOME=!JAVA_HOME:"=!

@REM find the java command
if NOT defined JAVA_HOME (
  if NOT defined JRE_HOME (
    set JAVA_CMD_QUOTED="java"
  ) else (
    set JAVA_CMD_QUOTED="%JRE_HOME%\bin\java"
  )
) else (
  if exist "%JAVA_HOME%\jre\bin\java.exe" set JAVA_HOME=!JAVA_HOME!\jre
  set JAVA_CMD_QUOTED="!JAVA_HOME!\bin\java"
)

set JVM_ARGS=-Djava.awt.headless=true !JVM_ARGS!
set TOOL_JAVA_CMD_QUOTED=!JAVA_CMD_QUOTED! !JVM_ARGS! -jar "!CURRENT_DIR!\tools\larsClient.jar"

@REM Execute the tool JAR.
!TOOL_JAVA_CMD_QUOTED! %*
set RC=%errorlevel%
call:javaCmdResult
goto:exit

@REM
@REM Check the result of a Java command.
@REM
:javaCmdResult
  if %RC% == 0 goto:eof

  if !JAVA_CMD_QUOTED! == "java" (
    @REM The command does not contain "\", so errorlevel 9009 will be reported
    @REM if the command does not exist.
    if %RC% neq 9009 goto:eof
  ) else (
    @REM The command contains "\", so errorlevel 3 will be reported.  We can't
    @REM distinguish that from our own exit codes, so check for the existence
    @REM of java.exe.
    if exist !JAVA_CMD_QUOTED!.exe goto:eof
  )

  @REM Windows prints a generic "The system cannot find the path specified.",
  @REM so echo the java command.
  echo !JAVA_CMD_QUOTED!
goto:eof

:exit
%COMSPEC% /c exit %RC%
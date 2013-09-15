REM go to directory where we run it:
cd %~dp0

REM build it:
call build.bat config.test compile 

REM if build failed then exit:
if %errorlevel% neq 0 exit /b %errorlevel%

REM run it:
set CLASSPATH=build
echo %CLASSPATH%
java %1 %2 %3






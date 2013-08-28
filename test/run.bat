cd %~dp0
call build.bat compile
set CLASSPATH=build
echo %CLASSPATH%
java %1 %2 %3






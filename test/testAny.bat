REM go to directory where we run it:
cd %~dp0

REM build it:
call build.bat config.test compile

REM if build failed then exit:
if %errorlevel% neq 0 exit /b %errorlevel%

REM run it:
set CLASSPATH=build;lib/jar/jsch-0.1.51.jar
java -Xmx98m -Xms16m -Xshare:off %1 %2 %3 %4 %5 %6 %7 %8 %9
REM c:\Programs\JDK18060\bin\java  -Xshare:off %1 %2 %3 %4 %5 %6 %7 %8 %9
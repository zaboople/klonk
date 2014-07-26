cd %~dp0
cd ..
call env.bat
call %ANT_HOME%\bin\ant.bat %1 %2 %3 %4 %5

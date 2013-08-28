rem dir /S C:\windows
cd %~dp0
cd ..
set JAVA_HOME=C:\Programs\JDK17021
set ANT_HOME=C:\programs\Ant170\apache-ant-1.7.0
call C:\programs\Ant170\apache-ant-1.7.0\bin\ant.bat %1 %2 %3 %4 %5

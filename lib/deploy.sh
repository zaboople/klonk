ant clean config.prod exe
cp dist/bin/* /c/troy/apps
ln -f -s /c/troy/apps/klonk.exe /c/troy/toolbar/apps/klonk.lnk
ant clean 

#Xshare:off is I think very important. Oh yeah let's share memory. No, thanks but really, fuck you.
cd /c/troy/dev/textedit
ant config.test compile && java \
    -Xshare:off \
    -Xms6m \
    -classpath build org.tmotte.klonk.Klonk \
    -home test/home "$@"

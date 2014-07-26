#Xshare:off is I think very important. Oh yeah let's share memory. No, thanks but really, fuck you.
pwd
ant config.test compile && java \
    -Xshare:off \
    -Xms6m \
    -classpath build org.tmotte.klonk.config.Boot \
    -home test/home "$@"

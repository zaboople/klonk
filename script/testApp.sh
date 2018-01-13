#Xshare:off is I think very important. Oh yeah let's share memory. No, thanks.
cd $(dirname $0)/..
source lib/classpath.sh
ant config.test compile || exit 1
java \
    -Xshare:off \
    -Xms6m \
    -classpath 'build;lib\vngx-jsch-0.10.jar' org.tmotte.klonk.config.BootContext \
    -home test/home "$@"




#!/bin/bash -e
cd $(dirname $0)/..
ant config.test compile
source lib/classpath.sh
java -classpath build org.tmotte.klonk.io.LockTest test\dink 1 &
java -classpath build org.tmotte.klonk.io.LockTest test\dink 2 &
java -classpath build org.tmotte.klonk.io.LockTest test\dink 3 &
java -classpath build org.tmotte.klonk.io.LockTest test\dink 4 &
java -classpath build org.tmotte.klonk.io.LockTest test\dink 5 &
java -classpath build org.tmotte.klonk.io.LockTest test\dink 6 &
java -classpath build org.tmotte.klonk.io.LockTest test\dink 7 &
java -classpath build org.tmotte.klonk.io.LockTest test\dink 8 &


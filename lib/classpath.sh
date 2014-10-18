export CLASSPATH="build"$(find lib -name '*.jar' | gawk '{printf ";"$1}')
echo $CLASSPATH
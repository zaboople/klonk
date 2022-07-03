#!/bin/bash -e
repo="klonk"
ant_command='clean config.prod exe dist'

cd $(dirname $0)/..
ant $ant_command

cd ../zaboople.github.generate/lib/external-$repo
echo
echo "Current directory: "$(pwd)
echo -n "WARNING: I am about to do an rm -rf. Is that okay? "
read answer
if [[ $answer == y* ]]; then
  rm -rf *
fi

echo
cd ../../../$repo
echo "Current directory: "$(pwd)
echo -n "I am about to cp -r from here to zaboople.github.generate... Is that okay? "
read answer
if [[ $answer == y* ]]; then
  cp -r dist/site/* ../zaboople.github.generate/lib/external-$repo
  ant clean
fi

echo
cd ../zaboople.github.generate
echo "Current directory: "$(pwd)
git status
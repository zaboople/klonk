#!/bin/bash
cd $(dirname $0)/..
#java -version
ant "$@" || exit 1


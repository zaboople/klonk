#!/bin/bash
cd $(dirname $0)/..
java -version
ant compile || exit 1


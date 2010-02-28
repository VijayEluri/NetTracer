#!/bin/bash

TARGET="$1"
shift

if [[ -z $1 ]]
then
	RENDERFILE="../scenes/example4.scn"
else
	RENDERFILE=../"$1"
fi

cd build
java -Xms1884m -Xmx1885m -XX:+UseConcMarkSweepGC -XX:+UseParNewGC \
	RaytracerApplication -h "$TARGET" "$RENDERFILE"
cd -

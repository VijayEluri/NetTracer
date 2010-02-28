#!/bin/bash

if [[ "$1" == "-n" ]]
then
	NICE="NO"
	shift
else
	NICE="YES"
fi

TARGET="$1"
shift

if [[ -z $1 ]]
then
	RENDERFILE="../scenes/example4.scn"
else
	RENDERFILE=../"$1"
fi

cd build
if [[ $NICE == "YES" ]]
then
	nice -n 19 java -Xms1884m -Xmx1885m \
		-XX:+UseConcMarkSweepGC -XX:+UseParNewGC \
		RaytracerApplication -h "$TARGET" "$RENDERFILE"
else
	java -Xms1884m -Xmx1885m \
		-XX:+UseConcMarkSweepGC -XX:+UseParNewGC \
		RaytracerApplication -h "$TARGET" "$RENDERFILE"
fi
cd -

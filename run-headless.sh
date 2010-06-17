#!/bin/bash

cd -- "$(dirname "$(readlink -e "$0")")" || exit 1

if [[ "$1" == "-n" ]]
then
	NICE="NO"
	shift
else
	NICE="YES"
fi

TARGET="$1"
shift

if [[ $NICE == "YES" ]]
then
	nice -n 19 java -Xms1884m -Xmx1885m \
		-XX:+UseConcMarkSweepGC -XX:+UseParNewGC \
		-cp antDist/Raytracer.jar \
		raytracer.core.RaytracerApplication -h "$TARGET" "$@"
else
	java -Xms1884m -Xmx1885m \
		-XX:+UseConcMarkSweepGC -XX:+UseParNewGC \
		-cp antDist/Raytracer.jar \
		raytracer.core.RaytracerApplication -h "$TARGET" "$@"
fi

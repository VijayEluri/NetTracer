#!/bin/bash

cd -- "$(dirname "$(readlink -e "$0")")" || exit 1

java -Xmx500m -cp antDist/Raytracer.jar \
	-XX:+UseConcMarkSweepGC -XX:+UseParNewGC \
	raytracer.net.NetNode "$@"

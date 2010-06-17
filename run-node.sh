#!/bin/bash

cd -- "$(dirname "$(readlink -e "$0")")" || exit 1
. run.conf || exit 1

java -Xmx${NODE_MEMSIZE} -cp antDist/Raytracer.jar \
	-XX:+UseConcMarkSweepGC -XX:+UseParNewGC \
	raytracer.net.NetNode "$@"

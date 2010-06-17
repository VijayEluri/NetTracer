#!/bin/bash

cd -- "$(dirname "$(readlink -e "$0")")" || exit 1
. run.conf || exit 1

java -Xmx${MAIN_MEMSIZE} -cp antDist/Raytracer.jar \
	raytracer.core.RaytracerApplication "$@"

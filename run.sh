#!/bin/bash

cd -- "$(dirname "$(readlink -e "$0")")" || exit 1

java -Xms1884m -Xmx1885m -cp antDist/Raytracer.jar \
	RaytracerApplication "$@"

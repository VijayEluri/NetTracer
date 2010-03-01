#!/bin/bash

java -Xms1884m -Xmx1885m -cp antDist/Raytracer.jar \
	RaytracerApplication "$@"

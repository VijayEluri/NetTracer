#!/bin/bash

java -Xmx500m -cp antDist/Raytracer.jar \
	-XX:+UseConcMarkSweepGC -XX:+UseParNewGC \
	NetNode "$@"

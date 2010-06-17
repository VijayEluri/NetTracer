#!/bin/bash

cd -- "$(dirname "$(readlink -e "$0")")" || exit 1

if (( $# != 2 ))
then
	echo "Usage: $0 <target image> <scene file>"
	exit 1
fi

cat nodes | java -Xmx1500m -cp antDist/Raytracer.jar \
	raytracer.net.NetMaster -s "$2" -t "$1"

#!/bin/bash

cd -- "$(dirname "$(readlink -e "$0")")" || exit 1
. run.conf || exit 1

if (( $# != 2 ))
then
	echo "Usage: $0 <target image> <scene file> < <list of nodes>"
	exit 1
fi

# This will read the list of nodes from stdin.
java -Xmx${MASTER_MEMSIZE} -cp antDist/Raytracer.jar \
	raytracer.net.NetMaster -s "$2" -t "$1"

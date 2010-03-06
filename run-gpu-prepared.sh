#!/bin/bash

if [[ ! -f "$1" ]]
then
	echo "Markiere die Ausgabe des GPU-Tracers, damit sie in der"
	echo "Zwischenablage ist. Gib als ersten Parameter hier die"
	echo "Skelettszene an."
	exit 1
fi

cat "$1" <(xclip -o) > /tmp/gpu.scn
./run.sh /tmp/gpu.scn

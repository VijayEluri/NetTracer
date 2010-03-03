#!/bin/bash

# Das ist nur ein Beispiel...
cat nodes | java -Xmx1500m -cp antDist/Raytracer.jar \
	NetMaster -s scn/example4.scn -t /tmp/image.tiff

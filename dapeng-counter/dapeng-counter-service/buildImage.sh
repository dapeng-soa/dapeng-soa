#!/usr/bin/env bash

mv target/dapeng-counter-service docker/
docker build -t dapengsoa/counter-service:2.0.0-SNAPSHOT docker/
rm -rf docker/dapeng-counter-service
#!/usr/bin/env bash

mvn clean package
mv target/dapeng-counter-service docker/
docker build -t dapengsoa/counter-service:2.2.0-SNAPSHOT docker/
rm -rf docker/dapeng-counter-service

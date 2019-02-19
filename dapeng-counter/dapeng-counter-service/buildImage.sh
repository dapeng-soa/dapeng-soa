#!/usr/bin/env bash

mvn clean package
mv target/dapeng-counter-service docker/
docker build -t docker.oa.isuwang.com:5000/system/counter-service:2.1.1 docker/
rm -rf docker/dapeng-counter-service


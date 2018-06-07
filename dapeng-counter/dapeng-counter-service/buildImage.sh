#!/usr/bin/env bash

mvn clean package
mv target/dapeng-counter-service docker/
docker build -t docker.today36524.com.cn:5000/biz/counter-service:graceful docker/
rm -rf docker/dapeng-counter-service